package com.mazhar.socialconnect.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mazhar.socialconnect.data.model.Post
import com.mazhar.socialconnect.data.model.User
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

    fun fetchCurrentUserData() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            _currentUserData.value = snapshot.toObject(User::class.java)
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

        try {
            firestore.collection("posts").document(post.id).update(
                "likedBy", newLikedBy,
                "likesCount", newLikesCount
            ).await()
            // Refresh posts
            fetchPosts()
        } catch (e: Exception) {
            // Handle error
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
}
