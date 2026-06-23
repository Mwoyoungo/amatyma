package com.lokaleza.amatyma.voip

import android.net.Uri
import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import android.view.Surface
import com.cometchat.chat.core.Call

/**
 * The Android Telecom entry point. When we hand an incoming call to the
 * system (via TelecomManager.addNewIncomingCall in Stage 3), the OS calls
 * back here to create the [CallConnection] that backs the native call UI.
 *
 * Faithful adaptation of CometChat's official sample `CallConnectionService`.
 */
class CallConnectionService : ConnectionService() {

    companion object {
        private const val TAG = "CallConnectionService"
        @JvmStatic var conn: CallConnection? = null
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection? {
        val bundle = request?.extras ?: return null

        val sessionId   = bundle.getString(VoipConstants.SESSION_ID)
        val name        = bundle.getString(VoipConstants.NAME)
        val type        = bundle.getString(VoipConstants.TYPE)
        val rawCallType = bundle.getString(VoipConstants.CALL_TYPE) ?: "audio"
        val callType    = rawCallType.replaceFirstChar { it.uppercase() }
        val receiverUID = bundle.getString(VoipConstants.RECEIVER_ID)
        val workflow    = bundle.getString(VoipConstants.CALL_WORKFLOW) ?: VoipConstants.WORKFLOW_DEFAULT

        if (receiverUID == null || type == null) {
            Log.e(TAG, "Missing receiver/type in incoming connection extras")
            return null
        }

        val call = Call(receiverUID, type, callType).apply { this.sessionId = sessionId }
        val connection = CallConnection(this, call, workflow)
        conn = connection

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            connection.connectionProperties = Connection.PROPERTY_SELF_MANAGED
        }
        connection.setCallerDisplayName(name, TelecomManager.PRESENTATION_ALLOWED)
        connection.setAddress(Uri.parse("$callType Call"), TelecomManager.PRESENTATION_ALLOWED)
        connection.setInitializing()
        connection.videoProvider = noOpVideoProvider()
        connection.setActive()
        return connection
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
        Log.e(TAG, "onCreateIncomingConnectionFailed")
    }

    private fun noOpVideoProvider() = object : Connection.VideoProvider() {
        override fun onSetCamera(cameraId: String?) {}
        override fun onSetPreviewSurface(surface: Surface?) {}
        override fun onSetDisplaySurface(surface: Surface?) {}
        override fun onSetDeviceOrientation(rotation: Int) {}
        override fun onSetZoom(value: Float) {}
        override fun onSendSessionModifyRequest(fromProfile: VideoProfile?, toProfile: VideoProfile?) {}
        override fun onSendSessionModifyResponse(responseProfile: VideoProfile?) {}
        override fun onRequestCameraCapabilities() {}
        override fun onRequestConnectionDataUsage() {}
        override fun onSetPauseImage(uri: Uri?) {}
    }
}
