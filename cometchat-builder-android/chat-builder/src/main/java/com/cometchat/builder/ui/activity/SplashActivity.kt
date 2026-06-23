package com.cometchat.builder.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.builder.AppCredentials
import com.cometchat.builder.R
import com.cometchat.builder.databinding.BuilderActivitySplashBinding
import com.cometchat.builder.utils.AppUtils
import com.cometchat.builder.viewmodels.SplashViewModel

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = BuilderActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkAppCredentials()
    }

    private fun checkAppCredentials() {
        val appId = AppUtils.getDataFromSharedPref(this, String::class.java, R.string.builder_cred_id, AppCredentials.APP_ID)

        if (appId.isNullOrEmpty()) {
            startActivity(Intent(this, AppCredentialsActivity::class.java))
            finish()
        } else {
            initViewModel()
        }
    }

    private fun initViewModel() { // Initialize ViewModel
        val viewModel: SplashViewModel = ViewModelProvider(this)[SplashViewModel::class.java] // Initialize CometChat UIKit
        if (!CometChatUIKit.isSDKInitialized()) {
            viewModel.initUIKit(this)
        } else {
            viewModel.checkUserIsNotLoggedIn()
        } // Observe login status
        viewModel.getLoginStatus().observe(this) { isLoggedIn ->
            if (isLoggedIn != null) {
                if (isLoggedIn) {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(
                        this@SplashActivity, LoginActivity::class.java
                    )
                    Toast.makeText(
                        this@SplashActivity, R.string.builder_not_logged_in, Toast.LENGTH_SHORT
                    ).show()
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
