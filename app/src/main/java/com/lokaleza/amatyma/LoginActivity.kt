package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.lokaleza.amatyma.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already logged in
        if (CometChatUIKit.getLoggedInUser() != null) {
            navigateToConversations()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val userId = binding.etUserId.text.toString().trim()

            if (validateUserId(userId)) {
                loginUser(userId)
            } else {
                showError("User ID must be at least 3 characters and contain no spaces")
            }
        }
    }

    private fun validateUserId(userId: String): Boolean {
        return userId.length >= 3 && !userId.contains(" ")
    }

    private fun loginUser(userId: String) {
        showLoading(true)
        hideError()

        CometChatUIKit.login(userId, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User?) {
                showLoading(false)
                if (user != null) {
                    Toast.makeText(this@LoginActivity, "Welcome ${user.name}!", Toast.LENGTH_SHORT).show()
                    navigateToConversations()
                }
            }

            override fun onError(exception: CometChatException?) {
                showLoading(false)
                showError("Login failed: ${exception?.message ?: "Please try again"}")
            }
        })
    }

    private fun navigateToConversations() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.etUserId.isEnabled = !show
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }
}
