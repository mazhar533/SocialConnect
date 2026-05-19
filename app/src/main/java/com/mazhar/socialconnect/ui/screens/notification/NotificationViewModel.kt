package com.mazhar.socialconnect.ui.screens.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mazhar.socialconnect.data.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        fetchNotifications()
    }

    private fun fetchNotifications() {
        val userId = auth.currentUser?.uid ?: return
        _loading.value = true

        db.collection("notifications")
            .whereEqualTo("targetUserId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                _loading.value = false
                if (error != null) return@addSnapshotListener
                
                val list = snapshot?.documents?.mapNotNull { it.toObject(Notification::class.java)?.copy(id = it.id) }
                _notifications.value = list ?: emptyList()
            }
    }

    fun markAsRead(notificationId: String) {
        db.collection("notifications").document(notificationId)
            .update("isRead", true)
    }

    fun clearAll() = viewModelScope.launch {
        val userId = auth.currentUser?.uid ?: return@launch
        // Immediate UI update
        val previousList = _notifications.value
        _notifications.value = emptyList()
        
        try {
            val snapshot = db.collection("notifications")
                .whereEqualTo("targetUserId", userId)
                .get().await()
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        } catch (e: Exception) {
            // Rollback if failed
            _notifications.value = previousList
        }
    }
}
