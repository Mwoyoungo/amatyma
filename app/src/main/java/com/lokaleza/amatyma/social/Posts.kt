package com.lokaleza.amatyma.social

import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Creates image & text feed posts directly (status "ready"). Video posts go
 * through the FastPix flow (CreateVideoViewModel) and are server-created.
 */
object Posts {

    private fun base(user: SocialUser?, uid: String): HashMap<String, Any> = hashMapOf(
        "authorUid" to uid,
        "authorName" to (user?.name ?: ""),
        "authorHandle" to (user?.handle ?: ""),
        "authorAvatar" to (user?.avatarUrl ?: ""),
        "kudosCount" to 0,
        "commentCount" to 0,
        "status" to "ready",
        "createdAt" to FieldValue.serverTimestamp(),
    )

    fun createText(user: SocialUser?, caption: String, topic: String, bgColor: String, onDone: (Boolean) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val text = caption.trim()
        if (uid == null || text.isEmpty()) { onDone(false); return }
        val data = base(user, uid)
        data["type"] = "text"
        data["caption"] = text
        data["topic"] = topic
        data["bgColor"] = bgColor
        FirebaseFirestore.getInstance().collection("videoPosts").add(data)
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    fun createImage(user: SocialUser?, uris: List<Uri>, caption: String, topic: String, onDone: (Boolean) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || uris.isEmpty()) { onDone(false); return }
        val db = FirebaseFirestore.getInstance()
        val doc = db.collection("videoPosts").document()
        val storage = FirebaseStorage.getInstance().reference

        // Upload each image; Tasks.whenAllSuccess preserves input order.
        val uploads = uris.mapIndexed { i, uri ->
            val ref = storage.child("social_posts/$uid/${doc.id}/$i.jpg")
            ref.putFile(uri).continueWithTask { ref.downloadUrl }
        }
        Tasks.whenAllSuccess<Uri>(uploads)
            .addOnSuccessListener { urls ->
                val imageUrls = urls.map { it.toString() }
                val data = base(user, uid)
                data["type"] = "image"
                data["imageUrls"] = imageUrls
                data["posterUrl"] = (imageUrls.firstOrNull() ?: "")
                data["caption"] = caption.trim()
                data["topic"] = topic
                doc.set(data)
                    .addOnSuccessListener { onDone(true) }
                    .addOnFailureListener { onDone(false) }
            }
            .addOnFailureListener { onDone(false) }
    }
}
