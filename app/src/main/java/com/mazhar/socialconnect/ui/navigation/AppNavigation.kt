package com.mazhar.socialconnect.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.mazhar.socialconnect.ui.screens.auth.LoginScreen
import com.mazhar.socialconnect.ui.screens.auth.RecoveryScreen
import com.mazhar.socialconnect.ui.screens.auth.SignUpScreen
import com.mazhar.socialconnect.ui.screens.home.CreatePostScreen
import com.mazhar.socialconnect.ui.screens.home.HomeScreen
import com.mazhar.socialconnect.ui.screens.home.PostDetailScreen
import com.mazhar.socialconnect.ui.screens.profile.ProfileEditScreen
import com.mazhar.socialconnect.ui.screens.profile.ProfileScreen
import com.mazhar.socialconnect.ui.screens.settings.SettingsScreen

import com.mazhar.socialconnect.ui.screens.notification.NotificationScreen
import com.mazhar.socialconnect.ui.screens.chat.ChatListScreen
import com.mazhar.socialconnect.ui.screens.chat.ChatDetailScreen
import com.mazhar.socialconnect.data.model.ChatRoom
import com.mazhar.socialconnect.data.model.User
import kotlinx.coroutines.tasks.await

@Composable
fun AppNavigation(
    initialPostId: String? = null,
    initialCommentId: String? = null,
    initialNotificationType: String? = null,
    initialFromUserId: String? = null,
    onNavigationHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    
    val isSdkReady = auth.currentUser != null && currentBackStackEntry != null
    androidx.compose.runtime.LaunchedEffect(isSdkReady, initialPostId, initialCommentId, initialNotificationType, initialFromUserId) {
        // Wait until NavHost has initialized (isSdkReady is true) before attempting to navigate
        if (isSdkReady) {
            if (initialPostId != null) {
                val route = if (initialCommentId != null) {
                    "post_detail?postId=$initialPostId&commentId=$initialCommentId"
                } else {
                    "post_detail?postId=$initialPostId"
                }
                navController.navigate(route)
                onNavigationHandled()
            } else if (initialNotificationType != null) {
                android.util.Log.d("AppNavigation", "Navigating from notification type: $initialNotificationType, from: $initialFromUserId")
                when (initialNotificationType) {
                    "follow_request" -> {
                        navController.navigate("profile?showRequests=true")
                        onNavigationHandled()
                    }
                    "follow", "follow_accept" -> {
                        if (!initialFromUserId.isNullOrEmpty()) {
                            navController.navigate("profile?userId=$initialFromUserId")
                        } else {
                            navController.navigate("profile")
                        }
                        onNavigationHandled()
                    }
                    "message" -> {
                        if (!initialFromUserId.isNullOrEmpty()) {
                            val currentUserId = auth.currentUser?.uid
                            if (currentUserId != null) {
                                try {
                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    val snapshot = db.collection("chatRooms")
                                        .whereArrayContains("participants", currentUserId)
                                        .get().await()

                                    val rooms = snapshot.toObjects(ChatRoom::class.java)
                                    val existingRoom = rooms.find { it.participants.contains(initialFromUserId) }

                                    if (existingRoom != null) {
                                        val targetName = existingRoom.participantNames[initialFromUserId] ?: "Chat"
                                        val targetImage = existingRoom.participantImages[initialFromUserId] ?: ""
                                        navController.navigate("chat_detail?roomId=${existingRoom.id}&userName=$targetName&profileImage=$targetImage")
                                    } else {
                                        val currentUserSnapshot = db.collection("users").document(currentUserId).get().await()
                                        val targetUserSnapshot = db.collection("users").document(initialFromUserId).get().await()
                                        
                                        val currentUser = currentUserSnapshot.toObject(User::class.java)
                                        val targetUser = targetUserSnapshot.toObject(User::class.java)

                                        if (currentUser != null && targetUser != null) {
                                            val roomId = db.collection("chatRooms").document().id
                                            val newRoom = ChatRoom(
                                                id = roomId,
                                                participants = listOf(currentUserId, targetUser.uid),
                                                participantNames = mapOf(
                                                    currentUserId to currentUser.name,
                                                    targetUser.uid to targetUser.name
                                                ),
                                                participantImages = mapOf(
                                                    currentUserId to currentUser.profilePictureUrl,
                                                    targetUser.uid to targetUser.profilePictureUrl
                                                ),
                                                lastMessage = "Say Hi!",
                                                lastMessageTimestamp = System.currentTimeMillis()
                                            )
                                            db.collection("chatRooms").document(roomId).set(newRoom).await()
                                            navController.navigate("chat_detail?roomId=$roomId&userName=${targetUser.name}&profileImage=${targetUser.profilePictureUrl ?: ""}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("AppNavigation", "Error routing to message chat: ${e.localizedMessage}")
                                }
                            }
                        }
                        onNavigationHandled()
                    }
                    else -> {
                        // Unhandled notification type routing
                        onNavigationHandled()
                    }
                }
            }
        }
    }

    val startDestination = if (auth.currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onNavigateToSignUp = { navController.navigate("signup") },
                onNavigateToHome = { 
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    } 
                },
                onNavigateToRecovery = { navController.navigate("recovery") }
            )
        }
        composable("signup") {
            SignUpScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("signup") { inclusive = true }
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("recovery") {
            RecoveryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("home") {
            HomeScreen(
                onNavigateToProfile = { userId ->
                    val route = if (userId != null) "profile?userId=$userId" else "profile"
                    navController.navigate(route)
                },
                onNavigateToChats = { navController.navigate("chats") },
                onNavigateToCreatePost = { postId ->
                    val route = if (postId != null) "create_post?postId=$postId" else "create_post"
                    navController.navigate(route)
                },
                onNavigateToNotifications = { navController.navigate("notifications") },
                onNavigateToPost = { postId -> navController.navigate("post_detail?postId=$postId") }
            )
        }
        composable("notifications") {
            NotificationScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToProfile = { userId ->
                    val route = if (userId.isNotEmpty()) "profile?userId=$userId" else "profile"
                    navController.navigate(route)
                },
                onNavigateToRequests = {
                    navController.navigate("profile?showRequests=true")
                },
                onNavigateToPost = { postId, commentId -> 
                    if (commentId != null) {
                        navController.navigate("post_detail?postId=$postId&commentId=$commentId")
                    } else {
                        navController.navigate("post_detail?postId=$postId")
                    }
                },
                onNavigateToChats = { navController.navigate("chats") },
                onNavigateToChatDetail = { roomId, name, image ->
                    navController.navigate("chat_detail?roomId=$roomId&userName=$name&profileImage=$image")
                },
                onNavigateToCreatePost = { postId ->
                    val route = if (postId != null) "create_post?postId=$postId" else "create_post"
                    navController.navigate(route)
                }
            )
        }
        composable("create_post?postId={postId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId")
            CreatePostScreen(
                onNavigateBack = { navController.popBackStack() },
                postId = postId
            )
        }
        composable("profile?userId={userId}&showRequests={showRequests}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            val showRequests = backStackEntry.arguments?.getString("showRequests") == "true"
            ProfileScreen(
                userId = userId,
                showRequests = showRequests,
                onNavigateToEditProfile = { navController.navigate("profile_edit") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToChats = { navController.navigate("chats") },
                onNavigateToChatDetail = { roomId, name, image -> 
                    navController.navigate("chat_detail?roomId=$roomId&userName=$name&profileImage=$image") 
                },
                onNavigateToCreatePost = { postId ->
                    val route = if (postId != null) "create_post?postId=$postId" else "create_post"
                    navController.navigate(route)
                },
                onNavigateToNotifications = { navController.navigate("notifications") },
                onNavigateToProfile = { targetUserId ->
                    val route = if (targetUserId.isNotEmpty()) "profile?userId=$targetUserId" else "profile"
                    navController.navigate(route)
                },
                onNavigateToPost = { postId -> navController.navigate("post_detail?postId=$postId") }
            )
        }
        composable("profile_edit") {
            ProfileEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true } // Clear entire backstack
                    }
                },
                onNavigateToCreatePost = { postId ->
                    val route = if (postId != null) "create_post?postId=$postId" else "create_post"
                    navController.navigate(route)
                }
            )
        }
        composable("chats") {
            ChatListScreen(
                onNavigateToChatDetail = { roomId, name, image -> 
                    navController.navigate("chat_detail?roomId=$roomId&userName=$name&profileImage=$image") 
                },
                onNavigateToHome = {
                    navController.navigate("home") { popUpTo("home") { inclusive = true } }
                },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToCreatePost = { postId ->
                    val route = if (postId != null) "create_post?postId=$postId" else "create_post"
                    navController.navigate(route)
                },
                onNavigateToNotifications = { navController.navigate("notifications") }
            )
        }
        composable("chat_detail?roomId={roomId}&userName={userName}&profileImage={profileImage}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val userName = backStackEntry.arguments?.getString("userName")
            val profileImage = backStackEntry.arguments?.getString("profileImage")
            ChatDetailScreen(
                roomId = roomId,
                initialUserName = userName,
                initialProfileImage = profileImage,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPost = { postId -> navController.navigate("post_detail?postId=$postId") }
            )
        }
        composable("post_detail?postId={postId}&commentId={commentId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            val commentId = backStackEntry.arguments?.getString("commentId")
            PostDetailScreen(
                postId = postId,
                commentId = commentId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId ->
                    val route = if (userId.isNotEmpty()) "profile?userId=$userId" else "profile"
                    navController.navigate(route)
                }
            )
        }
    }
}
