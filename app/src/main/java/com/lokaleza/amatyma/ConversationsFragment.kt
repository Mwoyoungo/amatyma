package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.lokaleza.amatyma.databinding.FragmentConversationsBinding

class ConversationsFragment : Fragment() {

    private var _binding: FragmentConversationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupConversations()
    }

    private fun setupConversations() {
        binding.conversations.setOnItemClick { _: View, _: Int, conversation: Conversation ->
            when (conversation.conversationType) {
                CometChatConstants.CONVERSATION_TYPE_GROUP -> {
                    val group = conversation.conversationWith as Group
                    navigateToMessages(null, group)
                }
                else -> {
                    val user = conversation.conversationWith as User
                    navigateToMessages(user, null)
                }
            }
        }
    }

    private fun navigateToMessages(user: User?, group: Group?) {
        val intent = Intent(requireActivity(), MessagesActivity::class.java).apply {
            user?.let {
                putExtra("USER_ID", it.uid)
                putExtra("USER_NAME", it.name)
            }
            group?.let {
                putExtra("GROUP_ID", it.guid)
                putExtra("GROUP_NAME", it.name)
            }
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
