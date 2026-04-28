package com.mazhar.socialconnect.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.mazhar.socialconnect.ui.screens.home.CustomBottomNavigationBar
import com.mazhar.socialconnect.ui.theme.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazhar.socialconnect.ui.components.PostCard
import com.google.firebase.auth.FirebaseAuth
import com.mazhar.socialconnect.ui.components.PostCardSkeleton
import com.mazhar.socialconnect.ui.components.ProfileHeaderSkeleton
import com.mazhar.socialconnect.ui.components.FeedHeaderSkeleton
@Composable
fun ProfileScreen(
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToCreatePost: (String?) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val userData by viewModel.userData.collectAsState()
    val userPosts by viewModel.userPosts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    Scaffold(
        bottomBar = {
            CustomBottomNavigationBar(
                onHomeClick = onNavigateToHome,
                onEditClick = { onNavigateToCreatePost(null) },
                onProfileClick = {}, // We are here
                onSettingsClick = onNavigateToSettings,
                selectedRoute = "profile"
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                                .height(180.dp)
                                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                                .background(PrimaryPurple)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "@${userData?.name?.lowercase()?.replace(" ", "") ?: "user"}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                IconButton(onClick = onNavigateToSettings) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                                }
                            }
                        }

                        // Profile Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(top = 110.dp)
                                .align(Alignment.TopCenter),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
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

                                Text(userData?.name ?: "Loading...", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text(
                                    userData?.bio ?: "No bio yet",
                                    fontSize = 14.sp,
                                    color = TextGray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Stats
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
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
                                            color = BorderColor
                                        )
                                        StatItem(userData?.followersCount?.toString() ?: "0", "FOLLOWERS")
                                        VerticalDivider(
                                            modifier = Modifier.height(32.dp),
                                            thickness = 1.dp,
                                            color = BorderColor
                                        )
                                        StatItem(userData?.followingCount?.toString() ?: "0", "FOLLOWING")
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Button(
                                        onClick = onNavigateToEditProfile,
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Edit Profile", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Button(
                                        onClick = { /* Share action */ },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFFF0E6),
                                            contentColor = OrangeAccent
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Share", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Profile Image (Overlapping)
                        Box(
                            modifier = Modifier
                                .padding(top = 60.dp) // This will position the circle
                                .size(100.dp)
                                .align(Alignment.TopCenter)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
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

            item {
                Spacer(modifier = Modifier.height(32.dp))

                if (userPosts.isEmpty() && loading) {
                    FeedHeaderSkeleton()
                } else {
                    // My Feed Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("My Feed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { }, modifier = Modifier.size(36.dp).clip(CircleShape).background(TextFieldBg)) {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "List View", tint = PrimaryPurpleSoft, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.GridView, contentDescription = "Grid View", tint = TextGray, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (userPosts.isEmpty() && loading) {
                items(3) {
                    PostCardSkeleton()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else if (userPosts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No posts yet", color = TextGray)
                    }
                }
            } else {
                items(userPosts) { post ->
                    PostCard(
                        post = post,
                        currentUserId = currentUserId,
                        onLikeClick = { viewModel.likePost(post) },
                        onCommentClick = { /* Handle comment */ },
                        onShareClick = { /* Handle share */ },
                        onEditClick = { onNavigateToCreatePost(post.id) },
                        onDeleteClick = { viewModel.deletePost(post.id) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray)
    }
}
