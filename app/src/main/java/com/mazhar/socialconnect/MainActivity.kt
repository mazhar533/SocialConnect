package com.mazhar.socialconnect

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mazhar.socialconnect.ui.navigation.AppNavigation
import com.mazhar.socialconnect.ui.theme.SocialConnectTheme
import kotlinx.coroutines.flow.MutableStateFlow
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    
    private var preferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private val darkModeFlow = MutableStateFlow(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            updateFcmToken()
        }
    }

    private val initialPostId = MutableStateFlow<String?>(null)
    private val initialCommentId = MutableStateFlow<String?>(null)
    private val initialNotificationType = MutableStateFlow<String?>(null)
    private val initialFromUserId = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        createNotificationChannel()
        handleIntent(intent)
        
        val prefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        darkModeFlow.value = prefs.getBoolean("darkMode", false)
        
        preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "darkMode") {
                darkModeFlow.value = p.getBoolean("darkMode", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        askNotificationPermission()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                AndroidColor.parseColor("#363273")
            )
        )
        
        setContent {
            val isDarkMode by darkModeFlow.collectAsState()
            val postId by initialPostId.collectAsState()
            val commentId by initialCommentId.collectAsState()
            val notificationType by initialNotificationType.collectAsState()
            val fromUserId by initialFromUserId.collectAsState()
            
            SocialConnectTheme(darkTheme = isDarkMode, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        initialPostId = postId, 
                        initialCommentId = commentId,
                        initialNotificationType = notificationType,
                        initialFromUserId = fromUserId,
                        onNavigationHandled = {
                            initialPostId.value = null
                            initialCommentId.value = null
                            initialNotificationType.value = null
                            initialFromUserId.value = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent) {
        android.util.Log.d("MainActivity", "Handling Intent: $intent")
        intent.extras?.let { bundle ->
            bundle.keySet().forEach { key ->
                android.util.Log.d("MainActivity", "Intent Extra: $key = ${bundle.get(key)}")
            }
        }
        
        val postId = intent.getStringExtra("postId")
        val commentId = intent.getStringExtra("commentId")
        val type = intent.getStringExtra("type") ?: intent.getStringExtra("notificationType")
        val fromUserId = intent.getStringExtra("fromUserId")
        
        android.util.Log.d("MainActivity", "Extracted postId: $postId, commentId: $commentId, type: $type, fromUserId: $fromUserId")
        
        if (!postId.isNullOrEmpty()) {
            initialPostId.value = postId
            initialCommentId.value = if (commentId.isNullOrEmpty()) null else commentId
        } else if (!type.isNullOrEmpty()) {
            initialNotificationType.value = type
            initialFromUserId.value = fromUserId
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val prefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                updateFcmToken()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            updateFcmToken()
        }
    }

    private fun updateFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("fcmToken", token)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "social_connect_v2"
            val channelName = "Social Connect V2"
            val channelDescription = "Social activity notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            android.util.Log.d("MainActivity", "Notification channel created: $channelId")
        }
    }
}