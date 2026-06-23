package com.lokaleza.amatyma.voip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives in-app call lifecycle events (action [VoipConstants.COMETCHAT_CALL_EVENT]).
 * Stage 1: registered but inert — used in Stage 5 to dismiss the native ring
 * when the caller cancels or the call goes unanswered.
 */
class CallEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {}
}
