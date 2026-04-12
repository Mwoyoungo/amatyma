package com.lokaleza.amatyma

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.lokaleza.amatyma.databinding.ActivityMessagesBinding

class MessagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessagesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMessages()
    }

    private fun setupMessages() {
        val userId = intent.getStringExtra("USER_ID")
        val groupId = intent.getStringExtra("GROUP_ID")
        val preFilledMessage = intent.getStringExtra("PRE_FILLED_MESSAGE")

        when {
            userId != null -> {
                setupUserChat(userId, preFilledMessage)
            }
            groupId != null -> {
                setupGroupChat(groupId, preFilledMessage)
            }
            else -> {
                finish()
            }
        }
    }

    private fun setupUserChat(userId: String, preFilledMessage: String?) {
        CometChat.getUser(userId, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User?) {
                if (user != null) {
                    runOnUiThread {
                        binding.messageHeader.setUser(user)
                        binding.messageList.setUser(user)

                        // Pre-fill message if provided, before setting user
                        preFilledMessage?.let {
                            binding.messageComposer.setInitialComposerText(it)
                        }

                        binding.messageComposer.setUser(user)
                    }
                }
            }

            override fun onError(exception: CometChatException?) {
                Log.e("MessagesActivity", "Error fetching user: ${exception?.message}")
                runOnUiThread {
                    finish()
                }
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

                        // Pre-fill message if provided, before setting group
                        preFilledMessage?.let {
                            binding.messageComposer.setInitialComposerText(it)
                        }

                        binding.messageComposer.setGroup(group)
                    }
                }
            }

            override fun onError(exception: CometChatException?) {
                Log.e("MessagesActivity", "Error fetching group: ${exception?.message}")
                runOnUiThread {
                    finish()
                }
            }
        })
    }
}
