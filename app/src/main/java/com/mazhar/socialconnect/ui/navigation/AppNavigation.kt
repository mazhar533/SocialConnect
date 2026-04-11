package com.mazhar.socialconnect.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.mazhar.socialconnect.ui.screens.auth.LoginScreen
import com.mazhar.socialconnect.ui.screens.auth.RecoveryScreen
import com.mazhar.socialconnect.ui.screens.auth.SignUpScreen
import com.mazhar.socialconnect.ui.screens.home.HomeScreen
import com.mazhar.socialconnect.ui.screens.profile.ProfileEditScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    
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
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToSettings = { /* Todo Settings */ }
            )
        }
        composable("profile") {
            com.mazhar.socialconnect.ui.screens.profile.ProfileScreen(
                onNavigateToEditProfile = { navController.navigate("profile_edit") },
                onNavigateToSettings = { /* Todo Settings */ },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
        composable("profile_edit") {
            ProfileEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
