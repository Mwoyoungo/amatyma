package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.lokaleza.amatyma.databinding.FragmentGroupsWithMessagesBinding

class GroupsWithMessagesFragment : Fragment() {

    private var _binding: FragmentGroupsWithMessagesBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "GroupsWithMessages"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupsWithMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGroups()
    }

    private fun setupGroups() {
        binding.groupsWithMessages.setLoadingStateVisibility(View.GONE)

        binding.groupsWithMessages.setOnItemClick { _: View, _: Int, group: Group ->
            handleGroupClick(group)
        }
    }


    private fun handleGroupClick(group: Group) {
        // Try to join the group - if already joined, will handle via error callback
        joinGroup(group)
    }

    private fun joinGroup(group: Group) {
        val groupType = group.groupType
        val password = ""

        CometChat.joinGroup(group.guid, groupType, password, object : CometChat.CallbackListener<Group>() {
            override fun onSuccess(joinedGroup: Group?) {
                Log.d(TAG, "Group joined successfully: ${joinedGroup?.name}")
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Joined ${joinedGroup?.name}", Toast.LENGTH_SHORT).show()
                    // Navigate to messages after successful join
                    joinedGroup?.let { navigateToMessages(it) }
                }
            }

            override fun onError(exception: CometChatException?) {
                Log.e(TAG, "Failed to join group: ${exception?.message}")
                requireActivity().runOnUiThread {
                    when (exception?.code) {
                        "ERR_ALREADY_JOINED" -> {
                            // User is already in the group, just navigate
                            navigateToMessages(group)
                        }
                        "ERR_PASSWORD_REQUIRED" -> {
                            Toast.makeText(requireContext(), "This group requires a password", Toast.LENGTH_SHORT).show()
                            // TODO: Show password dialog
                        }
                        else -> {
                            Toast.makeText(requireContext(), "Failed to join group: ${exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun navigateToMessages(group: Group) {
        val intent = Intent(requireActivity(), MessagesActivity::class.java).apply {
            putExtra("GROUP_ID", group.guid)
            putExtra("GROUP_NAME", group.name)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
