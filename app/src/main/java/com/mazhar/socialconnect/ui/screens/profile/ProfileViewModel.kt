package com.mazhar.socialconnect.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.mazhar.socialconnect.data.model.Post
import com.mazhar.socialconnect.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.mazhar.socialconnect.data.FcmNotificationSender

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val prefs = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private var profileListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    private val _isGridView = MutableStateFlow(prefs.getBoolean("is_grid_view", false))
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    fun setGridView(isGrid: Boolean) {
        _isGridView.value = isGrid
        prefs.edit().putBoolean("is_grid_view", isGrid).apply()
    }

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _postsLoading = MutableStateFlow(false)
    val postsLoading: StateFlow<Boolean> = _postsLoading.asStateFlow()

    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData.asStateFlow()

    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts.asStateFlow()

    private val _isOwnProfile = MutableStateFlow(true)
    val isOwnProfile: StateFlow<Boolean> = _isOwnProfile.asStateFlow()

    private val _followList = MutableStateFlow<List<User>>(emptyList())
    val followList: StateFlow<List<User>> = _followList.asStateFlow()

    private val _followingUsers = MutableStateFlow<List<User>>(emptyList())
    val followingUsers: StateFlow<List<User>> = _followingUsers.asStateFlow()

    private val _followRequestsList = MutableStateFlow<List<User>>(emptyList())
    val followRequestsList: StateFlow<List<User>> = _followRequestsList.asStateFlow()

    private var currentTargetUserId: String? = null

    fun loadProfile(userId: String?) {
        val currentUserId = auth.currentUser?.uid ?: return
        val targetId = userId ?: currentUserId
        
        currentTargetUserId = targetId
        _isOwnProfile.value = (targetId == currentUserId)
        
        profileListenerRegistration?.remove()
        profileListenerRegistration = null
        
        fetchUserProfile(targetId)
        fetchUserPosts(targetId)
    }

    fun clearMessage() { _message.value = null }

    private fun fetchUserProfile(uid: String) {
        val currentUserId = auth.currentUser?.uid
        if (uid == currentUserId) {
            val cachedUser = com.mazhar.socialconnect.data.UserRepository.currentUserData.value
            if (cachedUser != null) {
                _userData.value = cachedUser
                fetchFollowingUsers(cachedUser.following)
                fetchFollowRequestsList(cachedUser.followRequests)
            } else {
                _loading.value = true
            }
        } else {
            _loading.value = true
        }
        profileListenerRegistration?.remove()
        
        val docRef = firestore.collection("users").document(uid)
        profileListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            _loading.value = false
            if (error != null) {
                _message.value = "Failed to load profile: ${error.localizedMessage}"
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                _userData.value = user
                
                // If it's own profile, fetch following users for sharing
                if (uid == currentUserId) {
                    fetchFollowingUsers(user?.following ?: emptyList())
                    fetchFollowRequestsList(user?.followRequests ?: emptyList())
                }
            }
        }
    }

    private fun fetchUserPosts(uid: String) = viewModelScope.launch {
        _postsLoading.value = true
        try {
            val snapshot = firestore.collection("posts")
                .whereEqualTo("userId", uid)
                .get().await()
            val postsList = snapshot.toObjects(Post::class.java)
            _userPosts.value = postsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            _message.value = "Failed to load posts: ${e.localizedMessage}"
        } finally {
            _postsLoading.value = false
        }
    }

    fun likePost(post: Post) = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        val isLiked = post.likedBy.contains(uid)
        
        val newLikedBy = if (isLiked) {
            post.likedBy - uid
        } else {
            post.likedBy + uid
        }
        
        val newLikesCount = newLikedBy.size

        // Optimistic UI Update
        val currentPosts = _userPosts.value
        _userPosts.value = currentPosts.map { p ->
            if (p.id == post.id) {
                p.copy(likedBy = newLikedBy, likesCount = newLikesCount)
            } else {
                p
            }
        }

        try {
            firestore.collection("posts").document(post.id).update(
                "likedBy", newLikedBy,
                "likesCount", newLikesCount
            ).await()
            if (!isLiked) {
                sendNotification(post.userId, "like", post.id, post.imageUrl)
            }
        } catch (e: Exception) {
            // Revert state on failure
            _userPosts.value = currentPosts
            _message.value = "Failed to like post"
        }
    }

    fun deletePost(postId: String) = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        try {
            firestore.collection("posts").document(postId).delete().await()
            // Update user post count
            val currentCount = _userData.value?.postsCount ?: 0
            if (currentCount > 0) {
                firestore.collection("users").document(uid).update("postsCount", currentCount - 1).await()
                currentTargetUserId?.let { fetchUserProfile(it) }
            }
            currentTargetUserId?.let { fetchUserPosts(it) }
            _message.value = "Post deleted"
        } catch (e: Exception) {
            _message.value = "Failed to delete post"
        }
    }

    fun saveProfile(name: String, bio: String, imageUri: Uri?) = viewModelScope.launch {
        _loading.value = true
        try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
            
            var imageUrl = _userData.value?.profilePictureUrl ?: ""
            if (imageUri != null && imageUri.scheme != "https") {
                val ref = storage.reference.child("profile_pictures/${UUID.randomUUID()}")
                ref.putFile(imageUri).await()
                imageUrl = ref.downloadUrl.await().toString()
            } else if (imageUri != null) {
                imageUrl = imageUri.toString()
            }

            val updates = mutableMapOf<String, Any>(
                "name" to name,
                "bio" to bio
            )
            if (imageUrl.isNotEmpty()) {
                updates["profilePictureUrl"] = imageUrl
            }

            firestore.collection("users").document(uid).update(updates).await()
            _message.value = "Profile updated successfully"
            currentTargetUserId?.let { fetchUserProfile(it) } // Refresh data
        } catch (e: Exception) {
            _message.value = e.localizedMessage ?: "Failed to update profile"
        } finally {
            _loading.value = false
        }
    }

    fun toggleFollow(targetUserId: String) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        if (currentUserId == targetUserId) return@launch

        val targetUserVal = _userData.value
        val isPrivate = targetUserVal?.isPrivate == true

        // Optimistic UI Update
        if (targetUserVal != null && targetUserVal.uid == targetUserId) {
            val isCurrentlyFollowing = targetUserVal.followers.contains(currentUserId)
            if (isPrivate && !isCurrentlyFollowing) {
                val alreadyRequested = targetUserVal.followRequests.contains(currentUserId)
                val newRequests = if (alreadyRequested) {
                    targetUserVal.followRequests - currentUserId
                } else {
                    targetUserVal.followRequests + currentUserId
                }
                _userData.value = targetUserVal.copy(
                    followRequests = newRequests
                )
            } else {
                val newFollowers = if (isCurrentlyFollowing) {
                    targetUserVal.followers - currentUserId
                } else {
                    targetUserVal.followers + currentUserId
                }
                _userData.value = targetUserVal.copy(
                    followers = newFollowers,
                    followersCount = newFollowers.size
                )
            }
        }

        try {
            val currentUserRef = firestore.collection("users").document(currentUserId)
            val targetUserRef = firestore.collection("users").document(targetUserId)

            val notificationType = firestore.runTransaction { transaction ->
                val currentUserSnapshot = transaction.get(currentUserRef)
                val targetUserSnapshot = transaction.get(targetUserRef)

                val currentUser = currentUserSnapshot.toObject(User::class.java) ?: return@runTransaction null
                val targetUser = targetUserSnapshot.toObject(User::class.java) ?: return@runTransaction null

                val isFollowing = currentUser.following.contains(targetUserId)
                val notificationToSend: String?

                if (targetUser.isPrivate && !isFollowing) {
                    val alreadyRequested = targetUser.followRequests.contains(currentUserId)
                    notificationToSend = if (alreadyRequested) null else "follow_request"
                    val newRequests = if (alreadyRequested) {
                        targetUser.followRequests - currentUserId
                    } else {
                        targetUser.followRequests + currentUserId
                    }
                    transaction.update(targetUserRef, "followRequests", newRequests)
                } else {
                    notificationToSend = if (isFollowing) null else "follow"
                    val newFollowing = if (isFollowing) {
                        currentUser.following - targetUserId
                    } else {
                        currentUser.following + targetUserId
                    }

                    val newFollowers = if (isFollowing) {
                        targetUser.followers - currentUserId
                    } else {
                        targetUser.followers + currentUserId
                    }

                    transaction.update(currentUserRef, "following", newFollowing)
                    transaction.update(currentUserRef, "followingCount", newFollowing.size)

                    transaction.update(targetUserRef, "followers", newFollowers)
                    transaction.update(targetUserRef, "followersCount", newFollowers.size)
                }
                notificationToSend
            }.await()

            notificationType?.let { type ->
                sendNotification(targetUserId, type)
            }
        } catch (e: Exception) {
            _message.value = "Failed to toggle follow"
        }
    }

    fun startChat(onComplete: (String) -> Unit) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        val targetUserId = currentTargetUserId ?: return@launch
        if (currentUserId == targetUserId) return@launch

        try {
            // Check for existing room
            val snapshot = firestore.collection("chatRooms")
                .whereArrayContains("participants", currentUserId)
                .get().await()

            val rooms = snapshot.toObjects(com.mazhar.socialconnect.data.model.ChatRoom::class.java)
            val existingRoom = rooms.find { it.participants.contains(targetUserId) }

            if (existingRoom != null) {
                onComplete(existingRoom.id)
                return@launch
            }

            // Create new room
            val currentUserSnapshot = firestore.collection("users").document(currentUserId).get().await()
            val targetUserSnapshot = firestore.collection("users").document(targetUserId).get().await()
            
            val currentUser = currentUserSnapshot.toObject(User::class.java) ?: return@launch
            val targetUser = targetUserSnapshot.toObject(User::class.java) ?: return@launch

            val roomId = firestore.collection("chatRooms").document().id
            val newRoom = com.mazhar.socialconnect.data.model.ChatRoom(
                id = roomId,
                participants = listOf(currentUserId, targetUser.uid),
                participantNames = mapOf(
                    currentUserId to currentUser.name,
                    targetUser.uid to targetUser.name
                ),
                participantImages = mapOf(
                    currentUserId to currentUser.profilePictureUrl,
                    targetUser.uid to targetUser.profilePictureUrl
                ),
                lastMessage = "Say Hi!",
                lastMessageTimestamp = System.currentTimeMillis()
            )

            firestore.collection("chatRooms").document(roomId).set(newRoom).await()
            onComplete(roomId)
        } catch (e: Exception) {
            _message.value = "Failed to start chat: ${e.localizedMessage}"
        }
    }

    fun fetchFollowList(uids: List<String>) = viewModelScope.launch {
        if (uids.isEmpty()) {
            _followList.value = emptyList()
            return@launch
        }
        _loading.value = true
        try {
            val users = mutableListOf<User>()
            val chunkedUids = uids.chunked(30)
            for (chunk in chunkedUids) {
                val snapshot = firestore.collection("users")
                    .whereIn("uid", chunk)
                    .get().await()
                users.addAll(snapshot.toObjects(User::class.java))
            }
            _followList.value = users
        } finally {
            _loading.value = false
        }
    }

    private fun fetchFollowingUsers(followingUids: List<String>) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        
        try {
            // Also get users from chat rooms
            val chatSnapshot = firestore.collection("chatRooms")
                .whereArrayContains("participants", currentUserId)
                .get().await()
            
            val chatUids = chatSnapshot.toObjects(com.mazhar.socialconnect.data.model.ChatRoom::class.java)
                .flatMap { it.participants }
                .filter { it != currentUserId }
            
            val combinedUids = (followingUids + chatUids).distinct()

            if (combinedUids.isEmpty()) {
                _followingUsers.value = emptyList()
                return@launch
            }
            
            val snapshot = firestore.collection("users")
                .whereIn("uid", combinedUids.take(10))
                .get().await()
            _followingUsers.value = snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun fetchFollowRequestsList(uids: List<String>) = viewModelScope.launch {
        if (uids.isEmpty()) {
            _followRequestsList.value = emptyList()
            return@launch
        }
        try {
            val users = mutableListOf<User>()
            for (chunk in uids.chunked(10)) {
                val snapshot = firestore.collection("users")
                    .whereIn("uid", chunk)
                    .get().await()
                users.addAll(snapshot.toObjects(User::class.java))
            }
            _followRequestsList.value = users
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun acceptFollowRequest(requesterUid: String) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        
        val previousUserData = _userData.value
        val previousRequestsList = _followRequestsList.value
        
        // Optimistic UI Update
        val currentUserVal = _userData.value
        if (currentUserVal != null && currentUserVal.uid == currentUserId) {
            val containsRequester = currentUserVal.followers.contains(requesterUid)
            val newFollowers = if (containsRequester) currentUserVal.followers else currentUserVal.followers + requesterUid
            _userData.value = currentUserVal.copy(
                followRequests = currentUserVal.followRequests - requesterUid,
                followers = newFollowers,
                followersCount = newFollowers.size
            )
        }
        _followRequestsList.value = _followRequestsList.value.filter { it.uid != requesterUid }

        try {
            val currentUserRef = firestore.collection("users").document(currentUserId)
            val requesterRef = firestore.collection("users").document(requesterUid)

            firestore.runTransaction { transaction ->
                val currentUserSnapshot = transaction.get(currentUserRef)
                val requesterSnapshot = transaction.get(requesterRef)

                val currentUser = currentUserSnapshot.toObject(User::class.java) ?: return@runTransaction
                val requester = requesterSnapshot.toObject(User::class.java) ?: return@runTransaction

                // Remove from followRequests, add to followers
                val newRequests = currentUser.followRequests - requesterUid
                val newFollowers = if (currentUser.followers.contains(requesterUid)) currentUser.followers else currentUser.followers + requesterUid

                // Add currentUserId to requester's following
                val newFollowing = if (requester.following.contains(currentUserId)) requester.following else requester.following + currentUserId

                transaction.update(currentUserRef, "followRequests", newRequests)
                transaction.update(currentUserRef, "followers", newFollowers)
                transaction.update(currentUserRef, "followersCount", newFollowers.size)

                transaction.update(requesterRef, "following", newFollowing)
                transaction.update(requesterRef, "followingCount", newFollowing.size)
            }.await()

            sendNotification(requesterUid, "follow_accept")
        } catch (e: Exception) {
            // Revert state on failure
            _userData.value = previousUserData
            _followRequestsList.value = previousRequestsList
            _message.value = "Failed to accept follow request"
        }
    }

    fun rejectFollowRequest(requesterUid: String) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        
        val previousUserData = _userData.value
        val previousRequestsList = _followRequestsList.value
        
        // Optimistic UI Update
        val currentUserVal = _userData.value
        if (currentUserVal != null && currentUserVal.uid == currentUserId) {
            _userData.value = currentUserVal.copy(
                followRequests = currentUserVal.followRequests - requesterUid
            )
        }
        _followRequestsList.value = _followRequestsList.value.filter { it.uid != requesterUid }

        try {
            val currentUserRef = firestore.collection("users").document(currentUserId)

            firestore.runTransaction { transaction ->
                val currentUserSnapshot = transaction.get(currentUserRef)
                val currentUser = currentUserSnapshot.toObject(User::class.java) ?: return@runTransaction

                // Remove from followRequests
                val newRequests = currentUser.followRequests - requesterUid
                transaction.update(currentUserRef, "followRequests", newRequests)
            }.await()
        } catch (e: Exception) {
            // Revert state on failure
            _userData.value = previousUserData
            _followRequestsList.value = previousRequestsList
            _message.value = "Failed to reject follow request"
        }
    }

    fun sharePost(post: Post, targetUserId: String) = viewModelScope.launch {
        sendNotification(targetUserId, "share", post.id, post.imageUrl, post.content)
        sendShareMessage(post, targetUserId)
    }

    private fun sendShareMessage(post: Post, targetUserId: String) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        
        try {
            // Find existing room
            val snapshot = firestore.collection("chatRooms")
                .whereArrayContains("participants", currentUserId)
                .get().await()
            
            val rooms = snapshot.toObjects(com.mazhar.socialconnect.data.model.ChatRoom::class.java)
            val room = rooms.find { it.participants.contains(targetUserId) }
            
            if (room != null) {
                // Send message to the room
                val messageId = firestore.collection("chatRooms").document(room.id).collection("messages").document().id
                val message = com.mazhar.socialconnect.data.model.Message(
                    id = messageId,
                    senderId = currentUserId,
                    text = if (post.content.length > 100) post.content.take(100) + "..." else post.content,
                    imageUrl = post.imageUrl,
                    postId = post.id,
                    timestamp = System.currentTimeMillis()
                )
                
                firestore.collection("chatRooms").document(room.id).collection("messages").document(messageId).set(message).await()
                
                // Update room last message info
                firestore.collection("chatRooms").document(room.id).update(
                    "lastMessage", if (post.content.length > 30) post.content.take(30) + "..." else post.content,
                    "lastMessageTimestamp", System.currentTimeMillis()
                ).await()
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun sendNotification(
        targetUserId: String,
        type: String,
        postId: String? = null,
        postImage: String? = null,
        postContent: String? = null
    ) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        if (currentUserId == targetUserId) return@launch // Don't notify yourself

        try {
            // Get current user info for notification
            val currentUserSnapshot = firestore.collection("users").document(currentUserId).get().await()
            val fromName = currentUserSnapshot.getString("name") ?: "User"
            val fromUserImage = currentUserSnapshot.getString("profilePictureUrl")

            val notification = com.mazhar.socialconnect.data.model.Notification(
                id = firestore.collection("notifications").document().id,
                type = type,
                fromUserId = currentUserId,
                fromUserName = fromName,
                fromUserProfilePicture = fromUserImage,
                targetUserId = targetUserId,
                postId = postId,
                postImage = postImage,
                postContent = postContent,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )

            firestore.collection("notifications").document(notification.id).set(notification).await()


        } catch (e: Exception) {
            // Log error
        }
    }

    fun sendEmailVerification(onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            user.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onComplete(true, null)
                    } else {
                        onComplete(false, task.exception?.localizedMessage)
                    }
                }
        } else {
            onComplete(false, "User not logged in")
        }
    }

    fun reloadUser(onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            user.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseEmail = user.email
                    val currentFirestoreUser = _userData.value
                    if (firebaseEmail != null && currentFirestoreUser != null && firebaseEmail != currentFirestoreUser.email) {
                        firestore.collection("users").document(user.uid)
                            .update("email", firebaseEmail)
                            .addOnCompleteListener { firestoreTask ->
                                onComplete(firestoreTask.isSuccessful)
                            }
                    } else {
                        onComplete(true)
                    }
                } else {
                    onComplete(false)
                }
            }
        } else {
            onComplete(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileListenerRegistration?.remove()
    }
}

