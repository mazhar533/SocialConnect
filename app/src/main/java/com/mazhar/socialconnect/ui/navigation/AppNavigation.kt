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

@Composable
fun AppNavigation(
    initialPostId: String? = null,
    initialCommentId: String? = null,
    onNavigationHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    
    // Handle initial navigation if coming from notification
    androidx.compose.runtime.LaunchedEffect(initialPostId, currentBackStackEntry) {
        // Wait until NavHost has initialized (currentBackStackEntry is not null) before attempting to navigate
        if (initialPostId != null && auth.currentUser != null && currentBackStackEntry != null) {
            val route = if (initialCommentId != null) {
                "post_detail?postId=$initialPostId&commentId=$initialCommentId"
            } else {
                "post_detail?postId=$initialPostId"
            }
            navController.navigate(route)
            onNavigationHandled()
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
                onNavigateToPost = { postId, commentId -> 
                    if (commentId != null) {
                        navController.navigate("post_detail?postId=$postId&commentId=$commentId")
                    } else {
                        navController.navigate("post_detail?postId=$postId")
                    }
                },
                onNavigateToChats = { navController.navigate("chats") },
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
        composable("profile?userId={userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            ProfileScreen(
                userId = userId,
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
