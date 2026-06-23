package com.lokaleza.amatyma.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/** A ready-to-play video post, projected from the `videoPosts` Firestore doc. */
data class VideoPost(
    val postId: String,
    val authorUid: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatar: String,
    val caption: String,
    val topic: String,
    val hlsUrl: String,
    val posterUrl: String,
    val kudosCount: Int,
    val commentCount: Int,
    val type: String = "video",            // video | image | text
    val imageUrls: List<String> = emptyList(),
    val bgColor: String = "",              // optional background for text posts
)

/**
 * Live feed of ready video posts. Listens to `videoPosts` ordered newest-first
 * and filters to `status == "ready"` client-side (so no composite index needed).
 */
class VideoFeedViewModel : ViewModel() {

    var posts by mutableStateOf<List<VideoPost>>(emptyList())
        private set

    private var registration: ListenerRegistration? = null

    init { listen() }

    private fun listen() {
        registration = FirebaseFirestore.getInstance()
            .collection("videoPosts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                posts = snapshot.documents.mapNotNull { doc ->
                    if (doc.getString("status") != "ready") return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    val images = (doc.get("imageUrls") as? List<String>) ?: emptyList()
                    VideoPost(
                        postId = doc.id,
                        authorUid = doc.getString("authorUid") ?: "",
                        authorName = doc.getString("authorName") ?: "",
                        authorHandle = doc.getString("authorHandle") ?: "",
                        authorAvatar = doc.getString("authorAvatar") ?: "",
                        caption = doc.getString("caption") ?: "",
                        topic = doc.getString("topic") ?: "",
                        hlsUrl = doc.getString("hlsUrl") ?: "",
                        posterUrl = doc.getString("posterUrl") ?: "",
                        kudosCount = (doc.getLong("kudosCount") ?: 0L).toInt(),
                        commentCount = (doc.getLong("commentCount") ?: 0L).toInt(),
                        type = doc.getString("type") ?: "video",
                        imageUrls = images,
                        bgColor = doc.getString("bgColor") ?: "",
                    )
                }
            }
    }

    override fun onCleared() {
        registration?.remove()
        registration = null
    }
}
