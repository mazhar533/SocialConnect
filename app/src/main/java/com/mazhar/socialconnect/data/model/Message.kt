package com.mazhar.socialconnect.data.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val postId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
