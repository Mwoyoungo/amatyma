package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.lokaleza.amatyma.databinding.ActivityMessagesBinding
import com.lokaleza.amatyma.db.ChatRepository
import com.lokaleza.amatyma.db.MessagePreviewAdapter
import kotlinx.coroutines.launch

class MessagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessagesBinding
    private lateinit var repo: ChatRepository
    private lateinit var previewAdapter: MessagePreviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val navBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(
                top = statusBarInsets.top,
                bottom = maxOf(imeInsets.bottom, navBarInsets.bottom)
            )
            windowInsets
        }

        repo = ChatRepository(this)

        binding.messageList.setLoadingStateView(R.layout.layout_empty)

        setupMessages()
    }

    private fun setupMessages() {
        val userId = intent.getStringExtra("USER_ID")
        val groupId = intent.getStringExtra("GROUP_ID")
        val preFilledMessage = intent.getStringExtra("PRE_FILLED_MESSAGE")

        val conversationId = userId ?: groupId ?: run { finish(); return }
        val isGroup = groupId != null

        setupPreviewList(conversationId, isGroup)

        when {
            userId != null -> setupUserChat(userId, preFilledMessage)
            groupId != null -> setupGroupChat(groupId, preFilledMessage)
        }
    }

    // ─── Pre-paint from Room ──────────────────────────────────────────────────

    private fun setupPreviewList(conversationId: String, isGroup: Boolean) {
        previewAdapter = MessagePreviewAdapter(isGroup)

        binding.rvPreview.apply {
            layoutManager = LinearLayoutManager(this@MessagesActivity).apply {
                stackFromEnd = true  // newest messages at the bottom
            }
            adapter = previewAdapter
        }

        // Load cached messages immediately — this runs before the network call,
        // painting the screen with whatever we already have locally.
        lifecycleScope.launch {
            val cached = repo.getMessagesCached(conversationId)
            if (cached.isNotEmpty()) {
                previewAdapter.submitList(cached)
                binding.rvPreview.visibility = View.VISIBLE
                // Scroll to bottom so user sees the most recent message
                binding.rvPreview.scrollToPosition(previewAdapter.itemCount - 1)
            }
        }
    }

    // Called once CometChat has loaded its message list — hide our pre-paint layer.
    private fun hidePrepaint() {
        binding.rvPreview.visibility = View.GONE
    }

    // ─── CometChat setup ─────────────────────────────────────────────────────

    private fun setupUserChat(userId: String, preFilledMessage: String?) {
        CometChat.getUser(userId, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User?) {
                if (user != null) {
                    runOnUiThread {
                        binding.messageHeader.setUser(user)
                        binding.messageList.setUser(user)

                        preFilledMessage?.let {
                            binding.messageComposer.setInitialComposerText(it)
                        }

                        binding.messageComposer.setUser(user)
                        hidePrepaint()
                        repo.markAsRead(userId)

                        // Tapping the header area opens the user's profile
                        binding.messageHeader.setOnClickListener {
                            startActivity(
                                Intent(this@MessagesActivity, UserDetailsActivity::class.java).apply {
                                    putExtra(UserDetailsActivity.EXTRA_USER_ID, user.uid)
                                }
                            )
                        }
                    }
                }
            }

            override fun onError(exception: CometChatException?) {
                Log.e("MessagesActivity", "Error fetching user: ${exception?.message}")
            }
        })
    }

    private fun setupGroupChat(groupId: String, preFilledMessage: String?) {
        CometChat.getGroup(groupId, object : CometChat.CallbackListener<Group>() {
            override fun onSuccess(group: Group?) {
                if (group != null) {
                    runOnUiThread {
                        binding.messageHeader.setGroup(group)
                        binding.messageList.setGroup(group)

                        preFilledMessage?.let {
                            binding.messageComposer.setInitialComposerText(it)
                        }

                        binding.messageComposer.setGroup(group)

                        hidePrepaint()
                        repo.markAsRead(groupId)

                        binding.messageHeader.setOnClickListener {
                            startActivity(
                                Intent(this@MessagesActivity, GroupDetailsActivity::class.java).apply {
                                    putExtra(GroupDetailsActivity.EXTRA_GROUP, com.google.gson.Gson().toJson(group))
                                }
                            )
                        }
                    }
                }
            }

            override fun onError(exception: CometChatException?) {
                Log.e("MessagesActivity", "Error fetching group: ${exception?.message}")
                // Pre-paint stays visible — offline read mode
            }
        })
    }
}
