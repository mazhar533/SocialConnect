package com.mazhar.socialconnect.ui.screens.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.mazhar.socialconnect.data.model.ChatRoom
import com.mazhar.socialconnect.ui.screens.home.CustomBottomNavigationBar
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListScreen(
    onNavigateToChatDetail: (String, String, String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCreatePost: (String?) -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: ChatListViewModel = viewModel()
) {
    val chatRooms by viewModel.chatRooms.collectAsState()
    val followingUsers by viewModel.followingUsers.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var roomToDelete by remember { mutableStateOf<ChatRoom?>(null) }
    var showDeleteWarningDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            CustomBottomNavigationBar(
                onHomeClick = onNavigateToHome,
                onEditClick = { onNavigateToCreatePost(null) },
                onProfileClick = onNavigateToProfile,
                onChatsClick = {}, // We are here
                onNotificationsClick = onNavigateToNotifications,
                selectedRoute = "chats"
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
                    .padding(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 16.dp)
            ) {
                Column {
                    Text(
                        text = "SocialConnect",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Messages", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Following Horizontal List (Start a chat)
            if (followingUsers.isNotEmpty()) {
                Text(
                    "Start a conversation",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(followingUsers) { user ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                viewModel.getOrCreateChatRoom(user) { roomId ->
                                    onNavigateToChatDetail(roomId, user.name, user.profilePictureUrl)
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
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
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(user.name.split(" ").firstOrNull() ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Chat Rooms List
            Text(
                "Recent Chats",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (chatRooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No messages yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(chatRooms) { room ->
                        val targetId = room.participants.firstOrNull { it != currentUserId } ?: ""
                        val targetName = room.participantNames[targetId] ?: "User"
                        val targetImage = room.participantImages[targetId] ?: ""
                        
                        ChatRoomItem(
                            room = room,
                            currentUserId = currentUserId,
                            onClick = { onNavigateToChatDetail(room.id, targetName, targetImage) },
                            onDeleteClick = {
                                roomToDelete = room
                                showDeleteWarningDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteWarningDialog && roomToDelete != null) {
        val targetId = roomToDelete!!.participants.firstOrNull { it != currentUserId } ?: ""
        val targetName = roomToDelete!!.participantNames[targetId] ?: "User"
        
        AlertDialog(
            onDismissRequest = {
                showDeleteWarningDialog = false
                roomToDelete = null
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )
            },
            title = {
                Text("Delete Chat?")
            },
            text = {
                Text("Are you sure you want to delete your chat with $targetName? This will hide the chat room from your list and clear your message history, but the other participant will still see the conversation.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChatRoom(roomToDelete!!.id)
                        showDeleteWarningDialog = false
                        roomToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFF9800)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteWarningDialog = false
                        roomToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatRoomItem(
    room: ChatRoom, 
    currentUserId: String, 
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val targetUserId = room.participants.firstOrNull { it != currentUserId } ?: return
    val targetName = room.participantNames[targetUserId] ?: "User"
    val targetImage = room.participantImages[targetUserId] ?: ""
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .background(Color.Transparent)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            ) {
                if (targetImage.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(targetImage),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(targetName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                    Text(formatTime(room.lastMessageTimestamp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    room.lastMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete Chat", color = Color(0xFFFF9800)) },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF9800)
                    )
                }
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
