package com.lokaleza.amatyma

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.google.firebase.auth.FirebaseAuth

/**
 * Handles the inline "Reply" action on a message notification — lets the
 * user answer WhatsApp-style without opening the app, via RemoteInput.
 */
class MessageReplyBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val senderUid = intent.getStringExtra("sender_uid") ?: return
        val notificationId = intent.getIntExtra("notification_id", 0)

        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(AmatymaFirebaseMessagingService.KEY_REPLY_TEXT)
            ?.toString()
            ?.trim()

        if (replyText.isNullOrEmpty()) return

        val pending = goAsync()

        fun send() {
            val textMessage = TextMessage(senderUid, replyText, CometChatConstants.RECEIVER_TYPE_USER)
            CometChat.sendMessage(textMessage, object : CometChat.CallbackListener<TextMessage>() {
                override fun onSuccess(message: TextMessage?) {
                    // Confirms to the user their reply went through
                    context.getSystemService(NotificationManager::class.java)?.cancel(notificationId)
                    pending.finish()
                }
                override fun onError(e: CometChatException?) { pending.finish() }
            })
        }

        if (CometChatUIKit.getLoggedInUser() != null) {
            send()
        } else {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                pending.finish()
                return
            }
            CometChatUIKit.login(uid, object : CometChat.CallbackListener<User>() {
                override fun onSuccess(u: User?) { send() }
                override fun onError(e: CometChatException?) { pending.finish() }
            })
        }
    }
}
