package com.lokaleza.amatyma

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.lokaleza.amatyma.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var auth: FirebaseAuth
    private val splashDuration = 2500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, splashDuration)
    }

    private fun navigateToNextScreen() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // No Firebase session — go to auth
            startActivity(Intent(this, AuthActivity::class.java).apply {
                putExtra("FROM_SPLASH", true)
            })
            finish()
            return
        }

        // Firebase user exists. Now check if CometChat onboarding was completed.
        // CometChat needs a user created via Cloud Function before login can work.
        // We check: (1) in-memory CometChat session first (fastest, no network),
        // then (2) SharedPreferences flag we set after successful setup,
        // then (3) Firestore cache (offline-first, no network needed if previously loaded).
        // This prevents users who skipped ProfileSetupActivity from reaching MainActivity
        // with a broken CometChat state.

        if (CometChatUIKit.getLoggedInUser() != null) {
            // CometChat session is active — go straight to main
            goToMain()
            return
        }

        // Check local flag first (fastest path, works offline)
        val prefs = getSharedPreferences("amatyma_prefs", MODE_PRIVATE)
        val setupComplete = prefs.getBoolean("cometchat_setup_${currentUser.uid}", false)

        if (setupComplete) {
            // Setup was completed in a previous session. CometChat session expired but user
            // exists — MainActivity will re-login CometChat from there.
            goToMain()
            return
        }

        // No local flag — check Firestore cache (works offline if data was ever fetched).
        // Use CACHE source to avoid blocking on poor networks.
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUser.uid)
            .get(Source.CACHE)
            .addOnSuccessListener { document ->
                val displayName = document.getString("displayName")
                val cometChatSynced = document.getBoolean("cometChatSynced") ?: false

                if (!displayName.isNullOrEmpty() && cometChatSynced) {
                    // Profile and CometChat are set up — save the flag so future launches are instant
                    prefs.edit().putBoolean("cometchat_setup_${currentUser.uid}", true).apply()
                    goToMain()
                } else {
                    // Profile/CometChat setup incomplete — force them through setup
                    goToProfileSetup()
                }
            }
            .addOnFailureListener {
                // Cache miss (fresh install or cache cleared). Try the network as a fallback.
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .get(Source.SERVER)
                    .addOnSuccessListener { document ->
                        val displayName = document.getString("displayName")
                        val cometChatSynced = document.getBoolean("cometChatSynced") ?: false

                        if (!displayName.isNullOrEmpty() && cometChatSynced) {
                            prefs.edit().putBoolean("cometchat_setup_${currentUser.uid}", true).apply()
                            goToMain()
                        } else {
                            goToProfileSetup()
                        }
                    }
                    .addOnFailureListener {
                        // Network also unavailable. Safest bet: send to ProfileSetupActivity.
                        // It will detect any partial state and handle it gracefully.
                        goToProfileSetup()
                    }
            }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun goToProfileSetup() {
        startActivity(Intent(this, ProfileSetupActivity::class.java))
        finish()
    }
}
