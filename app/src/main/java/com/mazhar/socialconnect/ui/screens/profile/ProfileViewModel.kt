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

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData.asStateFlow()

    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts.asStateFlow()

    init {
        fetchUserProfile()
        fetchUserPosts()
    }

    fun clearMessage() { _message.value = null }

    fun fetchUserProfile() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        _loading.value = true
        try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            _userData.value = snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            _message.value = "Failed to load profile"
        } finally {
            if (_userPosts.value.isNotEmpty() || _userData.value != null) {
                // Only stop loading if we have some data or both failed
            }
            _loading.value = false
        }
    }

    fun fetchUserPosts() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        _loading.value = true
        try {
            val snapshot = firestore.collection("posts")
                .whereEqualTo("userId", uid)
                .get().await()
            val postsList = snapshot.toObjects(Post::class.java)
            _userPosts.value = postsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            _message.value = "Failed to load posts: ${e.localizedMessage}"
        } finally {
            _loading.value = false
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
            fetchUserPosts()
        } catch (e: Exception) {
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
                fetchUserProfile()
            }
            fetchUserPosts()
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
            fetchUserProfile() // Refresh data
        } catch (e: Exception) {
            _message.value = e.localizedMessage ?: "Failed to update profile"
        } finally {
            _loading.value = false
        }
    }
}
