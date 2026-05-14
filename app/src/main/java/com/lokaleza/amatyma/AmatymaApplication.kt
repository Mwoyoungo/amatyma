package com.lokaleza.amatyma

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDelegate
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.calls.CallingExtension
import com.cometchat.chatuikit.calls.incomingcall.CometChatIncomingCall
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKitHelper
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings
import com.cometchat.chatuikit.shared.events.CometChatCallEvents
import com.cometchat.chatuikit.shared.interfaces.OnError
import com.cometchat.chatuikit.shared.resources.soundmanager.CometChatSoundManager
import com.cometchat.chatuikit.shared.resources.soundmanager.Sound
import com.cometchat.chatuikit.shared.resources.utils.Utils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.lokaleza.amatyma.db.AmatymaDatabase

class AmatymaApplication : Application() {

    private var currentActivity: Activity? = null
    private var snackBar: Snackbar? = null
    private lateinit var soundManager: CometChatSoundManager
    private var tempCall: Call? = null

    companion object {
        private const val TAG = "AmatymaApplication"
        private const val APP_ID   = "1678655d5116b4d9e"
        private const val REGION   = "us"
        private const val AUTH_KEY = "645135588ebf9d6fa298be23f0fea0d49d97fb57"

        private const val PREFS_NAME   = "amatyma_app_prefs"
        private const val KEY_APP_ID   = "cometchat_app_id"
        private val CALL_LISTENER_ID   = "AmatymaApp_${System.currentTimeMillis()}"
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        soundManager = CometChatSoundManager(this)
        registerActivityTracker()
        addCallListener()
        clearStaleDataIfAccountChanged()
        initializeFirestore()
        initializeCometChat()
    }

    private fun registerActivityTracker() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var activityReferences = 0
            private var isChangingConfig = false

            override fun onActivityCreated(activity: Activity, b: Bundle?) { currentActivity = activity }
            override fun onActivityStarted(activity: Activity) {
                currentActivity = activity
                if (++activityReferences == 1 && !isChangingConfig) Unit
            }
            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
                if (snackBar != null && tempCall != null) showIncomingCallBanner(tempCall)
                else dismissIncomingCallBanner()
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                isChangingConfig = activity.isChangingConfigurations
                if (--activityReferences == 0 && !isChangingConfig) Unit
            }
            override fun onActivitySaveInstanceState(activity: Activity, b: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity === activity) currentActivity = null
            }
        })
    }

    private fun addCallListener() {
        CometChat.addCallListener(CALL_LISTENER_ID, object : CometChat.CallListener() {
            override fun onIncomingCallReceived(call: Call) {
                soundManager.play(Sound.incomingCall)
                launchIncomingCallPopup(call)
            }
            override fun onOutgoingCallAccepted(call: Call) { dismissIncomingCallBanner() }
            override fun onOutgoingCallRejected(call: Call) { dismissIncomingCallBanner() }
            override fun onIncomingCallCancelled(call: Call) { dismissIncomingCallBanner() }
            override fun onCallEndedMessageReceived(call: Call) { dismissIncomingCallBanner() }
        })

        CometChatCallEvents.addListener(CALL_LISTENER_ID, object : CometChatCallEvents() {
            override fun ccCallAccepted(call: Call) { dismissIncomingCallBanner() }
            override fun ccCallRejected(call: Call) { dismissIncomingCallBanner() }
        })
    }

    private fun launchIncomingCallPopup(call: Call) {
        if (CometChat.getActiveCall() == null && CallingExtension.getActiveCall() == null && !CallingExtension.isActiveMeeting()) {
            CallingExtension.setActiveCall(call)
            showIncomingCallBanner(call)
        } else {
            rejectWithBusy(call)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showIncomingCallBanner(call: Call?) {
        val activity = currentActivity ?: return
        if (call == null) return
        tempCall = call

        val incomingCallView = CometChatIncomingCall(activity).apply {
            disableSoundForCalls(true)
            this.call = call
            fitsSystemWindows = true
            onError = OnError { dismissIncomingCallBanner() }
        }

        snackBar = Snackbar.make(activity.findViewById(android.R.id.content), " ", Snackbar.LENGTH_INDEFINITE)
        val layout = snackBar!!.view as Snackbar.SnackbarLayout
        val params = layout.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        params.topMargin = Utils.convertDpToPx(this, 35)
        layout.layoutParams = params
        layout.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
        layout.addView(incomingCallView, 0)
        snackBar!!.show()
    }

    private fun dismissIncomingCallBanner() {
        if (snackBar?.isShown == true) {
            snackBar!!.dismiss()
            snackBar = null
            tempCall = null
            CallingExtension.setActiveCall(null)
        }
        soundManager.pauseSilently()
    }

    private fun rejectWithBusy(call: Call) {
        Thread {
            try { Thread.sleep(2000) } catch (e: InterruptedException) { return@Thread }
            CometChat.rejectCall(call.sessionId, CometChatConstants.CALL_STATUS_BUSY, object : CometChat.CallbackListener<Call>() {
                override fun onSuccess(c: Call) { CometChatUIKitHelper.onCallRejected(c) }
                override fun onError(e: CometChatException) {}
            })
        }.start()
    }

    /**
     * If the CometChat APP_ID has changed since the last launch (e.g. we
     * migrated to a new account), wipe all locally cached chat data so the
     * user never sees conversations that belong to the old account.
     */
    private fun clearStaleDataIfAccountChanged() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedAppId = prefs.getString(KEY_APP_ID, null)

        if (storedAppId != APP_ID) {
            Log.w(TAG, "CometChat APP_ID changed ($storedAppId → $APP_ID) — clearing local cache")

            // Wipe Room database
            try {
                val db = AmatymaDatabase.get(this)
                db.clearAllTables()
                Log.d(TAG, "Room database cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear Room database: ${e.message}")
                // Fallback: delete the database file entirely
                deleteDatabase("amatyma_chat.db")
            }

            // Wipe SharedPreferences caches used by profile/auth screens
            listOf("profile_cache", "amatyma_prefs").forEach { name ->
                getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
            }

            // Wipe Firestore offline cache
            try {
                FirebaseFirestore.getInstance().clearPersistence()
            } catch (e: Exception) {
                Log.e(TAG, "Firestore clearPersistence failed: ${e.message}")
            }

            // Record the new APP_ID so we don't wipe again next launch
            prefs.edit().putString(KEY_APP_ID, APP_ID).apply()
        }
    }

    private fun initializeFirestore() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings
            Log.d(TAG, "Firestore offline persistence enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling Firestore persistence: ${e.message}")
        }
    }

    private fun initializeCometChat() {
        val uiKitSettings = UIKitSettings.UIKitSettingsBuilder()
            .setAppId(APP_ID)
            .setRegion(REGION)
            .setAuthKey(AUTH_KEY)
            .subscribePresenceForAllUsers()
            .build()

        CometChatUIKit.init(this, uiKitSettings, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(p0: String?) {
                Log.d(TAG, "CometChat initialized successfully")
            }

            override fun onError(p0: CometChatException?) {
                Log.e(TAG, "CometChat initialization failed: ${p0?.message}")
            }
        })
    }
}
