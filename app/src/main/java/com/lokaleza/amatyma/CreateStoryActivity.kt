package com.lokaleza.amatyma

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.lokaleza.amatyma.databinding.ActivityCreateStoryBinding
import java.util.*

class CreateStoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateStoryBinding
    private var selectedMediaUri: Uri? = null
    private var mediaType: MediaType = MediaType.IMAGE

    private enum class MediaType {
        IMAGE, VIDEO
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            mediaType = MediaType.IMAGE
            displayMedia(it, MediaType.IMAGE)
        }
    }

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (isVideoValid(it)) {
                selectedMediaUri = it
                mediaType = MediaType.VIDEO
                displayMedia(it, MediaType.VIDEO)
            } else {
                android.widget.Toast.makeText(
                    this,
                    "Video must be 15 seconds or less",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.btnAddImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnAddVideo.setOnClickListener {
            videoPickerLauncher.launch("video/*")
        }

        binding.fabRemove.setOnClickListener {
            clearMedia()
        }

        binding.btnPostStory.setOnClickListener {
            if (selectedMediaUri != null) {
                uploadStory()
            } else {
                android.widget.Toast.makeText(this, "Please select media first", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayMedia(uri: Uri, type: MediaType) {
        binding.cardPreview.visibility = View.VISIBLE

        when (type) {
            MediaType.IMAGE -> {
                binding.ivPreview.visibility = View.VISIBLE
                binding.videoPreview.visibility = View.GONE
                binding.ivPlayIcon.visibility = View.GONE

                binding.ivPreview.load(uri) {
                    placeholder(R.drawable.ic_default_avatar)
                    error(R.drawable.ic_default_avatar)
                }
            }
            MediaType.VIDEO -> {
                binding.ivPreview.visibility = View.GONE
                binding.videoPreview.visibility = View.VISIBLE
                binding.ivPlayIcon.visibility = View.VISIBLE

                binding.videoPreview.setVideoURI(uri)
                binding.videoPreview.setOnPreparedListener { mp ->
                    mp.isLooping = true
                }

                var isPlaying = false
                val clickListener = View.OnClickListener {
                    if (isPlaying) {
                        binding.videoPreview.pause()
                        binding.ivPlayIcon.visibility = View.VISIBLE
                    } else {
                        binding.videoPreview.start()
                        binding.ivPlayIcon.visibility = View.GONE
                    }
                    isPlaying = !isPlaying
                }

                binding.videoPreview.setOnClickListener(clickListener)
                binding.ivPlayIcon.setOnClickListener(clickListener)
            }
        }
    }

    private fun clearMedia() {
        selectedMediaUri = null
        binding.cardPreview.visibility = View.GONE
        binding.ivPreview.setImageDrawable(null)
        binding.videoPreview.stopPlayback()
    }

    private fun isVideoValid(uri: Uri): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = duration?.toLongOrNull() ?: 0L
            val maxDurationMs = 15 * 1000 // 15 seconds
            durationMs <= maxDurationMs
        } catch (e: Exception) {
            Log.e(TAG, "Error checking video duration", e)
            false
        } finally {
            retriever.release()
        }
    }

    private fun uploadStory() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            android.widget.Toast.makeText(this, "User not authenticated", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val uri = selectedMediaUri ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnPostStory.isEnabled = false

        // First get business info
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("businesses").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    android.widget.Toast.makeText(this, "Please create a business profile first", android.widget.Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    binding.btnPostStory.isEnabled = true
                    return@addOnSuccessListener
                }

                val businessName = document.getString("businessName") ?: ""
                val logoUrl = document.getString("logoUrl") ?: ""

                // Upload to Firebase Storage
                val storyId = UUID.randomUUID().toString()
                val fileExtension = if (mediaType == MediaType.IMAGE) "jpg" else "mp4"
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("stories/$userId/$storyId.$fileExtension")

                storageRef.putFile(uri)
                    .addOnSuccessListener { taskSnapshot ->
                        taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                            saveStory(userId, storyId, businessName, logoUrl, downloadUrl.toString())
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error uploading story", e)
                        android.widget.Toast.makeText(this, "Failed to upload story", android.widget.Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                        binding.btnPostStory.isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading business info", e)
                android.widget.Toast.makeText(this, "Error loading business info", android.widget.Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.btnPostStory.isEnabled = true
            }
    }

    private fun saveStory(
        userId: String,
        storyId: String,
        businessName: String,
        logoUrl: String,
        mediaUrl: String
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val now = Timestamp.now()
        val expiresAt = Timestamp(Date(now.toDate().time + 24 * 60 * 60 * 1000)) // 24 hours later

        val storyData = hashMapOf(
            "storyId" to storyId,
            "userId" to userId,
            "businessName" to businessName,
            "logoUrl" to logoUrl,
            "mediaUrl" to mediaUrl,
            "mediaType" to if (mediaType == MediaType.IMAGE) "image" else "video",
            "createdAt" to now,
            "expiresAt" to expiresAt
        )

        firestore.collection("stories").document(storyId)
            .set(storyData)
            .addOnSuccessListener {
                android.widget.Toast.makeText(this, "Story shared!", android.widget.Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving story", e)
                android.widget.Toast.makeText(this, "Failed to save story", android.widget.Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.btnPostStory.isEnabled = true
            }
    }

    companion object {
        private const val TAG = "CreateStory"
    }
}
