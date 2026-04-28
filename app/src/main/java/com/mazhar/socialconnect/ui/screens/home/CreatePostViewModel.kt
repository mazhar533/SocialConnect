package com.mazhar.socialconnect.ui.screens.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.mazhar.socialconnect.data.model.Post
import com.mazhar.socialconnect.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CreatePostViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _currentUserData = MutableStateFlow<User?>(null)
    val currentUserData: StateFlow<User?> = _currentUserData.asStateFlow()

    private val _postSuccess = MutableStateFlow(false)
    val postSuccess: StateFlow<Boolean> = _postSuccess.asStateFlow()

    init {
        fetchCurrentUserData()
    }

    private fun fetchCurrentUserData() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            _currentUserData.value = snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            // Handle error
        }
    }

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _postToEdit = MutableStateFlow<Post?>(null)
    val postToEdit: StateFlow<Post?> = _postToEdit.asStateFlow()

    fun fetchPostToEdit(postId: String) = viewModelScope.launch {
        _loading.value = true
        try {
            val snapshot = firestore.collection("posts").document(postId).get().await()
            _postToEdit.value = snapshot.toObject(Post::class.java)
        } catch (e: Exception) {
            _error.value = "Failed to load post for editing"
        } finally {
            _loading.value = false
        }
    }

    fun createPost(content: String, imageUri: Uri?, editingPostId: String? = null) = viewModelScope.launch {
        if (content.isBlank() && imageUri == null) {
            _error.value = "Post cannot be empty"
            return@launch
        }

        _loading.value = true
        try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
            
            // Get user info for the post
            val userSnapshot = firestore.collection("users").document(uid).get().await()
            val user = userSnapshot.toObject(User::class.java)

            var imageUrl: String? = null
            if (imageUri != null && imageUri.scheme != "https") {
                val ref = storage.reference.child("post_images/${UUID.randomUUID()}")
                ref.putFile(imageUri).await()
                imageUrl = ref.downloadUrl.await().toString()
            } else if (imageUri != null) {
                imageUrl = imageUri.toString()
            } else if (editingPostId != null) {
                imageUrl = _postToEdit.value?.imageUrl
            }

            val postId = editingPostId ?: UUID.randomUUID().toString()
            val post = Post(
                id = postId,
                userId = uid,
                userName = user?.name ?: "Unknown",
                userProfilePicture = user?.profilePictureUrl ?: "",
                content = content,
                imageUrl = imageUrl,
                timestamp = if (editingPostId != null) _postToEdit.value?.timestamp ?: System.currentTimeMillis() else System.currentTimeMillis(),
                likedBy = if (editingPostId != null) _postToEdit.value?.likedBy ?: emptyList() else emptyList(),
                likesCount = if (editingPostId != null) _postToEdit.value?.likesCount ?: 0 else 0,
                commentsCount = if (editingPostId != null) _postToEdit.value?.commentsCount ?: 0 else 0
            )

            firestore.collection("posts").document(postId).set(post).await()
            
            if (editingPostId == null) {
                // Update user post count only for new posts
                firestore.collection("users").document(uid).update("postsCount", (user?.postsCount ?: 0) + 1).await()
            }

            _postSuccess.value = true
        } catch (e: Exception) {
            _error.value = e.localizedMessage ?: "Failed to save post"
        } finally {
            _loading.value = false
        }
    }

    fun resetState() {
        _postSuccess.value = false
        _error.value = null
    }
}
