package com.lokaleza.amatyma.voip

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException
import com.cometchat.calls.listeners.CometChatCallsEventsListener
import com.cometchat.calls.model.AudioMode
import com.cometchat.calls.model.CallSwitchRequestInfo
import com.cometchat.calls.model.RTCMutedUser
import com.cometchat.calls.model.RTCRecordingInfo
import com.cometchat.calls.model.RTCUser
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chatuikit.shared.constants.UIKitConstants
import com.lokaleza.amatyma.databinding.ActivityOngoingCallBinding

/**
 * Shown to the recipient after they accept an incoming call. Hosts CometChat
 * UIKit's [com.cometchat.chatuikit.calls.ongoingcall.CometChatOngoingCall]
 * view, which renders the live connected call.
 *
 * Adds the two things the bare sample omitted (which caused audio to keep
 * playing after hang-up):
 *   1. a CallSettingsBuilder event listener that finishes this screen the
 *      moment the call ends (remote hang-up or the on-screen end button), and
 *   2. registering the call view as a lifecycle observer so its WebRTC
 *      session/audio is released when the screen is destroyed.
 */
class OngoingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOngoingCallBinding
    private var finishing = false

    companion object {
        const val EXTRA_SESSION_ID    = "session_id"
        const val EXTRA_RECEIVER_TYPE = "receiver_type"
        const val EXTRA_CALL_TYPE     = "call_type"
        const val EXTRA_WORKFLOW      = "workflow"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Accept can come straight from the native lock-screen call UI, so the
        // connected screen must also be able to appear over the lock screen.
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON   or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityOngoingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        val callType  = intent.getStringExtra(EXTRA_CALL_TYPE) ?: CometChatConstants.CALL_TYPE_AUDIO
        val workflow  = intent.getStringExtra(EXTRA_WORKFLOW) ?: VoipConstants.WORKFLOW_DEFAULT

        if (sessionId.isNullOrEmpty()) { finish(); return }

        val isAudioOnly = callType.equals(CometChatConstants.CALL_TYPE_AUDIO, ignoreCase = true)
        val callWorkFlow = if (workflow == VoipConstants.WORKFLOW_MEETING)
            UIKitConstants.CallWorkFlow.MEETING else UIKitConstants.CallWorkFlow.DEFAULT

        val callSettings = CometChatCalls.CallSettingsBuilder(this)
            .setDefaultLayoutEnable(true)
            .setIsAudioOnly(isAudioOnly)
            .setEventListener(object : CometChatCallsEventsListener {
                override fun onCallEnded() { endAndFinish() }
                override fun onCallEndButtonPressed() { endAndFinish() }
                override fun onUserJoined(user: RTCUser?) {}
                override fun onUserLeft(user: RTCUser?) {}
                override fun onUserListChanged(users: ArrayList<RTCUser>?) {}
                override fun onAudioModeChanged(modes: ArrayList<AudioMode>?) {}
                override fun onCallSwitchedToVideo(info: CallSwitchRequestInfo?) {}
                override fun onUserMuted(info: RTCMutedUser?) {}
                override fun onRecordingToggled(info: RTCRecordingInfo?) {}
                override fun onError(e: CometChatException?) {} // transient — let onCallEnded close the screen
            })

        // v5.2.12 CometChatOngoingCall: session id + call type drive the connected
        // call; there is no setReceiverType (removed since v4).
        binding.ongoingCall.apply {
            setCallWorkFlow(callWorkFlow)
            setSessionId(sessionId)
            setCallType(callType)
            setCallSettingsBuilder(callSettings)
            startCall()
        }

        // Tie the call view to this screen's lifecycle so it releases the
        // WebRTC session/audio when the screen is destroyed.
        lifecycle.addObserver(binding.ongoingCall)
    }

    private fun endAndFinish() {
        if (finishing) return
        finishing = true
        runOnUiThread {
            try { binding.ongoingCall.endCall(true) } catch (_: Exception) {}
            finish()
        }
    }

    override fun onDestroy() {
        // Belt-and-braces: make sure the session is torn down even if the
        // screen is dismissed some other way (back press, system kill).
        try { binding.ongoingCall.endCall(true) } catch (_: Exception) {}
        super.onDestroy()
    }
}
