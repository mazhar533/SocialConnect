package com.mazhar.socialconnect.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mazhar.socialconnect.data.model.ChatRoom
import com.mazhar.socialconnect.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatListViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    private val _followingUsers = MutableStateFlow<List<User>>(emptyList())
    val followingUsers: StateFlow<List<User>> = _followingUsers.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        fetchChatRooms()
        fetchFollowingUsers()
    }

    private fun fetchChatRooms() = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        _loading.value = true
        try {
            firestore.collection("chatRooms")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("ChatListViewModel", "Error fetching chat rooms: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val rooms = snapshot.toObjects(ChatRoom::class.java)
                            .map { room ->
                                val userClearedAt = room.clearedAt[currentUserId] ?: 0L
                                val effectiveTimestamp = room.userLastMessageTimestamp[currentUserId] ?: room.lastMessageTimestamp
                                val effectiveLastMessage = if (effectiveTimestamp <= userClearedAt) {
                                    ""
                                } else {
                                    room.userLastMessage[currentUserId] ?: room.lastMessage
                                }
                                room.copy(
                                    lastMessage = effectiveLastMessage,
                                    lastMessageTimestamp = effectiveTimestamp
                                )
                            }
                            .filter { room ->
                                val userDeletedAt = room.deletedAt[currentUserId] ?: 0L
                                room.lastMessageTimestamp > userDeletedAt
                            }
                            .sortedByDescending { it.lastMessageTimestamp }
                        _chatRooms.value = rooms
                    }
                }
        } catch (_: Exception) {
            // Handle error
        } finally {
            _loading.value = false
        }
    }

    private fun fetchFollowingUsers() = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        try {
            val userSnapshot = firestore.collection("users").document(currentUserId).get().await()
            val user = userSnapshot.toObject(User::class.java)
            val followingIds = user?.following ?: emptyList()
            
            if (followingIds.isNotEmpty()) {
                val users = mutableListOf<User>()
                // Fetch in chunks or individually (since 'in' query is limited to 10)
                for (id in followingIds.take(10)) {
                    val uSnap = firestore.collection("users").document(id).get().await()
                    uSnap.toObject(User::class.java)?.let { users.add(it) }
                }
                _followingUsers.value = users
            }
        } catch (_: Exception) {
            // Handle
        }
    }

    fun getOrCreateChatRoom(targetUser: User, onComplete: (String) -> Unit) = viewModelScope.launch {
        val currentUserId = auth.currentUser?.uid ?: return@launch
        
        // Find existing chat room
        val existingRoom = _chatRooms.value.find { it.participants.contains(targetUser.uid) }
        if (existingRoom != null) {
            onComplete(existingRoom.id)
            return@launch
        }

        // Fetch current user details for room metadata
        val currentUserSnapshot = firestore.collection("users").document(currentUserId).get().await()
        val currentUser = currentUserSnapshot.toObject(User::class.java) ?: return@launch

        // Create new room
        val roomId = firestore.collection("chatRooms").document().id
        val now = System.currentTimeMillis()
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
            lastMessageTimestamp = now,
            userLastMessage = mapOf(
                currentUserId to "Say Hi!",
                targetUser.uid to "Say Hi!"
            ),
            userLastMessageTimestamp = mapOf(
                currentUserId to now,
                targetUser.uid to now
            )
        )

        firestore.collection("chatRooms").document(roomId).set(newRoom).await()
        onComplete(roomId)
    }

    fun deleteChatRoom(roomId: String) = viewModelScope.launch {
        try {
            val currentUserId = auth.currentUser?.uid ?: return@launch
            val roomRef = firestore.collection("chatRooms").document(roomId)
            val currentTime = System.currentTimeMillis()
            
            // 1. Update the deletedAt map for the current user in Firestore
            roomRef.update("deletedAt.$currentUserId", currentTime).await()
            
            // 2. Fetch the updated chat room to verify if all participants have deleted it
            val snapshot = roomRef.get().await()
            val room = snapshot.toObject(ChatRoom::class.java)
            if (room != null) {
                val participants = room.participants
                val deletedAtMap = room.deletedAt
                
                // If all participants have deleted the chat room, clean it up permanently
                val allDeleted = participants.isNotEmpty() && participants.all { deletedAtMap.containsKey(it) }
                if (allDeleted) {
                    val messagesSnapshot = roomRef.collection("messages").get().await()
                    val batch = firestore.batch()
                    batch.delete(roomRef)
                    if (!messagesSnapshot.isEmpty) {
                        for (doc in messagesSnapshot.documents) {
                            batch.delete(doc.reference)
                        }
                    }
                    batch.commit().await()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatListViewModel", "Error deleting chat room: ${e.message}", e)
        }
    }
}
