package com.mazhar.socialconnect.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mazhar.socialconnect.MainActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.URL
import kotlinx.coroutines.Dispatchers

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        android.util.Log.d("FCM_SERVICE", "Message Received! Data: ${remoteMessage.data}")
        
        val prefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications", true)
        if (!notificationsEnabled) {
            android.util.Log.d("FCM_SERVICE", "Notifications disabled in settings. Skipping FCM notification display.")
            return
        }

        remoteMessage.notification?.let {
            val title = it.title ?: "SocialConnect"
            val body = it.body ?: ""
            val imageUrl = it.imageUrl?.toString() ?: remoteMessage.data["image"]
            val userImage = remoteMessage.data["userImage"]
            val postContent = remoteMessage.data["postContent"]
            val postId = remoteMessage.data["postId"]
            val commentId = remoteMessage.data["commentId"]
            val type = remoteMessage.data["type"]
            val fromUserId = remoteMessage.data["fromUserId"]
            showNotification(title, body, imageUrl, userImage, postContent, postId, commentId, type, fromUserId)
        } ?: remoteMessage.data["title"]?.let { title ->
            val body = remoteMessage.data["body"] ?: ""
            val imageUrl = remoteMessage.data["image"]
            val userImage = remoteMessage.data["userImage"]
            val postContent = remoteMessage.data["postContent"]
            val postId = remoteMessage.data["postId"]
            val commentId = remoteMessage.data["commentId"]
            val type = remoteMessage.data["type"]
            val fromUserId = remoteMessage.data["fromUserId"]
            
            android.util.Log.d("FCM_SERVICE", "Showing custom notification: postId=$postId")
            
            showNotification(title, body, imageUrl, userImage, postContent, postId, commentId, type, fromUserId)
        }
    }

    private fun showNotification(
        title: String, 
        message: String, 
        imageUrl: String? = null,
        userImage: String? = null,
        postContent: String? = null,
        postId: String? = null,
        commentId: String? = null,
        type: String? = null,
        fromUserId: String? = null
    ) {
        val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        coroutineScope.launch {
            val bigPicture = if (!imageUrl.isNullOrEmpty()) downloadBitmap(imageUrl) else null
            val channelId = "social_connect_v2"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Social Connect V2",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Social activity notifications"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(this@MyFirebaseMessagingService, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("postId", postId)
                putExtra("commentId", commentId)
                putExtra("type", type)
                putExtra("fromUserId", fromUserId)
            }
            val pendingIntent = PendingIntent.getActivity(
                this@MyFirebaseMessagingService, System.currentTimeMillis().toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val previewContent = if (!postContent.isNullOrEmpty() && type != "message") {
                if (postContent.length > 40) postContent.take(40) + "..." else postContent
            } else null

            val builder = NotificationCompat.Builder(this@MyFirebaseMessagingService, channelId)
                .setContentTitle(title)
                .setContentText(if (previewContent != null) "$message: \"$previewContent\"" else message)
                .setSmallIcon(com.mazhar.socialconnect.R.drawable.applogo)
                .setColor(android.graphics.Color.parseColor("#E65100"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

            if (bigPicture != null) {
                builder.setStyle(NotificationCompat.BigPictureStyle()
                    .bigPicture(bigPicture)
                    .setSummaryText(if (previewContent != null) "$message: $previewContent" else message))
            } else if (type == "message") {
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
            } else if (!postContent.isNullOrEmpty()) {
                builder.setStyle(NotificationCompat.BigTextStyle().bigText("$message:\n$postContent"))
            }

            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    private suspend fun downloadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.doInput = true
            connection.connect()
            val input = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("fcmToken", token)
        }
    }
}
