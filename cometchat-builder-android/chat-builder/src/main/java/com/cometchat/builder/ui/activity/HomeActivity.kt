package com.cometchat.builder.ui.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.CometChatTheme
import com.cometchat.builder.R
import com.cometchat.builder.data.interfaces.OnItemClickListener
import com.cometchat.builder.databinding.BuilderActivityHomeBinding
import com.cometchat.builder.ui.fragments.CallsFragment
import com.cometchat.builder.ui.fragments.ChatsFragment
import com.cometchat.builder.ui.fragments.GroupsFragment
import com.cometchat.builder.ui.fragments.UsersFragment
import com.cometchat.builder.BuilderSettingsHelper

class HomeActivity : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding: BuilderActivityHomeBinding
    private var currentFragment = R.id.nav_chats // Default to the Chats fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BuilderActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState != null) {
            currentFragment = savedInstanceState.getInt(SELECTED_FRAGMENT_KEY, R.id.nav_chats)
        } // Set the selected item in the bottom navigation to match the current fragment
        binding.bottomNavigationView.selectedItemId = currentFragment
        loadFragment(getFragment(currentFragment))
        configureBottomNavigation()
    }

    /**
     * Loads the specified fragment into the fragment container.
     *
     * @param fragment
     * The fragment to be loaded.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }

    /**
     * Configures the bottom navigation view and its item selection listener.
     * Updates the displayed fragment based on user selection.
     */
    private fun configureBottomNavigation() {
        BuilderSettingsHelper.applySettingsToBottomNavigationView(
            binding.bottomNavigationView,
            object : CometChat.CallbackListener<Int?>() {
                override fun onSuccess(result: Int?) {
                    binding.bottomNavigationView.selectedItemId = result!!
                    loadFragment(getFragment(result))
                }

                override fun onError(error: CometChatException) {
                }
            })

        binding.bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            if (currentFragment == item.itemId) {
                return@setOnItemSelectedListener true // No action needed if the fragment is already selected
            }
            currentFragment = item.itemId
            loadFragment(
                getFragment(
                    currentFragment
                )
            )
            true
        } // Create a ColorStateList for icon and text color based on the checked state
        val colorStateList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(
                CometChatTheme.getIconTintHighlight(this), CometChatTheme.getIconTintSecondary(
                    this
                )
            )
        )

        binding.bottomNavigationView.itemIconTintList = colorStateList
        binding.bottomNavigationView.itemTextColor = colorStateList
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_FRAGMENT_KEY, currentFragment) // Save the selected fragment ID
    }

    override fun onItemClick() {
        val intent = Intent(this, NewChatActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private val TAG: String = HomeActivity::class.java.simpleName
        private const val SELECTED_FRAGMENT_KEY = "selected_fragment"

        /**
         * Returns the appropriate fragment based on the selected menu itemId.
         *
         * @param itemId
         * The selected menu itemId ID.
         * @return The corresponding fragment, or null if no match is found.
         */
        private fun getFragment(itemId: Int): Fragment {
            val selectedFragment = when (itemId) {
                R.id.nav_chats -> {
                    ChatsFragment()
                }

                R.id.nav_calls -> {
                    CallsFragment()
                }

                R.id.nav_users -> {
                    UsersFragment()
                }

                R.id.nav_groups -> {
                    GroupsFragment()
                }

                else -> {
                    ChatsFragment()
                }
            }
            return selectedFragment
        }
    }

}