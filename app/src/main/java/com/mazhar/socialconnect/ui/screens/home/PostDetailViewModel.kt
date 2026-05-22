package com.mazhar.socialconnect.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mazhar.socialconnect.data.model.Comment
import com.mazhar.socialconnect.data.model.Notification
import com.mazhar.socialconnect.data.model.Post
import com.mazhar.socialconnect.data.model.User
import com.mazhar.socialconnect.data.FcmNotificationSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PostDetailViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadPost(postId: String) = viewModelScope.launch {
        _loading.value = true
        try {
            val snapshot = firestore.collection("posts").document(postId).get().await()
            val postObj = snapshot.toObject(Post::class.java)
            
            if (postObj != null) {
                val currentUserId = auth.currentUser?.uid
                val authorId = postObj.userId
                var isAllowed = true
                
                if (currentUserId != null && authorId != currentUserId) {
                    val authorDoc = firestore.collection("users").document(authorId).get().await()
                    val author = authorDoc.toObject(User::class.java)
                    if (author != null && author.isPrivate) {
                        val currentUserDoc = firestore.collection("users").document(currentUserId).get().await()
                        val currentUser = currentUserDoc.toObject(User::class.java)
                        val isFollowing = currentUser?.following?.contains(authorId) == true
                        if (!isFollowing) {
                            isAllowed = false
                        }
                    }
                }
                
                if (isAllowed) {
                    _post.value = postObj
                    fetchComments(postId)
                } else {
                    _post.value = null
                }
            } else {
                _post.value = null
            }
        } catch (e: Exception) {
            // Handle error
            _post.value = null
        } finally {
            _loading.value = false
        }
    }

    private fun fetchComments(postId: String) {
        firestore.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    _comments.value = snapshot.toObjects(Comment::class.java)
                }
            }
    }

    fun toggleLike(postId: String, uid: String) = viewModelScope.launch {
        val currentPost = _post.value ?: return@launch
        val isLiked = currentPost.likedBy.contains(uid)
        val newLikedBy = if (isLiked) currentPost.likedBy - uid else currentPost.likedBy + uid
        val newLikesCount = newLikedBy.size

        // Optimistic UI Update
        _post.value = currentPost.copy(likedBy = newLikedBy, likesCount = newLikesCount)

        try {
            val postRef = firestore.collection("posts").document(postId)
            postRef.update(
                "likedBy", newLikedBy,
                "likesCount", newLikesCount
            ).await()
            
            if (!isLiked && currentPost.userId != uid) {
                sendNotification(currentPost.userId, "like", postId, currentPost.imageUrl, currentPost.content)
            }
        } catch (e: Exception) {
            // Revert state on failure
            _post.value = currentPost
        }
    }

    fun addComment(postId: String, text: String) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        if (text.isBlank()) return@launch

        try {
            val userSnapshot = firestore.collection("users").document(currentUserId).get().await()
            val currentUser = userSnapshot.toObject(User::class.java) ?: return@launch

            val commentRef = firestore.collection("posts").document(postId).collection("comments").document()
            val comment = Comment(
                id = commentRef.id,
                userId = currentUserId,
                userName = currentUser.name,
                userProfilePicture = currentUser.profilePictureUrl,
                text = text,
                timestamp = System.currentTimeMillis()
            )

            commentRef.set(comment).await()

            // Update comments count in post
            val postSnap = firestore.collection("posts").document(postId).get().await()
            val currentCount = postSnap.getLong("commentsCount") ?: 0
            firestore.collection("posts").document(postId).update("commentsCount", currentCount + 1).await()

            // Send Notification
            val targetUserId = _post.value?.userId
            if (targetUserId != null && targetUserId != currentUserId) {
                sendNotification(targetUserId, "comment", postId, _post.value?.imageUrl, _post.value?.content, commentId = commentRef.id)
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
        val currentUserId = auth.currentUser?.uid ?: return@launch
        
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
                commentId = commentId,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )

            firestore.collection("notifications").document(notification.id).set(notification).await()


        } catch (e: Exception) {
            // Log error
        }
    }
}
