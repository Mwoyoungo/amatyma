package com.lokaleza.amatyma.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * App-wide "what I've liked" set, backed by `users/{me}/likes` (single source of
 * truth; a Cloud Function trigger maintains each post's kudosCount). Toggling is
 * optimistic — the heart flips instantly, the listener reconciles.
 */
object Likes {
    var liked by mutableStateOf<Set<String>>(emptySet())
        private set

    private var registration: ListenerRegistration? = null
    private var startedFor: String? = null

    fun ensureStarted() {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (startedFor == me && registration != null) return
        registration?.remove()
        startedFor = me
        registration = FirebaseFirestore.getInstance()
            .collection("users").document(me).collection("likes")
            .addSnapshotListener { snap, _ ->
                if (snap != null) liked = snap.documents.map { it.id }.toSet()
            }
    }

    fun isLiked(postId: String): Boolean = postId in liked

    fun toggle(postId: String) {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseFirestore.getInstance()
            .collection("users").document(me).collection("likes").document(postId)
        if (postId in liked) {
            liked = liked - postId           // optimistic un-like
            ref.delete()
        } else {
            liked = liked + postId           // optimistic like
            ref.set(mapOf("createdAt" to FieldValue.serverTimestamp()))
        }
    }
}
