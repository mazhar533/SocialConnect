package com.mazhar.socialconnect.data.model

data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(), // UIDs of participants
    val participantNames: Map<String, String> = emptyMap(),
    val participantImages: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val lastMessageSenderId: String = "",
    val lastMessageBlockedFor: List<String> = emptyList(),
    val userLastMessage: Map<String, String> = emptyMap(),
    val userLastMessageTimestamp: Map<String, Long> = emptyMap(),
    val typing: Map<String, Boolean> = emptyMap(),
    val deletedAt: Map<String, Long> = emptyMap(),
    val clearedAt: Map<String, Long> = emptyMap(),
    val blockedAt: Map<String, Long> = emptyMap()
)
