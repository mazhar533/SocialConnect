package com.mazhar.socialconnect.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mazhar.socialconnect.data.model.Post
import com.mazhar.socialconnect.data.model.User
import com.mazhar.socialconnect.data.FcmNotificationSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _currentUserData = MutableStateFlow<User?>(null)
    val currentUserData: StateFlow<User?> = _currentUserData.asStateFlow()

    private val _followingUsers = MutableStateFlow<List<User>>(emptyList())
    val followingUsers: StateFlow<List<User>> = _followingUsers.asStateFlow()

    init {
        fetchPosts()
        fetchCurrentUserData()
    }

    fun fetchPosts() = viewModelScope.launch {
        _loading.value = true
        try {
            val snapshot = firestore.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()
            _posts.value = snapshot.toObjects(Post::class.java)
        } catch (e: Exception) {
            // Handle error
        } finally {
            _loading.value = false
        }
    }

    fun fetchCurrentUserData() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                _currentUserData.value = snapshot.toObject(User::class.java)
                fetchFollowingUsers() // Update following users list
            }
        }
    }

    fun fetchFollowingUsers() = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        val followingUids = _currentUserData.value?.following ?: emptyList()
        
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
            
            // For now, get up to 10 users (Firestore 'whereIn' limit)
            val snapshot = firestore.collection("users")
                .whereIn("uid", combinedUids.take(10))
                .get().await()
            _followingUsers.value = snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            // Handle error
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
        val currentPosts = _posts.value
        _posts.value = currentPosts.map { p ->
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
                sendNotification(post.userId, "like", post.id, post.imageUrl, post.content)
            }
        } catch (e: Exception) {
            // Revert state on failure
            _posts.value = currentPosts
        }
    }

    fun deletePost(postId: String) = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        try {
            // Get post to find if it belongs to current user (it should if delete button was shown)
            val postSnapshot = firestore.collection("posts").document(postId).get().await()
            val post = postSnapshot.toObject(Post::class.java)

            if (post?.userId == uid) {
                firestore.collection("posts").document(postId).delete().await()
                
                // Update user post count
                val userSnapshot = firestore.collection("users").document(uid).get().await()
                val currentCount = userSnapshot.getLong("postsCount") ?: 0
                if (currentCount > 0) {
                    firestore.collection("users").document(uid).update("postsCount", currentCount - 1).await()
                }
                
                fetchPosts()
                fetchCurrentUserData() // Refresh user data to update UI if needed
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun toggleFollow(targetUserId: String) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        if (currentUserId == targetUserId) return@launch

        // Optimistic UI Update
        val currentUserVal = _currentUserData.value
        if (currentUserVal != null) {
            val isCurrentlyFollowing = currentUserVal.following.contains(targetUserId)
            val newFollowing = if (isCurrentlyFollowing) {
                currentUserVal.following - targetUserId
            } else {
                currentUserVal.following + targetUserId
            }
            _currentUserData.value = currentUserVal.copy(
                following = newFollowing,
                followingCount = newFollowing.size
            )
        }

        try {
            val currentUserRef = firestore.collection("users").document(currentUserId)
            val targetUserRef = firestore.collection("users").document(targetUserId)

            firestore.runTransaction { transaction ->
                val currentUserSnapshot = transaction.get(currentUserRef)
                val targetUserSnapshot = transaction.get(targetUserRef)

                val currentUser = currentUserSnapshot.toObject(User::class.java) ?: return@runTransaction
                val targetUser = targetUserSnapshot.toObject(User::class.java) ?: return@runTransaction

                val isFollowing = currentUser.following.contains(targetUserId)

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
                
                if (!isFollowing) {
                    // We can't call sendNotification inside transaction easily 
                    // but we can trigger it after success
                }
            }.await()

            val isCurrentlyFollowing = _currentUserData.value?.following?.contains(targetUserId) == true
            if (isCurrentlyFollowing) {
                sendNotification(targetUserId, "follow")
            }

            // Refresh user data to get updated following list
            fetchCurrentUserData()
        } catch (e: Exception) {
            // Handle error
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
        postContent: String? = null,
        commentId: String? = null
    ) = viewModelScope.launch {
        android.util.Log.e("DEBUG_NOTIF", "TRIGGERED: type=$type, content=$postContent")
        android.util.Log.d("HomeViewModel", "sendNotification: type=$type, postId=$postId, postContent=$postContent")
        val currentUserId = auth.currentUser?.uid ?: return@launch
        if (currentUserId == targetUserId) return@launch // Don't notify yourself

        try {
            // Get current user info for notification
            val currentUserSnapshot = firestore.collection("users").document(currentUserId).get().await()
            val fromName = currentUserSnapshot.getString("name") ?: "User"
            val fromUserImage = currentUserSnapshot.getString("profilePictureUrl")
            
            android.util.Log.d("HomeViewModel", "Manual Fetch - Name: $fromName, Image: $fromUserImage")

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

            // Fetch target user's FCM token and send push notification
            val targetUserSnapshot = firestore.collection("users").document(targetUserId).get().await()
            val targetToken = targetUserSnapshot.getString("fcmToken")
            if (!targetToken.isNullOrEmpty()) {
                val title = "SocialConnect"
                val body = when (type) {
                    "like" -> "$fromName liked your post"
                    "comment" -> "$fromName commented on your post"
                    "follow" -> "$fromName started following you"
                    "share" -> "$fromName shared a post with you"
                    else -> "$fromName notified you"
                }
                FcmNotificationSender.sendNotification(
                    targetToken, 
                    title, 
                    body,
                    imageUrl = postImage,
                    data = mapOf(
                        "userImage" to (fromUserImage ?: ""), 
                        "postId" to (postId ?: ""),
                        "postContent" to (postContent ?: ""),
                        "commentId" to (commentId ?: "")
                    )
                )
            }
        } catch (e: Exception) {
            // Log error
        }
    }
}
