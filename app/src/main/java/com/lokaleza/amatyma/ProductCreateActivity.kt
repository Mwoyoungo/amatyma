package com.lokaleza.amatyma

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.lokaleza.amatyma.databinding.ActivityProductCreateBinding

class ProductCreateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductCreateBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth

    private val selectedImages = mutableListOf<Uri>()
    private lateinit var imageAdapter: SelectedImagesAdapter

    private val categories = listOf("Clothing", "Accessories")

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImages.addAll(uris)
            imageAdapter.notifyDataSetChanged()
            updateImageCount()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupImageRecyclerView()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupImageRecyclerView() {
        imageAdapter = SelectedImagesAdapter(selectedImages) { position ->
            // Remove image at position
            selectedImages.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
            updateImageCount()
        }
        binding.rvSelectedImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSelectedImages.adapter = imageAdapter
    }

    private fun setupClickListeners() {
        binding.btnSelectImages.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnCreateProduct.setOnClickListener {
            createProduct()
        }
    }

    private fun updateImageCount() {
        binding.tvImageCount.text = "${selectedImages.size} image(s) selected"
        binding.tvImageCount.visibility = if (selectedImages.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun createProduct() {
        val name = binding.etProductName.text.toString().trim()
        val price = binding.etPrice.text.toString().trim()
        val briefDesc = binding.etBriefDescription.text.toString().trim()
        val fullDesc = binding.etFullDescription.text.toString().trim()
        val category = when (binding.rgCategory.checkedRadioButtonId) {
            R.id.rb_clothing -> "Clothing"
            R.id.rb_accessories -> "Accessories"
            else -> "Clothing"
        }
        val sizesText = binding.etSizes.text.toString().trim()
        val quantity = binding.etQuantity.text.toString().trim()
        val websiteUrl = binding.etWebsiteUrl.text.toString().trim()

        // Validation
        if (!validateInput(name, price, briefDesc, fullDesc, category, quantity, websiteUrl)) {
            return
        }

        if (selectedImages.isEmpty()) {
            showError("Please select at least one product image")
            return
        }

        showLoading(true)

        // Parse sizes (comma-separated)
        val sizes = if (sizesText.isNotEmpty()) {
            sizesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        // Upload images first
        uploadImages { imageUrls ->
            if (imageUrls.isNotEmpty()) {
                saveProduct(
                    name = name,
                    price = price.toDouble(),
                    briefDesc = briefDesc,
                    fullDesc = fullDesc,
                    category = category,
                    sizes = sizes,
                    quantity = quantity.toInt(),
                    websiteUrl = websiteUrl,
                    imageUrls = imageUrls
                )
            } else {
                showLoading(false)
                showError("Failed to upload images. Please try again.")
            }
        }
    }

    private fun validateInput(
        name: String,
        price: String,
        briefDesc: String,
        fullDesc: String,
        category: String,
        quantity: String,
        websiteUrl: String
    ): Boolean {
        if (name.isEmpty()) {
            showError("Please enter product name")
            return false
        }

        if (price.isEmpty() || price.toDoubleOrNull() == null || price.toDouble() <= 0) {
            showError("Please enter a valid price")
            return false
        }

        if (briefDesc.isEmpty()) {
            showError("Please enter a brief description")
            return false
        }

        if (fullDesc.isEmpty()) {
            showError("Please enter a full description")
            return false
        }

        if (category.isEmpty()) {
            showError("Please select a category")
            return false
        }

        if (quantity.isEmpty() || quantity.toIntOrNull() == null || quantity.toInt() < 0) {
            showError("Please enter a valid quantity")
            return false
        }

        if (websiteUrl.isEmpty()) {
            showError("Please enter the product website URL")
            return false
        }

        return true
    }

    private fun uploadImages(onComplete: (List<String>) -> Unit) {
        val uploadedUrls = mutableListOf<String>()
        var uploadCount = 0
        val totalImages = selectedImages.size

        selectedImages.forEachIndexed { index, uri ->
            val timestamp = System.currentTimeMillis()
            val storageRef = storage.reference.child("products/${auth.currentUser?.uid}/${timestamp}_$index.jpg")

            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        uploadedUrls.add(downloadUri.toString())
                        uploadCount++

                        if (uploadCount == totalImages) {
                            onComplete(uploadedUrls)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProductCreate", "Failed to upload image $index", e)
                    uploadCount++

                    if (uploadCount == totalImages) {
                        onComplete(uploadedUrls)
                    }
                }
        }
    }

    private fun saveProduct(
        name: String,
        price: Double,
        briefDesc: String,
        fullDesc: String,
        category: String,
        sizes: List<String>,
        quantity: Int,
        websiteUrl: String,
        imageUrls: List<String>
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showLoading(false)
            showError("You must be logged in to create products")
            return
        }

        val productData = hashMapOf(
            "userId" to userId,
            "name" to name,
            "price" to price,
            "briefDescription" to briefDesc,
            "fullDescription" to fullDesc,
            "category" to category,
            "sizes" to sizes,
            "quantity" to quantity,
            "websiteUrl" to websiteUrl,
            "images" to imageUrls,
            "createdAt" to Timestamp.now()
        )

        firestore.collection("products")
            .add(productData)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Product created successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e("ProductCreate", "Failed to save product", e)
                showError("Failed to save product: ${e.message}")
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnCreateProduct.isEnabled = !show
        binding.btnSelectImages.isEnabled = !show
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }
}

class SelectedImagesAdapter(
    private val images: List<Uri>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<SelectedImagesAdapter.ViewHolder>() {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val imageView: android.widget.ImageView = view.findViewById(R.id.iv_image)
        val removeButton: android.widget.ImageView = view.findViewById(R.id.iv_remove)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.imageView.load(images[position]) {
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }

        holder.removeButton.setOnClickListener {
            onRemove(position)
        }
    }

    override fun getItemCount() = images.size
}
