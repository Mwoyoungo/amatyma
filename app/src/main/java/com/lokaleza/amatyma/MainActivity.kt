package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val conversationsWithMessagesFragment = ConversationsWithMessagesFragment()
    private val callsFragment = CallsFragment()
    private val usersWithMessagesFragment = UsersWithMessagesFragment()
    private val groupsWithMessagesFragment = GroupsWithMessagesFragment()

    private var activeFragment: Fragment = conversationsWithMessagesFragment
    private lateinit var syncManager: CometChatSyncManager

    companion object {
        const val EXTRA_SELECTED_TAB = "selected_tab"
        const val TAB_CHATS = 0
        const val TAB_CALLS = 1
        const val TAB_USERS = 2
        const val TAB_GROUPS = 3
        private const val TAG = "MainActivity"
        private const val KEY_ACTIVE_TAG = "active_fragment_tag"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            val tag = savedInstanceState.getString(KEY_ACTIVE_TAG, "chats")
            activeFragment = when (tag) {
                "calls"  -> callsFragment
                "users"  -> usersWithMessagesFragment
                "groups" -> groupsWithMessagesFragment
                else     -> conversationsWithMessagesFragment
            }
        }

        setupBottomNavigation()
        showLoadingState(true)
        ensureCometChatLogin()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_ACTIVE_TAG, activeFragment.tag)
    }

    private fun initializeFragments() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, conversationsWithMessagesFragment, "chats")
            add(R.id.fragment_container, callsFragment, "calls").hide(callsFragment)
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

    private fun saveFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .update("fcmTokens", FieldValue.arrayUnion(token))
                .addOnFailureListener {
                    // Document might not exist yet — use set with merge
                    FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .set(mapOf("fcmTokens" to listOf(token)), com.google.firebase.firestore.SetOptions.merge())
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

    private fun showLoadingState(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.fragmentContainer.visibility = if (show) View.GONE else View.VISIBLE
        binding.bottomNavigation.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chats  -> { showFragment(conversationsWithMessagesFragment); true }
                R.id.nav_calls  -> { showFragment(callsFragment); true }
                R.id.nav_users  -> { showFragment(usersWithMessagesFragment); true }
                R.id.nav_groups -> { showFragment(groupsWithMessagesFragment); true }
                else -> false
            }
        }
    }

    private fun selectTab(tab: Int) {
        when (tab) {
            TAB_CHATS  -> binding.bottomNavigation.selectedItemId = R.id.nav_chats
            TAB_CALLS  -> binding.bottomNavigation.selectedItemId = R.id.nav_calls
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
