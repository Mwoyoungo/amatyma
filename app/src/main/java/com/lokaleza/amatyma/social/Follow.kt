package com.lokaleza.amatyma.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/** Follow/unfollow a user. Counts are maintained by Cloud Function triggers. */
fun toggleFollow(targetUid: String, currentlyFollowing: Boolean) {
    val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
    if (me == targetUid || targetUid.isEmpty()) return
    val ref = FirebaseFirestore.getInstance()
        .collection("users").document(me)
        .collection("following").document(targetUid)
    if (currentlyFollowing) ref.delete()
    else ref.set(mapOf("createdAt" to FieldValue.serverTimestamp()))
}

/**
 * Live state for a profile (own or someone else's): identity, follower/following
 * counts (from /profiles), their videos, and whether I follow them.
 */
class ProfileState(private val uid: String) {

    var name by mutableStateOf(""); private set
    var handle by mutableStateOf(""); private set
    var avatar by mutableStateOf(""); private set
    var bio by mutableStateOf(""); private set
    var followersCount by mutableStateOf(0); private set
    var followingCount by mutableStateOf(0); private set
    var posts by mutableStateOf<List<VideoPost>>(emptyList()); private set
    var iFollow by mutableStateOf(false); private set

    val isMe: Boolean get() = FirebaseAuth.getInstance().currentUser?.uid == uid
    val postsCount: Int get() = posts.size
    val kudosTotal: Int get() = posts.sumOf { it.kudosCount }

    private val db = FirebaseFirestore.getInstance()
    private val registrations = mutableListOf<ListenerRegistration>()

    fun start(seed: SocialUser?) {
        if (seed != null && seed.uid == uid) {
            name = seed.name
            handle = seed.handle
            avatar = seed.avatarUrl
        }

        registrations += db.collection("profiles").document(uid)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    followersCount = (doc.getLong("followersCount") ?: 0L).toInt()
                    followingCount = (doc.getLong("followingCount") ?: 0L).toInt()
                    // Social overrides (set via Edit profile) win when present.
                    doc.getString("displayName")?.takeIf { it.isNotEmpty() }?.let { name = it }
                    doc.getString("photoURL")?.takeIf { it.isNotEmpty() }?.let { avatar = it }
                    bio = doc.getString("bio") ?: ""
                }
            }

        // Their videos (single-field query — no composite index). Sorted client-side.
        registrations += db.collection("videoPosts")
            .whereEqualTo("authorUid", uid)
            .limit(60)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        if (doc.getString("status") != "ready") return@mapNotNull null
                        val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                        @Suppress("UNCHECKED_CAST")
                        val images = (doc.get("imageUrls") as? List<String>) ?: emptyList()
                        createdAt to VideoPost(
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
                    }.sortedByDescending { it.first }.map { it.second }

                    posts = list
                    if (name.isEmpty() && list.isNotEmpty()) {
                        name = list[0].authorName
                        handle = list[0].authorHandle
                        avatar = list[0].authorAvatar
                    }
                }
            }

        val me = FirebaseAuth.getInstance().currentUser?.uid
        if (me != null && me != uid) {
            registrations += db.collection("users").document(me)
                .collection("following").document(uid)
                .addSnapshotListener { doc, _ -> iFollow = doc != null && doc.exists() }
        }
    }

    fun toggle() = toggleFollow(uid, iFollow)

    fun stop() {
        registrations.forEach { it.remove() }
        registrations.clear()
    }
}
