package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lokaleza.amatyma.databinding.FragmentDiscoverBinding
import com.lokaleza.amatyma.databinding.ItemBusinessFeedBinding

// Data classes for Discover feed
data class BusinessPost(
    val postId: String,
    val userId: String,
    val businessName: String,
    val category: String,
    val profileImage: String,
    val heroImage: String,
    val headline: String,
    val description: String,
    val messages: Int,
    val rating: Double,
    val distance: String,
    val mediaType: String = "image" // "image" or "video"
)

data class Article(
    val postId: String,
    val userId: String,
    val businessName: String,
    val category: String,
    val profileImage: String,
    val coverImage: String,
    val articleTitle: String,
    val articleCategory: String,
    val pdfUrl: String
)

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    private lateinit var feedAdapter: BusinessFeedAdapter
    private var allPosts = listOf<BusinessPost>()
    private var selectedCategory: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProfileIcon()
        setupFAB()
        setupCategoryChips()
        setupFeed()
    }

    private fun setupProfileIcon() {
        // Search icon
        binding.ivSearch.setOnClickListener {
            startActivity(Intent(requireActivity(), SearchActivity::class.java))
        }
    }

    private fun setupFAB() {
        binding.fabCreatePost.setOnClickListener {
            startActivity(Intent(requireActivity(), CreatePostActivity::class.java))
        }
    }

    private fun setupCategoryChips() {
        val categories = listOf(
            "All",
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

        categories.forEach { category ->
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = category
            chip.isCheckable = true

            // First chip (All) is selected by default
            if (category == "All") {
                chip.isChecked = true
            }

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedCategory = category
                    filterFeed()
                }
            }

            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun filterFeed() {
        val filteredPosts = if (selectedCategory == "All") {
            allPosts
        } else {
            allPosts.filter { it.category.contains(selectedCategory, ignoreCase = true) }
        }

        feedAdapter = BusinessFeedAdapter(filteredPosts, requireContext())
        binding.rvFeed.adapter = feedAdapter
    }

    private fun setupFeed() {
        binding.rvFeed.layoutManager = LinearLayoutManager(requireContext())

        // Load all posts from Firestore and filter client-side
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("posts")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("DiscoverFragment", "Total documents loaded: ${documents.size()}")

                allPosts = documents.mapNotNull { doc ->
                    val postType = doc.getString("postType")
                    Log.d("DiscoverFragment", "Post ${doc.id} has postType: $postType")

                    // Filter out events and articles - only show regular posts
                    if (postType == "event" || postType == "article") {
                        Log.d("DiscoverFragment", "Filtering out $postType: ${doc.id}")
                        return@mapNotNull null
                    }

                    // Regular post (or null postType)
                    BusinessPost(
                        postId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        businessName = doc.getString("businessName") ?: "",
                        category = doc.getString("category") ?: "",
                        profileImage = doc.getString("logoUrl") ?: "",
                        heroImage = doc.getString("mediaUrl") ?: "",
                        headline = doc.getString("caption") ?: "",
                        description = "",
                        messages = 0,
                        rating = 0.0,
                        distance = "",
                        mediaType = doc.getString("mediaType") ?: "image"
                    )
                }

                Log.d("DiscoverFragment", "Filtered posts count: ${allPosts.size}")

                // Display posts
                filterFeed()
            }
            .addOnFailureListener { e ->
                Log.e("DiscoverFragment", "Error loading posts", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class BusinessFeedAdapter(
    private val posts: List<BusinessPost>,
    private val context: android.content.Context
) : RecyclerView.Adapter<BusinessFeedAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBusinessFeedBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBusinessFeedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]

        with(holder.binding) {
            tvBusinessName.text = post.businessName
            tvCategory.text = post.category

            // Show description if available
            if (post.description.isNotEmpty()) {
                tvDescription.visibility = android.view.View.VISIBLE
                tvDescription.text = post.description
            } else {
                tvDescription.visibility = android.view.View.GONE
            }

            // Load profile image with Coil
            ivProfile.load(post.profileImage) {
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
            }

            // Navigate to public business profile when clicking profile or business name
            val profileClickListener = android.view.View.OnClickListener {
                if (post.userId.isEmpty()) {
                    android.widget.Toast.makeText(context, "User ID not found", android.widget.Toast.LENGTH_SHORT).show()
                    Log.e("BusinessFeedAdapter", "Empty userId for post: ${post.postId}")
                    return@OnClickListener
                }
                try {
                    val intent = Intent(context, PublicBusinessProfileActivity::class.java)
                    intent.putExtra("USER_ID", post.userId)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("BusinessFeedAdapter", "Error navigating to profile", e)
                    android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            ivProfile.setOnClickListener(profileClickListener)
            tvBusinessName.setOnClickListener(profileClickListener)

            // Handle media type (image vs video)
            if (post.mediaType == "video") {
                // Hide image, show video view
                ivHero.visibility = android.view.View.GONE
                videoView.visibility = android.view.View.VISIBLE
                ivPlayButton.visibility = android.view.View.VISIBLE

                // Setup video
                videoView.setVideoPath(post.heroImage)
                videoView.setOnPreparedListener { mp ->
                    mp.isLooping = true
                }

                // Play/pause on click
                var isPlaying = false
                val clickListener = android.view.View.OnClickListener {
                    if (isPlaying) {
                        videoView.pause()
                        ivPlayButton.visibility = android.view.View.VISIBLE
                    } else {
                        videoView.start()
                        ivPlayButton.visibility = android.view.View.GONE
                    }
                    isPlaying = !isPlaying
                }

                videoView.setOnClickListener(clickListener)
                ivPlayButton.setOnClickListener(clickListener)

            } else {
                // Show image, hide video
                ivHero.visibility = android.view.View.VISIBLE
                videoView.visibility = android.view.View.GONE
                ivPlayButton.visibility = android.view.View.GONE

                ivHero.load(post.heroImage) {
                    placeholder(R.drawable.ic_launcher_background)
                    error(R.drawable.ic_launcher_background)
                }

                // Navigate to post detail when clicking image
                ivHero.setOnClickListener {
                    val intent = Intent(context, PostDetailActivity::class.java)
                    intent.putExtra("POST_ID", post.postId)
                    context.startActivity(intent)
                }
            }

            // Message button - open CometChat with this user
            btnMessage.setOnClickListener {
                if (post.userId.isEmpty()) {
                    android.widget.Toast.makeText(context, "User ID not found", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Launch MessagesActivity with the userId
                val intent = Intent(context, MessagesActivity::class.java)
                intent.putExtra("USER_ID", post.userId)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = posts.size
}
