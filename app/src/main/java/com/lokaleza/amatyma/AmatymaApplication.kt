package com.lokaleza.amatyma

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.lokaleza.amatyma.db.AmatymaDatabase
import com.lokaleza.amatyma.voip.CometChatVoIPManager

class AmatymaApplication : Application() {

    private var currentActivity: Activity? = null

    companion object {
        private const val TAG = "AmatymaApplication"
        // Exposed (not private) so the VoIP telecom layer can re-init CometChat
        // with the same credentials if a call callback runs before init completes.
        const val APP_ID   = "1678655d5116b4d9e"
        const val REGION   = "us"
        const val AUTH_KEY = "645135588ebf9d6fa298be23f0fea0d49d97fb57"

        private const val PREFS_NAME   = "amatyma_app_prefs"
        private const val KEY_APP_ID   = "cometchat_app_id"

        /**
         * UID/GUID of the conversation currently on screen, or null if none.
         * Set/cleared by MessagesActivity as it resumes/pauses. Used to
         * suppress message notifications for a chat the user is already viewing.
         */
        @Volatile
        var activeConversationId: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Create the chat-message notification channel up front
        AmatymaFirebaseMessagingService.createNotificationChannel(
            getSystemService(NotificationManager::class.java)
        )
        registerActivityTracker()
        clearStaleDataIfAccountChanged()
        initializeFirestore()
        initializeCometChat()
        // Register the Telecom phone account so the OS can show the native
        // incoming-call screen for VoIP calls (full screen, over lock screen).
        CometChatVoIPManager.registerPhoneAccount(this)
    }

    private fun registerActivityTracker() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, b: Bundle?) { currentActivity = activity }
            override fun onActivityStarted(activity: Activity) { currentActivity = activity }
            override fun onActivityResumed(activity: Activity) { currentActivity = activity }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, b: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity === activity) currentActivity = null
            }
        })
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

