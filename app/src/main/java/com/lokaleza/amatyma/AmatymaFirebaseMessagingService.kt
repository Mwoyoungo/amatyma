package com.lokaleza.amatyma

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lokaleza.amatyma.voip.CometChatVoIPManager

class AmatymaFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID_MESSAGES = "chat_messages"
        const val KEY_REPLY_TEXT = "key_reply_text"
        private const val MESSAGE_NOTIFICATION_ID_BASE = 20000
        const val CHANNEL_ID_SOCIAL = "social_activity"
        private const val SOCIAL_NOTIFICATION_ID_BASE = 30000

        fun createSocialChannel(nm: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID_SOCIAL,
                        "Amatyma Activity",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Likes, comments, follows and new posts"
                        enableVibration(true)
                        setShowBadge(true)
                    }
                )
            }
        }

        fun createNotificationChannel(nm: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID_MESSAGES,
                        "Chat Messages",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "New message notifications"
                        enableVibration(true)
                        setShowBadge(true)
                        lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                    }
                )
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
        when (data["type"]) {
            "new_message" -> handleNewMessage(data)
            "call"        -> CometChatVoIPManager.handleCallPush(this, data)
            "social"      -> handleSocialNotification(data)
        }
    }

    // ─── New chat message ─────────────────────────────────────────────────────

    private fun handleNewMessage(data: Map<String, String>) {
        val senderUid  = data["senderUid"]  ?: data["conversationId"] ?: return
        val senderName = data["senderName"] ?: "Someone"
        val preview    = data["preview"]    ?: "New message"

        // Already looking at this exact chat — let the in-app UI handle it, no notification
        if (AmatymaApplication.activeConversationId == senderUid) {
            Log.d("FCMService", "Suppressing message notification — chat with $senderUid is open")
            return
        }

        val nm = getSystemService(NotificationManager::class.java)
        createNotificationChannel(nm)
        showMessageNotification(nm, senderUid, senderName, preview)
    }

    private fun showMessageNotification(
        nm: NotificationManager,
        senderUid: String,
        senderName: String,
        preview: String
    ) {
        val notificationId = MESSAGE_NOTIFICATION_ID_BASE + senderUid.hashCode()

        // Tapping the notification opens the chat with this person
        val openChatPending = PendingIntent.getActivity(
            this, senderUid.hashCode(),
            Intent(this, MessagesActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("USER_ID", senderUid)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Reply" inline action — the system shows a text box right on the notification.
        // The typed text is delivered to MessageReplyBroadcastReceiver via RemoteInput,
        // which is why this PendingIntent must stay mutable.
        val remoteInput = RemoteInput.Builder(KEY_REPLY_TEXT).setLabel("Reply").build()
        val replyPending = PendingIntent.getBroadcast(
            this, notificationId,
            Intent(this, MessageReplyBroadcastReceiver::class.java).apply {
                putExtra("sender_uid", senderUid)
                putExtra("notification_id", notificationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send, "Reply", replyPending
        ).addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        val sender = Person.Builder().setName(senderName).setImportant(true).build()
        val messagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName("You").build())
            .addMessage(preview, System.currentTimeMillis(), sender)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(openChatPending)
            .addAction(replyAction)

        nm.notify(notificationId, builder.build())
    }

    // ─── Social activity (likes / comments / follows / replies / new posts) ──────

    private fun handleSocialNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Amatyma"
        val body = data["body"] ?: ""

        val nm = getSystemService(NotificationManager::class.java)
        createSocialChannel(nm)

        // Tap opens the app on the social surface; in-app Alerts handle deep nav.
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, ((data["socialType"] ?: "") + (data["postId"] ?: "")).hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID_SOCIAL)
            .setSmallIcon(R.drawable.ic_stat_amatyma)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)

        nm.notify(SOCIAL_NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 100000).toInt(), builder.build())
    }
}
