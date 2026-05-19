package com.mazhar.socialconnect.ui.screens.notification

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.mazhar.socialconnect.data.model.Notification
import com.mazhar.socialconnect.ui.components.NotificationSkeleton
import com.mazhar.socialconnect.ui.screens.home.CustomBottomNavigationBar
import com.mazhar.socialconnect.ui.screens.home.formatTimestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPost: (String, String?) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToCreatePost: (String?) -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val loading by viewModel.loading.collectAsState()

    var showBanner by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            false
        )
    }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .statusBarsPadding()
                        .padding(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 10.dp)
                ) {
                    Column {
                        Text(
                            text = "SocialConnect",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Cursive,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Notifications",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (notifications.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.clearAll()
                                        showBanner = true
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(3000)
                                            showBanner = false
                                        }
                                    },
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = "Clear All",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                CustomBottomNavigationBar(
                    onHomeClick = onNavigateToHome,
                    onEditClick = { onNavigateToCreatePost(null) },
                    onProfileClick = { onNavigateToProfile("") }, // current user profile
                    onChatsClick = onNavigateToChats,
                    onNotificationsClick = { }, // already here
                    selectedRoute = "notifications"
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            if (loading && notifications.isEmpty()) {
                Box(modifier = Modifier.padding(paddingValues)) {
                    NotificationSkeleton()
                }
            } else if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notifications yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = {
                                viewModel.markAsRead(notification.id)
                                when (notification.type) {
                                    "follow" -> onNavigateToProfile(notification.fromUserId)
                                    "like", "comment", "share" -> {
                                        notification.postId?.let { onNavigateToPost(it, notification.commentId) }
                                    }
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }

            // Drop Banner
            AnimatedVisibility(
                visible = showBanner,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 24.dp, end = 24.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "All notifications cleared!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (notification.isRead) Color.Transparent else MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.05f
                )
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Image
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!notification.fromUserProfilePicture.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(notification.fromUserProfilePicture),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val text = when (notification.type) {
                "like" -> "liked your post"
                "comment" -> "commented on your post"
                "follow" -> "started following you"
                "message" -> "sent you a message"
                "share" -> "shared a post with you"
                else -> "notified you"
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.fromUserName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            Text(
                text = formatTimestamp(notification.timestamp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Icon indicator
        val icon = when (notification.type) {
            "like" -> Icons.Default.Favorite
            "comment" -> Icons.AutoMirrored.Filled.Message
            "follow" -> Icons.Default.PersonAdd
            "message" -> Icons.AutoMirrored.Filled.Message
            "share" -> Icons.Default.GridView
            else -> Icons.Default.Notifications
        }
        val iconTint = when (notification.type) {
            "like" -> Color.Red
            "comment" -> MaterialTheme.colorScheme.primary
            "follow" -> MaterialTheme.colorScheme.secondary
            "message" -> MaterialTheme.colorScheme.tertiary
            "share" -> Color(0xFFE65100) // Branded orange
            else -> Color.Gray
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!notification.postImage.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(notification.postImage),
                    contentDescription = "Post Preview",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}