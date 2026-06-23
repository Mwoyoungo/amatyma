package com.lokaleza.amatyma.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * App-wide "who I follow" set, backed by `users/{me}/following`. Drives the
 * Following feed filter and the Follow buttons (feed pill + Reels badge).
 * Toggling is optimistic; a Cloud Function trigger maintains follower counts.
 */
object Following {
    var ids by mutableStateOf<Set<String>>(emptySet())
        private set

    private var registration: ListenerRegistration? = null
    private var startedFor: String? = null

    fun ensureStarted() {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (startedFor == me && registration != null) return
        registration?.remove()
        startedFor = me
        registration = FirebaseFirestore.getInstance()
            .collection("users").document(me).collection("following")
            .addSnapshotListener { snap, _ ->
                if (snap != null) ids = snap.documents.map { it.id }.toSet()
            }
    }

    fun isFollowing(uid: String): Boolean = uid in ids

    fun toggle(uid: String) {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (me == uid || uid.isEmpty()) return
        val ref = FirebaseFirestore.getInstance()
            .collection("users").document(me).collection("following").document(uid)
        if (uid in ids) {
            ids = ids - uid                  // optimistic unfollow
            ref.delete()
        } else {
            ids = ids + uid                  // optimistic follow
            ref.set(mapOf("createdAt" to FieldValue.serverTimestamp()))
        }
    }
}
