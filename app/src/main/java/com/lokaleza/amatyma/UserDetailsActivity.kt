package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import coil.transform.CircleCropTransformation
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.calls.CometChatCallActivity
import com.lokaleza.amatyma.databinding.ActivityUserDetailsBinding

class UserDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDetailsBinding

    companion object {
        const val EXTRA_USER_ID = "user_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val uid = intent.getStringExtra(EXTRA_USER_ID)
        if (uid == null) { finish(); return }

        fetchAndShowUser(uid)
    }

    private fun fetchAndShowUser(uid: String) {
        CometChat.getUser(uid, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                runOnUiThread { bindUser(user) }
            }

            override fun onError(e: CometChatException?) {
                runOnUiThread {
                    Toast.makeText(this@UserDetailsActivity, "Could not load profile", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        })
    }

    private fun bindUser(user: User) {
        binding.tvName.text = user.name

        val isOnline = user.status == "online"
        binding.tvStatus.text = if (isOnline) "Online" else "Offline"
        binding.vStatusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (isOnline) getColor(R.color.success) else getColor(R.color.gray_400)
        )

        if (!user.avatar.isNullOrEmpty()) {
            binding.ivAvatar.load(user.avatar) {
                transformations(CircleCropTransformation())
                placeholder(R.drawable.circle_bg)
                error(R.drawable.circle_bg)
            }
        }

        binding.btnStartChat.setOnClickListener {
            startActivity(
                Intent(this, MessagesActivity::class.java).apply {
                    putExtra("USER_ID", user.uid)
                    putExtra("USER_NAME", user.name)
                }
            )
        }

        binding.btnAudioCall.setOnClickListener {
            initiateCall(user, CometChatConstants.CALL_TYPE_AUDIO)
        }

        binding.btnVideoCall.setOnClickListener {
            initiateCall(user, CometChatConstants.CALL_TYPE_VIDEO)
        }
    }

    private fun initiateCall(user: User, callType: String) {
        binding.btnAudioCall.isEnabled = false
        binding.btnVideoCall.isEnabled = false

        val call = Call(user.uid, CometChatConstants.RECEIVER_TYPE_USER, callType)
        CometChat.initiateCall(call, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(c: Call) {
                runOnUiThread {
                    binding.btnAudioCall.isEnabled = true
                    binding.btnVideoCall.isEnabled = true
                    CometChatCallActivity.launchOutgoingCallScreen(this@UserDetailsActivity, c, null)
                }
            }

            override fun onError(e: CometChatException?) {
                runOnUiThread {
                    binding.btnAudioCall.isEnabled = true
                    binding.btnVideoCall.isEnabled = true
                    Toast.makeText(this@UserDetailsActivity, "Call failed: ${e?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    // Prevent screen rotation from dimming the buttons mid-call-initiation
    override fun onResume() {
        super.onResume()
        binding.btnAudioCall.isEnabled = true
        binding.btnVideoCall.isEnabled = true
    }
}
