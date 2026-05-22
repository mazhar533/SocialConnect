package com.mazhar.socialconnect.ui.screens.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mazhar.socialconnect.data.model.Notification
import com.mazhar.socialconnect.data.model.ChatRoom
import com.mazhar.socialconnect.data.model.User
import com.mazhar.socialconnect.data.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val notifications: StateFlow<List<Notification>> = NotificationRepository.notifications
    val loading: StateFlow<Boolean> = NotificationRepository.loading

    init {
        NotificationRepository.startListeningToNotifications()
    }

    private fun fetchNotifications() {
        NotificationRepository.startListeningToNotifications()
    }

    fun markAsRead(notificationId: String) {
        db.collection("notifications").document(notificationId)
            .update("isRead", true)
    }

    fun clearAll() = viewModelScope.launch {
        val userId = auth.currentUser?.uid ?: return@launch
        // Immediate UI update
        val previousList = NotificationRepository.notifications.value
        NotificationRepository.setNotifications(emptyList())
        
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
            NotificationRepository.setNotifications(previousList)
        }
    }

    fun getOrCreateChatRoom(targetUserId: String, onComplete: (String) -> Unit) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        try {
            val snapshot = db.collection("chatRooms")
                .whereArrayContains("participants", currentUserId)
                .get().await()

            val rooms = snapshot.toObjects(ChatRoom::class.java)
            val existingRoom = rooms.find { it.participants.contains(targetUserId) }

            if (existingRoom != null) {
                onComplete(existingRoom.id)
            } else {
                val currentUserSnapshot = db.collection("users").document(currentUserId).get().await()
                val targetUserSnapshot = db.collection("users").document(targetUserId).get().await()
                
                val currentUser = currentUserSnapshot.toObject(User::class.java) ?: return@launch
                val targetUser = targetUserSnapshot.toObject(User::class.java) ?: return@launch

                val roomId = db.collection("chatRooms").document().id
                val newRoom = ChatRoom(
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

                db.collection("chatRooms").document(roomId).set(newRoom).await()
                onComplete(roomId)
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationViewModel", "Error resolving chat room: ${e.message}")
        }
    }
}
