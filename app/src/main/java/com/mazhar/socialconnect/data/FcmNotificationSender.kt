package com.mazhar.socialconnect.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object FcmNotificationSender {
    // PASTE YOUR SERVER KEY HERE
    private const val SERVER_KEY = "AIzaSyDLM9fWz3iKk4VC0TfWGSFiVhrxgw1Omn8"
    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"

    suspend fun sendNotification(
        targetToken: String,
        title: String,
        body: String,
        imageUrl: String? = null,
        data: Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        if (SERVER_KEY == "YOUR_SERVER_KEY_HERE") {
            Log.e("FcmSender", "Server Key not set!")
            return@withContext
        }

        try {
            val url = URL(FCM_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "key=$SERVER_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonBody = JSONObject()
            jsonBody.put("to", targetToken)
            
            val dataJson = JSONObject()
            dataJson.put("title", "!!! SOCIAL_V3_TEST !!!")
            dataJson.put("body", body)
            dataJson.put("postId", data["postId"] ?: "")
            dataJson.put("commentId", data["commentId"] ?: "")
            dataJson.put("userImage", data["userImage"] ?: "")
            dataJson.put("postContent", data["postContent"] ?: "")
            
            if (imageUrl != null) {
                dataJson.put("image", imageUrl)
            }
            
            jsonBody.put("data", dataJson)
            jsonBody.put("priority", "high")
            jsonBody.put("content_available", true)
            jsonBody.put("mutable_content", true)

            val os = conn.outputStream
            val writer = OutputStreamWriter(os, "UTF-8")
            val finalJson = jsonBody.toString()
            android.util.Log.d("FcmSender", "Final JSON: $finalJson")
            writer.write(finalJson)
            writer.flush()
            writer.close()
            os.close()

            val responseCode = conn.responseCode
            Log.d("FcmSender", "Response Code: $responseCode")
            
            if (responseCode == 200) {
                Log.d("FcmSender", "Notification sent successfully to $targetToken")
            } else {
                val errorStream = conn.errorStream.bufferedReader().readText()
                Log.e("FcmSender", "Error sending notification: $errorStream")
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e("FcmSender", "Exception: ${e.message}")
        }
    }
}
