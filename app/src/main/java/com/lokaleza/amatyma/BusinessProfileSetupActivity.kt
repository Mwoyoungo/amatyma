package com.lokaleza.amatyma

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.lokaleza.amatyma.databinding.ActivityBusinessProfileSetupBinding

class BusinessProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBusinessProfileSetupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var selectedLogoUri: Uri? = null

    private val categories = listOf(
        "Construction",
        "Tech",
        "Education",
        "Finance",
        "Legal",
        "Health",
        "Transport/Logistics",
        "Retail/Shopping",
        "Food & Beverage",
        "Beauty & Wellness",
        "Automotive",
        "Real Estate",
        "Home Services",
        "Entertainment",
        "Professional Services",
        "Hospitality",
        "Agriculture",
        "Manufacturing",
        "Arts & Crafts",
        "Sports & Recreation",
        "Media & Communications",
        "Fashion & Clothing",
        "Telecommunications",
        "Energy/Utilities",
        "Security Services",
        "Charity/NPO"
    )

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedLogoUri = it
            binding.ivBusinessLogo.load(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupCategoryDropdown()
        setupClickListeners()
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.acCategory.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.fabChangeLogo.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnCreate.setOnClickListener {
            createBusinessProfile()
        }
    }

    private fun createBusinessProfile() {
        val businessName = binding.etBusinessName.text.toString().trim()
        val category = binding.acCategory.text.toString().trim()
        val tagline = binding.etTagline.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val services = binding.etServices.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()

        // Validation
        if (!validateInput(businessName, category, tagline, description, location)) {
            return
        }

        showLoading(true)

        val userId = auth.currentUser?.uid
        if (userId == null) {
            showError("User not authenticated")
            showLoading(false)
            return
        }

        // Upload logo if selected
        if (selectedLogoUri != null) {
            uploadLogoAndSaveProfile(userId, businessName, category, tagline, description, services, location)
        } else {
            saveBusinessProfile(userId, businessName, category, tagline, description, services, location, "")
        }
    }

    private fun validateInput(
        businessName: String,
        category: String,
        tagline: String,
        description: String,
        location: String
    ): Boolean {
        if (businessName.isEmpty()) {
            showError("Please enter your business name")
            return false
        }

        if (category.isEmpty()) {
            showError("Please select a category")
            return false
        }

        if (tagline.isEmpty()) {
            showError("Please enter a tagline")
            return false
        }

        if (description.isEmpty()) {
            showError("Please describe your business")
            return false
        }

        if (location.isEmpty()) {
            showError("Please enter your location")
            return false
        }

        return true
    }

    private fun uploadLogoAndSaveProfile(
        userId: String,
        businessName: String,
        category: String,
        tagline: String,
        description: String,
        services: String,
        location: String
    ) {
        val storageRef = storage.reference.child("businesses/$userId/logo.jpg")

        storageRef.putFile(selectedLogoUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveBusinessProfile(
                        userId,
                        businessName,
                        category,
                        tagline,
                        description,
                        services,
                        location,
                        uri.toString()
                    )
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Failed to upload logo", e)
                showError("Failed to upload logo: ${e.message}")
            }
    }

    private fun saveBusinessProfile(
        userId: String,
        businessName: String,
        category: String,
        tagline: String,
        description: String,
        services: String,
        location: String,
        logoUrl: String
    ) {
        val servicesList = services.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val businessData = hashMapOf(
            "userId" to userId,
            "businessName" to businessName,
            "category" to category,
            "tagline" to tagline,
            "description" to description,
            "services" to servicesList,
            "location" to location,
            "logoUrl" to logoUrl,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now(),
            "rating" to 0.0,
            "totalMessages" to 0,
            "isActive" to true
        )

        firestore.collection("businesses").document(userId)
            .set(businessData)
            .addOnSuccessListener {
                showLoading(false)
                // Navigate to main app
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Failed to save business profile", e)
                showError("Failed to save business profile: ${e.message}")
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnCreate.isEnabled = !show
        binding.etBusinessName.isEnabled = !show
        binding.acCategory.isEnabled = !show
        binding.etTagline.isEnabled = !show
        binding.etDescription.isEnabled = !show
        binding.etServices.isEnabled = !show
        binding.etLocation.isEnabled = !show
        binding.fabChangeLogo.isEnabled = !show
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    companion object {
        private const val TAG = "BusinessProfileSetup"
    }
}
