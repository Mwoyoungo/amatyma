package com.lokaleza.amatyma

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AmatymaFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "incoming_calls_v2"   // v2 forces channel recreation on existing installs
        const val NOTIFICATION_ID = 9001
        private const val RC_FULLSCREEN = 9010
        private const val RC_ACCEPT    = 9011
        private const val RC_DECLINE   = 9012

        fun createNotificationChannel(nm: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Full-screen ringing for incoming calls"
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(false)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
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

        val nm = getSystemService(NotificationManager::class.java)
        createNotificationChannel(nm)

        // Wake the screen so the full-screen intent can fire
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "amatyma:IncomingCall"
        )
        wl.acquire(60_000L)

        // Try direct activity start first (works if exemption granted)
        try {
            startActivity(buildCallIntent(callerName, callType, sessionId, callerUid))
            Log.d("FCMService", "IncomingCallActivity launched directly")
        } catch (e: Exception) {
            Log.e("FCMService", "startActivity blocked — falling back to notification: ${e.message}")
        }

        wl.release()

        showCallNotification(nm, callerName, callType, sessionId, callerUid)
    }

    // ─── Intent builders ──────────────────────────────────────────────────────

    private fun buildCallIntent(
        callerName: String,
        callType: String,
        sessionId: String,
        callerUid: String,
        autoAccept: Boolean = false
    ) = Intent(this, IncomingCallActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("caller_name", callerName)
        putExtra("call_type",   callType)
        putExtra("session_id",  sessionId)
        putExtra("caller_uid",  callerUid)
        putExtra("auto_accept", autoAccept)
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun showCallNotification(
        nm: NotificationManager,
        callerName: String,
        callType: String,
        sessionId: String,
        callerUid: String
    ) {
        // Tapping anywhere on the notification opens the call screen
        val fullScreenPending = PendingIntent.getActivity(
            this, RC_FULLSCREEN,
            buildCallIntent(callerName, callType, sessionId, callerUid),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Green "Accept" button → opens IncomingCallActivity and auto-accepts
        val acceptPending = PendingIntent.getActivity(
            this, RC_ACCEPT,
            buildCallIntent(callerName, callType, sessionId, callerUid, autoAccept = true),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Red "Decline" button → BroadcastReceiver rejects in background (no UI shown)
        val declinePending = PendingIntent.getBroadcast(
            this, RC_DECLINE,
            Intent(this, CallDeclineBroadcastReceiver::class.java)
                .putExtra("session_id", sessionId),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callIcon = if (callType == "video") android.R.drawable.ic_menu_camera
                       else android.R.drawable.ic_menu_call

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(callIcon)
            .setContentTitle("Incoming $callType call")
            .setContentText("$callerName is calling you")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPending, true)
            .setContentIntent(fullScreenPending)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ — CallStyle gives the WhatsApp-style large call card
            // with a prominent green Accept and red Decline button
            val caller = Person.Builder()
                .setName(callerName)
                .setImportant(true)
                .build()
            builder.setStyle(
                NotificationCompat.CallStyle
                    .forIncomingCall(caller, declinePending, acceptPending)
            )
        } else {
            // Pre-Android 12 fallback — explicit action buttons
            builder
                .addAction(android.R.drawable.ic_delete,     "Decline", declinePending)
                .addAction(android.R.drawable.ic_menu_call,  "Accept",  acceptPending)
        }

        nm.notify(NOTIFICATION_ID, builder.build())
    }
}
