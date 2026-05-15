package com.lokaleza.amatyma

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AmatymaFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "incoming_calls"
        const val NOTIFICATION_ID = 9001
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save refreshed token to Firestore
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .update("fcmTokens", FieldValue.arrayUnion(token))
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        if (data["type"] != "incoming_call") return

        val callerName = data["callerName"] ?: "Someone"
        val callType   = data["callType"]   ?: "audio"
        val sessionId  = data["sessionId"]  ?: ""
        val callerUid  = data["callerUid"]  ?: ""

        createNotificationChannel()
        showIncomingCallNotification(callerName, callType, sessionId, callerUid)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ringing notifications for incoming calls"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun showIncomingCallNotification(
        callerName: String,
        callType: String,
        sessionId: String,
        callerUid: String
    ) {
        // Full-screen intent — opens IncomingCallActivity directly
        val openAppIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("caller_name", callerName)
            putExtra("call_type",   callType)
            putExtra("session_id",  sessionId)
            putExtra("caller_uid",  callerUid)
        }

        val fullScreenPending = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callIcon = if (callType == "video")
            android.R.drawable.ic_menu_camera
        else
            android.R.drawable.ic_menu_call

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(callIcon)
            .setContentTitle("Incoming ${callType} call")
            .setContentText("$callerName is calling you")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPending, true)
            .setContentIntent(fullScreenPending)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }
}
