package com.lokaleza.amatyma

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.google.firebase.auth.FirebaseAuth

class CallDeclineBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra("session_id") ?: return

        // Dismiss the notification immediately — the user already said no
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(AmatymaFirebaseMessagingService.NOTIFICATION_ID)

        // Reject the call in the background. goAsync() gives us ~10s before
        // the system considers the broadcast timed out.
        val pending = goAsync()

        fun reject() {
            CometChat.rejectCall(
                sessionId,
                CometChatConstants.CALL_STATUS_REJECTED,
                object : CometChat.CallbackListener<Call>() {
                    override fun onSuccess(c: Call) { pending.finish() }
                    override fun onError(e: CometChatException?) { pending.finish() }
                }
            )
        }

        if (CometChatUIKit.getLoggedInUser() != null) {
            reject()
        } else {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                pending.finish()
                return
            }
            CometChatUIKit.login(uid, object : CometChat.CallbackListener<User>() {
                override fun onSuccess(u: User?) { reject() }
                override fun onError(e: CometChatException?) { pending.finish() }
            })
        }
    }
}
