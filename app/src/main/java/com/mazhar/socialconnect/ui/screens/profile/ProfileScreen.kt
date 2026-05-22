package com.mazhar.socialconnect.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.mazhar.socialconnect.ui.screens.home.CustomBottomNavigationBar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazhar.socialconnect.ui.components.PostCard
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.filled.Info

import com.mazhar.socialconnect.ui.components.PostCardSkeleton
import com.mazhar.socialconnect.ui.components.ProfileHeaderSkeleton
import com.mazhar.socialconnect.ui.components.FeedHeaderSkeleton
import com.mazhar.socialconnect.ui.components.UserListSkeleton
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String? = null,
    showRequests: Boolean = false,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToChatDetail: (String, String, String) -> Unit,
    onNavigateToCreatePost: (String?) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPost: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val userData by viewModel.userData.collectAsState()
    val userPosts by viewModel.userPosts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val isOwnProfile by viewModel.isOwnProfile.collectAsState()
    val followList by viewModel.followList.collectAsState()
    val postsLoading by viewModel.postsLoading.collectAsState()
    
    val isGridView by viewModel.isGridView.collectAsState()
    var showFollowSheet by remember { mutableStateOf(false) }
    var sheetTitle by remember { mutableStateOf("") }
    
    var showShareSheet by remember { mutableStateOf(false) }
    var selectedPostForShare by remember { mutableStateOf<com.mazhar.socialconnect.data.model.Post?>(null) }
    var showRequestsSheet by remember { mutableStateOf(false) }
    val followingUsers by viewModel.followingUsers.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }
    LaunchedEffect(showRequests) {
        if (showRequests) {
            showRequestsSheet = true
        }
    }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isFollowingTarget = userData?.followers?.contains(currentUserId) == true

    Scaffold(
        bottomBar = {
            CustomBottomNavigationBar(
                onHomeClick = onNavigateToHome,
                onEditClick = { onNavigateToCreatePost(null) },
                onProfileClick = {}, // We are here
                onChatsClick = onNavigateToChats,
                onNotificationsClick = onNavigateToNotifications,
                selectedRoute = "profile"
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
            if (userData == null && loading) {
                item {
                    ProfileHeaderSkeleton()
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Header Background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .statusBarsPadding()
                                .padding(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.fillMaxWidth(0.45f)) {
                                    Text(
                                        text = "SocialConnect",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Cursive,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "@${userData?.name?.lowercase()?.replace(" ", "") ?: "user"}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                if (isOwnProfile) {
                                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        // Profile Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(top = 110.dp)
                                .align(Alignment.TopCenter),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(32.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(45.dp)) // Space for overlapping image

                                Text(userData?.name ?: "Loading...", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                 
                                 val userEmail = userData?.email
                                 if (!userEmail.isNullOrEmpty()) {
                                     Spacer(modifier = Modifier.height(2.dp))
                                     Text(
                                         text = userEmail,
                                         fontSize = 14.sp,
                                         color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                     )
                                 }

                                 if (isOwnProfile) {
                                     Spacer(modifier = Modifier.height(6.dp))
                                     var isEmailVerified by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.isEmailVerified == true) }
                                     
                                     val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                                     DisposableEffect(lifecycleOwner) {
                                         val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                                             if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                                 viewModel.reloadUser { success ->
                                                     if (success) {
                                                         isEmailVerified = FirebaseAuth.getInstance().currentUser?.isEmailVerified == true
                                                     }
                                                 }
                                             }
                                         }
                                         lifecycleOwner.lifecycle.addObserver(observer)
                                         onDispose {
                                             lifecycleOwner.lifecycle.removeObserver(observer)
                                         }
                                     }

                                     val badgeBgColor = if (isEmailVerified) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                     val badgeBorderColor = if (isEmailVerified) Color(0xFF81C784) else Color(0xFFFFB74D)
                                     val badgeTextColor = if (isEmailVerified) Color(0xFF2E7D32) else Color(0xFFE65100)
                                     val badgeText = if (isEmailVerified) "Verified" else "Unverified (Click to verify)"
                                     val badgeIcon = if (isEmailVerified) Icons.Default.Check else Icons.Default.Info

                                     Surface(
                                         modifier = Modifier
                                             .clip(RoundedCornerShape(12.dp))
                                             .clickable(enabled = !isEmailVerified) {
                                                 viewModel.sendEmailVerification { success, errorMsg ->
                                                     if (success) {
                                                         Toast.makeText(context, "Verification email sent to $userEmail", Toast.LENGTH_LONG).show()
                                                     } else {
                                                         Toast.makeText(context, "Failed to send: $errorMsg", Toast.LENGTH_LONG).show()
                                                     }
                                                 }
                                             },
                                         color = badgeBgColor,
                                         border = androidx.compose.foundation.BorderStroke(1.dp, badgeBorderColor),
                                         shape = RoundedCornerShape(12.dp)
                                     ) {
                                         Row(
                                             modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             Icon(
                                                 imageVector = badgeIcon,
                                                 contentDescription = null,
                                                 tint = badgeTextColor,
                                                 modifier = Modifier.size(14.dp)
                                             )
                                             Spacer(modifier = Modifier.width(6.dp))
                                             Text(
                                                 text = badgeText,
                                                 fontSize = 11.sp,
                                                 fontWeight = FontWeight.Bold,
                                                 color = badgeTextColor
                                             )
                                         }
                                     }
                                 }

                                 Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    userData?.bio ?: "No bio yet",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Stats
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    color = Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatItem(userData?.postsCount?.toString() ?: "0", "POSTS")
                                        VerticalDivider(
                                            modifier = Modifier.height(32.dp),
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        StatItem(userData?.followersCount?.toString() ?: "0", "FOLLOWERS") {
                                            sheetTitle = "Followers"
                                            viewModel.fetchFollowList(userData?.followers ?: emptyList())
                                            showFollowSheet = true
                                        }
                                        VerticalDivider(
                                            modifier = Modifier.height(32.dp),
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        StatItem(userData?.followingCount?.toString() ?: "0", "FOLLOWING") {
                                            sheetTitle = "Following"
                                            viewModel.fetchFollowList(userData?.following ?: emptyList())
                                            showFollowSheet = true
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (isOwnProfile) {
                                        val followRequests = userData?.followRequests ?: emptyList()
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Button(
                                                    onClick = onNavigateToEditProfile,
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    shape = RoundedCornerShape(16.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Edit Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                    }
                                                }
                                                if (followRequests.isNotEmpty()) {
                                                    OutlinedButton(
                                                        onClick = { showRequestsSheet = true },
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                                        shape = RoundedCornerShape(16.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 48.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("Requests (${followRequests.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { 
                                                viewModel.startChat { roomId -> 
                                                    onNavigateToChatDetail(roomId, userData?.name ?: "", userData?.profilePictureUrl ?: "") 
                                                } 
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(16.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Message", fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        val hasRequested = userData?.followRequests?.contains(currentUserId) == true
                                        OutlinedButton(
                                            onClick = { userData?.uid?.let { viewModel.toggleFollow(it) } },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isFollowingTarget || hasRequested) Color.Transparent else MaterialTheme.colorScheme.primary,
                                                contentColor = if (isFollowingTarget || hasRequested) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onPrimary
                                            ),
                                            border = if (isFollowingTarget || hasRequested) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                            shape = RoundedCornerShape(16.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    if (isFollowingTarget) Icons.Default.Check else if (hasRequested) Icons.Default.Check else Icons.Default.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isFollowingTarget) "Following" else if (hasRequested) "Requested" else "Follow",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Profile Image (Overlapping)
                        Box(
                            modifier = Modifier
                                .padding(top = 65.dp) // Adjusted top padding for smaller image
                                .size(90.dp)
                                .align(Alignment.TopCenter)
                                .clip(CircleShape)
                                .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                                .background(Color.LightGray)
                        ) {
                            if (userData?.profilePictureUrl?.isNotEmpty() == true) {
                                Image(
                                    painter = rememberAsyncImagePainter(userData?.profilePictureUrl),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.GridView, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(40.dp))
                            }
                        }
                    }
                }
            }

            val isPrivateLocked = !isOwnProfile && userData?.isPrivate == true && !isFollowingTarget

            if (isPrivateLocked) {
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Private Account",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "This Account is Private",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Follow this account to see their photos and videos.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(32.dp))

                    // My Feed Header (Static, no shimmer)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("My Feed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.setGridView(false) }, 
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(if (!isGridView) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List, 
                                        contentDescription = "List View", 
                                        tint = if (!isGridView) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant, 
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.setGridView(true) }, 
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isGridView) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                ) {
                                    Icon(
                                        Icons.Default.GridView, 
                                        contentDescription = "Grid View", 
                                        tint = if (isGridView) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant, 
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (postsLoading && userPosts.isEmpty()) {
                items(3) {
                    PostCardSkeleton()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else if (userPosts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No posts yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (isGridView) {
                val chunkedPosts = userPosts.chunked(3)
                items(chunkedPosts) { rowPosts ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPosts.forEach { post ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onNavigateToPost(post.id) }
                            ) {
                                if (post.imageUrl?.isNotEmpty() == true) {
                                    Image(
                                        painter = rememberAsyncImagePainter(post.imageUrl),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = post.content,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 4,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        // Fill remaining space
                        repeat(3 - rowPosts.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                items(userPosts) { post ->
                    PostCard(
                        post = post,
                        currentUserId = currentUserId,
                        isFollowing = isFollowingTarget,
                        onFollowClick = { viewModel.toggleFollow(post.userId) },
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
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showFollowSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFollowSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                        .padding(16.dp)
                ) {
                    Text(
                        text = sheetTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (loading && followList.isEmpty()) {
                        UserListSkeleton()
                    } else if (followList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No users found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(followList) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clickable { 
                                            showFollowSheet = false
                                            onNavigateToProfile(user.uid)
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.LightGray)
                                    ) {
                                        if (user.profilePictureUrl.isNotEmpty()) {
                                            Image(
                                                painter = rememberAsyncImagePainter(user.profilePictureUrl),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(user.name, fontWeight = FontWeight.Bold)
                                        Text("@${user.name.lowercase().replace(" ", "")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
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

        if (showRequestsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRequestsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                    Text("Follow Requests", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    val requestsUsers by viewModel.followRequestsList.collectAsState()
                    if (requestsUsers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No requests found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                            items(requestsUsers) { user ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray)) {
                                            if (user.profilePictureUrl.isNotEmpty()) {
                                                Image(painter = rememberAsyncImagePainter(user.profilePictureUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("@${user.name.lowercase().replace(" ", "")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.acceptFollowRequest(user.uid) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.rejectFollowRequest(user.uid) },
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}
