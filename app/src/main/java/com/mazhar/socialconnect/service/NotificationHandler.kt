package com.mazhar.socialconnect.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mazhar.socialconnect.MainActivity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

object NotificationHandler {

    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var userSettingsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var notificationsEnabled: Boolean = true

    fun startListening(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            android.util.Log.d("NotificationHandler", "No user logged in, listener not started")
            return
        }
        
        // Remove existing listeners if any
        listenerRegistration?.remove()
        userSettingsListener?.remove()
        
        val db = FirebaseFirestore.getInstance()
        
        // Listen to current user settings for real-time notification preference updates
        userSettingsListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("NotificationHandler", "User settings listen failed: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    notificationsEnabled = snapshot.getBoolean("notificationsEnabled") ?: true
                    android.util.Log.d("NotificationHandler", "Updated local notificationsEnabled setting: $notificationsEnabled")
                }
            }

        android.util.Log.d("NotificationHandler", "Started listening for user: $uid")
        
        // Subtract 5 seconds to catch notifications created exactly at start time
        val startTime = System.currentTimeMillis() - 5000 

        listenerRegistration = db.collection("notifications")
            .whereEqualTo("targetUserId", uid)
            .whereGreaterThan("timestamp", startTime)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("NotificationHandler", "Listen failed: ${e.message}")
                    return@addSnapshotListener
                }

                android.util.Log.d("NotificationHandler", "Change detected: ${snapshot?.documentChanges?.size ?: 0} docs")

                snapshot?.documentChanges?.forEach { dc ->
                    if (dc.type == DocumentChange.Type.ADDED) {
                        if (!notificationsEnabled) {
                            android.util.Log.d("NotificationHandler", "Notifications disabled by user. Skipping notification popup.")
                            return@forEach
                        }
                        val type = dc.document.getString("type") ?: ""
                        val fromName = dc.document.getString("fromUserName") ?: "Someone"
                        
                        android.util.Log.d("NotificationHandler", "New notification added: $type from $fromName")
                        
                        val userImage = dc.document.getString("fromUserProfilePicture")
                        val postImage = dc.document.getString("postImage")
                        val postContent = dc.document.get("postContent")?.toString() 
                            ?: dc.document.get("content")?.toString() 
                            ?: dc.document.get("text")?.toString()
                        
                        val title = if (type == "message") "$fromName (Chat)" else "SocialConnect"
                        val message = when (type) {
                            "like" -> "$fromName liked your post"
                            "comment" -> "$fromName commented on your post"
                            "follow" -> "$fromName started following you"
                            "follow_request" -> "$fromName sent you a follow request"
                            "follow_accept" -> "$fromName accepted your follow request"
                            "message" -> postContent ?: "Sent you a message"
                            "share" -> "$fromName shared a post with you"
                            else -> "$fromName notified you"
                        }
                        
                        val postId = dc.document.getString("postId")
                        val commentId = dc.document.getString("commentId")
                        val fromUserId = dc.document.getString("fromUserId")
                        
                        showNotification(context, title, message, userImage, postImage, postContent, postId, commentId, type, fromUserId)
                    }
                }
            }
    }

    private fun showNotification(
        context: Context, 
        title: String, 
        message: String,
        userImage: String? = null,
        postImage: String? = null,
        postContent: String? = null,
        postId: String? = null,
        commentId: String? = null,
        type: String? = null,
        fromUserId: String? = null
    ) {
        val coroutineScope = kotlinx.coroutines.MainScope()
        coroutineScope.launch(Dispatchers.Main) {
            val bigPicture = if (!postImage.isNullOrEmpty()) downloadBitmap(postImage) else null
            
            val channelId = "social_connect_notifications"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Social Connect Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Activity notifications"
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("postId", postId)
                putExtra("commentId", commentId)
                putExtra("type", type)
                putExtra("fromUserId", fromUserId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, System.currentTimeMillis().toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val previewContent = if (!postContent.isNullOrEmpty() && type != "message") {
                if (postContent.length > 40) postContent.take(40) + "..." else postContent
            } else null

            val builder = NotificationCompat.Builder(context, channelId)
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
}
