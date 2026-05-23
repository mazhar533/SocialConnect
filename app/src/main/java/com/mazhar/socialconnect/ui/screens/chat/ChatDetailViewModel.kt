package com.mazhar.socialconnect.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.mazhar.socialconnect.data.model.Message
import com.mazhar.socialconnect.data.model.User
import com.mazhar.socialconnect.data.FcmNotificationSender
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.mazhar.socialconnect.data.UserRepository

class ChatDetailViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _targetUserName = MutableStateFlow("Chat")
    val targetUserName: StateFlow<String> = _targetUserName.asStateFlow()

    private val _targetUserProfileImage = MutableStateFlow<String?>(null)
    val targetUserProfileImage: StateFlow<String?> = _targetUserProfileImage.asStateFlow()

    private val _isTargetTyping = MutableStateFlow(false)
    val isTargetTyping: StateFlow<Boolean> = _isTargetTyping.asStateFlow()

    private val _isBlockedByMe = MutableStateFlow(false)
    val isBlockedByMe: StateFlow<Boolean> = _isBlockedByMe.asStateFlow()

    private val _targetUserId = MutableStateFlow<String?>(null)
    val targetUserId: StateFlow<String?> = _targetUserId.asStateFlow()

    private val _isChatMuted = MutableStateFlow(false)
    val isChatMuted: StateFlow<Boolean> = _isChatMuted.asStateFlow()

    init {
        viewModelScope.launch {
            UserRepository.startListeningToCurrentUser()
            combine(
                UserRepository.currentUserData,
                _targetUserId
            ) { currentUser, targetId ->
                if (currentUser != null && targetId != null) {
                    currentUser.mutedChats.contains(targetId)
                } else {
                    false
                }
            }.collect { muted ->
                _isChatMuted.value = muted
            }
        }
    }

    private var messagesRegistration: ListenerRegistration? = null
    private var chatRoomRegistration: ListenerRegistration? = null
    private var currentRoomId: String? = null
    private var isSelfTypingLastValue: Boolean? = null

    // Track deletedAt for filtering messages
    private var userDeletedAt: Long = 0L
    private var userClearedAt: Long = 0L
    private var allMessagesList: List<Message> = emptyList()

    private fun updateFilteredMessages() {
        val currentUserId = auth.currentUser?.uid ?: ""
        _messages.value = allMessagesList.filter { message ->
            message.timestamp > userDeletedAt &&
            message.timestamp > userClearedAt &&
            !message.blockedFor.contains(currentUserId)
        }
    }

    fun setInitialTargetInfo(name: String?, image: String?) {
        if (!name.isNullOrEmpty()) _targetUserName.value = name
        if (!image.isNullOrEmpty()) _targetUserProfileImage.value = image
    }

    fun loadChat(roomId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        currentRoomId = roomId
        isSelfTypingLastValue = null
        userDeletedAt = 0L
        userClearedAt = 0L
        _isBlockedByMe.value = false
        allMessagesList = emptyList()
        _messages.value = emptyList()

        // Clean up previous listeners if any
        messagesRegistration?.remove()
        chatRoomRegistration?.remove()

        // Fetch target profile picture once
        viewModelScope.launch {
            try {
                val roomSnap = firestore.collection("chatRooms").document(roomId).get().await()
                val participants = roomSnap.get("participants") as? List<String> ?: emptyList()
                val targetId = participants.firstOrNull { it != currentUserId }
                _targetUserId.value = targetId
                if (targetId != null) {
                    val userSnap = firestore.collection("users").document(targetId).get().await()
                    _targetUserProfileImage.value = userSnap.getString("profilePictureUrl")
                }
            } catch (_: Exception) {}
        }

        // Listen for room changes (typing status, participant names, and deletedAt)
        chatRoomRegistration = firestore.collection("chatRooms").document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val participants = snapshot.get("participants") as? List<String> ?: emptyList()
                    val names = snapshot.get("participantNames") as? Map<String, String> ?: emptyMap()
                    val typingMap = snapshot.get("typing") as? Map<String, Boolean> ?: emptyMap()
                    
                    // Read deletedAt timestamp for the current user
                    val deletedAtMap = snapshot.get("deletedAt") as? Map<String, Long> ?: emptyMap()
                    userDeletedAt = deletedAtMap[currentUserId] ?: 0L

                    // Read clearedAt timestamp for the current user
                    val clearedAtMap = snapshot.get("clearedAt") as? Map<String, Long> ?: emptyMap()
                    userClearedAt = clearedAtMap[currentUserId] ?: 0L

                    // Read blockedAt timestamp for the current user
                    val blockedAtMap = snapshot.get("blockedAt") as? Map<String, Long> ?: emptyMap()
                    _isBlockedByMe.value = blockedAtMap.containsKey(currentUserId)
                    
                    val targetId = participants.firstOrNull { it != currentUserId }
                    if (targetId != null) {
                        _targetUserId.value = targetId
                        _targetUserName.value = names[targetId] ?: "Chat"
                        _isTargetTyping.value = typingMap[targetId] ?: false
                    }
                    
                    // Filter messages with updated delete timestamp
                    updateFilteredMessages()
                }
            }

        // Listen for messages
        messagesRegistration = firestore.collection("chatRooms").document(roomId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    allMessagesList = snapshot.toObjects(Message::class.java)
                    updateFilteredMessages()
                }
            }
    }

    fun setSelfTyping(roomId: String, isTyping: Boolean) {
        if (isSelfTypingLastValue == isTyping) return
        isSelfTypingLastValue = isTyping
        val currentUserId = auth.currentUser?.uid ?: return
        firestore.collection("chatRooms").document(roomId)
            .update("typing.$currentUserId", isTyping)
    }

    override fun onCleared() {
        super.onCleared()
        messagesRegistration?.remove()
        chatRoomRegistration?.remove()
        val roomId = currentRoomId
        if (roomId != null) {
            setSelfTyping(roomId, false)
        }
    }

    fun sendMessage(
        roomId: String,
        text: String,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSenderId: String? = null,
        replyToSenderName: String? = null
    ) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        if (text.isBlank()) return@launch

        try {
            val roomSnap = firestore.collection("chatRooms").document(roomId).get().await()
            val participants = roomSnap.get("participants") as? List<String> ?: emptyList()
            val targetId = participants.firstOrNull { it != currentUserId }
            val blockedAtMap = roomSnap.get("blockedAt") as? Map<String, Long> ?: emptyMap()
            val isTargetBlockedMe = targetId != null && (blockedAtMap[targetId] ?: 0L) > 0L

            val blockedForList = if (isTargetBlockedMe && targetId != null) listOf(targetId) else emptyList<String>()

            val messageRef = firestore.collection("chatRooms").document(roomId).collection("messages").document()
            val message = Message(
                id = messageRef.id,
                senderId = currentUserId,
                text = text,
                timestamp = System.currentTimeMillis(),
                replyToId = replyToId,
                replyToText = replyToText,
                replyToSenderId = replyToSenderId,
                replyToSenderName = replyToSenderName,
                blockedFor = blockedForList
            )

            messageRef.set(message).await()

            // Update last message in room
            val timestamp = System.currentTimeMillis()
            val updates = hashMapOf<String, Any>(
                "lastMessage" to text,
                "lastMessageTimestamp" to timestamp,
                "lastMessageSenderId" to currentUserId,
                "userLastMessage.$currentUserId" to text,
                "userLastMessageTimestamp.$currentUserId" to timestamp
            )
            if (targetId != null) {
                if (isTargetBlockedMe) {
                    updates["lastMessageBlockedFor"] = listOf(targetId)
                } else {
                    updates["lastMessageBlockedFor"] = emptyList<String>()
                    updates["userLastMessage.$targetId"] = text
                    updates["userLastMessageTimestamp.$targetId"] = timestamp
                }
            }

            firestore.collection("chatRooms").document(roomId).update(updates).await()

            // Send Notification
            if (targetId != null && !isTargetBlockedMe) {
                sendNotification(targetId, "message", text)
            }
        } catch (_: Exception) {
            // handle error
        }
    }

    fun clearChat(roomId: String) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        try {
            val roomRef = firestore.collection("chatRooms").document(roomId)
            val currentTime = System.currentTimeMillis()
            roomRef.update("clearedAt.$currentUserId", currentTime).await()
        } catch (e: Exception) {
            android.util.Log.e("ChatDetailViewModel", "Error clearing chat: ${e.message}", e)
        }
    }

    fun blockUser(roomId: String) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        try {
            val roomRef = firestore.collection("chatRooms").document(roomId)
            val currentTime = System.currentTimeMillis()
            roomRef.update("blockedAt.$currentUserId", currentTime).await()
        } catch (e: Exception) {
            android.util.Log.e("ChatDetailViewModel", "Error blocking user: ${e.message}", e)
        }
    }

    fun unblockUser(roomId: String) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        try {
            val roomRef = firestore.collection("chatRooms").document(roomId)
            roomRef.update("blockedAt.$currentUserId", FieldValue.delete()).await()
        } catch (e: Exception) {
            android.util.Log.e("ChatDetailViewModel", "Error unblocking user: ${e.message}", e)
        }
    }

    fun deleteMessage(roomId: String, messageId: String) = viewModelScope.launch {
        try {
            firestore.collection("chatRooms").document(roomId).collection("messages").document(messageId).delete().await()
            
            // Check if the deleted message was the last message
            val querySnapshot = firestore.collection("chatRooms").document(roomId).collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            val latestMessage = querySnapshot.toObjects(Message::class.java).firstOrNull()
            if (latestMessage != null) {
                firestore.collection("chatRooms").document(roomId).update(
                    "lastMessage", latestMessage.text,
                    "lastMessageTimestamp", latestMessage.timestamp
                ).await()
            } else {
                firestore.collection("chatRooms").document(roomId).update(
                    "lastMessage", "",
                    "lastMessageTimestamp", 0L
                ).await()
            }
        } catch (_: Exception) {
            // handle error
        }
    }

    fun editMessage(roomId: String, messageId: String, newText: String) = viewModelScope.launch {
        if (newText.isBlank()) return@launch
        try {
            firestore.collection("chatRooms").document(roomId).collection("messages").document(messageId)
                .update("text", newText, "edited", true).await()
            
            // If the edited message is the latest one, update lastMessage in chat room
            val querySnapshot = firestore.collection("chatRooms").document(roomId).collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            val latestMessage = querySnapshot.toObjects(Message::class.java).firstOrNull()
            if (latestMessage != null && latestMessage.id == messageId) {
                firestore.collection("chatRooms").document(roomId).update(
                    "lastMessage", newText
                ).await()
            }
        } catch (_: Exception) {
            // handle error
        }
    }

    fun toggleReaction(roomId: String, messageId: String, emoji: String) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        val messageRef = firestore.collection("chatRooms").document(roomId).collection("messages").document(messageId)
        
        val originalMessages = _messages.value
        val originalAllMessages = allMessagesList
        
        // Optimistic UI Update
        val updateBlock = { msg: Message ->
            if (msg.id == messageId) {
                val updatedReactions = msg.reactions.toMutableMap()
                if (updatedReactions[currentUserId] == emoji) {
                    updatedReactions.remove(currentUserId)
                } else {
                    updatedReactions[currentUserId] = emoji
                }
                msg.copy(reactions = updatedReactions)
            } else {
                msg
            }
        }
        allMessagesList = originalAllMessages.map(updateBlock)
        updateFilteredMessages()
        
        try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(messageRef)
                if (snapshot.exists()) {
                    val currentReactions = snapshot.get("reactions") as? Map<String, String> ?: emptyMap()
                    val updatedReactions = currentReactions.toMutableMap()
                    
                    if (updatedReactions[currentUserId] == emoji) {
                        updatedReactions.remove(currentUserId)
                    } else {
                        updatedReactions[currentUserId] = emoji
                    }
                    
                    transaction.update(messageRef, "reactions", updatedReactions)
                }
                null
            }.await()
        } catch (_: Exception) {
            // Rollback optimistic update on error
            allMessagesList = originalAllMessages
            _messages.value = originalMessages
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
                postContent = messageText,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )

            firestore.collection("notifications").document(notification.id).set(notification).await()
        } catch (_: Exception) {
            // Log error
        }
    }

    fun toggleMuteChat(roomId: String) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        val targetId = _targetUserId.value ?: return@launch
        val currentUser = UserRepository.currentUserData.value ?: return@launch

        val isMuted = currentUser.mutedChats.contains(targetId)
        val newMutedChats = if (isMuted) {
            currentUser.mutedChats - targetId
        } else {
            currentUser.mutedChats + targetId
        }

        // Optimistically update local cache
        UserRepository.updateCurrentUser(currentUser.copy(mutedChats = newMutedChats))

        try {
            firestore.collection("users").document(currentUserId)
                .update("mutedChats", newMutedChats).await()
        } catch (_: Exception) {
            // Revert on error
            UserRepository.updateCurrentUser(currentUser)
        }
    }
}
