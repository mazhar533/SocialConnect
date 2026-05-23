package com.mazhar.socialconnect.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazhar.socialconnect.data.FcmNotificationSender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.mazhar.socialconnect.ui.components.CustomToastManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility


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
    val postNotificationsEnabled by viewModel.postNotificationsEnabled.collectAsState()
    val chatNotificationsEnabled by viewModel.chatNotificationsEnabled.collectAsState()
    val privateAccountEnabled by viewModel.privateAccountEnabled.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()

    var isNotificationsExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEmailChangeDialog by remember { mutableStateOf(false) }
    var newEmailText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back", 
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "SocialConnect",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Cursive,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Preferences",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            HorizontalDivider(Modifier, DividerDefaults.Thickness, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("ACCOUNT CONTROL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingItem(
                        icon = Icons.Outlined.Notifications,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "General Notifications",
                        subtitle = "Enable or disable all notifications",
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications(it) },
                        isExpandable = notificationsEnabled,
                        isExpanded = isNotificationsExpanded,
                        onExpandToggle = {
                            if (notificationsEnabled) {
                                isNotificationsExpanded = !isNotificationsExpanded
                            }
                        }
                    )
                    if (notificationsEnabled && isNotificationsExpanded) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            thickness = DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(modifier = Modifier.padding(start = 16.dp)) {
                            SettingItem(
                                icon = Icons.Outlined.FavoriteBorder,
                                iconTint = Color(0xFFE91E63),
                                title = "Post Notifications",
                                subtitle = "Likes, comments, shares, follows",
                                checked = postNotificationsEnabled,
                                onCheckedChange = { viewModel.togglePostNotifications(it) },
                                isSmall = true
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            thickness = DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(modifier = Modifier.padding(start = 16.dp)) {
                            SettingItem(
                                icon = Icons.Outlined.Email,
                                iconTint = Color(0xFF2196F3),
                                title = "Chat Notifications",
                                subtitle = "Direct message push notifications",
                                checked = chatNotificationsEnabled,
                                onCheckedChange = { viewModel.toggleChatNotifications(it) },
                                isSmall = true
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outline
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
                        color = MaterialTheme.colorScheme.outline
                    )
                    ClickableSettingItem(
                        icon = Icons.Default.Email,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Change Email",
                        subtitle = "Update your login and profile email",
                        onClick = {
                            newEmailText = ""
                            showEmailChangeDialog = true
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("APP EXPERIENCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingItem(
                    icon = Icons.Outlined.ModeNight,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "Dark Mode",
                    subtitle = "Soothe your eyes",
                    checked = darkModeEnabled,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("ABOUT & SUPPORT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ExpandableSection(
                        title = "Frequently Asked Questions",
                        icon = Icons.Outlined.Info,
                        iconTint = MaterialTheme.colorScheme.primary
                    ) {
                        FAQList()
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outline
                    )
                    ExpandableSection(
                        title = "Privacy Policy",
                        icon = Icons.Outlined.Security,
                        iconTint = Color(0xFF00C48C)
                    ) {
                        PrivacyPolicyText()
                    }
                }
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { 
                    showDeleteConfirmation = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Account")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete My Account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                "SOCIALCONNECT v2.0",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "Delete Account",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete your account? This action is irreversible. All your posts, uploaded images, profile details, and notification history will be permanently deleted.",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteAccount(
                            onComplete = {
                                CustomToastManager.success("Account deleted successfully")
                                onNavigateToLogin()
                            },
                            onError = { errorMessage ->
                                CustomToastManager.error(errorMessage)
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEmailChangeDialog) {
        AlertDialog(
            onDismissRequest = { showEmailChangeDialog = false },
            title = {
                Text(
                    text = "Change Email Address",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your new email address. This will update your login email and profile.",
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = newEmailText,
                        onValueChange = { newEmailText = it },
                        label = { Text("New Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val email = newEmailText.trim()
                        if (email.isEmpty()) {
                            CustomToastManager.error("Email cannot be empty")
                            return@TextButton
                        }
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            CustomToastManager.error("Please enter a valid email address")
                            return@TextButton
                        }
                        showEmailChangeDialog = false
                        viewModel.changeEmail(
                            newEmail = email,
                            onComplete = {
                                CustomToastManager.success("Verification link sent! Verify to complete update.")
                            },
                            onError = { error ->
                                CustomToastManager.error(error)
                            }
                        )
                    }
                ) {
                    Text("Update", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEmailChangeDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isExpandable: Boolean = false,
    isExpanded: Boolean = false,
    onExpandToggle: (() -> Unit)? = null,
    isSmall: Boolean = false
) {
    val verticalPadding = if (isSmall) 8.dp else 16.dp
    val boxSize = if (isSmall) 36.dp else 48.dp
    val titleSize = if (isSmall) 14.sp else 16.sp
    val subtitleSize = if (isSmall) 11.sp else 12.sp
    val iconSize = if (isSmall) 18.dp else 24.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isExpandable && onExpandToggle != null) {
                    Modifier.clickable { onExpandToggle() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 24.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(boxSize)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                contentDescription = title, 
                tint = iconTint, 
                modifier = if (isSmall) Modifier.size(iconSize) else Modifier
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = titleSize, 
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (isExpandable && onExpandToggle != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(subtitle, fontSize = subtitleSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFE65100), // Deep/Dark Orange
                checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun ClickableSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = iconTint)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconTint)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun FAQList() {
    val faqs = listOf(
        "How do I verify my email?" to "Go to your profile page. Under your email, tap the red badge that says 'Unverified (Click to verify)'. A verification link will be sent to your email. Click that link and restart or return to the app to see the green 'Verified' badge.",
        "Can I change my email address?" to "Yes, tap the 'Change Email' option in Settings under 'Account Control' to update your email. If Firebase Auth requires a recent login, please sign out and sign back in before changing it.",
        "How do I delete my account?" to "Go to Settings and click on the 'Delete My Account' button. Note that deleting your account is irreversible and will delete all your posts, storage files (profile images and post images), messages, and database records.",
        "How does Private Account work?" to "When you enable 'Private Account' under Settings, only approved followers will be able to see your posts and profile activity. You can toggle this setting anytime."
    )
    Column {
        faqs.forEachIndexed { index, faq ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = faq.first,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = faq.second,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
            if (index < faqs.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun PrivacyPolicyText() {
    Text(
        text = "At SocialConnect, your privacy is our top priority. We collect and process user data including profile information, uploaded posts, images, and interactions solely to provide and improve the social networking experience.\n\n" +
                "1. Data Storage: All posts, messages, and user information are securely stored using Google Firebase Firestore and Authentication.\n\n" +
                "2. Image Uploads: Profile and post images are stored in Firebase Storage. If you choose to delete your account, all your images and documents are permanently deleted from our servers.\n\n" +
                "3. Third-Party Services: We do not share, sell, or distribute your personal details to third-party services. Notification services use secure tokens (FCM) to deliver notifications directly to your device.\n\n" +
                "For further inquiries, please contact our support team.",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 18.sp
    )
}
