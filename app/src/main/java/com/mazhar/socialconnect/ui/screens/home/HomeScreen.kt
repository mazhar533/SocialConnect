package com.mazhar.socialconnect.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.mazhar.socialconnect.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import com.mazhar.socialconnect.service.NotificationHandler

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazhar.socialconnect.data.model.Post
import com.mazhar.socialconnect.ui.components.PostCard
import com.google.firebase.auth.FirebaseAuth
import com.mazhar.socialconnect.ui.components.PostCardSkeleton
import com.mazhar.socialconnect.ui.components.HomeHeaderSkeleton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: (String?) -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToCreatePost: (String?) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val currentUserData by viewModel.currentUserData.collectAsState()
    
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    var showShareSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectedPostForShare by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Post?>(null) }
    val followingUsers by viewModel.followingUsers.collectAsState()

    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.fetchPosts()
        viewModel.fetchCurrentUserData()
        NotificationHandler.startListening(context)
    }

    Scaffold(
        bottomBar = {
            CustomBottomNavigationBar(
                onHomeClick = {},
                onEditClick = { onNavigateToCreatePost(null) },
                onProfileClick = { onNavigateToProfile(null) },
                onChatsClick = onNavigateToChats,
                onNotificationsClick = onNavigateToNotifications,
                selectedRoute = "home"
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                if (currentUserData == null && loading) {
                    HomeHeaderSkeleton()
                } else {
                    HomeHeader(currentUserData?.name ?: "User", currentUserData?.profilePictureUrl)
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            if (loading && posts.isEmpty()) {
                items(5) {
                    PostCardSkeleton()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else if (posts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No posts yet. Be the first to post!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(posts) { post ->
                    val isFollowing = currentUserData?.following?.contains(post.userId) == true
                    PostCard(
                        post = post,
                        currentUserId = currentUserId,
                        isFollowing = isFollowing,
                        onFollowClick = { viewModel.toggleFollow(post.userId) },
                        onUserClick = { onNavigateToProfile(post.userId) },
                        onLikeClick = { viewModel.likePost(post) },
                        onCommentClick = { onNavigateToPost(post.id) },
                        onShareClick = { 
                            selectedPostForShare = post
                            showShareSheet = true 
                        },
                        onEditClick = { onNavigateToCreatePost(post.id) },
                        onDeleteClick = { viewModel.deletePost(post.id) },
                        onPostClick = { onNavigateToPost(post.id) },
                        showShareIcon = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showShareSheet) {
            ModalBottomSheet(
                onDismissRequest = { showShareSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                    Text("Share to", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (followingUsers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No users found to share with", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                            items(followingUsers) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPostForShare?.let { post ->
                                                viewModel.sharePost(post, user.uid)
                                                android.widget.Toast.makeText(context, "Post shared with ${user.name}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                            showShareSheet = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                        if (user.profilePictureUrl.isNotEmpty()) {
                                            Image(painter = rememberAsyncImagePainter(user.profilePictureUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(user.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("@${user.name.lowercase().replace(" ", "")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(userName: String, profileImageUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            // App Name with stylized look

            Text(
                text = "SocialConnect",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 24.sp,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Good morning,",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Text(
                        userName,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // User profile image with border
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                        .padding(2.dp)
                        .clip(CircleShape)
                ) {
                    if (profileImageUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(profileImageUrl),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
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

@Composable
fun ChipIconValue(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, tint: Color, bg: Color, isOutlined: Boolean = false) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(36.dp),
        border = if (isOutlined) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, color = if (isOutlined) MaterialTheme.colorScheme.onBackground else tint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun CustomBottomNavigationBar(
    onHomeClick: () -> Unit,
    onEditClick: () -> Unit,
    onProfileClick: () -> Unit,
    onChatsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    selectedRoute: String = "home"
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            NavBarItem(icon = Icons.Default.Home, isSelected = selectedRoute == "home", onClick = onHomeClick)
            NavBarItem(icon = Icons.Default.Edit, isSelected = selectedRoute == "edit", onClick = onEditClick)
            NavBarItem(icon = Icons.Default.Notifications, isSelected = selectedRoute == "notifications", onClick = onNotificationsClick)
            NavBarItem(icon = Icons.Default.ChatBubble, isSelected = selectedRoute == "chats", onClick = onChatsClick)
            NavBarItem(icon = Icons.Default.PersonOutline, isSelected = selectedRoute == "profile", onClick = onProfileClick)
        }
    }
}

@Composable
fun NavBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White
        )
    }
}
