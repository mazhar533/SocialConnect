package com.mazhar.socialconnect.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import com.google.firebase.FirebaseApp
import com.google.auth.oauth2.GoogleCredentials

object FcmNotificationSender {

    suspend fun sendNotification(
        targetToken: String,
        title: String,
        body: String,
        imageUrl: String? = null,
        data: Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        var accessToken: String? = null
        var projectId: String? = null

        try {
            val context = FirebaseApp.getInstance().applicationContext
            val jsonString = context.assets.open("service-account.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            projectId = jsonObject.optString("project_id")

            val credentials = GoogleCredentials.fromStream(jsonString.byteInputStream())
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            credentials.refreshIfExpired()
            accessToken = credentials.accessToken.tokenValue
        } catch (e: Exception) {
            Log.e("FcmSender", "Failed to load service-account.json from assets: ${e.localizedMessage}")
            Log.e("FcmSender", "Please make sure app/src/main/assets/service-account.json exists with valid service account credentials.")
            return@withContext
        }

        if (accessToken.isNullOrEmpty() || projectId.isNullOrEmpty()) {
            Log.e("FcmSender", "AccessToken or Project ID is null/empty")
            return@withContext
        }

        try {
            val fcmUrl = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"
            val url = URL(fcmUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonBody = JSONObject()
            val messageJson = JSONObject()
            messageJson.put("token", targetToken)

            // Notification section
            val notificationJson = JSONObject()
            notificationJson.put("title", title)
            notificationJson.put("body", body)
            if (!imageUrl.isNullOrEmpty()) {
                notificationJson.put("image", imageUrl)
            }
            messageJson.put("notification", notificationJson)

            // Android specific configuration for high priority delivery when app is closed/backgrounded
            val androidJson = JSONObject()
            androidJson.put("priority", "HIGH")
            messageJson.put("android", androidJson)

            // Data section for background message handling & extra details
            val dataJson = JSONObject()
            dataJson.put("title", title)
            dataJson.put("body", body)
            dataJson.put("postId", data["postId"] ?: "")
            dataJson.put("commentId", data["commentId"] ?: "")
            dataJson.put("userImage", data["userImage"] ?: "")
            dataJson.put("postContent", data["postContent"] ?: "")
            if (!imageUrl.isNullOrEmpty()) {
                dataJson.put("image", imageUrl)
            }
            messageJson.put("data", dataJson)

            jsonBody.put("message", messageJson)

            val os = conn.outputStream
            val writer = OutputStreamWriter(os, "UTF-8")
            val finalJson = jsonBody.toString()
            Log.d("FcmSender", "Final HTTP v1 JSON: $finalJson")
            writer.write(finalJson)
            writer.flush()
            writer.close()
            os.close()

            val responseCode = conn.responseCode
            Log.d("FcmSender", "Response Code: $responseCode")
            
            if (responseCode == 200) {
                Log.d("FcmSender", "Notification sent successfully to $targetToken via HTTP v1")
            } else {
                val errorStream = conn.errorStream.bufferedReader().readText()
                Log.e("FcmSender", "Error sending HTTP v1 notification: $errorStream")
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e("FcmSender", "Exception in sendNotification: ${e.message}")
        }
    }
}
