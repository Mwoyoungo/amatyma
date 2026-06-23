package com.lokaleza.amatyma.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Date

/** One story item. `type` is text | image | video. */
data class Story(
    val id: String,
    val authorUid: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatar: String,
    val type: String,
    val text: String,
    val bgColor: String,    // "#RRGGBB" for text stories
    val imageUrl: String,
    val hlsUrl: String,
    val posterUrl: String,
    val createdAtMillis: Long,
)

/** A followed author's active stories, grouped for the tray. */
data class StoryRing(
    val authorUid: String,
    val authorName: String,
    val authorAvatar: String,
    val stories: List<Story>,
    val allSeen: Boolean,
)

/**
 * App-wide stories store. Loads the last 24h of `socialStories`, then keeps only
 * the ones from people you follow (+ your own). Seen-state is your private
 * `users/{me}/storiesSeen` set, mirrored so rings show seen/unseen.
 */
object Stories {
    var myStories by mutableStateOf<List<Story>>(emptyList()); private set
    var rings by mutableStateOf<List<StoryRing>>(emptyList()); private set

    private var seenIds: Set<String> = emptySet()
    private var raw: List<Story> = emptyList()

    private var storiesReg: ListenerRegistration? = null
    private var seenReg: ListenerRegistration? = null
    private var startedFor: String? = null

    fun ensureStarted() {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (startedFor == me && storiesReg != null) return
        storiesReg?.remove(); seenReg?.remove()
        startedFor = me
        val db = FirebaseFirestore.getInstance()

        val cutoff = Timestamp(Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000))
        storiesReg = db.collection("socialStories")
            .whereGreaterThan("createdAt", cutoff)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(300)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    raw = snap.documents.mapNotNull { d ->
                        val type = d.getString("type") ?: return@mapNotNull null
                        val hls = d.getString("hlsUrl") ?: ""
                        if (type == "video" && hls.isEmpty()) return@mapNotNull null // still processing
                        Story(
                            id = d.id,
                            authorUid = d.getString("authorUid") ?: "",
                            authorName = d.getString("authorName") ?: "",
                            authorHandle = d.getString("authorHandle") ?: "",
                            authorAvatar = d.getString("authorAvatar") ?: "",
                            type = type,
                            text = d.getString("text") ?: "",
                            bgColor = d.getString("bgColor") ?: "#1A1F2B",
                            imageUrl = d.getString("imageUrl") ?: "",
                            hlsUrl = hls,
                            posterUrl = d.getString("posterUrl") ?: "",
                            createdAtMillis = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        )
                    }
                    rebuild()
                }
            }

        seenReg = db.collection("users").document(me).collection("storiesSeen")
            .addSnapshotListener { snap, _ ->
                if (snap != null) { seenIds = snap.documents.map { it.id }.toSet(); rebuild() }
            }
    }

    private fun rebuild() {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        myStories = raw.filter { it.authorUid == me }.sortedBy { it.createdAtMillis }

        val followed = Following.ids
        rings = raw.filter { it.authorUid != me && it.authorUid in followed }
            .groupBy { it.authorUid }
            .map { (uid, list) ->
                val sorted = list.sortedBy { it.createdAtMillis }
                StoryRing(
                    authorUid = uid,
                    authorName = sorted.last().authorName,
                    authorAvatar = sorted.last().authorAvatar,
                    stories = sorted,
                    allSeen = sorted.all { it.id in seenIds },
                )
            }
            // unseen rings first, then most-recent first
            .sortedWith(compareBy({ it.allSeen }, { -(it.stories.last().createdAtMillis) }))
    }

    val myAllSeen: Boolean get() = myStories.isNotEmpty() && myStories.all { it.id in seenIds }

    fun ringFor(authorUid: String): StoryRing? = rings.firstOrNull { it.authorUid == authorUid }

    fun markSeen(storyId: String) {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (storyId in seenIds) return
        FirebaseFirestore.getInstance()
            .collection("users").document(me).collection("storiesSeen").document(storyId)
            .set(mapOf("seenAt" to FieldValue.serverTimestamp()))
    }

    fun delete(storyId: String) {
        FirebaseFirestore.getInstance().collection("socialStories").document(storyId).delete()
    }
}
