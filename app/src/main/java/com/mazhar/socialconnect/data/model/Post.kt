package com.mazhar.socialconnect.data.model

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfilePicture: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val likedBy: List<String> = emptyList()
)
