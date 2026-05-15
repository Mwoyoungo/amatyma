package com.lokaleza.amatyma

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.calls.CometChatCallActivity
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.google.firebase.auth.FirebaseAuth
import com.lokaleza.amatyma.databinding.ActivityIncomingCallBinding

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingCallBinding

    private var callerName = "Unknown"
    private var callType   = "audio"
    private var sessionId  = ""
    private var callerUid  = ""

    private var cometChatReady = false
    private var acceptPending  = false

    companion object {
        private const val TAG = "IncomingCallActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Show on lock screen and wake the display immediately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON  or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        super.onCreate(savedInstanceState)
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Extract call data from the FCM notification intent
        callerName = intent.getStringExtra("caller_name") ?: "Unknown"
        callType   = intent.getStringExtra("call_type")   ?: "audio"
        sessionId  = intent.getStringExtra("session_id")  ?: ""
        callerUid  = intent.getStringExtra("caller_uid")  ?: ""

        // Populate UI immediately — no SDK needed for this
        binding.tvCallerName.text = callerName
        binding.tvCallType.text   = "Incoming ${callType} call"
        binding.avatar.setAvatar(callerName, "")

        binding.btnDecline.setOnClickListener { declineCall() }
        binding.btnAccept.setOnClickListener  { acceptCall()  }

        // Start CometChat init in background so it's ready when user taps Accept
        initCometChat()
    }

    private fun initCometChat() {
        // Already logged in — nothing to do
        if (CometChatUIKit.getLoggedInUser() != null) {
            Log.d(TAG, "CometChat already ready")
            cometChatReady = true
            if (acceptPending) doAccept()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.e(TAG, "No Firebase user — cannot init CometChat")
            return
        }

        Log.d(TAG, "Logging in to CometChat in background: $uid")
        CometChatUIKit.login(uid, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User?) {
                Log.d(TAG, "CometChat ready")
                runOnUiThread {
                    cometChatReady = true
                    if (acceptPending) doAccept()
                }
            }
            override fun onError(e: CometChatException?) {
                Log.e(TAG, "CometChat login failed: ${e?.message}")
                runOnUiThread {
                    if (acceptPending) {
                        Toast.makeText(this@IncomingCallActivity, "Could not connect — try again", Toast.LENGTH_SHORT).show()
                        binding.progress.visibility = android.view.View.GONE
                        binding.btnAccept.isClickable = true
                    }
                }
            }
        })
    }

    private fun acceptCall() {
        binding.btnAccept.isClickable = false

        if (cometChatReady) {
            doAccept()
        } else {
            // Show spinner — CometChat still initializing (rare, takes ~1-2s)
            acceptPending = true
            binding.progress.visibility = android.view.View.VISIBLE

            // Timeout safety — if CometChat takes > 5s, unblock the user
            Handler(Looper.getMainLooper()).postDelayed({
                if (acceptPending) {
                    binding.progress.visibility = android.view.View.GONE
                    binding.btnAccept.isClickable = true
                    acceptPending = false
                    Toast.makeText(this, "Connection slow — tap Accept again", Toast.LENGTH_SHORT).show()
                }
            }, 5000)
        }
    }

    private fun doAccept() {
        acceptPending = false
        binding.progress.visibility = android.view.View.GONE
        Log.d(TAG, "Accepting call: $sessionId")

        CometChat.acceptCall(sessionId, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(call: Call) {
                runOnUiThread {
                    dismissNotification()
                    CometChatCallActivity.launchOutgoingCallScreen(
                        this@IncomingCallActivity, call, null
                    )
                    finish()
                }
            }
            override fun onError(e: CometChatException?) {
                Log.e(TAG, "Accept failed: ${e?.message}")
                runOnUiThread {
                    binding.btnAccept.isClickable = true
                    Toast.makeText(this@IncomingCallActivity, "Call no longer available", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        })
    }

    private fun declineCall() {
        dismissNotification()

        if (sessionId.isNotEmpty()) {
            CometChat.rejectCall(
                sessionId,
                CometChatConstants.CALL_STATUS_REJECTED,
                object : CometChat.CallbackListener<Call>() {
                    override fun onSuccess(call: Call) {}
                    override fun onError(e: CometChatException?) {}
                }
            )
        }
        finish()
    }

    private fun dismissNotification() {
        getSystemService(NotificationManager::class.java)
            ?.cancel(AmatymaFirebaseMessagingService.NOTIFICATION_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissNotification()
    }
}
