package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.load
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lokaleza.amatyma.databinding.ActivityArticlesBinding
import com.lokaleza.amatyma.databinding.ItemArticleGridBinding

class ArticlesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticlesBinding
    private lateinit var articlesAdapter: ArticlesAdapter
    private var allArticles = listOf<Article>()
    private var selectedCategory: String = "All"
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticlesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSearch()
        setupCategoryChips()
        setupArticlesFeed()
        setupCreatePostFab()
    }

    private fun setupToolbar() {
        binding.ivClose.setOnClickListener {
            finish()
        }
    }

    private fun setupCreatePostFab() {
        binding.fabCreatePost.setOnClickListener {
            startActivity(Intent(this, CreatePostActivity::class.java))
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                filterArticles()
            }
        })
    }

    private fun setupCategoryChips() {
        val categories = listOf(
            "All",
            "Culture",
            "Religion & Spirituality",
            "Business & Entrepreneurship",
            "Health & Fitness",
            "Mental Health",
            "Relationships & Dating",
            "Finance & Investing",
            "Personal Development",
            "Career & Leadership",
            "Fashion & Grooming",
            "Nutrition & Diet",
            "Parenting & Fatherhood",
            "Sports & Recreation",
            "Philosophy & Wisdom",
            "Self-Improvement",
            "Motivation & Mindset"
        )

        categories.forEach { category ->
            val chip = com.google.android.material.chip.Chip(this)
            chip.text = category
            chip.isCheckable = true

            // First chip (All) is selected by default
            if (category == "All") {
                chip.isChecked = true
            }

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedCategory = category
                    filterArticles()
                }
            }

            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun filterArticles() {
        var filteredArticles = allArticles

        // Filter by category
        if (selectedCategory != "All") {
            filteredArticles = filteredArticles.filter {
                it.articleCategory.contains(selectedCategory, ignoreCase = true)
            }
        }

        // Filter by search query
        if (searchQuery.isNotEmpty()) {
            filteredArticles = filteredArticles.filter { article ->
                article.articleTitle.contains(searchQuery, ignoreCase = true) ||
                article.articleCategory.contains(searchQuery, ignoreCase = true) ||
                article.businessName.contains(searchQuery, ignoreCase = true)
            }
        }

        articlesAdapter = ArticlesAdapter(filteredArticles, this)
        binding.rvArticles.adapter = articlesAdapter
    }

    private fun setupArticlesFeed() {
        // Pinterest-style staggered grid with 2 columns
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.rvArticles.layoutManager = layoutManager

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("posts")
            .whereEqualTo("postType", "article")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("ArticlesActivity", "Total articles loaded: ${documents.size()}")

                allArticles = documents.mapNotNull { doc ->
                    try {
                        Article(
                            postId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            businessName = doc.getString("businessName") ?: "",
                            category = doc.getString("category") ?: "",
                            profileImage = doc.getString("logoUrl") ?: "",
                            coverImage = doc.getString("mediaUrl") ?: "",
                            articleTitle = doc.getString("articleTitle") ?: "",
                            articleCategory = doc.getString("articleCategory") ?: "",
                            pdfUrl = doc.getString("pdfUrl") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("ArticlesActivity", "Error parsing article: ${e.message}")
                        null
                    }
                }

                Log.d("ArticlesActivity", "Parsed articles count: ${allArticles.size}")
                filterArticles()
            }
            .addOnFailureListener { e ->
                Log.e("ArticlesActivity", "Error loading articles", e)
            }
    }
}

class ArticlesAdapter(
    private val articles: List<Article>,
    private val context: android.content.Context
) : RecyclerView.Adapter<ArticlesAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemArticleGridBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemArticleGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = articles[position]

        with(holder.binding) {
            tvBusinessName.text = article.businessName
            tvArticleTitle.text = article.articleTitle

            // Load cover image
            ivCover.load(article.coverImage) {
                placeholder(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }

            // Click on card to open PDF in browser
            root.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = android.net.Uri.parse(article.pdfUrl)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        context,
                        "Cannot open PDF: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun getItemCount() = articles.size
}
