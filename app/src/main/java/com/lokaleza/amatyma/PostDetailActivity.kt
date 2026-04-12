package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.lokaleza.amatyma.databinding.ActivityPostDetailBinding
import java.text.SimpleDateFormat
import java.util.*

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private var postId: String? = null
    private var userId: String? = null
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get post ID from intent
        postId = intent.getStringExtra("POST_ID")
        if (postId == null) {
            Log.e("PostDetailActivity", "No POST_ID provided")
            finish()
            return
        }

        setupToolbar()
        loadPostDetails()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadPostDetails() {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("posts").document(postId!!)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.e("PostDetailActivity", "Post not found")
                    finish()
                    return@addOnSuccessListener
                }

                userId = document.getString("userId")
                val mediaUrl = document.getString("mediaUrl") ?: ""
                val mediaType = document.getString("mediaType") ?: "image"
                val caption = document.getString("caption") ?: ""
                val createdAt = document.getTimestamp("createdAt")

                // Display caption
                binding.tvCaption.text = caption

                // Display media
                displayMedia(mediaUrl, mediaType)

                // Format and display date
                if (createdAt != null) {
                    binding.tvPostDate.text = formatDate(createdAt)
                }

                // Load business info
                if (userId != null) {
                    loadBusinessInfo(userId!!)
                }

                // Setup click listener for business info section
                binding.businessInfoSection.setOnClickListener {
                    if (userId != null) {
                        val intent = Intent(this, PublicBusinessProfileActivity::class.java)
                        intent.putExtra("USER_ID", userId)
                        startActivity(intent)
                    }
                }

                // Setup message button
                binding.btnMessage.setOnClickListener {
                    if (userId != null) {
                        val intent = Intent(this, MessagesActivity::class.java)
                        intent.putExtra("USER_ID", userId)
                        startActivity(intent)
                    } else {
                        android.widget.Toast.makeText(this, "User ID not found", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("PostDetailActivity", "Error loading post", e)
                android.widget.Toast.makeText(this, "Error loading post", android.widget.Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayMedia(mediaUrl: String, mediaType: String) {
        if (mediaType == "video") {
            // Show video view
            binding.ivPostImage.visibility = View.GONE
            binding.videoView.visibility = View.VISIBLE
            binding.ivPlayButton.visibility = View.VISIBLE

            // Setup video
            binding.videoView.setVideoPath(mediaUrl)
            binding.videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
            }

            // Play/pause on click
            val clickListener = View.OnClickListener {
                if (isPlaying) {
                    binding.videoView.pause()
                    binding.ivPlayButton.visibility = View.VISIBLE
                } else {
                    binding.videoView.start()
                    binding.ivPlayButton.visibility = View.GONE
                }
                isPlaying = !isPlaying
            }

            binding.videoView.setOnClickListener(clickListener)
            binding.ivPlayButton.setOnClickListener(clickListener)

        } else {
            // Show image
            binding.ivPostImage.visibility = View.VISIBLE
            binding.videoView.visibility = View.GONE
            binding.ivPlayButton.visibility = View.GONE

            binding.ivPostImage.load(mediaUrl) {
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
            }
        }
    }

    private fun loadBusinessInfo(userId: String) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("businesses").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val businessName = document.getString("businessName") ?: "Unknown Business"
                    val category = document.getString("category") ?: ""
                    val logoUrl = document.getString("logoUrl") ?: ""

                    binding.tvBusinessName.text = businessName
                    binding.tvBusinessCategory.text = "🏗️ $category"

                    binding.ivBusinessLogo.load(logoUrl) {
                        placeholder(R.drawable.ic_launcher_background)
                        error(R.drawable.ic_launcher_background)
                    }

                    // TODO: Load actual stats
                    binding.tvMessages.text = "💬 0 messages"
                    binding.tvRating.text = "⭐ 0.0 rating"
                }
            }
            .addOnFailureListener { e ->
                Log.e("PostDetailActivity", "Error loading business info", e)
            }
    }

    private fun formatDate(timestamp: Timestamp): String {
        val date = timestamp.toDate()
        val now = Date()
        val diff = now.time - date.time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 7 -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
            days > 0 -> "$days days ago"
            hours > 0 -> "$hours hours ago"
            minutes > 0 -> "$minutes minutes ago"
            else -> "Just now"
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause video when activity is paused
        if (binding.videoView.isPlaying) {
            binding.videoView.pause()
            isPlaying = false
        }
    }
}
