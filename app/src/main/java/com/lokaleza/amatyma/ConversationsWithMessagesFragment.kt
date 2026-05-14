package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.interfaces.OnItemClick
import com.cometchat.chatuikit.shared.views.avatar.CometChatAvatar
import com.lokaleza.amatyma.databinding.FragmentConversationsWithMessagesBinding
import com.google.firebase.auth.FirebaseAuth

class ConversationsWithMessagesFragment : Fragment() {

    private var _binding: FragmentConversationsWithMessagesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationsWithMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupConversations()
    }

    private fun setupConversations() {
        binding.conversations.onItemClick = OnItemClick { _, _, conversation ->
            when (conversation.conversationType) {
                CometChatConstants.CONVERSATION_TYPE_GROUP -> {
                    val group = conversation.conversationWith as Group
                    navigateToMessages(groupId = group.guid, name = group.name)
                }
                else -> {
                    val user = conversation.conversationWith as User
                    navigateToMessages(userId = user.uid, name = user.name)
                }
            }
        }

        binding.conversations.overflowMenu = buildAvatarMenu()
    }

    private fun buildAvatarMenu(): View? {
        val user = CometChatUIKit.getLoggedInUser() ?: return null

        val avatar = CometChatAvatar(requireContext()).apply {
            setAvatar(user.name, user.avatar)
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(com.cometchat.chatuikit.R.dimen.cometchat_40dp),
                resources.getDimensionPixelSize(com.cometchat.chatuikit.R.dimen.cometchat_40dp)
            ).also { it.gravity = Gravity.CENTER_VERTICAL }
            setOnClickListener { showProfileMenu(this) }
        }

        return avatar
    }

    private fun showProfileMenu(anchor: View) {
        val user = CometChatUIKit.getLoggedInUser() ?: return
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, user.name).isEnabled = false
        popup.menu.add(0, 2, 1, "Logout")
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == 2) logout()
            true
        }
        popup.show()
    }

    private fun logout() {
        CometChatUIKit.logout(object : CometChat.CallbackListener<String>() {
            override fun onSuccess(s: String) {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }

            override fun onError(e: CometChatException) { }
        })
    }

    private fun navigateToMessages(userId: String? = null, groupId: String? = null, name: String? = null) {
        val intent = Intent(requireActivity(), MessagesActivity::class.java).apply {
            userId?.let { putExtra("USER_ID", it) }
            groupId?.let { putExtra("GROUP_ID", it) }
            name?.let { putExtra("CONVERSATION_NAME", it) }
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
