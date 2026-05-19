package com.mazhar.socialconnect.data.model

data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(), // UIDs of participants
    val participantNames: Map<String, String> = emptyMap(),
    val participantImages: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L
)
