package com.lokaleza.amatyma

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.lokaleza.amatyma.databinding.ActivityMainBinding
import com.lokaleza.amatyma.db.CometChatSyncManager
import com.lokaleza.amatyma.voip.CometChatVoIPManager
import com.lokaleza.amatyma.social.SocialShell
import com.lokaleza.amatyma.social.ui.theme.AmatymaSocialTheme
import androidx.activity.OnBackPressedCallback

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val conversationsWithMessagesFragment = ConversationsWithMessagesFragment()
    private val usersWithMessagesFragment = UsersWithMessagesFragment()
    private val groupsWithMessagesFragment = GroupsWithMessagesFragment()

    private var activeFragment: Fragment = conversationsWithMessagesFragment
    private lateinit var syncManager: CometChatSyncManager
    private var socialBackCallback: OnBackPressedCallback? = null

    companion object {
        const val EXTRA_SELECTED_TAB = "selected_tab"
        const val TAB_CHATS = 0
        const val TAB_USERS = 2
        const val TAB_GROUPS = 3
        private const val TAG = "MainActivity"
        private const val KEY_ACTIVE_TAG = "active_fragment_tag"

        private const val VOIP_PERMISSION_REQUEST_CODE = 1002
        private val VOIP_PERMISSIONS = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ── Social surface (Jetpack Compose) mounted ON TOP of the chat shell as
        // the primary view. Everything below — CometChat login recovery, FCM token,
        // Room sync, VoIP setup — is unchanged and keeps running underneath, so
        // chat/calls/notifications behave exactly as before. ──
        mountSocialShell()

        if (savedInstanceState != null) {
            val tag = savedInstanceState.getString(KEY_ACTIVE_TAG, "chats")
            activeFragment = when (tag) {
                "users"  -> usersWithMessagesFragment
                "groups" -> groupsWithMessagesFragment
                else     -> conversationsWithMessagesFragment
            }
        }

        requestNotificationPermission()
        configureVoIP()
        setupBottomNavigation()
        showLoadingState(true)
        ensureCometChatLogin()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_ACTIVE_TAG, activeFragment.tag)
    }

    // ─── Social surface mount (additive — no chat bootstrap is modified) ────────

    /**
     * Hosts the Compose social shell over the chat views. Chat sits underneath,
     * fully alive (its bootstrap still runs in onCreate); it's surfaced on demand
     * via [showChat] from the social feed's header chat icon.
     */
    private fun mountSocialShell() {
        binding.socialCompose.setContent {
            AmatymaSocialTheme {
                SocialShell(onOpenChat = { showChat() }, onLogout = { performSocialLogout() })
            }
        }
        socialBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                // Back while in chat returns to the social surface.
                binding.socialCompose.visibility = View.VISIBLE
                isEnabled = false
            }
        }.also { onBackPressedDispatcher.addCallback(this, it) }
    }

    /** Reveals the existing chat shell (hides the social overlay). */
    private fun showChat() {
        binding.socialCompose.visibility = View.GONE
        socialBackCallback?.isEnabled = true
    }

    private fun initializeFragments() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, conversationsWithMessagesFragment, "chats")
            add(R.id.fragment_container, usersWithMessagesFragment, "users").hide(usersWithMessagesFragment)
            add(R.id.fragment_container, groupsWithMessagesFragment, "groups").hide(groupsWithMessagesFragment)
            commitNow()
        }
        activeFragment = conversationsWithMessagesFragment
    }

    private fun ensureCometChatLogin() {
        val currentUser = CometChatUIKit.getLoggedInUser()

        if (currentUser != null) {
            Log.d(TAG, "CometChat already logged in: ${currentUser.uid}")
            onCometChatReady()
            return
        }

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            Log.e(TAG, "Firebase user is null, redirecting to auth")
            redirectToAuth()
            return
        }

        Log.d(TAG, "CometChat session expired, re-logging in for UID: ${firebaseUser.uid}")
        loginToCometChat(firebaseUser.uid)
    }

    private fun loginToCometChat(uid: String) {
        CometChatUIKit.login(uid, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User?) {
                Log.d(TAG, "CometChat login successful: ${user?.name}")
                runOnUiThread { onCometChatReady() }
            }

            override fun onError(exception: CometChatException?) {
                Log.e(TAG, "CometChat direct login failed (code: ${exception?.code}): ${exception?.message}")
                runOnUiThread { loginViaCometChatFunction(uid) }
            }
        })
    }

    private fun loginViaCometChatFunction(uid: String) {
        Log.d(TAG, "Attempting CometChat login via Cloud Function for UID: $uid")

        FirebaseFunctions.getInstance()
            .getHttpsCallable("getCometChatAuthToken")
            .call()
            .addOnSuccessListener { result ->
                val authToken = (result.getData() as? Map<*, *>)?.get("authToken") as? String
                if (authToken != null) {
                    CometChatUIKit.login(uid, object : CometChat.CallbackListener<User>() {
                        override fun onSuccess(user: User?) {
                            Log.d(TAG, "CometChat login via token successful: ${user?.name}")
                            runOnUiThread { onCometChatReady() }
                        }

                        override fun onError(exception: CometChatException?) {
                            Log.e(TAG, "CometChat login via token failed: ${exception?.message}")
                            runOnUiThread { handleCometChatLoginFailure(uid) }
                        }
                    })
                } else {
                    runOnUiThread { handleCometChatLoginFailure(uid) }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Cloud Function call failed: ${e.message}")
                runOnUiThread { showCometChatRetryBanner(uid) }
            }
    }

    private fun handleCometChatLoginFailure(uid: String) {
        Log.w(TAG, "CometChat login could not be recovered, redirecting to profile setup")
        showLoadingState(false)
        val prefs = getSharedPreferences("amatyma_prefs", MODE_PRIVATE)
        prefs.edit().remove("cometchat_setup_$uid").apply()

        AlertDialog.Builder(this)
            .setTitle("Chat Setup Incomplete")
            .setMessage("Your chat account needs to be set up. This will only take a moment.")
            .setPositiveButton("Set Up Now") { _, _ ->
                startActivity(Intent(this, ProfileSetupActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showCometChatRetryBanner(uid: String) {
        if (supportFragmentManager.findFragmentByTag("chats") == null) {
            initializeFragments()
        }
        val selectedTab = intent.getIntExtra(EXTRA_SELECTED_TAB, TAB_CHATS)
        selectTab(selectedTab)
        showLoadingState(false)

        Toast.makeText(
            this,
            "Connecting to chat — tap here to retry if messages don't load.",
            Toast.LENGTH_LONG
        ).show()
        binding.fragmentContainer.setOnClickListener {
            binding.fragmentContainer.setOnClickListener(null)
            showLoadingState(true)
            loginToCometChat(uid)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    // ─── Native VoIP calling setup ────────────────────────────────────────────

    private var voipEnablePrompted = false

    /**
     * Requests the phone permissions the native call screen needs, then (once
     * granted) nudges the user to enable Amatyma's calling account so Android
     * will show the native incoming-call screen. Mirrors CometChat's official
     * sample VoIP setup.
     */
    private fun configureVoIP() {
        val missing = VOIP_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), VOIP_PERMISSION_REQUEST_CODE)
        } else {
            maybePromptEnableCallingAccount()
        }
    }

    private fun maybePromptEnableCallingAccount() {
        if (voipEnablePrompted) return
        if (CometChatVoIPManager.isPhoneAccountEnabled(this)) return

        voipEnablePrompted = true
        AlertDialog.Builder(this)
            .setTitle("Enable Amatyma calls")
            .setMessage(
                "To receive Amatyma voice and video calls like a normal phone call, " +
                "turn on the Amatyma calling account on the next screen."
            )
            .setPositiveButton("Open settings") { _, _ ->
                CometChatVoIPManager.openCallingAccountsSettings(this)
            }
            .setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == VOIP_PERMISSION_REQUEST_CODE) {
            maybePromptEnableCallingAccount()
        }
    }

    private fun saveFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val db = FirebaseFirestore.getInstance()
            val opts = com.google.firebase.firestore.SetOptions.merge()
            val payload = mapOf("fcmTokens" to FieldValue.arrayUnion(token))

            // Save under the real Firebase UID
            db.collection("users").document(uid).set(payload, opts)

            // Also save under lowercase UID — CometChat lowercases UIDs in webhooks
            val lowerUid = uid.lowercase()
            if (lowerUid != uid) {
                db.collection("users").document(lowerUid).set(payload, opts)
            }
        }
    }

    private fun onCometChatReady() {
        syncManager = CometChatSyncManager(this)
        syncManager.start()

        FirebaseAuth.getInstance().currentUser?.uid?.let { saveFcmToken(it) }

        if (supportFragmentManager.findFragmentByTag("chats") == null) {
            initializeFragments()
        }
        val selectedTab = intent.getIntExtra(EXTRA_SELECTED_TAB, TAB_CHATS)
        selectTab(selectedTab)
        showLoadingState(false)
    }

    private fun redirectToAuth() {
        val intent = Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    /**
     * Logout from the social surface. Mirrors the proven sequence in
     * MyBusinessProfileActivity exactly: clear caches, log out CometChat, sign out
     * Firebase, return to AuthActivity. CometChat is only disconnected here, on an
     * explicit logout — never during normal use.
     */
    private fun performSocialLogout() {
        getSharedPreferences("profile_cache", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("amatyma_prefs", MODE_PRIVATE).edit().clear().apply()
        CometChat.logout(object : CometChat.CallbackListener<String>() {
            override fun onSuccess(message: String?) { Log.d(TAG, "Social logout: CometChat logout ok") }
            override fun onError(exception: CometChatException?) {
                Log.e(TAG, "Social logout: CometChat logout error: ${exception?.message}")
            }
        })
        FirebaseAuth.getInstance().signOut()
        redirectToAuth()
    }

    private fun showLoadingState(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.fragmentContainer.visibility = if (show) View.GONE else View.VISIBLE
        binding.bottomNavigation.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chats  -> { showFragment(conversationsWithMessagesFragment); true }
                R.id.nav_users  -> { showFragment(usersWithMessagesFragment); true }
                R.id.nav_groups -> { showFragment(groupsWithMessagesFragment); true }
                else -> false
            }
        }
    }

    private fun selectTab(tab: Int) {
        when (tab) {
            TAB_CHATS  -> binding.bottomNavigation.selectedItemId = R.id.nav_chats
            TAB_USERS  -> binding.bottomNavigation.selectedItemId = R.id.nav_users
            TAB_GROUPS -> binding.bottomNavigation.selectedItemId = R.id.nav_groups
        }
    }

    private fun showFragment(fragment: Fragment) {
        if (fragment == activeFragment) return

        supportFragmentManager.beginTransaction().apply {
            hide(activeFragment)
            show(fragment)
            commitNow()
        }
        activeFragment = fragment
    }
}
