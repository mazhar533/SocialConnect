package com.mazhar.socialconnect.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.mazhar.socialconnect.data.model.Post
import com.mazhar.socialconnect.ui.screens.home.ChipIconValue
import com.mazhar.socialconnect.ui.theme.*

@Composable
fun PostCard(
    post: Post,
    currentUserId: String,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isOwnPost = post.userId == currentUserId
    val isLiked = post.likedBy.contains(currentUserId)

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                ) {
                    if (post.userProfilePicture.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(post.userProfilePicture),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.userName, fontWeight = FontWeight.Bold, color = TextDark)
                    Text(formatTimestamp(post.timestamp), color = TextGray, fontSize = 12.sp)
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextGray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(CardBackground)
                    ) {
                        if (isOwnPost) {
                            DropdownMenuItem(
                                text = { Text("Edit Post", color = TextDark) },
                                onClick = {
                                    showMenu = false
                                    onEditClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryPurple) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Post", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Report Post", color = TextDark) },
                                onClick = { showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, tint = OrangeAccent) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(post.content, color = TextDark, fontSize = 14.sp)

            if (post.imageUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    painter = rememberAsyncImagePainter(model = post.imageUrl),
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLikeClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else TextGray
                    )
                }
                Text(post.likesCount.toString(), color = TextGray, fontSize = 14.sp)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                IconButton(onClick = onCommentClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comment", tint = TextGray)
                }
                Text(post.commentsCount.toString(), color = TextGray, fontSize = 14.sp)
                
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = onShareClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.IosShare, contentDescription = "Share", tint = TextGray)
                }
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "$days days ago"
        hours > 0 -> "$hours hours ago"
        minutes > 0 -> "$minutes minutes ago"
        else -> "Just now"
    }
}
