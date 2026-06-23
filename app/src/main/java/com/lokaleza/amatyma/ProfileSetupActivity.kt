package com.lokaleza.amatyma

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep the NEXT button above the keyboard/nav bar on Android 15+ edge-to-edge devices.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val navBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = maxOf(imeInsets.bottom, navBarInsets.bottom))
            windowInsets
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        functions = FirebaseFunctions.getInstance()

        // Block back press — this screen is mandatory.
        // If the user skips, CometChat user never gets created and they can't message anyone.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@ProfileSetupActivity)
                    .setTitle("Setup Required")
                    .setMessage("You need to complete your profile to use Amatyma. Please enter your name to continue.")
                    .setPositiveButton("Continue Setup", null)
                    .setNegativeButton("Logout") { _, _ ->
                        // Let them escape only by logging out fully
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this@ProfileSetupActivity, AuthActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    }
                    .show()
            }
        })

        // If this user somehow already completed setup (e.g. re-entering this screen after a
        // session expiry), detect it and fast-path them through instead of re-running the flow.
        checkIfAlreadySetUp()

        setupClickListeners()
    }

    private fun checkIfAlreadySetUp() {
        val userId = auth.currentUser?.uid ?: return

        // Check local flag first — avoids any network call
        val prefs = getSharedPreferences("amatyma_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("cometchat_setup_$userId", false)) {
            // Already fully set up. Just log in to CometChat and go to main.
            showLoading(true)
            CometChatUIKit.login(userId, object : CometChat.CallbackListener<User>() {
                override fun onSuccess(user: User?) {
                    showLoading(false)
                    startActivity(Intent(this@ProfileSetupActivity, MainActivity::class.java))
                    finish()
                }
                override fun onError(exception: CometChatException?) {
                    // Login failed — let them re-do setup to recover
                    showLoading(false)
                }
            })
        }
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
        // Fix 1: use the authToken returned by the Cloud Function, not the AUTH_KEY.
        // loginWithAuthToken is the secure path for client-side login.
        CometChatUIKit.loginWithAuthToken(authToken, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User?) {
                // Persist the setup-complete flag locally so SplashActivity can route
                // instantly on future launches without a Firestore round-trip.
                val prefs = getSharedPreferences("amatyma_prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("cometchat_setup_$uid", true).apply()

                // Mark as synced in Firestore (best-effort — don't block navigation on this)
                firestore.collection("users").document(uid)
                    .update("cometChatSynced", true)
                    .addOnSuccessListener {
                        showLoading(false)
                        startActivity(Intent(this@ProfileSetupActivity, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        // Firestore update failed (poor network) but CometChat login succeeded.
                        // Still navigate — the flag is saved locally and Firestore will sync later.
                        showLoading(false)
                        startActivity(Intent(this@ProfileSetupActivity, MainActivity::class.java))
                        finish()
                    }
            }

            override fun onError(exception: CometChatException?) {
                showLoading(false)
                showError("Chat login failed. Check your connection and try again.")
                Log.e("ProfileSetup", "CometChat login error: ${exception?.message}")
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
