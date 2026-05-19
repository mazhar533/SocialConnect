package com.mazhar.socialconnect.data.model

data class Notification(
    val id: String = "",
    val type: String = "", // "like", "comment", "follow"
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserProfilePicture: String? = null,
    val targetUserId: String = "", // The user receiving the notification
    val postId: String? = null,
    val postImage: String? = null,
    val postContent: String? = null,
    val commentId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
