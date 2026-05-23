package com.mazhar.socialconnect.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.mazhar.socialconnect.data.model.Message
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    roomId: String,
    initialUserName: String? = null,
    initialProfileImage: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    viewModel: ChatDetailViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val targetUserName by viewModel.targetUserName.collectAsState()
    val targetUserId by viewModel.targetUserId.collectAsState()
    val isChatMuted by viewModel.isChatMuted.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    DisposableEffect(targetUserId) {
        if (targetUserId != null) {
            com.mazhar.socialconnect.data.ActiveChatTracker.activeUserId = targetUserId
        }
        onDispose {
            com.mazhar.socialconnect.data.ActiveChatTracker.activeUserId = null
        }
    }
    var messageText by remember { mutableStateOf("") }
    val isTargetTyping by viewModel.isTargetTyping.collectAsState()
    var replyMessage by remember { mutableStateOf<Message?>(null) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var selectedMessageForOptions by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val isBlockedByMe by viewModel.isBlockedByMe.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(replyMessage) {
        if (replyMessage != null) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(editingMessage) {
        if (editingMessage != null) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(highlightedMessageId) {
        if (highlightedMessageId != null) {
            delay(1500)
            highlightedMessageId = null
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(roomId) {
        viewModel.setInitialTargetInfo(initialUserName, initialProfileImage)
        viewModel.loadChat(roomId)
    }

    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) {
            viewModel.setSelfTyping(roomId, true)
            delay(2000)
            viewModel.setSelfTyping(roomId, false)
        } else {
            viewModel.setSelfTyping(roomId, false)
        }
    }

    DisposableEffect(roomId) {
        onDispose {
            viewModel.setSelfTyping(roomId, false)
        }
    }
    Scaffold(
        topBar = {
            val profileImage by viewModel.targetUserProfileImage.collectAsState()
            Surface(
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImage != null) {
                            AsyncImage(
                                model = profileImage,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = targetUserName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        if (isTargetTyping) {
                            Text(
                                text = "typing...",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isChatMuted) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "Muted",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Clear Chat") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.clearChat(roomId)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isBlockedByMe) "Unblock" else "Block") },
                                    onClick = {
                                        showMenu = false
                                        if (isBlockedByMe) {
                                            viewModel.unblockUser(roomId)
                                        } else {
                                            viewModel.blockUser(roomId)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isChatMuted) "Unmute Notifications" else "Mute Notifications") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.toggleMuteChat(roomId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (editingMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(36.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Editing Message",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = editingMessage?.text ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = {
                                    editingMessage = null
                                    messageText = ""
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel edit",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else if (replyMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(36.dp)
                                    .background(Color(0xFFF57C00), RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (replyMessage?.senderId == currentUserId) "Replying to You" else "Replying to $targetUserName",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF57C00)
                                )
                                Text(
                                    text = replyMessage?.text ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = { replyMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (isBlockedByMe) {
                        Button(
                            onClick = { viewModel.unblockUser(roomId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "You blocked this user. Tap to Unblock",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                placeholder = { Text("Type a message...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .focusRequester(focusRequester),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        val currentEditMsg = editingMessage
                                        if (currentEditMsg != null) {
                                            viewModel.editMessage(
                                                roomId = roomId,
                                                messageId = currentEditMsg.id,
                                                newText = messageText
                                            )
                                            editingMessage = null
                                        } else {
                                            viewModel.sendMessage(
                                                roomId = roomId,
                                                text = messageText,
                                                replyToId = replyMessage?.id,
                                                replyToText = replyMessage?.text,
                                                replyToSenderId = replyMessage?.senderId,
                                                replyToSenderName = if (replyMessage?.senderId == currentUserId) "You" else targetUserName
                                            )
                                        }
                                        messageText = ""
                                        replyMessage = null
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusManager.clearFocus()
                        }
                    )
                },
            reverseLayout = true
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            if (isTargetTyping) {
                item {
                    TypingBubble()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            itemsIndexed(messages) { index, message ->
                MessageBubble(
                    message = message, 
                    isOwnMessage = message.senderId == currentUserId,
                    isHighlighted = message.id == highlightedMessageId,
                    onPostClick = onNavigateToPost,
                    onReplyClick = { 
                        replyMessage = message 
                        editingMessage = null
                        coroutineScope.launch {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    },
                    onReplyPreviewClick = { replyId ->
                        val replyIndex = messages.indexOfFirst { it.id == replyId }
                        if (replyIndex != -1) {
                            coroutineScope.launch {
                                val targetScrollIndex = 1 + (if (isTargetTyping) 1 else 0) + replyIndex
                                listState.animateScrollToItem(targetScrollIndex)
                                highlightedMessageId = replyId
                            }
                        }
                    },
                    onLongClick = {
                        selectedMessageForOptions = message
                        showOptionsSheet = true
                    },
                    onReactionClick = { emoji ->
                        viewModel.toggleReaction(roomId, message.id, emoji)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                val nextMessage = if (index + 1 < messages.size) messages[index + 1] else null
                val showHeader = if (nextMessage == null) {
                    true
                } else {
                    val currentCal = Calendar.getInstance().apply { timeInMillis = message.timestamp }
                    val nextCal = Calendar.getInstance().apply { timeInMillis = nextMessage.timestamp }
                    !isSameDay(currentCal, nextCal)
                }

                if (showHeader) {
                    DateHeader(getChatDateHeader(message.timestamp))
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showOptionsSheet && selectedMessageForOptions != null) {
        val selectedMsg = selectedMessageForOptions!!
        ModalBottomSheet(
            onDismissRequest = {
                showOptionsSheet = false
                selectedMessageForOptions = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Reactions Section
                Text(
                    text = "React",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.Start).padding(start = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Reaction emojis Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val emojis = listOf("❤️", "👍", "😂", "😮", "😢", "🙏")
                    emojis.forEach { emoji ->
                        val isReacted = selectedMsg.reactions[currentUserId] == emoji
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isReacted) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    viewModel.toggleReaction(roomId, selectedMsg.id, emoji)
                                    showOptionsSheet = false
                                    selectedMessageForOptions = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 24.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Actions List
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Reply Option
                        ListItem(
                            headlineContent = { Text("Reply") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.clickable {
                                replyMessage = selectedMsg
                                editingMessage = null
                                showOptionsSheet = false
                                selectedMessageForOptions = null
                                coroutineScope.launch {
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            }
                        )
                        
                        // Edit Option (if current user sent it)
                        if (selectedMsg.senderId == currentUserId) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ListItem(
                                headlineContent = { Text("Edit Message") },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.clickable {
                                    editingMessage = selectedMsg
                                    replyMessage = null
                                    messageText = selectedMsg.text
                                    showOptionsSheet = false
                                    selectedMessageForOptions = null
                                    coroutineScope.launch {
                                        focusRequester.requestFocus()
                                        keyboardController?.show()
                                    }
                                }
                            )
                        }
                        
                        // Delete Option (if current user sent it)
                        if (selectedMsg.senderId == currentUserId) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ListItem(
                                headlineContent = { Text("Delete Message", color = MaterialTheme.colorScheme.error) },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                modifier = Modifier.clickable {
                                    viewModel.deleteMessage(roomId, selectedMsg.id)
                                    showOptionsSheet = false
                                    selectedMessageForOptions = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message, 
    isOwnMessage: Boolean, 
    isHighlighted: Boolean = false,
    onPostClick: (String) -> Unit = {},
    onReplyClick: () -> Unit = {},
    onReplyPreviewClick: (String) -> Unit = {},
    onLongClick: () -> Unit = {},
    onReactionClick: (String) -> Unit = {}
) {
    val baseBgColor = if (isOwnMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val baseTextColor = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    val bgColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            baseBgColor
        },
        animationSpec = tween(durationMillis = 350)
    )

    val textColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            baseTextColor
        },
        animationSpec = tween(durationMillis = 350)
    )

    val shape = if (isOwnMessage) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val focusManager = LocalFocusManager.current
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var hasTicked by remember { mutableStateOf(false) }

    val thresholdDp = 50.dp
    val thresholdPx = with(density) { thresholdDp.toPx() }
    val maxDragDp = 80.dp
    val maxDragPx = with(density) { maxDragDp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        hasTicked = false
                    },
                    onDragEnd = {
                        val currentOffset = offsetX.value
                        coroutineScope.launch {
                            if (currentOffset >= thresholdPx) {
                                onReplyClick()
                            }
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        val newOffset = (offsetX.value + dragAmount).coerceIn(0f, maxDragPx)
                        coroutineScope.launch {
                            offsetX.snapTo(newOffset)
                            if (newOffset >= thresholdPx && !hasTicked) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                hasTicked = true
                            } else if (newOffset < thresholdPx && hasTicked) {
                                hasTicked = false
                            }
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.CenterStart
            ) {
            val progress = (offsetX.value / thresholdPx).coerceIn(0f, 1f)
            val scale = progress
            val alpha = progress
            
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .padding(start = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (offsetX.value >= thresholdPx) Color(0xFFF57C00).copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Reply",
                        tint = if (offsetX.value >= thresholdPx) Color(0xFFF57C00) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .widthIn(max = 280.dp),
                horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
            ) {
                Box(
                    contentAlignment = if (isOwnMessage) Alignment.BottomEnd else Alignment.BottomStart
                ) {
                    Box(
                        modifier = Modifier
                            .clip(shape)
                            .background(bgColor)
                            .combinedClickable(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (message.postId != null) onPostClick(message.postId)
                                },
                                onLongClick = onLongClick
                            )
                            .padding(if (message.postId != null) 4.dp else 12.dp)
                    ) {
                        Column {
                            if (message.replyToId != null) {
                                val replyBgColor = if (isOwnMessage) {
                                    Color.White.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                }
                                val baseReplyTextColor = if (isOwnMessage) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                }
                                val replyTextColor = if (isHighlighted) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                } else {
                                    baseReplyTextColor
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(replyBgColor)
                                        .clickable { 
                                            focusManager.clearFocus()
                                            message.replyToId?.let { onReplyPreviewClick(it) }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(24.dp)
                                            .background(Color(0xFFF57C00), RoundedCornerShape(1.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = message.replyToSenderName ?: "User",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF57C00)
                                        )
                                        Text(
                                            text = message.replyToText ?: "",
                                            fontSize = 11.sp,
                                            color = replyTextColor,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (message.postId != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.GridView, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFFE65100)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Shared Post", 
                                        fontSize = 10.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            if (!message.imageUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = message.imageUrl,
                                    contentDescription = "Shared Post",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            Text(
                                message.text, 
                                color = textColor, 
                                fontSize = 14.sp,
                                modifier = if (message.postId != null) Modifier.padding(horizontal = 8.dp, vertical = 4.dp) else Modifier
                            )
                            if (message.reactions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    if (message.reactions.isNotEmpty()) {
                        ReactionsPill(
                            reactions = message.reactions,
                            isOwnMessage = isOwnMessage,
                            onReactionClick = onReactionClick,
                            modifier = Modifier.offset(
                                x = if (isOwnMessage) (-8).dp else 8.dp,
                                y = 8.dp
                            )
                        )
                    }
                }
                
                val spacingHeight = if (message.reactions.isNotEmpty()) 10.dp else 4.dp
                Spacer(modifier = Modifier.height(spacingHeight))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    if (message.edited) {
                        Text(
                            text = "(edited)  ",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontStyle = FontStyle.Italic
                        )
                    }
                    Text(
                        formatTime(message.timestamp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
}

@Composable
fun DateHeader(date: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = date,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getChatDateHeader(timestamp: Long): String {
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

    return when {
        isSameDay(date, today) -> "Today"
        isSameDay(date, yesterday) -> "Yesterday"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date.time)
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun TypingBubble() {
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    val bgColor = MaterialTheme.colorScheme.surface
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(bgColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val transition = rememberInfiniteTransition(label = "typing")
                
                val dot1Scale by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 1200
                            0.3f at 0
                            1f at 300
                            0.3f at 600
                        },
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "dot1"
                )

                val dot2Scale by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 1200
                            0.3f at 200
                            1f at 500
                            0.3f at 800
                        },
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "dot2"
                )

                val dot3Scale by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 1200
                            0.3f at 400
                            1f at 700
                            0.3f at 1000
                        },
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "dot3"
                )

                val dotColor = Color(0xFFF57C00)

                Dot(scale = dot1Scale, color = dotColor)
                Dot(scale = dot2Scale, color = dotColor)
                Dot(scale = dot3Scale, color = dotColor)
            }
        }
    }
}

@Composable
fun Dot(scale: Float, color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun ReactionsPill(
    reactions: Map<String, String>,
    isOwnMessage: Boolean,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = remember(reactions) {
        reactions.values.groupBy { it }.mapValues { it.value.size }
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            grouped.forEach { (emoji, count) ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onReactionClick(emoji) }
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(text = emoji, fontSize = 11.sp)
                    if (reactions.size > 1) {
                        Text(
                            text = count.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (reactions.size > 1 && grouped.size > 1) {
                Text(
                    text = reactions.size.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}