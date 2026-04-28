package com.mazhar.socialconnect.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazhar.socialconnect.ui.screens.home.CustomBottomNavigationBar
import com.mazhar.socialconnect.ui.theme.*

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToCreatePost: (String?) -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { SettingsViewModel(context) }
    
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val privateAccountEnabled by viewModel.privateAccountEnabled.collectAsState()
    val twoFactorEnabled by viewModel.twoFactorEnabled.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.background(BorderColor, CircleShape).size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryPurple)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Preferences",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
        },
        bottomBar = {
            CustomBottomNavigationBar(
                onHomeClick = onNavigateToHome,
                onEditClick = { onNavigateToCreatePost(null) },
                onProfileClick = onNavigateToProfile,
                onSettingsClick = {}, // We are here
                selectedRoute = "settings"
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            HorizontalDivider(Modifier, DividerDefaults.Thickness, color = BorderColor)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("ACCOUNT CONTROL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryPurpleSoft, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingItem(
                        icon = Icons.Outlined.Notifications,
                        iconTint = OrangeAccent,
                        title = "Notifications",
                        subtitle = "Push, email, SMS alerts",
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications(it) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        thickness = DividerDefaults.Thickness,
                        color = BorderColor
                    )
                    SettingItem(
                        icon = Icons.Outlined.Lock,
                        iconTint = Color(0xFF00C48C),
                        title = "Private Account",
                        subtitle = "Only followers can see your posts",
                        checked = privateAccountEnabled,
                        onCheckedChange = { viewModel.togglePrivateAccount(it) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        thickness = DividerDefaults.Thickness,
                        color = BorderColor
                    )
                    SettingItem(
                        icon = Icons.Outlined.Security,
                        iconTint = Color.Red,
                        title = "Two-Factor Auth",
                        subtitle = "Extra layer of security",
                        checked = twoFactorEnabled,
                        onCheckedChange = { viewModel.toggleTwoFactor(it) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("APP EXPERIENCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryPurpleSoft, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingItem(
                    icon = Icons.Outlined.ModeNight,
                    iconTint = PrimaryPurpleSoft,
                    title = "Dark Mode",
                    subtitle = "Soothe your eyes",
                    checked = darkModeEnabled,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            OutlinedButton(
                onClick = { 
                    viewModel.signOut(onSignOutComplete = onNavigateToLogin)
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red, containerColor = Color(0xFFFFEBEB)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDCD)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = "Sign Out")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out of Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "VIBECONNECT v2.0",
                fontSize = 10.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(TextFieldBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = iconTint)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
            Text(subtitle, fontSize = 12.sp, color = TextGray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryPurpleSoft,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = BorderColor,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
