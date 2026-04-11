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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.mazhar.socialconnect.ui.theme.*

data class Post(
    val id: String,
    val userName: String,
    val timeAgo: String,
    val content: String,
    val imageUrl: String? = null,
    val likes: Int,
    val comments: Int
)

@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val mockPosts = listOf(
        Post(
            id = "1",
            userName = "Sara Ahmed",
            timeAgo = "about 1 hour ago",
            content = "Just finished my new project! Super excited to share it with everyone. \uD83D\uDE80",
            likes = 42,
            comments = 1
        ),
        Post(
            id = "2",
            userName = "Zainab Noor",
            timeAgo = "about 2 hours ago",
            content = "What a beautiful evening in the city today.",
            imageUrl = "https://images.unsplash.com/photo-1506748686214-e9df14d4d9d0",
            likes = 120,
            comments = 0
        )
    )

    Scaffold(
        bottomBar = {
            CustomBottomNavigationBar(
                onHomeClick = {},
                onEditClick = {},
                onProfileClick = onNavigateToProfile,
                onSettingsClick = onNavigateToSettings
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
            item {
                HomeHeader()
            }
            items(mockPosts) { post ->
                PostCard(post)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HomeHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(PrimaryPurple)
            .padding(24.dp)
            .padding(top = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Good morning,", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Text("Ali", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            // Mock profile image
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun PostCard(post: Post) {
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
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.userName, fontWeight = FontWeight.Bold, color = TextDark)
                    Text(post.timeAgo, color = TextGray, fontSize = 12.sp)
                }
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextGray)
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
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ChipIconValue(icon = Icons.Default.LocalFireDepartment, value = post.likes.toString(), tint = OrangeAccent, bg = Color(0xFFFFF0E6))
                Spacer(modifier = Modifier.width(12.dp))
                ChipIconValue(icon = Icons.Default.ChatBubbleOutline, value = post.comments.toString(), tint = TextGray, bg = Color.Transparent, isOutlined = true)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { /* Share */ }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.IosShare, contentDescription = "Share", tint = TextGray)
                }
            }
        }
    }
}

@Composable
fun ChipIconValue(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, tint: Color, bg: Color, isOutlined: Boolean = false) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(36.dp),
        border = if (isOutlined) androidx.compose.foundation.BorderStroke(1.dp, BorderColor) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, color = if (isOutlined) TextDark else tint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun CustomBottomNavigationBar(
    onHomeClick: () -> Unit,
    onEditClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    selectedRoute: String = "home"
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(PrimaryPurple)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(icon = Icons.Default.Home, isSelected = selectedRoute == "home", onClick = onHomeClick)
            NavBarItem(icon = Icons.Default.Edit, isSelected = selectedRoute == "edit", onClick = onEditClick)
            NavBarItem(icon = Icons.Default.PersonOutline, isSelected = selectedRoute == "profile", onClick = onProfileClick)
            NavBarItem(icon = Icons.Default.Settings, isSelected = selectedRoute == "settings", onClick = onSettingsClick)
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
            .background(if (isSelected) OrangeAccent else Color.Transparent)
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
