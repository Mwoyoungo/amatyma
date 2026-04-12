package com.lokaleza.amatyma

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.lokaleza.amatyma.databinding.ActivityCreatePostBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var selectedMediaUri: Uri? = null
    private var mediaType: MediaType = MediaType.NONE
    private var selectedPdfUri: Uri? = null
    private var selectedPdfName: String? = null

    private enum class MediaType {
        NONE, IMAGE, VIDEO
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            mediaType = MediaType.IMAGE
            showMediaPreview()
        }
    }

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (isVideoValid(it)) {
                selectedMediaUri = it
                mediaType = MediaType.VIDEO
                showMediaPreview()
            } else {
                showError("Video must be 1 minute or less")
            }
        }
    }

    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPdfUri = it
            selectedPdfName = getPdfFileName(it)
            binding.tvPdfName.text = "PDF: $selectedPdfName"
            binding.tvPdfName.visibility = View.VISIBLE
        }
    }

    private fun getPdfFileName(uri: Uri): String {
        var fileName = "document.pdf"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupToolbar()
        setupArticleCategories()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupArticleCategories() {
        val categories = resources.getStringArray(R.array.article_categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.etArticleCategory.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.btnAddImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnAddVideo.setOnClickListener {
            videoPickerLauncher.launch("video/*")
        }

        binding.btnAddPdf.setOnClickListener {
            pdfPickerLauncher.launch("application/pdf")
        }

        binding.fabRemoveMedia.setOnClickListener {
            removeMedia()
        }

        binding.btnPost.setOnClickListener {
            createPost()
        }

        // Post type selection
        binding.rgPostType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_event -> {
                    binding.layoutEventFields.visibility = View.VISIBLE
                    binding.layoutArticleFields.visibility = View.GONE
                    binding.layoutMediaButtons.visibility = View.VISIBLE
                    binding.cardMediaPreview.visibility = if (selectedMediaUri != null) View.VISIBLE else View.GONE
                }
                R.id.rb_article -> {
                    binding.layoutEventFields.visibility = View.GONE
                    binding.layoutArticleFields.visibility = View.VISIBLE
                    binding.layoutMediaButtons.visibility = View.VISIBLE
                    binding.cardMediaPreview.visibility = if (selectedMediaUri != null) View.VISIBLE else View.GONE
                }
                R.id.rb_regular_post -> {
                    binding.layoutEventFields.visibility = View.GONE
                    binding.layoutArticleFields.visibility = View.GONE
                    binding.layoutMediaButtons.visibility = View.VISIBLE
                    binding.cardMediaPreview.visibility = if (selectedMediaUri != null) View.VISIBLE else View.GONE
                }
            }
        }

        // Date picker
        binding.etEventDate.setOnClickListener {
            showDatePicker()
        }

        // Time picker
        binding.etEventTime.setOnClickListener {
            showTimePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                binding.etEventDate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() // No past dates
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                binding.etEventTime.setText(timeFormat.format(calendar.time))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false // 12-hour format
        ).show()
    }

    private fun isVideoValid(uri: Uri): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = duration?.toLongOrNull() ?: 0L
            val maxDurationMs = 60 * 1000 // 1 minute
            durationMs <= maxDurationMs
        } catch (e: Exception) {
            Log.e(TAG, "Error checking video duration", e)
            false
        } finally {
            retriever.release()
        }
    }

    private fun showMediaPreview() {
        binding.cardMediaPreview.visibility = View.VISIBLE
        binding.layoutMediaButtons.visibility = View.GONE

        when (mediaType) {
            MediaType.IMAGE -> {
                binding.ivPreview.visibility = View.VISIBLE
                binding.videoPreview.visibility = View.GONE
                binding.ivPlayIcon.visibility = View.GONE
                binding.ivPreview.load(selectedMediaUri)
            }
            MediaType.VIDEO -> {
                binding.ivPreview.visibility = View.GONE
                binding.videoPreview.visibility = View.VISIBLE
                binding.ivPlayIcon.visibility = View.VISIBLE
                binding.videoPreview.setVideoURI(selectedMediaUri)
                binding.videoPreview.seekTo(1) // Show first frame
            }
            MediaType.NONE -> {}
        }
    }

    private fun removeMedia() {
        selectedMediaUri = null
        mediaType = MediaType.NONE
        binding.cardMediaPreview.visibility = View.GONE
        binding.layoutMediaButtons.visibility = View.VISIBLE
        binding.ivPreview.setImageDrawable(null)
        binding.videoPreview.stopPlayback()
    }

    private fun createPost() {
        val caption = binding.etCaption.text.toString().trim()
        val isEvent = binding.rbEvent.isChecked
        val isArticle = binding.rbArticle.isChecked

        // Validation for regular posts and events
        if (!isArticle && selectedMediaUri == null) {
            showError("Please add an image or video")
            return
        }

        // Article-specific validation
        if (isArticle) {
            val articleTitle = binding.etArticleTitle.text.toString().trim()
            val articleCategory = binding.etArticleCategory.text.toString().trim()

            if (selectedMediaUri == null) {
                showError("Please add a cover image")
                return
            }
            if (selectedPdfUri == null) {
                showError("Please upload a PDF file")
                return
            }
            if (articleTitle.isEmpty()) {
                showError("Please enter an article title")
                return
            }
            if (articleCategory.isEmpty()) {
                showError("Please select a category")
                return
            }
        } else {
            // Regular post and event validation
            if (caption.isEmpty()) {
                showError("Please write a caption")
                return
            }
        }

        // Event-specific validation
        if (isEvent) {
            val eventTitle = binding.etEventTitle.text.toString().trim()
            val eventDate = binding.etEventDate.text.toString().trim()
            val eventTime = binding.etEventTime.text.toString().trim()
            val eventPrice = binding.etEventPrice.text.toString().trim()

            if (eventTitle.isEmpty()) {
                showError("Please enter an event title")
                return
            }
            if (eventDate.isEmpty()) {
                showError("Please select an event date")
                return
            }
            if (eventTime.isEmpty()) {
                showError("Please select an event time")
                return
            }
            if (eventPrice.isEmpty()) {
                showError("Please enter an event price")
                return
            }
        }

        showLoading(true)

        val userId = auth.currentUser?.uid
        if (userId == null) {
            showError("User not authenticated")
            showLoading(false)
            return
        }

        // Upload media first (and PDF if article)
        if (isArticle) {
            uploadMediaAndPdf(userId, caption, isEvent, isArticle)
        } else {
            uploadMedia(userId, caption, isEvent, isArticle)
        }
    }

    private fun uploadMedia(userId: String, caption: String, isEvent: Boolean, isArticle: Boolean) {
        val postId = UUID.randomUUID().toString()
        val fileExtension = if (mediaType == MediaType.IMAGE) "jpg" else "mp4"
        val storageRef = storage.reference.child("posts/$userId/$postId.$fileExtension")

        storageRef.putFile(selectedMediaUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    savePost(userId, postId, caption, uri.toString(), null, isEvent, isArticle)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Failed to upload media", e)
                showError("Failed to upload media: ${e.message}")
            }
    }

    private fun uploadMediaAndPdf(userId: String, caption: String, isEvent: Boolean, isArticle: Boolean) {
        val postId = UUID.randomUUID().toString()
        val fileExtension = if (mediaType == MediaType.IMAGE) "jpg" else "mp4"
        val mediaStorageRef = storage.reference.child("posts/$userId/$postId.$fileExtension")
        val pdfStorageRef = storage.reference.child("articles/$userId/$postId.pdf")

        // Upload cover image first
        mediaStorageRef.putFile(selectedMediaUri!!)
            .addOnSuccessListener {
                mediaStorageRef.downloadUrl.addOnSuccessListener { mediaUri ->
                    // Then upload PDF
                    pdfStorageRef.putFile(selectedPdfUri!!)
                        .addOnSuccessListener {
                            pdfStorageRef.downloadUrl.addOnSuccessListener { pdfUri ->
                                savePost(userId, postId, caption, mediaUri.toString(), pdfUri.toString(), isEvent, isArticle)
                            }
                        }
                        .addOnFailureListener { e ->
                            showLoading(false)
                            Log.e(TAG, "Failed to upload PDF", e)
                            showError("Failed to upload PDF: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Failed to upload cover image", e)
                showError("Failed to upload cover image: ${e.message}")
            }
    }

    private fun savePost(userId: String, postId: String, caption: String, mediaUrl: String, pdfUrl: String?, isEvent: Boolean, isArticle: Boolean) {
        // Get business info
        firestore.collection("businesses").document(userId)
            .get()
            .addOnSuccessListener { businessDoc ->
                val businessName = businessDoc.getString("businessName") ?: ""
                val category = businessDoc.getString("category") ?: ""
                val logoUrl = businessDoc.getString("logoUrl") ?: ""

                val postType = when {
                    isArticle -> "article"
                    isEvent -> "event"
                    else -> "post"
                }

                val postData = hashMapOf(
                    "postId" to postId,
                    "userId" to userId,
                    "businessName" to businessName,
                    "category" to category,
                    "logoUrl" to logoUrl,
                    "caption" to caption,
                    "mediaUrl" to mediaUrl,
                    "mediaType" to if (mediaType == MediaType.IMAGE) "image" else "video",
                    "postType" to postType,
                    "createdAt" to Timestamp.now(),
                    "likes" to 0,
                    "comments" to 0
                )

                // Add event-specific fields if it's an event
                if (isEvent) {
                    postData["eventTitle"] = binding.etEventTitle.text.toString().trim()
                    postData["eventDate"] = binding.etEventDate.text.toString().trim()
                    postData["eventTime"] = binding.etEventTime.text.toString().trim()
                    postData["eventPrice"] = binding.etEventPrice.text.toString().trim()
                }

                // Add article-specific fields if it's an article
                if (isArticle) {
                    postData["articleTitle"] = binding.etArticleTitle.text.toString().trim()
                    postData["articleCategory"] = binding.etArticleCategory.text.toString().trim()
                    postData["pdfUrl"] = pdfUrl ?: ""
                }

                firestore.collection("posts").document(postId)
                    .set(postData)
                    .addOnSuccessListener {
                        showLoading(false)
                        setResult(RESULT_OK)

                        // Navigate based on post type
                        when {
                            isArticle -> {
                                // Navigate to articles page
                                val intent = Intent(this, ArticlesActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                            }
                            isEvent -> {
                                // Navigate to MainActivity - Events now in drawer menu
                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                                Toast.makeText(this, "Event posted! Access Events from the menu", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                // Navigate to MainActivity - Discover now in drawer menu
                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                                Toast.makeText(this, "Post created! Access Discover from the menu", Toast.LENGTH_SHORT).show()
                            }
                        }

                        finish()
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        Log.e(TAG, "Failed to save post", e)
                        showError("Failed to save post: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Failed to get business info", e)
                showError("Failed to get business info: ${e.message}")
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnPost.isEnabled = !show
        binding.btnAddImage.isEnabled = !show
        binding.btnAddVideo.isEnabled = !show
        binding.etCaption.isEnabled = !show
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    companion object {
        private const val TAG = "CreatePostActivity"
    }
}
