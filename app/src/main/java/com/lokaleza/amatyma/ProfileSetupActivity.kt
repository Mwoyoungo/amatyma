package com.lokaleza.amatyma

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.lokaleza.amatyma.databinding.ActivityProfileSetupBinding
import java.util.UUID

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var functions: FirebaseFunctions

    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivProfilePhoto.load(it) {
                crossfade(true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        functions = FirebaseFunctions.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.fabChangePhoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.tvChangePhoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnNext.setOnClickListener {
            val displayName = binding.etDisplayName.text.toString().trim()

            if (displayName.isEmpty()) {
                showError("Please enter your name")
                return@setOnClickListener
            }

            if (displayName.length < 2) {
                showError("Name must be at least 2 characters")
                return@setOnClickListener
            }

            saveProfile(displayName)
        }
    }

    private fun saveProfile(displayName: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showError("User not logged in")
            return
        }

        showLoading(true)

        if (selectedImageUri != null) {
            // Upload photo first, then save profile
            uploadPhoto(userId, displayName)
        } else {
            // No photo, save profile without photo
            saveToFirestore(userId, displayName, null)
        }
    }

    private fun uploadPhoto(userId: String, displayName: String) {
        val imageRef = storage.reference.child("users/$userId/profile.jpg")

        selectedImageUri?.let { uri ->
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    // Get download URL
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveToFirestore(userId, displayName, downloadUri.toString())
                    }.addOnFailureListener { e ->
                        showLoading(false)
                        showError("Failed to get photo URL: ${e.message}")
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    showError("Failed to upload photo: ${e.message}")
                }
        }
    }

    private fun saveToFirestore(userId: String, displayName: String, photoURL: String?) {
        // Get existing user data
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val username = document.getString("username") ?: ""
                val email = document.getString("email") ?: ""

                // Update with profile data
                val updates = hashMapOf<String, Any>(
                    "displayName" to displayName,
                    "photoURL" to (photoURL ?: ""),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )

                firestore.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener {
                        // Now create CometChat user
                        createCometChatUser(userId, displayName, photoURL ?: "", username, email)
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        showError("Failed to save profile: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Failed to get user data: ${e.message}")
            }
    }

    private fun createCometChatUser(
        uid: String,
        name: String,
        avatar: String,
        username: String,
        email: String
    ) {
        val data = hashMapOf(
            "uid" to uid,
            "name" to name,
            "avatar" to avatar,
            "username" to username,
            "email" to email
        )

        functions.getHttpsCallable("createCometChatUser")
            .call(data)
            .addOnSuccessListener { result ->
                val resultData = result.getData() as? Map<*, *>
                val authToken = resultData?.get("authToken") as? String

                if (authToken != null) {
                    // Login to CometChat
                    loginToCometChat(uid, authToken)
                } else {
                    showLoading(false)
                    showError("Failed to get auth token")
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e("ProfileSetup", "Cloud function error", e)
                showError("Failed to create chat account: ${e.message}")
            }
    }

    private fun loginToCometChat(uid: String, authToken: String) {
        CometChatUIKit.login(uid, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User?) {
                // Mark as synced in Firestore
                firestore.collection("users").document(uid)
                    .update("cometChatSynced", true)
                    .addOnSuccessListener {
                        showLoading(false)
                        // Navigate to main app
                        startActivity(Intent(this@ProfileSetupActivity, MainActivity::class.java))
                        finish()
                    }
            }

            override fun onError(exception: CometChatException?) {
                showLoading(false)
                showError("Chat login failed: ${exception?.message}")
            }
        })
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnNext.isEnabled = !show
        binding.etDisplayName.isEnabled = !show
        binding.fabChangePhoto.isEnabled = !show
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }
}
