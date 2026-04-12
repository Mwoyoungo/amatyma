package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.cometchat.chat.core.CometChat
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.lokaleza.amatyma.databinding.ActivityMyBusinessProfileBinding
import com.lokaleza.amatyma.databinding.ItemPostGridBinding

class MyBusinessProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyBusinessProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyBusinessProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User not authenticated")
            finish()
            return
        }

        setupToolbar()
        setupTabs()
        setupClickListeners()

        // Load from cache first for instant display
        loadCachedProfile()

        // Then fetch fresh data from Firestore
        loadBusinessProfile()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showPosts()
                    1 -> showAbout()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showPosts() {
        binding.rvPosts.visibility = View.VISIBLE
        binding.layoutAbout.visibility = View.GONE
    }

    private fun showAbout() {
        binding.rvPosts.visibility = View.GONE
        binding.layoutAbout.visibility = View.VISIBLE
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            // Navigate to BusinessProfileSetupActivity in edit mode
            startActivity(Intent(this, BusinessProfileSetupActivity::class.java))
        }

        binding.fabCreatePost.setOnClickListener {
            startActivity(Intent(this, CreatePostActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_profile, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        // Clear cached profile data
        val prefs = getSharedPreferences("profile_cache", MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d(TAG, "Profile cache cleared")

        // Logout from CometChat
        CometChat.logout(object : CometChat.CallbackListener<String>() {
            override fun onSuccess(message: String?) {
                Log.d(TAG, "CometChat logout successful")
            }

            override fun onError(exception: com.cometchat.chat.exceptions.CometChatException?) {
                Log.e(TAG, "CometChat logout error: ${exception?.message}")
            }
        })

        // Logout from Firebase
        auth.signOut()

        // Navigate to AuthActivity and clear task stack
        val intent = Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun loadCachedProfile() {
        val prefs = getSharedPreferences("profile_cache", MODE_PRIVATE)
        val cachedUserId = prefs.getString("cached_user_id", "")

        // Only load cache if it's for the current user
        if (cachedUserId == userId) {
            val businessName = prefs.getString("businessName", "")
            val category = prefs.getString("category", "")
            val tagline = prefs.getString("tagline", "")
            val description = prefs.getString("description", "")
            val services = prefs.getString("services", "")
            val location = prefs.getString("location", "")
            val logoUrl = prefs.getString("logoUrl", "")
            val totalMessages = prefs.getInt("totalMessages", 0)
            val rating = prefs.getFloat("rating", 0f)
            val postCount = prefs.getInt("postCount", 0)

            // Display cached data immediately
            if (businessName?.isNotEmpty() == true) {
                updateUI(businessName, category ?: "", tagline ?: "", description ?: "",
                    services ?: "", location ?: "", logoUrl ?: "", totalMessages, rating.toDouble(), postCount)
                Log.d(TAG, "Loaded profile from cache")
            }
        }
    }

    private fun loadBusinessProfile() {
        if (userId == null) return

        // Use Firestore cache-first strategy
        val docRef = firestore.collection("businesses").document(userId!!)

        docRef.get(com.google.firebase.firestore.Source.CACHE).addOnCompleteListener { cacheTask ->
            if (cacheTask.isSuccessful && cacheTask.result?.exists() == true) {
                // Display cached data from Firestore
                updateProfileFromDocument(cacheTask.result!!)
            }
        }

        // Always try to fetch fresh data
        docRef.get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    updateProfileFromDocument(document)
                    cacheProfileData(document)
                    loadPostsCount(userId!!)
                } else {
                    Log.e(TAG, "Business profile not found")
                    if (!isFinishing) {
                        android.widget.Toast.makeText(this, "Profile not found", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading business profile", e)
                // Don't finish - show cached data if available
                if (binding.tvBusinessName.text.isEmpty()) {
                    if (!isFinishing) {
                        android.widget.Toast.makeText(this, "Offline mode - showing cached data", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun updateProfileFromDocument(document: com.google.firebase.firestore.DocumentSnapshot) {
        val businessName = document.getString("businessName") ?: ""
        val category = document.getString("category") ?: ""
        val tagline = document.getString("tagline") ?: ""
        val description = document.getString("description") ?: ""
        val services = document.get("services") as? List<*>
        val location = document.getString("location") ?: ""
        val logoUrl = document.getString("logoUrl") ?: ""
        val totalMessages = document.getLong("totalMessages")?.toInt() ?: 0
        val rating = document.getDouble("rating") ?: 0.0

        val servicesText = services?.joinToString(", ") ?: ""
        updateUI(businessName, category, tagline, description, servicesText, location, logoUrl, totalMessages, rating, 0)
    }

    private fun updateUI(
        businessName: String,
        category: String,
        tagline: String,
        description: String,
        services: String,
        location: String,
        logoUrl: String,
        totalMessages: Int,
        rating: Double,
        postCount: Int
    ) {
        binding.tvBusinessName.text = businessName
        binding.tvCategory.text = "🏗️ $category"
        binding.tvTagline.text = tagline
        binding.tvDescription.text = description
        binding.tvServices.text = services
        binding.tvLocation.text = location
        binding.tvMessageCount.text = totalMessages.toString()
        binding.tvRating.text = String.format("%.1f", rating)

        if (postCount > 0) {
            binding.tvPostCount.text = postCount.toString()
        }

        if (logoUrl.isNotEmpty()) {
            binding.ivLogo.load(logoUrl) {
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
            }
        }
    }

    private fun cacheProfileData(document: com.google.firebase.firestore.DocumentSnapshot) {
        val prefs = getSharedPreferences("profile_cache", MODE_PRIVATE)
        prefs.edit().apply {
            putString("cached_user_id", userId)
            putString("businessName", document.getString("businessName") ?: "")
            putString("category", document.getString("category") ?: "")
            putString("tagline", document.getString("tagline") ?: "")
            putString("description", document.getString("description") ?: "")
            val services = document.get("services") as? List<*>
            putString("services", services?.joinToString(", ") ?: "")
            putString("location", document.getString("location") ?: "")
            putString("logoUrl", document.getString("logoUrl") ?: "")
            putInt("totalMessages", document.getLong("totalMessages")?.toInt() ?: 0)
            putFloat("rating", document.getDouble("rating")?.toFloat() ?: 0f)
            apply()
        }
        Log.d(TAG, "Profile data cached")
    }

    private fun loadPostsCount(userId: String) {
        // Try cache first
        firestore.collection("posts")
            .whereEqualTo("userId", userId)
            .get(com.google.firebase.firestore.Source.CACHE)
            .addOnSuccessListener { documents ->
                displayPosts(documents)
            }

        // Then fetch from server
        firestore.collection("posts")
            .whereEqualTo("userId", userId)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { documents ->
                displayPosts(documents)
                cachePostCount(documents.size())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading posts count", e)
                // Only update to 0 if we have no cached data
                if (binding.tvPostCount.text.isEmpty()) {
                    binding.tvPostCount.text = "0"
                }
            }
    }

    private fun displayPosts(documents: com.google.firebase.firestore.QuerySnapshot) {
        binding.tvPostCount.text = documents.size().toString()

        // Load posts in grid
        val posts = documents.map { doc ->
            Post(
                postId = doc.getString("postId") ?: "",
                mediaUrl = doc.getString("mediaUrl") ?: "",
                mediaType = doc.getString("mediaType") ?: "image",
                caption = doc.getString("caption") ?: ""
            )
        }

        binding.rvPosts.layoutManager = GridLayoutManager(this, 3)
        binding.rvPosts.adapter = PostGridAdapter(
            posts = posts,
            onPostClick = null,
            onPostLongClick = { post -> showPostOptions(post) }
        )
    }

    private fun cachePostCount(count: Int) {
        val prefs = getSharedPreferences("profile_cache", MODE_PRIVATE)
        prefs.edit().putInt("postCount", count).apply()
    }

    private fun showPostOptions(post: Post) {
        val options = arrayOf("Edit Caption", "Delete Post")

        AlertDialog.Builder(this)
            .setTitle("Post Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditCaptionDialog(post)
                    1 -> showDeleteConfirmDialog(post)
                }
            }
            .show()
    }

    private fun showEditCaptionDialog(post: Post) {
        val editText = EditText(this)
        editText.setText(post.caption)
        editText.setHint("Caption")

        AlertDialog.Builder(this)
            .setTitle("Edit Caption")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newCaption = editText.text.toString().trim()
                if (newCaption.isNotEmpty()) {
                    updatePostCaption(post.postId, newCaption)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePostCaption(postId: String, newCaption: String) {
        firestore.collection("posts").document(postId)
            .update("caption", newCaption)
            .addOnSuccessListener {
                android.widget.Toast.makeText(this, "Caption updated", android.widget.Toast.LENGTH_SHORT).show()
                // Reload posts
                loadBusinessProfile()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating caption", e)
                android.widget.Toast.makeText(this, "Failed to update caption", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmDialog(post: Post) {
        AlertDialog.Builder(this)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                deletePost(post)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePost(post: Post) {
        val userId = auth.currentUser?.uid ?: return

        // Delete from Storage
        val storage = FirebaseStorage.getInstance()
        val fileExtension = if (post.mediaType == "image") "jpg" else "mp4"
        val storageRef = storage.reference.child("posts/$userId/${post.postId}.$fileExtension")

        storageRef.delete()
            .addOnSuccessListener {
                // Delete from Firestore
                firestore.collection("posts").document(post.postId)
                    .delete()
                    .addOnSuccessListener {
                        android.widget.Toast.makeText(this, "Post deleted", android.widget.Toast.LENGTH_SHORT).show()
                        // Reload posts
                        loadBusinessProfile()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error deleting post from Firestore", e)
                        android.widget.Toast.makeText(this, "Failed to delete post", android.widget.Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting post from Storage", e)
                // Try to delete from Firestore anyway
                firestore.collection("posts").document(post.postId).delete()
            }
    }

    data class Post(
        val postId: String,
        val mediaUrl: String,
        val mediaType: String,
        val caption: String
    )

    class PostGridAdapter(
        private val posts: List<Post>,
        private val onPostClick: ((Post) -> Unit)? = null,
        private val onPostLongClick: ((Post) -> Unit)? = null
    ) : RecyclerView.Adapter<PostGridAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemPostGridBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPostGridBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val post = posts[position]

            holder.binding.ivPost.load(post.mediaUrl) {
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
            }

            // Show video indicator if it's a video
            if (post.mediaType == "video") {
                holder.binding.ivVideoIndicator.visibility = View.VISIBLE
            } else {
                holder.binding.ivVideoIndicator.visibility = View.GONE
            }

            // Regular click - open post detail
            onPostClick?.let { callback ->
                holder.binding.root.setOnClickListener {
                    callback(post)
                }
            }

            // Long press for edit/delete options
            onPostLongClick?.let { callback ->
                holder.binding.root.setOnLongClickListener {
                    callback(post)
                    true
                }
            }
        }

        override fun getItemCount() = posts.size
    }

    companion object {
        private const val TAG = "MyBusinessProfile"
    }
}
