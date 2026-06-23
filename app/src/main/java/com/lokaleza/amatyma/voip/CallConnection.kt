package com.lokaleza.amatyma.voip

import android.content.Intent
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException

/**
 * A single self-managed telecom Connection representing one incoming call.
 *
 * The native system call screen drives this object: Answer → [onAnswer],
 * Reject → [onReject], hang-up → [onDisconnect]. Each is wired to the
 * matching CometChat call action.
 *
 * Faithful adaptation of CometChat's official sample `CallConnection`,
 * with the in-call screen swapped to CometChat UIKit's own call activity.
 *
 * NOTE: the accept → in-call-screen launch is validated/refined in Stage 4.
 */
class CallConnection(
    private val service: CallConnectionService,
    private val call: Call,
    private val workflow: String = VoipConstants.WORKFLOW_DEFAULT
) : Connection() {

    companion object { private const val TAG = "CallConnection" }

    private val isMeeting = workflow == VoipConstants.WORKFLOW_MEETING

    // Guards against the native UI firing answer/reject callbacks more than once
    private var handled = false

    override fun onCallAudioStateChanged(state: CallAudioState?) {}

    override fun onDisconnect() {
        super.onDisconnect()
        destroyConnection()
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL, VoipConstants.MISSED))
        CometChat.getActiveCall()?.let { rejectActiveCall(it) }
    }

    private fun rejectActiveCall(activeCall: Call) {
        CometChat.rejectCall(
            activeCall.sessionId,
            CometChatConstants.CALL_STATUS_CANCELLED,
            object : CometChat.CallbackListener<Call>() {
                override fun onSuccess(p0: Call?) {}
                override fun onError(e: CometChatException?) { Log.e(TAG, "disconnect error: ${e?.message}") }
            }
        )
    }

    fun destroyConnection() {
        setDisconnected(DisconnectCause(DisconnectCause.REMOTE, VoipConstants.REJECTED))
        super.destroy()
    }

    override fun onAnswer(videoState: Int) = answer()

    override fun onAnswer() = answer()

    private fun answer() {
        if (handled) return
        handled = true
        val sessionId = call.sessionId ?: return
        CometChatVoIPManager.ensureInitialized(service)

        if (isMeeting) {
            // Group/direct calling — no acceptCall; the member just joins the session.
            launchCallScreen(sessionId, call.type)
            destroyConnection()
            return
        }

        CometChat.acceptCall(sessionId, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(accepted: Call) {
                service.sendBroadcast(callEventIntent())
                launchCallScreen(accepted.sessionId, accepted.type)
                destroyConnection()
            }
            override fun onError(e: CometChatException?) {
                destroyConnection()
                Log.e(TAG, "accept error: ${e?.message}")
            }
        })
    }

    private fun launchCallScreen(sessionId: String?, callType: String?) {
        try {
            val intent = Intent(service, OngoingCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(OngoingCallActivity.EXTRA_SESSION_ID, sessionId)
                putExtra(OngoingCallActivity.EXTRA_CALL_TYPE, callType)
                putExtra(OngoingCallActivity.EXTRA_WORKFLOW, workflow)
            }
            service.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "launch ongoing call screen failed: ${e.message}")
        }
    }

    override fun onShowIncomingCallUi() {}
    override fun onHold() {}
    override fun onUnhold() {}

    override fun onReject() {
        if (handled) return
        handled = true
        val sessionId = call.sessionId ?: return

        if (isMeeting) {
            // Direct calling has no per-callee reject — just dismiss the ring locally.
            destroyConnection()
            setDisconnected(DisconnectCause(DisconnectCause.REJECTED, VoipConstants.REJECTED))
            return
        }

        CometChatVoIPManager.ensureInitialized(service)
        CometChat.rejectCall(
            sessionId,
            CometChatConstants.CALL_STATUS_REJECTED,
            object : CometChat.CallbackListener<Call>() {
                override fun onSuccess(p0: Call?) {
                    destroyConnection()
                    setDisconnected(DisconnectCause(DisconnectCause.REJECTED, VoipConstants.REJECTED))
                }
                override fun onError(e: CometChatException?) {
                    destroyConnection()
                    Log.e(TAG, "reject error: ${e?.message}")
                }
            }
        )
    }

    private fun callEventIntent() = Intent(VoipConstants.COMETCHAT_CALL_EVENT).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(VoipConstants.SESSION_ID, call.sessionId)
    }
}
