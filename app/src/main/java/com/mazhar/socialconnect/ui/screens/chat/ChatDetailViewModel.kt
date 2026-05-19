package com.mazhar.socialconnect.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mazhar.socialconnect.data.model.Message
import com.mazhar.socialconnect.data.FcmNotificationSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatDetailViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _targetUserName = MutableStateFlow("Chat")
    val targetUserName: StateFlow<String> = _targetUserName.asStateFlow()

    private val _targetUserProfileImage = MutableStateFlow<String?>(null)
    val targetUserProfileImage: StateFlow<String?> = _targetUserProfileImage.asStateFlow()

    fun setInitialTargetInfo(name: String?, image: String?) {
        if (!name.isNullOrEmpty()) _targetUserName.value = name
        if (!image.isNullOrEmpty()) _targetUserProfileImage.value = image
    }

    fun loadChat(roomId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        // Fetch room info for header
        viewModelScope.launch {
            try {
                val roomSnap = firestore.collection("chatRooms").document(roomId).get().await()
                val participants = roomSnap.get("participants") as? List<String> ?: emptyList()
                val names = roomSnap.get("participantNames") as? Map<String, String> ?: emptyMap()
                val targetId = participants.firstOrNull { it != currentUserId }
                if (targetId != null) {
                    _targetUserName.value = names[targetId] ?: "Chat"
                    
                    // Fetch profile image from users collection
                    val userSnap = firestore.collection("users").document(targetId).get().await()
                    _targetUserProfileImage.value = userSnap.getString("profilePictureUrl")
                }
            } catch (_: Exception) {
                // handle error
            }
        }

        // Listen for messages
        firestore.collection("chatRooms").document(roomId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    _messages.value = snapshot.toObjects(Message::class.java)
                }
            }
    }

    fun sendMessage(roomId: String, text: String) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        if (text.isBlank()) return@launch

        val messageRef = firestore.collection("chatRooms").document(roomId).collection("messages").document()
        val message = Message(
            id = messageRef.id,
            senderId = currentUserId,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        try {
            messageRef.set(message).await()
            // Update last message in room
            firestore.collection("chatRooms").document(roomId).update(
                "lastMessage", text,
                "lastMessageTimestamp", System.currentTimeMillis()
            ).await()

            // Send Notification
            val roomSnap = firestore.collection("chatRooms").document(roomId).get().await()
            val participants = roomSnap.get("participants") as? List<String> ?: emptyList()
            val targetId = participants.firstOrNull { it != currentUserId }
            if (targetId != null) {
                sendNotification(targetId, "message", text)
            }
        } catch (_: Exception) {
            // handle error
        }
    }

    private fun sendNotification(
        targetUserId: String,
        type: String,
        messageText: String
    ) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        if (currentUserId == targetUserId) return@launch

        try {
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
                timestamp = System.currentTimeMillis(),
                isRead = false
            )

            firestore.collection("notifications").document(notification.id).set(notification).await()

            // Fetch target user's FCM token and send push notification
            val targetUserSnapshot = firestore.collection("users").document(targetUserId).get().await()
            val targetToken = targetUserSnapshot.getString("fcmToken")
            if (!targetToken.isNullOrEmpty()) {
                val title = fromName
                val body = messageText
                FcmNotificationSender.sendNotification(
                    targetToken, 
                    title, 
                    body,
                    data = mapOf(
                        "userImage" to (fromUserImage ?: ""),
                        "type" to "message"
                    )
                )
            }
        } catch (_: Exception) {
            // Log error
        }
    }
}
