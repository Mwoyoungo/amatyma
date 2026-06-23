package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lokaleza.amatyma.databinding.FragmentEventsBinding
import com.lokaleza.amatyma.databinding.ItemEventFeedBinding

data class Event(
    val postId: String,
    val userId: String,
    val businessName: String,
    val category: String,
    val profileImage: String,
    val heroImage: String,
    val eventTitle: String,
    val description: String,
    val eventDate: String,
    val eventTime: String,
    val eventPrice: String
)

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventsAdapter: EventsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProfileIcon()
        setupEventsFeed()
    }

    private fun setupProfileIcon() {
        binding.ivProfile.setOnClickListener {
            checkBusinessProfileAndNavigate()
        }
    }

    private fun checkBusinessProfileAndNavigate() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("EventsFragment", "User not authenticated")
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("businesses").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    startActivity(Intent(requireActivity(), MyBusinessProfileActivity::class.java))
                } else {
                    startActivity(Intent(requireActivity(), BusinessProfileSetupActivity::class.java))
                }
            }
            .addOnFailureListener { e ->
                Log.e("EventsFragment", "Error checking business profile", e)
                android.widget.Toast.makeText(requireContext(), "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupEventsFeed() {
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())

        // Load all posts and filter events client-side
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("posts")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val events = documents.mapNotNull { doc ->
                    // Only include events
                    val postType = doc.getString("postType") ?: "post"
                    if (postType != "event") return@mapNotNull null
                    Event(
                        postId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        businessName = doc.getString("businessName") ?: "",
                        category = doc.getString("category") ?: "",
                        profileImage = doc.getString("logoUrl") ?: "",
                        heroImage = doc.getString("mediaUrl") ?: "",
                        eventTitle = doc.getString("eventTitle") ?: "",
                        description = doc.getString("caption") ?: "",
                        eventDate = doc.getString("eventDate") ?: "",
                        eventTime = doc.getString("eventTime") ?: "",
                        eventPrice = doc.getString("eventPrice") ?: ""
                    )
                }

                eventsAdapter = EventsAdapter(events, requireContext())
                binding.rvEvents.adapter = eventsAdapter
            }
            .addOnFailureListener { e ->
                Log.e("EventsFragment", "Error loading events", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class EventsAdapter(
    private val events: List<Event>,
    private val context: android.content.Context
) : RecyclerView.Adapter<EventsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemEventFeedBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEventFeedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]

        with(holder.binding) {
            tvBusinessName.text = event.businessName
            tvCategory.text = event.category
            tvEventTitle.text = event.eventTitle
            tvEventDate.text = "${event.eventDate} • ${event.eventTime}"
            tvEventPrice.text = event.eventPrice

            // Show description if available
            if (event.description.isNotEmpty()) {
                tvDescription.visibility = android.view.View.VISIBLE
                tvDescription.text = event.description
            } else {
                tvDescription.visibility = android.view.View.GONE
            }

            // Load profile image
            ivProfile.load(event.profileImage) {
                placeholder(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }

            // Load event image
            ivHero.load(event.heroImage) {
                placeholder(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }

            // Navigate to public business profile
            val profileClickListener = android.view.View.OnClickListener {
                if (event.userId.isEmpty()) {
                    android.widget.Toast.makeText(context, "User ID not found", android.widget.Toast.LENGTH_SHORT).show()
                    return@OnClickListener
                }
                val intent = Intent(context, PublicBusinessProfileActivity::class.java)
                intent.putExtra("USER_ID", event.userId)
                context.startActivity(intent)
            }
            ivProfile.setOnClickListener(profileClickListener)
            tvBusinessName.setOnClickListener(profileClickListener)

            // Message button - open CometChat with organizer
            btnMessage.setOnClickListener {
                if (event.userId.isEmpty()) {
                    android.widget.Toast.makeText(context, "User ID not found", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val intent = Intent(context, MessagesActivity::class.java)
                intent.putExtra("USER_ID", event.userId)
                // Pre-fill message with event enquiry
                val enquiryMessage = "I want to enquire about the ${event.eventTitle} on the ${event.eventDate}"
                intent.putExtra("PRE_FILLED_MESSAGE", enquiryMessage)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = events.size
}
