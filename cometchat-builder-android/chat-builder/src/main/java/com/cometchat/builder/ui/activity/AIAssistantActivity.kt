package com.cometchat.builder.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.cometchat.builder.BuilderSettingsHelper
import com.cometchat.builder.R
import com.cometchat.builder.databinding.BuilderActivityAiAssistantBinding
import com.cometchat.builder.utils.AppUtils
import com.cometchat.builder.viewmodels.MessagesViewModel
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.resources.utils.Utils
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.max

class AIAssistantActivity : AppCompatActivity() {
    private lateinit var binding: BuilderActivityAiAssistantBinding
    private var user: User? = null
    private var parentMessage: BaseMessage? = null
    private lateinit var viewModel: MessagesViewModel
    private var isAgentChat = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BuilderActivityAiAssistantBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        try {
            val messageJson = intent.getStringExtra(getString(R.string.builder_base_message))
            val userJson = intent.getStringExtra(getString(R.string.builder_user))
            if (userJson != null && !userJson.isEmpty())
                user = User.fromJson(userJson)
            if (messageJson != null && !messageJson.isEmpty())
                parentMessage = BaseMessage.processMessage(JSONObject(messageJson))
        } catch (e: JSONException) {
            Log.e(AIAssistantActivity::class.simpleName, "onCreate: " + e.message)
        }

        isAgentChat = AppUtils.isAgentChat(user)

        setUpTheme()
        adjustWindowSettings()
        applyWindowInsets()
        // Create an instance of the MessagesViewModel
        initViewModel()
        // Initialize UI components
        addViews()

        // Set click listener for the unblock button
        initClickListeners()
        onBackPressedDispatcher.addCallback(this, callback)
    }

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Utils.hideKeyBoard(this@AIAssistantActivity, binding.root)
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                finish()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initClickListeners() {
        binding.cometchatAiAssistantChatHistory.setOnTouchListener { _, _ ->
            true
        }
        binding.messageHeader.setNewChatButtonClick {
            Utils.hideKeyBoard(this@AIAssistantActivity, binding.getRoot())
            val intent = Intent(this@AIAssistantActivity, AIAssistantActivity::class.java)
            intent.putExtra(getString(R.string.builder_user), user!!.toJson().toString())
            startActivity(intent)
            finish()
        }
        binding.messageHeader.setChatHistoryButtonClick {
            binding.drawerLayout.refreshDrawableState()
            binding.cometchatAiAssistantChatHistory.setUser(user)
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
        binding.cometchatAiAssistantChatHistory.setOnItemClickListener { view, position, message ->
            val appEntity = message.getReceiver()
            if (appEntity is User) {
                user = appEntity
                val intent = Intent(this@AIAssistantActivity, AIAssistantActivity::class.java)
                intent.putExtra(getString(R.string.builder_user), appEntity.toJson().toString())
                intent.putExtra(
                    getString(R.string.app_base_message),
                    message.getRawMessage().toString()
                )
                startActivity(intent)
                finish()
            }
        }
        binding.cometchatAiAssistantChatHistory.setOnNewChatClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                binding.drawerLayout.closeDrawer(GravityCompat.END)
                val intent = Intent(this@AIAssistantActivity, AIAssistantActivity::class.java)
                intent.putExtra(getString(R.string.builder_user), user!!.toJson().toString())
                startActivity(intent)
                finish()
            }
        }
        binding.cometchatAiAssistantChatHistory.setOnCloseClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            }
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider.NewInstanceFactory().create(MessagesViewModel::class.java)
        viewModel.setUser(user)

        // Add listeners for ViewModel updates
        viewModel.addListener()
        viewModel.openUserChat().observe(this) { user: User? -> this.openUserChat(user) }
        viewModel.isExitActivity.observe(this) { exit: Boolean -> this.exitActivity(exit) }
        viewModel.sentMessage.observe(this, Observer { value: Boolean -> this.messageSent(value) })
    }

    private fun messageSent(value: Boolean) {
        if (value && isAgentChat) {
            Utils.hideKeyBoard(this, binding.getRoot())
        }
    }

    private fun setUpTheme() {
        // binding.parentView.setBackgroundColor(CometChatTheme.getBackgroundColor1(this))
    }

    /**
     * Adjusts the window settings for the activity.
     */

    private fun adjustWindowSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val bottomInset = max(ime.bottom.toDouble(), nav.bottom.toDouble()).toInt()

            if (isImeVisible && binding.messageComposer.messageInput.composeBox.isFocused) {
                binding.messageList.scrollToBottom()
            }

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                bottomInset
            )
            insets
        }
    }

    /**
     * Opens the chat interface for the specified user.
     *
     * @param user
     * The user object representing the chat participant. Must not be null.
     */
    private fun openUserChat(user: User?) {
        if (user != null) {
            val intent = Intent(this, AIAssistantActivity::class.java)
            intent.putExtra(getString(R.string.builder_user), user.toJson().toString())
            startActivity(intent)
            if (isAgentChat) exitActivity(true)
        }
    }

    /**
     * Exits the activity if the exit flag is true.
     *
     * @param exit
     * Indicates whether to exit the activity.
     */
    private fun exitActivity(exit: Boolean) {
        if (exit) {
            finish()
        }
    }

    /** Initializes UI components */
    private fun addViews() {
        // Set user or group data to the message header and composer
        if (user != null) {
            if (parentMessage != null) {
                // Set message id of parent message to fetch messages with parent.
                // Here we are setting parent message id to message list to fetch messages and message composer to send reply to that message.
                // Here this is being used for AIAssistantChatHistory
                binding.messageList.setParentMessage(parentMessage!!.getId())
                binding.messageComposer.parentMessageId = parentMessage!!.getId()
            }
            binding.messageHeader.user = user!!
            binding.messageList.user = user
            binding.messageComposer.user = user
            if (isAgentChat) {
                binding.messageList.setStyle(R.style.CustomCometChatMessageListStyle)
                binding.messageComposer.style = R.style.CustomMessageComposerStyle
            }
        }

        // Set up back button behavior
        binding.messageHeader.setOnBackButtonPressed {
            Utils.hideKeyBoard(this, binding.root)
            finish()
        }

        // BuilderSettingsHelper.applySettingsToMessageHeader(binding.messageHeader)
        BuilderSettingsHelper.applySettingsToMessageList(binding.messageList)
    }

    override fun onDestroy() {
        super.onDestroy() // Remove listener from the ViewModel to prevent memory leaks
        viewModel.removeListener()
    }
}
