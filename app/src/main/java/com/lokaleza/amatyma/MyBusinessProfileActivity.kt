package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.cometchat.chat.core.CometChat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lokaleza.amatyma.databinding.ActivityMyBusinessProfileBinding

class MyBusinessProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyBusinessProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var userId: String? = null

    companion object {
        private const val TAG = "MyBusinessProfile"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyBusinessProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        userId = auth.currentUser?.uid
        if (userId == null) { finish(); return }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnLogout.setOnClickListener { showLogoutConfirmDialog() }

        loadCachedProfile()
        loadProfile()
    }

    private fun loadCachedProfile() {
        val prefs = getSharedPreferences("profile_cache", MODE_PRIVATE)
        if (prefs.getString("cached_user_id", "") != userId) return

        val displayName = prefs.getString("displayName", "")
        val username    = prefs.getString("username", "")
        val photoURL    = prefs.getString("photoURL", "")

        if (!displayName.isNullOrEmpty()) binding.tvBusinessName.text = displayName
        if (!username.isNullOrEmpty())    binding.tvCategory.text = "@$username"
        if (!photoURL.isNullOrEmpty()) {
            binding.ivLogo.load(photoURL) {
                placeholder(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }
        }
    }

    private fun loadProfile() {
        val uid = userId ?: return

        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val displayName = doc.getString("displayName") ?: ""
                val username    = doc.getString("username") ?: ""
                val photoURL    = doc.getString("photoURL") ?: ""

                binding.tvBusinessName.text = displayName.ifEmpty { username }
                binding.tvCategory.text = if (username.isNotEmpty()) "@$username" else ""

                if (photoURL.isNotEmpty()) {
                    binding.ivLogo.load(photoURL) {
                        placeholder(R.drawable.ic_default_avatar)
                        error(R.drawable.ic_default_avatar)
                    }
                }

                getSharedPreferences("profile_cache", MODE_PRIVATE).edit().apply {
                    putString("cached_user_id", uid)
                    putString("displayName", displayName)
                    putString("username", username)
                    putString("photoURL", photoURL)
                    apply()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading profile", e)
            }
    }

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ -> performLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        getSharedPreferences("profile_cache", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("amatyma_prefs", MODE_PRIVATE).edit().clear().apply()

        CometChat.logout(object : CometChat.CallbackListener<String>() {
            override fun onSuccess(message: String?) {
                Log.d(TAG, "CometChat logout successful")
            }
            override fun onError(exception: com.cometchat.chat.exceptions.CometChatException?) {
                Log.e(TAG, "CometChat logout error: ${exception?.message}")
            }
        })

        auth.signOut()

        startActivity(Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
