package com.lokaleza.amatyma

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.constants.UIKitConstants
import com.cometchat.chatuikit.shared.resources.utils.Utils
import com.google.gson.Gson
import com.lokaleza.amatyma.databinding.ActivityGroupDetailsBinding
import com.lokaleza.amatyma.databinding.LayoutAddMembersDialogBinding
import com.lokaleza.amatyma.databinding.LayoutGroupMembersDialogBinding

class GroupDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupDetailsBinding
    private lateinit var group: Group
    private var dialog: Dialog? = null

    companion object {
        const val EXTRA_GROUP = "extra_group"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }

        group = Gson().fromJson(intent.getStringExtra(EXTRA_GROUP), Group::class.java)

        bindGroupData()
        setupClicks()
    }

    private fun bindGroupData() {
        binding.avatar.setAvatar(group.name, group.icon)
        binding.tvGroupName.text = group.name
        val memberText = if (group.membersCount == 1) "1 Member" else "${group.membersCount} Members"
        binding.tvMemberCount.text = memberText

        applyOptionsVisibility()
    }

    private fun applyOptionsVisibility() {
        binding.viewMembers.visibility = View.GONE
        binding.viewAddMembers.visibility = View.GONE
        binding.leaveGroupLay.visibility = View.GONE
        binding.deleteGroupLay.visibility = View.GONE

        if (!group.isJoined) return

        binding.viewMembers.visibility = View.VISIBLE

        when (group.scope) {
            UIKitConstants.GroupMemberScope.PARTICIPANTS -> {
                binding.leaveGroupLay.visibility = View.VISIBLE
            }
            UIKitConstants.GroupMemberScope.MODERATOR -> {
                binding.viewAddMembers.visibility = View.VISIBLE
                binding.leaveGroupLay.visibility = View.VISIBLE
            }
            UIKitConstants.GroupMemberScope.ADMIN -> {
                binding.viewAddMembers.visibility = View.VISIBLE
                binding.deleteGroupLay.visibility = View.VISIBLE
                if (group.membersCount > 1) binding.leaveGroupLay.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClicks() {
        binding.ivBack.setOnClickListener { finish() }

        binding.viewMembers.setOnClickListener { showGroupMembersDialog() }

        binding.viewAddMembers.setOnClickListener { showAddMembersDialog() }

        binding.leaveGroupLay.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Leave Group")
                .setMessage("Are you sure you want to leave ${group.name}?")
                .setPositiveButton("Leave") { _, _ -> leaveGroup() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.deleteGroupLay.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete & Exit")
                .setMessage("This will permanently delete ${group.name}. This cannot be undone.")
                .setPositiveButton("Delete") { _, _ -> deleteGroup() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showGroupMembersDialog() {
        val dialogBinding = LayoutGroupMembersDialogBinding.inflate(layoutInflater)
        dialogBinding.viewMembers.setGroup(group)

        val builder = AlertDialog.Builder(this, androidx.appcompat.R.style.AlertDialog_AppCompat)
        Utils.removeParentFromView(dialogBinding.root)
        builder.setView(dialogBinding.root)
        dialog = builder.create()
        dialog?.show()

        dialogBinding.viewMembers.setOnBackPressListener { dialog?.dismiss() }
    }

    private fun showAddMembersDialog() {
        val dialogBinding = LayoutAddMembersDialogBinding.inflate(layoutInflater)

        dialogBinding.addMembers.setTitleText(getString(com.cometchat.chatuikit.R.string.cometchat_add_members))
        dialogBinding.addMembers.setSelectionMode(UIKitConstants.SelectionMode.MULTIPLE)
        dialogBinding.addMembers.setSubmitSelectionIconVisibility(View.GONE)
        dialogBinding.addMembers.backIconVisibility = View.VISIBLE
        dialogBinding.addMembers.setOnItemClick { _, _, user ->
            dialogBinding.addMembers.selectUser(user, UIKitConstants.SelectionMode.MULTIPLE)
        }

        dialogBinding.addMembersBtn.setOnClickListener {
            val selectedUsers = dialogBinding.addMembers.selectedUsers
            if (selectedUsers.isEmpty()) return@setOnClickListener
            addMembers(selectedUsers, dialogBinding)
        }

        val builder = AlertDialog.Builder(this, androidx.appcompat.R.style.AlertDialog_AppCompat)
        Utils.removeParentFromView(dialogBinding.root)
        builder.setView(dialogBinding.root)
        dialog = builder.create()
        dialog?.show()

        dialogBinding.addMembers.setOnBackPressListener { dialog?.dismiss() }
    }

    private fun addMembers(users: List<User>, dialogBinding: LayoutAddMembersDialogBinding) {
        dialogBinding.tvAddMembers.visibility = View.GONE
        dialogBinding.progress.visibility = View.VISIBLE

        val members = users.map { user ->
            GroupMember(user.uid, CometChatConstants.SCOPE_PARTICIPANT).apply {
                avatar = user.avatar
                name = user.name
                status = user.status
            }
        }

        CometChat.addMembersToGroup(group.guid, members, arrayListOf(), object : CometChat.CallbackListener<HashMap<String, String>>() {
            override fun onSuccess(result: HashMap<String, String>) {
                runOnUiThread {
                    group.membersCount += members.size
                    binding.tvMemberCount.text = "${group.membersCount} Members"
                    dialog?.dismiss()
                    Toast.makeText(this@GroupDetailsActivity, "Members added", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(e: CometChatException) {
                runOnUiThread {
                    dialogBinding.progress.visibility = View.GONE
                    dialogBinding.tvAddMembers.visibility = View.VISIBLE
                    dialogBinding.tvError.text = e.message
                    dialogBinding.tvError.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun leaveGroup() {
        CometChat.leaveGroup(group.guid, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(s: String) {
                runOnUiThread { finishAffinity() }
            }
            override fun onError(e: CometChatException) {
                runOnUiThread {
                    Toast.makeText(this@GroupDetailsActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun deleteGroup() {
        CometChat.deleteGroup(group.guid, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(s: String) {
                runOnUiThread { finishAffinity() }
            }
            override fun onError(e: CometChatException) {
                runOnUiThread {
                    Toast.makeText(this@GroupDetailsActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
