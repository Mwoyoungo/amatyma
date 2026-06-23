package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lokaleza.amatyma.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var isSignUpMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep the login button above the keyboard/nav bar on Android 15+ edge-to-edge devices.
        // fitsSystemWindows on the ScrollView handles the nav bar; this listener handles the IME.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val navBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = maxOf(imeInsets.bottom, navBarInsets.bottom))
            windowInsets
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Check if coming from splash screen
        val fromSplash = intent.getBooleanExtra("FROM_SPLASH", false)

        if (fromSplash) {
            // Coming from splash, skip auto-check (splash already checked)
            setupUI()
            setupClickListeners()
        } else {
            // Not from splash, check if user is already logged in
            checkExistingAuth()
        }
    }

    private fun checkExistingAuth() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    // Fix 3: must check cometChatSynced too — displayName alone doesn't mean
                    // CometChat user was created. Without it MainActivity will fail to log in.
                    val hasProfile = document.exists() && document.contains("displayName")
                    val cometChatSynced = document.getBoolean("cometChatSynced") ?: false
                    if (hasProfile && cometChatSynced) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        startActivity(Intent(this, ProfileSetupActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener {
                    setupUI()
                    setupClickListeners()
                }
        } else {
            setupUI()
            setupClickListeners()
        }
    }

    private fun setupUI() {
        updateUI()
    }

    private fun updateUI() {
        if (isSignUpMode) {
            binding.btnPrimary.text = "Sign Up"
            binding.tvToggle.text = "Already have an account? Login"
            binding.tilUsername.visibility = View.VISIBLE
            binding.tvForgotPassword.visibility = View.GONE
        } else {
            binding.btnPrimary.text = "Login"
            binding.tvToggle.text = "Don't have an account? Sign Up"
            binding.tilUsername.visibility = View.GONE
            binding.tvForgotPassword.visibility = View.VISIBLE
        }
        hideError()
    }

    private fun setupClickListeners() {
        binding.btnPrimary.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val username = binding.etUsername.text.toString().trim().lowercase()
            val password = binding.etPassword.text.toString()

            if (isSignUpMode) {
                signUp(email, username, password)
            } else {
                login(email, password)
            }
        }

        binding.tvToggle.setOnClickListener {
            isSignUpMode = !isSignUpMode
            updateUI()
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun signUp(email: String, username: String, password: String) {
        // Validation
        if (!validateSignUp(email, username, password)) return

        showLoading(true)

        // Check if username is already taken
        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    showLoading(false)
                    showError("Username already taken")
                    return@addOnSuccessListener
                }

                // Create Firebase account
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        if (user != null) {
                            // Save username to Firestore
                            val userData = hashMapOf(
                                "uid" to user.uid,
                                "email" to email,
                                "username" to username,
                                "cometChatSynced" to false,
                                "createdAt" to com.google.firebase.Timestamp.now()
                            )

                            firestore.collection("users").document(user.uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    showLoading(false)
                                    // Navigate to Profile Setup
                                    startActivity(Intent(this, ProfileSetupActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    showLoading(false)
                                    showError("Failed to save user data: ${e.message}")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        showError("Sign up failed: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Error checking username: ${e.message}")
            }
    }

    private fun login(email: String, password: String) {
        if (!validateLogin(email, password)) return

        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                // Check if profile is complete
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    firestore.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            showLoading(false)
                            // Fix 3: same check as checkExistingAuth — need both displayName
                            // and cometChatSynced before allowing entry to MainActivity.
                            val hasProfile = document.exists() && document.contains("displayName")
                            val cometChatSynced = document.getBoolean("cometChatSynced") ?: false
                            if (hasProfile && cometChatSynced) {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                startActivity(Intent(this, ProfileSetupActivity::class.java))
                                finish()
                            }
                        }
                        .addOnFailureListener {
                            showLoading(false)
                            startActivity(Intent(this, ProfileSetupActivity::class.java))
                            finish()
                        }
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Login failed: ${e.message}")
            }
    }

    private fun showForgotPasswordDialog() {
        val emailInput = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            hint = "Enter your email"
            // Pre-fill if the user already typed their email on the login screen
            val existingEmail = binding.etEmail.text.toString().trim()
            if (existingEmail.isNotEmpty()) setText(existingEmail)
            setPadding(48, 32, 48, 16)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("We'll send a reset link to your email.")
            .setView(emailInput)
            .setPositiveButton("Send") { _, _ ->
                val email = emailInput.text.toString().trim()
                if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showError("Please enter a valid email address")
                    return@setPositiveButton
                }
                sendPasswordReset(email)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendPasswordReset(email: String) {
        showLoading(true)
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                showLoading(false)
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Check your email")
                    .setMessage("A reset link has been sent to $email. Check your inbox and follow the link to set a new password.")
                    .setPositiveButton("OK", null)
                    .show()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                // Firebase returns a generic error for unknown emails intentionally (security).
                // Show a neutral message so we don't leak whether an account exists.
                showError("Could not send reset email. Check the address and try again.")
            }
    }

    private fun validateSignUp(email: String, username: String, password: String): Boolean {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email")
            return false
        }

        if (!isValidUsername(username)) {
            showError("Username must be 3-20 characters, lowercase letters, numbers, or underscore")
            return false
        }

        if (password.length < 6) {
            showError("Password must be at least 6 characters")
            return false
        }

        return true
    }

    private fun validateLogin(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            showError("Please enter your email")
            return false
        }

        if (password.isEmpty()) {
            showError("Please enter your password")
            return false
        }

        return true
    }

    private fun isValidUsername(username: String): Boolean {
        val regex = "^[a-z0-9_]{3,20}$".toRegex()
        return username.matches(regex)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnPrimary.isEnabled = !show
        binding.etEmail.isEnabled = !show
        binding.etUsername.isEnabled = !show
        binding.etPassword.isEnabled = !show
        binding.tvToggle.isEnabled = !show
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }
}
