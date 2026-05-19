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
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        _chatRooms.value = snapshot.toObjects(ChatRoom::class.java)
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

        firestore.collection("chatRooms").document(roomId).set(newRoom).await()
        onComplete(roomId)
    }
}
