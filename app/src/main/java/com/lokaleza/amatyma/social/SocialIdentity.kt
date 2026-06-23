package com.lokaleza.amatyma.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** The signed-in person, projected for the social UI (handle/name/avatar). */
data class SocialUser(
    val uid: String,
    val name: String,
    val handle: String,    // "@username", or "" if not set
    val avatarUrl: String,
)

/**
 * Reuses the existing Firebase Auth + `users` doc as the social identity.
 * No new auth — the same account that powers chat powers the social profile.
 */
class SocialIdentityViewModel : ViewModel() {

    var user by mutableStateOf<SocialUser?>(null)
        private set

    private var uid = ""
    private var baseName = ""      // from the chat users doc (read-only)
    private var baseUsername = ""
    private var basePhoto = ""
    private var ovName = ""        // social overrides from /profiles
    private var ovPhoto = ""

    init { load() }

    /** Social overrides win when present; otherwise fall back to the chat users doc. */
    private fun apply() {
        if (uid.isEmpty()) return
        user = SocialUser(
            uid = uid,
            name = ovName.ifEmpty { baseName },
            handle = if (baseUsername.isNotEmpty()) "@$baseUsername" else "",
            avatarUrl = ovPhoto.ifEmpty { basePhoto },
        )
    }

    private fun load() {
        val fbUser = FirebaseAuth.getInstance().currentUser ?: return
        uid = fbUser.uid
        baseName = fbUser.displayName ?: ""
        apply()

        val db = FirebaseFirestore.getInstance()

        // Chat identity — READ ONLY. We never write this doc (chat owns it).
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                baseName = doc.getString("displayName") ?: fbUser.displayName ?: ""
                baseUsername = doc.getString("username") ?: ""
                basePhoto = doc.getString("photoURL") ?: ""
                apply()
            }

        // Social overrides — live, so profile edits reflect across the app instantly.
        db.collection("profiles").document(uid)
            .addSnapshotListener { d, _ ->
                ovName = d?.getString("displayName") ?: ""
                ovPhoto = d?.getString("photoURL") ?: ""
                apply()
            }
    }
}
