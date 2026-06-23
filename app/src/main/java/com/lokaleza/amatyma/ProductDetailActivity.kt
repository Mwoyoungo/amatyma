package com.lokaleza.amatyma

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.firestore.FirebaseFirestore
import com.lokaleza.amatyma.databinding.ActivityProductDetailBinding

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private var productId: String? = null
    private var product: Product? = null
    private lateinit var imageAdapter: ProductImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productId = intent.getStringExtra("PRODUCT_ID")

        setupToolbar()
        setupImageCarousel()
        loadProductDetails()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupImageCarousel() {
        binding.rvProductImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        imageAdapter = ProductImageAdapter(emptyList())
        binding.rvProductImages.adapter = imageAdapter
    }

    private fun loadProductDetails() {
        if (productId == null) {
            Log.e("ProductDetailActivity", "No product ID provided")
            finish()
            return
        }

        // Check if it's a mock product
        if (productId!!.startsWith("mock_")) {
            loadMockProduct()
            return
        }

        // Load from Firestore
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("products").document(productId!!)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    product = Product(
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
                    displayProduct()
                } else {
                    Log.e("ProductDetailActivity", "Product not found")
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProductDetailActivity", "Error loading product", e)
                finish()
            }
    }

    private fun loadMockProduct() {
        // Get mock products and find the matching one
        val mockProducts = getMockProducts()
        product = mockProducts.find { it.productId == productId }

        if (product != null) {
            displayProduct()
        } else {
            Log.e("ProductDetailActivity", "Mock product not found")
            finish()
        }
    }

    private fun displayProduct() {
        product?.let { prod ->
            binding.tvProductName.text = prod.name
            binding.tvProductPrice.text = "R${String.format("%.2f", prod.price)}"
            binding.tvProductCategory.text = prod.category
            binding.tvProductDescription.text = prod.fullDescription
            binding.tvProductQuantity.text = if (prod.quantity > 0) {
                "In Stock (${prod.quantity} available)"
            } else {
                "Out of Stock"
            }

            // Display sizes
            if (prod.sizes.isNotEmpty()) {
                binding.tvSizesLabel.visibility = View.VISIBLE
                binding.tvProductSizes.visibility = View.VISIBLE
                binding.tvProductSizes.text = "Available sizes: ${prod.sizes.joinToString(", ")}"
            } else {
                binding.tvSizesLabel.visibility = View.GONE
                binding.tvProductSizes.visibility = View.GONE
            }

            // Load images
            if (prod.images.isNotEmpty()) {
                imageAdapter.updateImages(prod.images)

                // Load first image as main image
                binding.ivMainProductImage.load(prod.images[0]) {
                    placeholder(R.drawable.ic_default_avatar)
                    error(R.drawable.ic_default_avatar)
                }
            }

            // Setup buy button
            binding.btnBuyNow.setOnClickListener {
                if (prod.websiteUrl.isNotEmpty()) {
                    openWebsite(prod.websiteUrl)
                } else {
                    android.widget.Toast.makeText(this, "Product website not available", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            // Disable button if out of stock
            if (prod.quantity <= 0) {
                binding.btnBuyNow.isEnabled = false
                binding.btnBuyNow.text = "Out of Stock"
            }
        }
    }

    private fun openWebsite(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ProductDetailActivity", "Error opening website", e)
            android.widget.Toast.makeText(this, "Could not open website", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMockProducts(): List<Product> {
        return listOf(
            Product(
                productId = "mock_1",
                name = "Amatyma Classic T-Shirt",
                price = 299.99,
                briefDescription = "Premium cotton tee with Amatyma logo",
                fullDescription = "High-quality 100% cotton t-shirt featuring the iconic Amatyma logo. Comfortable, durable, and stylish - perfect for everyday wear.\n\nFeatures:\n• 100% premium cotton\n• Pre-shrunk fabric\n• Reinforced stitching\n• Machine washable\n• Unisex fit",
                category = "Clothing",
                sizes = listOf("S", "M", "L", "XL", "XXL"),
                quantity = 50,
                images = listOf(
                    "https://via.placeholder.com/600x600/4CAF50/FFFFFF?text=Amatyma+Tee+Front",
                    "https://via.placeholder.com/600x600/388E3C/FFFFFF?text=Amatyma+Tee+Back",
                    "https://via.placeholder.com/600x600/2E7D32/FFFFFF?text=Amatyma+Tee+Detail"
                ),
                websiteUrl = "https://example.com/products/amatyma-tshirt",
                createdAt = com.google.firebase.Timestamp.now()
            ),
            Product(
                productId = "mock_2",
                name = "Amatyma Hoodie",
                price = 599.99,
                briefDescription = "Cozy hoodie with embroidered logo",
                fullDescription = "Stay warm and represent Amatyma with this premium hoodie. Features embroidered logo, kangaroo pocket, and soft fleece lining.\n\nFeatures:\n• 80% cotton, 20% polyester blend\n• Soft fleece lining\n• Embroidered logo\n• Kangaroo pocket\n• Adjustable drawstring hood\n• Ribbed cuffs and hem",
                category = "Clothing",
                sizes = listOf("S", "M", "L", "XL", "XXL"),
                quantity = 30,
                images = listOf(
                    "https://via.placeholder.com/600x600/2196F3/FFFFFF?text=Amatyma+Hoodie+Front",
                    "https://via.placeholder.com/600x600/1976D2/FFFFFF?text=Amatyma+Hoodie+Back",
                    "https://via.placeholder.com/600x600/1565C0/FFFFFF?text=Amatyma+Hoodie+Detail"
                ),
                websiteUrl = "https://example.com/products/amatyma-hoodie",
                createdAt = com.google.firebase.Timestamp.now()
            ),
            Product(
                productId = "mock_3",
                name = "Amatyma Cap",
                price = 199.99,
                briefDescription = "Adjustable snapback cap",
                fullDescription = "Classic snapback cap with embroidered Amatyma logo. Adjustable strap ensures perfect fit. Available in multiple colors.\n\nFeatures:\n• Embroidered logo\n• Adjustable snapback closure\n• Structured 6-panel design\n• Curved brim\n• Breathable eyelets\n• One size fits most",
                category = "Accessories",
                sizes = listOf("One Size"),
                quantity = 100,
                images = listOf(
                    "https://via.placeholder.com/600x600/FF9800/FFFFFF?text=Amatyma+Cap+Front",
                    "https://via.placeholder.com/600x600/F57C00/FFFFFF?text=Amatyma+Cap+Side",
                    "https://via.placeholder.com/600x600/E65100/FFFFFF?text=Amatyma+Cap+Back"
                ),
                websiteUrl = "https://example.com/products/amatyma-cap",
                createdAt = com.google.firebase.Timestamp.now()
            ),
            Product(
                productId = "mock_4",
                name = "Amatyma Tote Bag",
                price = 249.99,
                briefDescription = "Durable canvas tote bag",
                fullDescription = "Eco-friendly canvas tote bag perfect for shopping or daily use. Features printed Amatyma branding and reinforced handles.\n\nFeatures:\n• 100% natural canvas\n• Reinforced handles\n• Large main compartment\n• Inner pocket for valuables\n• Printed logo design\n• Machine washable\n• Dimensions: 38cm x 42cm x 10cm",
                category = "Accessories",
                sizes = listOf("One Size"),
                quantity = 75,
                images = listOf(
                    "https://via.placeholder.com/600x600/9C27B0/FFFFFF?text=Amatyma+Tote+Front",
                    "https://via.placeholder.com/600x600/7B1FA2/FFFFFF?text=Amatyma+Tote+Side",
                    "https://via.placeholder.com/600x600/6A1B9A/FFFFFF?text=Amatyma+Tote+Detail"
                ),
                websiteUrl = "https://example.com/products/amatyma-tote",
                createdAt = com.google.firebase.Timestamp.now()
            )
        )
    }
}

class ProductImageAdapter(
    private var images: List<String>
) : RecyclerView.Adapter<ProductImageAdapter.ViewHolder>() {

    class ViewHolder(val imageView: android.widget.ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val imageView = android.widget.ImageView(parent.context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                400, // width in pixels
                400  // height in pixels
            )
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            setPadding(8, 8, 8, 8)
        }
        return ViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.imageView.load(images[position]) {
            placeholder(R.drawable.ic_default_avatar)
            error(R.drawable.ic_default_avatar)
        }
    }

    override fun getItemCount() = images.size

    fun updateImages(newImages: List<String>) {
        images = newImages
        notifyDataSetChanged()
    }
}
