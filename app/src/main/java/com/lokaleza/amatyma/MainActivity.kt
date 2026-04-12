package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.lokaleza.amatyma.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    // Keep CometChat fragments in memory to avoid reloading
    private val conversationsWithMessagesFragment = ConversationsWithMessagesFragment()
    private val usersWithMessagesFragment = UsersWithMessagesFragment()
    private val groupsWithMessagesFragment = GroupsWithMessagesFragment()

    // Business feature fragments
    private val discoverFragment = DiscoverFragment()
    private val eventsFragment = EventsFragment()
    private val shopFragment = ShopFragment()

    private var activeFragment: Fragment = conversationsWithMessagesFragment

    companion object {
        const val EXTRA_SELECTED_TAB = "selected_tab"
        const val TAB_CHATS = 0
        const val TAB_USERS = 1
        const val TAB_GROUPS = 2
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)

        // Set up drawer
        setupDrawer()

        // CRITICAL: Login to CometChat if not already logged in
        ensureCometChatLogin()

        // Initialize all fragments once
        if (savedInstanceState == null) {
            initializeFragments()

            // Set up bottom navigation
            setupBottomNavigation()

            // Always start with the tab specified in intent, defaulting to Chats
            val selectedTab = intent.getIntExtra(EXTRA_SELECTED_TAB, TAB_CHATS)
            selectTab(selectedTab)
            Log.d(TAG, "Starting MainActivity with tab: $selectedTab")
        } else {
            // Set up bottom navigation
            setupBottomNavigation()

            // After recreation, explicitly select Chats tab
            selectTab(TAB_CHATS)
            Log.d(TAG, "Recreating MainActivity, forcing Chats tab")
        }
    }

    private fun setupDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.app_name,
            R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)
    }

    private fun initializeFragments() {
        // Add CometChat fragments, hide all except conversations
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, conversationsWithMessagesFragment, "chats")
            add(R.id.fragment_container, usersWithMessagesFragment, "users").hide(usersWithMessagesFragment)
            add(R.id.fragment_container, groupsWithMessagesFragment, "groups").hide(groupsWithMessagesFragment)
            // Business fragments added on demand
            commit()
        }
        // Ensure activeFragment is set to conversationsWithMessagesFragment
        activeFragment = conversationsWithMessagesFragment
    }

    private fun ensureCometChatLogin() {
        val currentUser = CometChatUIKit.getLoggedInUser()

        if (currentUser == null) {
            // Not logged into CometChat, need to login
            val firebaseUser = FirebaseAuth.getInstance().currentUser

            if (firebaseUser != null) {
                Log.d(TAG, "CometChat not logged in, logging in with UID: ${firebaseUser.uid}")
                // Show loading state while logging in
                showLoadingState(true)
                loginToCometChat(firebaseUser.uid)
            } else {
                Log.e(TAG, "Firebase user is null, cannot login to CometChat")
            }
        } else {
            Log.d(TAG, "CometChat already logged in: ${currentUser.uid}")
            // Already logged in, no need to show loading
            showLoadingState(false)
        }
    }

    private fun loginToCometChat(uid: String) {
        CometChatUIKit.login(uid, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User?) {
                Log.d(TAG, "CometChat login successful: ${user?.name}")
                runOnUiThread {
                    // Hide loading and show content
                    showLoadingState(false)
                }
            }

            override fun onError(exception: CometChatException?) {
                Log.e(TAG, "CometChat login failed: ${exception?.message}")
                runOnUiThread {
                    // Hide loading and show error
                    showLoadingState(false)
                    Toast.makeText(
                        this@MainActivity,
                        "Chat login failed: ${exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    private fun showLoadingState(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.fragmentContainer.visibility = if (show) View.GONE else View.VISIBLE
        binding.bottomNavigation.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chats -> {
                    showFragment(conversationsWithMessagesFragment)
                    true
                }
                R.id.nav_users -> {
                    showFragment(usersWithMessagesFragment)
                    true
                }
                R.id.nav_groups -> {
                    showFragment(groupsWithMessagesFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun selectTab(tab: Int) {
        when (tab) {
            TAB_CHATS -> binding.bottomNavigation.selectedItemId = R.id.nav_chats
            TAB_USERS -> binding.bottomNavigation.selectedItemId = R.id.nav_users
            TAB_GROUPS -> binding.bottomNavigation.selectedItemId = R.id.nav_groups
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_shop -> {
                addFragmentIfNeeded(shopFragment, "shop")
                showFragment(shopFragment)
            }
            R.id.nav_discover -> {
                addFragmentIfNeeded(discoverFragment, "discover")
                showFragment(discoverFragment)
            }
            R.id.nav_events -> {
                addFragmentIfNeeded(eventsFragment, "events")
                showFragment(eventsFragment)
            }
            R.id.nav_articles -> {
                startActivity(Intent(this, ArticlesActivity::class.java))
            }
            R.id.nav_profile -> {
                startActivity(Intent(this, MyBusinessProfileActivity::class.java))
            }
            R.id.nav_logout -> {
                showLogoutDialog()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun addFragmentIfNeeded(fragment: Fragment, tag: String) {
        if (supportFragmentManager.findFragmentByTag(tag) == null) {
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, fragment, tag).hide(fragment)
                commit()
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        // Clear cached profile data
        val prefs = getSharedPreferences("profile_cache", MODE_PRIVATE)
        prefs.edit().clear().apply()

        // Logout from CometChat
        CometChat.logout(object : CometChat.CallbackListener<String>() {
            override fun onSuccess(message: String?) {
                Log.d(TAG, "CometChat logout successful")
            }

            override fun onError(exception: CometChatException?) {
                Log.e(TAG, "CometChat logout error: ${exception?.message}")
            }
        })

        // Logout from Firebase
        FirebaseAuth.getInstance().signOut()

        // Navigate to AuthActivity and clear task stack
        val intent = Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showFragment(fragment: Fragment) {
        if (fragment == activeFragment) return

        supportFragmentManager.beginTransaction().apply {
            hide(activeFragment)
            show(fragment)
            commit()
        }
        activeFragment = fragment
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Don't save BottomNavigationView state - always start fresh on Chats
        // This prevents the app from restoring to wrong tab after logout/login
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
