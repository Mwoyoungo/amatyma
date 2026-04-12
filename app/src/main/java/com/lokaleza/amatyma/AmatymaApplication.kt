package com.lokaleza.amatyma

import android.app.Application
import android.util.Log
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class AmatymaApplication : Application() {

    companion object {
        private const val TAG = "AmatymaApplication"
        private const val APP_ID = "281421fd397d9bf6"
        private const val REGION = "us"
        private const val AUTH_KEY = "5d7e15509f2034cf002555883a2e732d412d358a"
    }

    override fun onCreate() {
        super.onCreate()
        initializeFirestore()
        initializeCometChat()
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
                Log.d(TAG, "CometChat initialized successfully with Calls SDK")
            }

            override fun onError(p0: CometChatException?) {
                Log.e(TAG, "CometChat initialization failed: ${p0?.message}")
            }
        })
    }
}
