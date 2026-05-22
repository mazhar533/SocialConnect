package com.mazhar.socialconnect.data.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val postId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSenderId: String? = null,
    val replyToSenderName: String? = null,
    val edited: Boolean = false,
    val reactions: Map<String, String> = emptyMap(),
    val blockedFor: List<String> = emptyList()
)
