package com.lokaleza.amatyma

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.lokaleza.amatyma.databinding.ActivityStoryViewerBinding
import java.util.concurrent.TimeUnit

data class StoryData(
    val storyId: String,
    val userId: String,
    val businessName: String,
    val logoUrl: String,
    val mediaUrl: String,
    val mediaType: String,
    val createdAt: Timestamp
)

class StoryViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoryViewerBinding
    private var stories = listOf<StoryData>()
    private var currentStoryIndex = 0

    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private val progressBars = mutableListOf<LinearProgressIndicator>()

    private val storyDuration = 5000L // 5 seconds per story
    private val progressUpdateInterval = 50L // Update progress every 50ms

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = intent.getStringExtra("USER_ID")
        if (userId == null) {
            Log.e(TAG, "No USER_ID provided")
            finish()
            return
        }

        setupClickListeners()
        loadUserStories(userId)
    }

    private fun setupClickListeners() {
        binding.ivClose.setOnClickListener {
            finish()
        }

        // Tap left side to go to previous story
        binding.touchLeft.setOnClickListener {
            showPreviousStory()
        }

        // Tap right side to go to next story
        binding.touchRight.setOnClickListener {
            showNextStory()
        }

        // Long press to pause
        var isPaused = false
        binding.root.setOnLongClickListener {
            if (isPaused) {
                resumeStory()
            } else {
                pauseStory()
            }
            isPaused = !isPaused
            true
        }
    }

    private fun loadUserStories(userId: String) {
        Log.d(TAG, "Loading stories for userId: $userId")
        val firestore = FirebaseFirestore.getInstance()
        val now = Timestamp.now()

        firestore.collection("stories")
            .whereEqualTo("userId", userId)
            .whereGreaterThan("expiresAt", now)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} stories for user $userId")
                stories = documents.map { doc ->
                    StoryData(
                        storyId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        businessName = doc.getString("businessName") ?: "",
                        logoUrl = doc.getString("logoUrl") ?: "",
                        mediaUrl = doc.getString("mediaUrl") ?: "",
                        mediaType = doc.getString("mediaType") ?: "image",
                        createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                    )
                }.sortedBy { it.createdAt }

                if (stories.isEmpty()) {
                    Log.d(TAG, "No stories found, finishing activity")
                    finish()
                    return@addOnSuccessListener
                }

                Log.d(TAG, "Setting up progress bars and showing first story")
                setupProgressBars()
                showStory(0)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading stories", e)
                finish()
            }
    }

    private fun setupProgressBars() {
        binding.progressContainer.removeAllViews()
        progressBars.clear()

        stories.forEach { _ ->
            val progressBar = LayoutInflater.from(this)
                .inflate(R.layout.item_story_progress, binding.progressContainer, false)
                as LinearProgressIndicator

            binding.progressContainer.addView(progressBar)
            progressBars.add(progressBar)
        }
    }

    private fun showStory(index: Int) {
        if (index < 0 || index >= stories.size) {
            finish()
            return
        }

        currentStoryIndex = index
        val story = stories[index]

        // Update business info
        binding.tvBusinessName.text = story.businessName
        binding.tvTimeAgo.text = getTimeAgo(story.createdAt)

        binding.ivBusinessLogo.load(story.logoUrl) {
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }

        // Display story media
        when (story.mediaType) {
            "video" -> {
                binding.ivStory.visibility = View.GONE
                binding.videoStory.visibility = View.VISIBLE

                binding.videoStory.setVideoPath(story.mediaUrl)
                binding.videoStory.setOnPreparedListener { mp ->
                    mp.start()
                    startProgress()
                }
                binding.videoStory.setOnCompletionListener {
                    showNextStory()
                }
            }
            else -> { // image
                binding.ivStory.visibility = View.VISIBLE
                binding.videoStory.visibility = View.GONE

                binding.ivStory.load(story.mediaUrl) {
                    placeholder(R.drawable.ic_launcher_background)
                    error(R.drawable.ic_launcher_background)
                }

                startProgress()
            }
        }

        // Reset all progress bars
        progressBars.forEachIndexed { i, bar ->
            when {
                i < index -> bar.progress = 100
                i == index -> bar.progress = 0
                else -> bar.progress = 0
            }
        }
    }

    private fun startProgress() {
        stopProgress()

        val startTime = System.currentTimeMillis()
        progressRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = ((elapsed.toFloat() / storyDuration) * 100).toInt()

                if (currentStoryIndex < progressBars.size) {
                    progressBars[currentStoryIndex].progress = progress
                }

                if (elapsed >= storyDuration) {
                    showNextStory()
                } else {
                    handler.postDelayed(this, progressUpdateInterval)
                }
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun stopProgress() {
        progressRunnable?.let {
            handler.removeCallbacks(it)
        }
        progressRunnable = null
    }

    private fun pauseStory() {
        stopProgress()
        if (binding.videoStory.visibility == View.VISIBLE && binding.videoStory.isPlaying) {
            binding.videoStory.pause()
        }
    }

    private fun resumeStory() {
        if (binding.videoStory.visibility == View.VISIBLE && !binding.videoStory.isPlaying) {
            binding.videoStory.start()
        } else {
            startProgress()
        }
    }

    private fun showNextStory() {
        if (currentStoryIndex < stories.size - 1) {
            showStory(currentStoryIndex + 1)
        } else {
            finish()
        }
    }

    private fun showPreviousStory() {
        if (currentStoryIndex > 0) {
            showStory(currentStoryIndex - 1)
        }
    }

    private fun getTimeAgo(timestamp: Timestamp): String {
        val diff = System.currentTimeMillis() - timestamp.toDate().time

        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)

        return when {
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }

    override fun onPause() {
        super.onPause()
        pauseStory()
    }

    override fun onResume() {
        super.onResume()
        if (stories.isNotEmpty()) {
            resumeStory()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProgress()
        if (binding.videoStory.visibility == View.VISIBLE) {
            binding.videoStory.stopPlayback()
        }
    }

    companion object {
        private const val TAG = "StoryViewer"
    }
}
