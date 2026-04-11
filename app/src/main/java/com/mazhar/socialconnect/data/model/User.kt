package com.mazhar.socialconnect.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val bio: String = "",
    val profilePictureUrl: String = "",
    val postsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0
)
