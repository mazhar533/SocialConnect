package com.mazhar.socialconnect.data.model

data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfilePicture: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
