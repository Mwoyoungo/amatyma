package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import com.lokaleza.amatyma.databinding.ActivitySearchBinding
import com.lokaleza.amatyma.databinding.ItemSearchResultBinding

data class BusinessSearchResult(
    val userId: String,
    val businessName: String,
    val category: String,
    val location: String,
    val tagline: String,
    val logoUrl: String
)

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private var selectedCategory: String? = null
    private var selectedLocation: String? = null
    private lateinit var resultsAdapter: SearchResultsAdapter
    private val allResults = mutableListOf<BusinessSearchResult>()

    // All business categories
    private val categories = listOf(
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

    // South African Provinces
    private val locations = listOf(
        "All",
        "Gauteng",
        "Western Cape",
        "Eastern Cape",
        "KwaZulu-Natal",
        "Free State",
        "Limpopo",
        "Mpumalanga",
        "Northern Cape",
        "North West"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCategoryChips()
        setupLocationChips()
        setupSearchBar()
        setupButtons()
        setupRecyclerView()
        setupExpandCollapse()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupCategoryChips() {
        categories.forEach { category ->
            val chip = Chip(this)
            chip.text = category
            chip.isCheckable = true

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedCategory = if (category == "All") null else category
                }
            }

            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun setupLocationChips() {
        locations.forEach { location ->
            val chip = Chip(this)
            chip.text = location
            chip.isCheckable = true

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedLocation = if (location == "All") null else location
                }
            }

            binding.chipGroupLocations.addView(chip)
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Auto-filter as user types
                filterResults()
            }
        })
    }

    private fun setupButtons() {
        binding.btnSearch.setOnClickListener {
            performSearch()
        }

        binding.btnClear.setOnClickListener {
            clearFilters()
        }
    }

    private fun setupRecyclerView() {
        resultsAdapter = SearchResultsAdapter(emptyList()) { business ->
            // Navigate to public business profile
            val intent = Intent(this, PublicBusinessProfileActivity::class.java)
            intent.putExtra("USER_ID", business.userId)
            startActivity(intent)
        }
        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = resultsAdapter
    }

    private fun performSearch() {
        val firestore = FirebaseFirestore.getInstance()
        var query: com.google.firebase.firestore.Query = firestore.collection("businesses")

        // Apply category filter if selected
        if (selectedCategory != null) {
            query = query.whereEqualTo("category", selectedCategory)
        }

        // Apply location filter if selected
        if (selectedLocation != null) {
            query = query.whereEqualTo("location", selectedLocation)
        }

        query.get()
            .addOnSuccessListener { documents ->
                allResults.clear()
                allResults.addAll(documents.map { doc ->
                    BusinessSearchResult(
                        userId = doc.id,
                        businessName = doc.getString("businessName") ?: "",
                        category = doc.getString("category") ?: "",
                        location = doc.getString("location") ?: "",
                        tagline = doc.getString("tagline") ?: "",
                        logoUrl = doc.getString("logoUrl") ?: ""
                    )
                })

                // Apply text filter
                filterResults()
            }
            .addOnFailureListener { e ->
                Log.e("SearchActivity", "Error searching businesses", e)
                showEmptyState()
            }
    }

    private fun filterResults() {
        val searchText = binding.etSearch.text.toString().trim().lowercase()

        val filteredResults = if (searchText.isEmpty()) {
            allResults
        } else {
            allResults.filter { business ->
                business.businessName.lowercase().contains(searchText) ||
                        business.category.lowercase().contains(searchText) ||
                        business.location.lowercase().contains(searchText) ||
                        business.tagline.lowercase().contains(searchText)
            }
        }

        displayResults(filteredResults)
    }

    private fun displayResults(results: List<BusinessSearchResult>) {
        if (results.isEmpty()) {
            showEmptyState()
        } else {
            binding.rvResults.visibility = View.VISIBLE
            binding.tvResultsHeader.visibility = View.VISIBLE
            binding.tvResultsCount.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE

            binding.tvResultsCount.text = "${results.size} business${if (results.size != 1) "es" else ""} found"
            resultsAdapter.updateResults(results)
        }
    }

    private fun showEmptyState() {
        binding.rvResults.visibility = View.GONE
        binding.tvResultsHeader.visibility = View.GONE
        binding.tvResultsCount.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun clearFilters() {
        // Clear search text
        binding.etSearch.text?.clear()

        // Uncheck all chips
        binding.chipGroupCategories.clearCheck()
        binding.chipGroupLocations.clearCheck()

        selectedCategory = null
        selectedLocation = null

        // Clear results
        allResults.clear()
        resultsAdapter.updateResults(emptyList())

        // Hide results section
        binding.rvResults.visibility = View.GONE
        binding.tvResultsHeader.visibility = View.GONE
        binding.tvResultsCount.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
    }

    private fun setupExpandCollapse() {
        // Category expand/collapse
        binding.categoryHeader.setOnClickListener {
            if (binding.chipGroupCategories.visibility == View.GONE) {
                binding.chipGroupCategories.visibility = View.VISIBLE
                binding.ivCategoryArrow.rotation = 180f
            } else {
                binding.chipGroupCategories.visibility = View.GONE
                binding.ivCategoryArrow.rotation = 0f
            }
        }

        // Location expand/collapse
        binding.locationHeader.setOnClickListener {
            if (binding.chipGroupLocations.visibility == View.GONE) {
                binding.chipGroupLocations.visibility = View.VISIBLE
                binding.ivLocationArrow.rotation = 180f
            } else {
                binding.chipGroupLocations.visibility = View.GONE
                binding.ivLocationArrow.rotation = 0f
            }
        }
    }
}

class SearchResultsAdapter(
    private var results: List<BusinessSearchResult>,
    private val onItemClick: (BusinessSearchResult) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val business = results[position]

        with(holder.binding) {
            tvBusinessName.text = business.businessName
            tvCategory.text = "🏗️ ${business.category}"
            tvLocation.text = "📍 ${business.location}"
            tvTagline.text = business.tagline

            ivLogo.load(business.logoUrl) {
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
            }

            btnViewProfile.setOnClickListener {
                onItemClick(business)
            }

            root.setOnClickListener {
                onItemClick(business)
            }
        }
    }

    override fun getItemCount() = results.size

    fun updateResults(newResults: List<BusinessSearchResult>) {
        results = newResults
        notifyDataSetChanged()
    }
}
