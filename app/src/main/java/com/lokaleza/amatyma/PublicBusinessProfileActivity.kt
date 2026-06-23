package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lokaleza.amatyma.databinding.ActivityPublicBusinessProfileBinding

class PublicBusinessProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPublicBusinessProfileBinding
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublicBusinessProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get user ID from intent
        userId = intent.getStringExtra("USER_ID")
        if (userId == null || userId!!.isEmpty()) {
            Log.e("PublicBusinessProfile", "No USER_ID provided")
            android.widget.Toast.makeText(this, "Invalid user ID", android.widget.Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("PublicBusinessProfile", "Loading profile for userId: $userId")

        setupToolbar()
        setupTabs()
        loadBusinessInfo()
        loadPosts()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // Posts tab
                        binding.rvPosts.visibility = View.VISIBLE
                        binding.layoutAbout.visibility = View.GONE
                    }
                    1 -> {
                        // About tab
                        binding.rvPosts.visibility = View.GONE
                        binding.layoutAbout.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun loadBusinessInfo() {
        val firestore = FirebaseFirestore.getInstance()

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollView.visibility = View.GONE

        firestore.collection("businesses").document(userId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val businessName = document.getString("businessName") ?: ""
                    val category = document.getString("category") ?: ""
                    val tagline = document.getString("tagline") ?: ""
                    val description = document.getString("description") ?: ""
                    val services = document.get("services") as? List<*>
                    val location = document.getString("location") ?: ""
                    val logoUrl = document.getString("logoUrl") ?: ""

                    binding.tvBusinessName.text = businessName
                    binding.tvCategory.text = "🏗️ $category"
                    binding.tvTagline.text = tagline
                    binding.tvDescription.text = description
                    binding.tvServices.text = services?.joinToString(", ") ?: ""
                    binding.tvLocation.text = location

                    binding.ivLogo.load(logoUrl) {
                        placeholder(R.drawable.ic_default_avatar)
                        error(R.drawable.ic_default_avatar)
                    }

                    // TODO: Load actual stats
                    binding.tvMessageCount.text = "0"
                    binding.tvRating.text = "0.0"

                    // Setup toolbar title
                    binding.toolbar.title = businessName

                    // Hide loading, show content
                    binding.progressBar.visibility = View.GONE
                    binding.scrollView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Log.e("PublicBusinessProfile", "Error loading business info", e)
                android.widget.Toast.makeText(this, "Error loading profile", android.widget.Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                finish()
            }

        // Setup message button
        binding.btnMessage.setOnClickListener {
            if (userId != null) {
                val intent = Intent(this, MessagesActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
        }
    }

    private fun loadPosts() {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("posts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val posts = documents.map { doc ->
                    Post(
                        postId = doc.getString("postId") ?: "",
                        mediaUrl = doc.getString("mediaUrl") ?: "",
                        mediaType = doc.getString("mediaType") ?: "image",
                        caption = doc.getString("caption") ?: ""
                    )
                }.sortedByDescending { it.postId }

                binding.tvPostCount.text = posts.size.toString()

                binding.rvPosts.layoutManager = GridLayoutManager(this, 3)
                binding.rvPosts.adapter = PostGridAdapter(
                    posts = posts,
                    onPostClick = { post ->
                        val intent = Intent(this, PostDetailActivity::class.java)
                        intent.putExtra("POST_ID", post.postId)
                        startActivity(intent)
                    },
                    onPostLongClick = null
                )
            }
            .addOnFailureListener { e ->
                Log.e("PublicBusinessProfile", "Error loading posts", e)
                android.widget.Toast.makeText(this, "Failed to load posts", android.widget.Toast.LENGTH_SHORT).show()
            }
    }
}
