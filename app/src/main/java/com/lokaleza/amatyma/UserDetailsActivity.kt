package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import coil.transform.CircleCropTransformation
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
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
    }
}
