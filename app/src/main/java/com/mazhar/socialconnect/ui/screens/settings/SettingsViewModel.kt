package com.mazhar.socialconnect.ui.screens.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mazhar.socialconnect.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsViewModel(context: Context) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _privateAccountEnabled = MutableStateFlow(prefs.getBoolean("privateAccount", false))
    val privateAccountEnabled: StateFlow<Boolean> = _privateAccountEnabled.asStateFlow()

    private val _darkModeEnabled = MutableStateFlow(prefs.getBoolean("darkMode", false))
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled.asStateFlow()

    init {
        loadSettingsFromFirestore()
    }

    private fun loadSettingsFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java) ?: return@addSnapshotListener
                    
                    _notificationsEnabled.value = user.notificationsEnabled
                    prefs.edit().putBoolean("notifications", user.notificationsEnabled).apply()

                    _privateAccountEnabled.value = user.isPrivate
                    prefs.edit().putBoolean("privateAccount", user.isPrivate).apply()
                }
            }
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications", enabled).apply()
        
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .update("notificationsEnabled", enabled).await()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun togglePrivateAccount(enabled: Boolean) {
        _privateAccountEnabled.value = enabled
        prefs.edit().putBoolean("privateAccount", enabled).apply()

        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .update("isPrivate", enabled).await()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _darkModeEnabled.value = enabled
        prefs.edit().putBoolean("darkMode", enabled).apply()
    }

    fun signOut(onSignOutComplete: () -> Unit) {
        auth.signOut()
        onSignOutComplete()
    }

    fun changeEmail(newEmail: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onError("User not logged in")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Send verification mail before updating email in Firebase Auth (handles security constraints)
                user.verifyBeforeUpdateEmail(newEmail).await()
                onComplete()
            } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                onError("Re-authentication required. Please sign out and sign back in, then try again.")
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to send verification to new email")
            }
        }
    }


    fun deleteAccount(onComplete: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onError("User not logged in")
            return
        }
        val uid = user.uid

        viewModelScope.launch {
            try {
                // 1. Gather all documents to delete from Firestore
                val refsToDelete = mutableListOf<com.google.firebase.firestore.DocumentReference>()

                // A. User document
                val userDocRef = firestore.collection("users").document(uid)
                val userSnapshot = userDocRef.get().await()
                refsToDelete.add(userDocRef)

                // B. Gather all posts and their comments, and collect post image URLs
                val postImageUrls = mutableListOf<String>()
                val postsSnapshot = firestore.collection("posts")
                    .whereEqualTo("userId", uid)
                    .get().await()

                for (postDoc in postsSnapshot.documents) {
                    refsToDelete.add(postDoc.reference)
                    val imageUrl = postDoc.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        postImageUrls.add(imageUrl)
                    }

                    // Get comments for this post
                    val commentsSnapshot = postDoc.reference.collection("comments").get().await()
                    for (commentDoc in commentsSnapshot.documents) {
                        refsToDelete.add(commentDoc.reference)
                    }
                }

                // C. Gather notifications sent or received by this user
                val receivedNotifications = firestore.collection("notifications")
                    .whereEqualTo("targetUserId", uid)
                    .get().await()
                for (doc in receivedNotifications.documents) {
                    refsToDelete.add(doc.reference)
                }

                val sentNotifications = firestore.collection("notifications")
                    .whereEqualTo("fromUserId", uid)
                    .get().await()
                for (doc in sentNotifications.documents) {
                    refsToDelete.add(doc.reference)
                }

                // 2. Delete images from Firebase Storage
                // Profile image
                val profilePicUrl = userSnapshot.getString("profilePictureUrl")
                if (!profilePicUrl.isNullOrEmpty() && profilePicUrl.startsWith("http")) {
                    try {
                        com.google.firebase.storage.FirebaseStorage.getInstance().getReferenceFromUrl(profilePicUrl).delete().await()
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsViewModel", "Failed to delete profile picture: ${e.message}")
                    }
                }

                // Post images
                for (url in postImageUrls) {
                    if (url.startsWith("http")) {
                        try {
                            com.google.firebase.storage.FirebaseStorage.getInstance().getReferenceFromUrl(url).delete().await()
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsViewModel", "Failed to delete post image: ${e.message}")
                        }
                    }
                }

                // 3. Batch delete Firestore documents (chunked to stay under 500 limit)
                refsToDelete.distinctBy { it.path }.chunked(400).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { ref ->
                        batch.delete(ref)
                    }
                    batch.commit().await()
                }

                // 4. Delete user from Firebase Auth
                try {
                    user.delete().await()
                } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                    onError("Re-authentication required. Please sign out and sign back in, then try again.")
                    return@launch
                }

                // 5. Clear repository caches
                com.mazhar.socialconnect.data.UserRepository.clear()
                com.mazhar.socialconnect.data.NotificationRepository.clear()

                onComplete()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to delete account")
            }
        }
    }
}

