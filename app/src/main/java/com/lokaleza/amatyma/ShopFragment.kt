package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.firestore.FirebaseFirestore
import com.lokaleza.amatyma.databinding.FragmentShopBinding
import com.lokaleza.amatyma.databinding.ItemProductBinding

data class Product(
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val briefDescription: String = "",
    val fullDescription: String = "",
    val category: String = "", // "Clothing" or "Accessories"
    val sizes: List<String> = emptyList(), // e.g., ["S", "M", "L", "XL"]
    val quantity: Int = 0,
    val images: List<String> = emptyList(), // Multiple image URLs
    val websiteUrl: String = "", // External checkout URL
    val createdAt: com.google.firebase.Timestamp? = null
)

class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!
    private lateinit var productAdapter: ProductAdapter
    private var allProducts = listOf<Product>()
    private var selectedCategory: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoryChips()
        setupProductGrid()
        setupCreateProductIcon()
        loadProducts()
    }

    private fun setupCreateProductIcon() {
        binding.ivCreateProduct.setOnClickListener {
            val intent = Intent(requireActivity(), ProductCreateActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupCategoryChips() {
        val categories = listOf("All", "Clothing", "Accessories")

        categories.forEach { category ->
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = category
            chip.isCheckable = true

            if (category == "All") {
                chip.isChecked = true
            }

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedCategory = category
                    filterProducts()
                }
            }

            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun setupProductGrid() {
        binding.rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        productAdapter = ProductAdapter(emptyList()) { product ->
            // Navigate to product detail
            val intent = Intent(requireActivity(), ProductDetailActivity::class.java)
            intent.putExtra("PRODUCT_ID", product.productId)
            startActivity(intent)
        }
        binding.rvProducts.adapter = productAdapter
    }

    private fun loadProducts() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("products")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Show mock products
                    Log.d("ShopFragment", "No products in database, showing mock products")
                    allProducts = getMockProducts()
                } else {
                    // Load real products
                    Log.d("ShopFragment", "Loaded ${documents.size()} products from Firestore")
                    allProducts = documents.map { doc ->
                        Product(
                            productId = doc.id,
                            name = doc.getString("name") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            briefDescription = doc.getString("briefDescription") ?: "",
                            fullDescription = doc.getString("fullDescription") ?: "",
                            category = doc.getString("category") ?: "",
                            sizes = doc.get("sizes") as? List<String> ?: emptyList(),
                            quantity = doc.getLong("quantity")?.toInt() ?: 0,
                            images = doc.get("images") as? List<String> ?: emptyList(),
                            websiteUrl = doc.getString("websiteUrl") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    }
                }
                filterProducts()
            }
            .addOnFailureListener { e ->
                Log.e("ShopFragment", "Error loading products", e)
                // Show mock products on error
                allProducts = getMockProducts()
                filterProducts()
            }
    }

    private fun filterProducts() {
        val filteredProducts = if (selectedCategory == "All") {
            allProducts
        } else {
            allProducts.filter { it.category == selectedCategory }
        }

        if (filteredProducts.isEmpty()) {
            binding.rvProducts.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvProducts.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
            productAdapter.updateProducts(filteredProducts)
        }
    }

    private fun getMockProducts(): List<Product> {
        return listOf(
            Product(
                productId = "mock_1",
                name = "Amatyma Classic T-Shirt",
                price = 299.99,
                briefDescription = "Premium cotton tee with Amatyma logo",
                fullDescription = "High-quality 100% cotton t-shirt featuring the iconic Amatyma logo. Comfortable, durable, and stylish - perfect for everyday wear.",
                category = "Clothing",
                sizes = listOf("S", "M", "L", "XL", "XXL"),
                quantity = 50,
                images = listOf("https://via.placeholder.com/400x400/4CAF50/FFFFFF?text=Amatyma+Tee"),
                websiteUrl = "https://example.com/products/amatyma-tshirt",
                createdAt = com.google.firebase.Timestamp.now()
            ),
            Product(
                productId = "mock_2",
                name = "Amatyma Hoodie",
                price = 599.99,
                briefDescription = "Cozy hoodie with embroidered logo",
                fullDescription = "Stay warm and represent Amatyma with this premium hoodie. Features embroidered logo, kangaroo pocket, and soft fleece lining.",
                category = "Clothing",
                sizes = listOf("S", "M", "L", "XL", "XXL"),
                quantity = 30,
                images = listOf("https://via.placeholder.com/400x400/2196F3/FFFFFF?text=Amatyma+Hoodie"),
                websiteUrl = "https://example.com/products/amatyma-hoodie",
                createdAt = com.google.firebase.Timestamp.now()
            ),
            Product(
                productId = "mock_3",
                name = "Amatyma Cap",
                price = 199.99,
                briefDescription = "Adjustable snapback cap",
                fullDescription = "Classic snapback cap with embroidered Amatyma logo. Adjustable strap ensures perfect fit. Available in multiple colors.",
                category = "Accessories",
                sizes = listOf("One Size"),
                quantity = 100,
                images = listOf("https://via.placeholder.com/400x400/FF9800/FFFFFF?text=Amatyma+Cap"),
                websiteUrl = "https://example.com/products/amatyma-cap",
                createdAt = com.google.firebase.Timestamp.now()
            ),
            Product(
                productId = "mock_4",
                name = "Amatyma Tote Bag",
                price = 249.99,
                briefDescription = "Durable canvas tote bag",
                fullDescription = "Eco-friendly canvas tote bag perfect for shopping or daily use. Features printed Amatyma branding and reinforced handles.",
                category = "Accessories",
                sizes = listOf("One Size"),
                quantity = 75,
                images = listOf("https://via.placeholder.com/400x400/9C27B0/FFFFFF?text=Amatyma+Tote"),
                websiteUrl = "https://example.com/products/amatyma-tote",
                createdAt = com.google.firebase.Timestamp.now()
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ProductAdapter(
    private var products: List<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        with(holder.binding) {
            tvProductName.text = product.name
            tvProductPrice.text = "R${String.format("%.2f", product.price)}"
            tvProductDescription.text = product.briefDescription

            // Load first image
            if (product.images.isNotEmpty()) {
                ivProductImage.load(product.images[0]) {
                    placeholder(R.drawable.ic_launcher_background)
                    error(R.drawable.ic_launcher_background)
                }
            }

            root.setOnClickListener {
                onItemClick(product)
            }
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}
