package com.informatika.sars.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "FCM_Service"
    }
    
    /**
     * Called when message is received.
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            val title = it.title ?: "SARS Notification"
            val body = it.body ?: ""
            showNotification(title, body, remoteMessage.data)
        }

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleNotificationData(remoteMessage.data)
        }
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * InstanceID token is initially generated so this is where you would retrieve
     * the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendTokenToServer(token)
    }

    /**
     * Show notification to user
     */
    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        try {
            val notificationService = NotificationService(this)
            notificationService.showNotification(
                title = title,
                message = message,
                requestId = data["requestId"]?.toLongOrNull()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }

    /**
     * Handle data payload from notification
     * Examples: requestId, status, action
     */
    private fun handleNotificationData(data: Map<String, String>) {
        try {
            val requestId = data["requestId"]?.toLongOrNull()
            val status = data["status"]
            
            Log.d(TAG, "Processing notification data - requestId: $requestId, status: $status")
            
            // Trigger UI refresh or any background task
            // This will be handled by the app when it's brought to foreground
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification data", e)
        }
    }

    /**
     * Send token to server (Supabase) so it can be used for sending push notifications
     */
    private fun sendTokenToServer(token: String) {
        Log.d(TAG, "Sending token to server: $token")
        // This will be called automatically when app starts
        // The token will be saved to SharedPreferences temporarily
        // And sent to Supabase when user logs in (see AuthViewModel)
    }
}
