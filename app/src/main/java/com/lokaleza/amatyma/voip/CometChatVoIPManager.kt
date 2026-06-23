package com.lokaleza.amatyma.voip

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import androidx.core.content.ContextCompat
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings
import com.lokaleza.amatyma.AmatymaApplication

/**
 * Owns the Android Telecom phone account that lets us show the native
 * system incoming-call UI (full screen, over the lock screen) the same
 * way WhatsApp does.
 *
 * Mirrors the phone-account registration in CometChat's official
 * push-notification sample (CometChatNotification's constructor).
 *
 * Stage 1: registration + init safety net only.
 * The incoming-call trigger (addNewIncomingCall) is added in Stage 3.
 */
object CometChatVoIPManager {

    private const val TAG = "CometChatVoIPManager"

    @Volatile
    private var phoneAccountHandle: PhoneAccountHandle? = null

    /**
     * Registers our self-managed phone account with the system Telecom
     * service. Safe to call repeatedly — registering an existing account
     * simply updates it. Call once on app start.
     */
    fun registerPhoneAccount(context: Context) {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val handle = phoneAccountHandle(context)

            // Use the app's display name ("Amatyma") as the account label so it
            // shows nicely in the system Calling-accounts screen and call UI.
            val label = context.applicationInfo.loadLabel(context.packageManager).toString()

            val phoneAccount = PhoneAccount.builder(handle, label)
                .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                .build()

            telecomManager.registerPhoneAccount(phoneAccount)
            Log.d(TAG, "Phone account registered as \"$label\"")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register phone account: ${e.message}")
        }
    }

    /**
     * True if our calling account is registered AND enabled by the user
     * (the toggle in the system Calling-accounts screen). Requires
     * READ_PHONE_STATE — returns false if that hasn't been granted yet.
     */
    @SuppressLint("MissingPermission")
    fun isPhoneAccountEnabled(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) return false

        return try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecomManager.callCapablePhoneAccounts.any {
                it.componentName.className == CallConnectionService::class.java.canonicalName
            }
        } catch (e: Exception) {
            Log.e(TAG, "isPhoneAccountEnabled check failed: ${e.message}")
            false
        }
    }

    /**
     * Opens the system "Calling accounts" screen so the user can flip on the
     * Amatyma account. Mirrors CometChat's official sample (launchVoIPSetting),
     * with a graceful fallback if the telecom settings component is missing.
     */
    fun openCallingAccountsSettings(context: Context) {
        try {
            val intent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS).apply {
                component = ComponentName(
                    "com.android.server.telecom",
                    "com.android.server.telecom.settings.EnableAccountPreferenceActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't open calling-accounts settings directly: ${e.message}")
            // Fallback — general call settings
            try {
                context.startActivity(
                    Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback settings launch failed: ${e2.message}")
            }
        }
    }

    fun phoneAccountHandle(context: Context): PhoneAccountHandle {
        return phoneAccountHandle ?: PhoneAccountHandle(
            ComponentName(context, CallConnectionService::class.java),
            context.packageName
        ).also { phoneAccountHandle = it }
    }

    /**
     * Safety net for the killed-app case: if a telecom callback runs before
     * CometChat has finished initializing, re-init with the same credentials
     * the Application uses. Normally the Application has already done this.
     */
    fun ensureInitialized(context: Context) {
        if (CometChat.isInitialized()) return

        val settings = UIKitSettings.UIKitSettingsBuilder()
            .setRegion(AmatymaApplication.REGION)
            .setAppId(AmatymaApplication.APP_ID)
            .setAuthKey(AmatymaApplication.AUTH_KEY)
            .subscribePresenceForAllUsers()
            .build()

        CometChatUIKit.init(context, settings, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(p0: String?) { Log.d(TAG, "CometChat re-initialized (telecom path)") }
            override fun onError(p0: CometChatException?) { Log.e(TAG, "CometChat re-init failed: ${p0?.message}") }
        })
    }

    // ─── Incoming call from FCM push ──────────────────────────────────────────

    /**
     * Entry point from the FCM service for a `type:"call"` push. Mirrors
     * CometChat's official `handleCallNotification`: only a fresh *initiated*
     * call (within 30s) rings; anything else stops a ringing/active call.
     */
    fun handleCallPush(context: Context, data: Map<String, String>) {
        val callAction = data[VoipConstants.CALL_ACTION] ?: return
        val sentAt = data[VoipConstants.SENT_AT]?.toLongOrNull() ?: 0L

        val isFreshInitiated = callAction == CometChatConstants.CALL_STATUS_INITIATED &&
            System.currentTimeMillis() <= sentAt + 30_000L

        if (isFreshInitiated) {
            val sessionId    = data[VoipConstants.SESSION_ID] ?: return
            val receiver     = data[VoipConstants.RECEIVER] ?: return
            val receiverType = data[VoipConstants.RECEIVER_TYPE] ?: CometChatConstants.RECEIVER_TYPE_USER
            val callType     = data[VoipConstants.CALL_TYPE] ?: CometChatConstants.CALL_TYPE_AUDIO
            val workflow     = data[VoipConstants.CALL_WORKFLOW] ?: VoipConstants.WORKFLOW_DEFAULT

            ensureInitialized(context)

            // Already on a call → reject the newcomer with "busy", don't ring.
            // (Group/meeting calls have no per-callee reject, so skip the busy reject for them.)
            if (isInCall(context)) {
                Log.d(TAG, "Already in a call — declining $sessionId as busy")
                if (workflow != VoipConstants.WORKFLOW_MEETING) rejectBusy(sessionId)
                return
            }

            val call = Call(receiver, receiverType, callType).apply { this.sessionId = sessionId }
            val caller = User().apply {
                uid    = receiver
                name   = data[VoipConstants.RECEIVER_NAME] ?: "Amatyma"
                avatar = data[VoipConstants.RECEIVER_AVATAR] ?: ""
            }
            call.callInitiator = caller
            call.callReceiver  = caller

            startIncomingCall(context, call, workflow)
        } else {
            // Caller hung up / call cancelled before answer → stop the ringing
            endRingingCall(context)
        }
    }

    private fun rejectBusy(sessionId: String) {
        CometChat.rejectCall(
            sessionId,
            CometChatConstants.CALL_STATUS_BUSY,
            object : CometChat.CallbackListener<Call>() {
                override fun onSuccess(p0: Call?) {}
                override fun onError(e: CometChatException?) { Log.e(TAG, "busy reject failed: ${e?.message}") }
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun isInCall(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) return false
        return try {
            (context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager).isInCall
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Tears down a ringing native call (caller cancelled before answer).
     * Disconnects our telecom connection directly, then falls back to ending
     * via the Telecom manager.
     */
    fun endRingingCall(context: Context) {
        try {
            CallConnectionService.conn?.destroyConnection()
        } catch (e: Exception) {
            Log.e(TAG, "endRingingCall destroy failed: ${e.message}")
        }
        CallConnectionService.conn = null
        endActiveCallIfAny(context)
    }

    /** Hands the call to Android's native call screen via the Telecom framework. */
    @SuppressLint("MissingPermission")
    private fun startIncomingCall(context: Context, call: Call, workflow: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.MANAGE_OWN_CALLS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "MANAGE_OWN_CALLS not granted — cannot show native call screen")
            return
        }

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val handle = phoneAccountHandle(context)
        val sessionId = call.sessionId ?: return
        val entity = call.callInitiator

        // A fake tel: address keeps the Telecom framework happy; the real
        // identity is the session id + the display name we attach below.
        val telDigits = sessionId.take(11)
        val address = Uri.fromParts(PhoneAccount.SCHEME_TEL, telDigits, null)

        val displayName = (entity as? User)?.name ?: (entity as? Group)?.name ?: "Amatyma"
        val displayId   = (entity as? User)?.uid ?: (entity as? Group)?.guid ?: ""

        val extras = Bundle().apply {
            putString(VoipConstants.SESSION_ID, sessionId)
            putString(VoipConstants.TYPE, call.receiverType)
            putString(VoipConstants.CALL_TYPE, call.type)
            putString(VoipConstants.CALL_WORKFLOW, workflow)
            putString(VoipConstants.NAME, displayName)
            putString(VoipConstants.ID, displayId)
            putInt(
                TelecomManager.EXTRA_INCOMING_VIDEO_STATE,
                if (call.type.equals(CometChatConstants.CALL_TYPE_VIDEO, ignoreCase = true))
                    VideoProfile.STATE_BIDIRECTIONAL else VideoProfile.STATE_AUDIO_ONLY
            )
            putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, address)
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
            putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true)
        }

        try {
            telecomManager.addNewIncomingCall(handle, extras)
            Log.d(TAG, "addNewIncomingCall fired for session $sessionId")
        } catch (e: SecurityException) {
            Log.e(TAG, "addNewIncomingCall SecurityException — account not enabled? ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "addNewIncomingCall failed: ${e.message}")
        }
    }

    /** Ends a ringing/active native call (used when a call is stale or cancelled). */
    @SuppressLint("MissingPermission")
    private fun endActiveCallIfAny(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && telecomManager.isInCall) {
                telecomManager.endCall()
            }
        } catch (e: Exception) {
            Log.e(TAG, "endActiveCallIfAny failed: ${e.message}")
        }
    }
}
