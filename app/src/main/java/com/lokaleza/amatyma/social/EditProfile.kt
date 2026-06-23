package com.lokaleza.amatyma.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.lokaleza.amatyma.R
import com.lokaleza.amatyma.social.ui.theme.AmaBlack
import com.lokaleza.amatyma.social.ui.theme.AmaCrimson
import com.lokaleza.amatyma.social.ui.theme.AmaHairline
import com.lokaleza.amatyma.social.ui.theme.AmaTextMuted
import com.lokaleza.amatyma.social.ui.theme.AmaTextSecondary

/**
 * Edit the SOCIAL profile only. Writes to `profiles/{uid}` (displayName/bio/photoURL)
 * and uploads the avatar to `social/{uid}/avatar.jpg` — the chat users doc and the
 * chat profile photo are never touched. Logout reuses the host's existing CometChat
 * logout sequence (passed in as onLogout).
 */
@Composable
fun EditProfileScreen(
    uid: String,
    seed: SocialUser?,
    onClose: () -> Unit,
    onLogout: () -> Unit,
) {
    var name by remember { mutableStateOf(seed?.name ?: "") }
    var bio by remember { mutableStateOf("") }
    var currentPhoto by remember { mutableStateOf(seed?.avatarUrl ?: "") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid.isEmpty()) return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("profiles").document(uid).get()
            .addOnSuccessListener { d ->
                d.getString("displayName")?.takeIf { it.isNotEmpty() }?.let { name = it }
                bio = d.getString("bio") ?: ""
                d.getString("photoURL")?.takeIf { it.isNotEmpty() }?.let { currentPhoto = it }
            }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) pickedUri = uri
    }

    fun save() {
        if (saving || uid.isEmpty()) return
        saving = true
        val db = FirebaseFirestore.getInstance()

        fun write(photoUrl: String?) {
            val data = hashMapOf<String, Any>(
                "displayName" to name.trim(),
                "bio" to bio.trim(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (photoUrl != null) data["photoURL"] = photoUrl
            db.collection("profiles").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener { saving = false; onClose() }
                .addOnFailureListener { saving = false; onClose() }
        }

        val toUpload = pickedUri
        if (toUpload != null) {
            val ref = FirebaseStorage.getInstance().reference.child("social/$uid/avatar.jpg")
            ref.putFile(toUpload)
                .continueWithTask { ref.downloadUrl }
                .addOnSuccessListener { url -> write(url.toString()) }
                .addOnFailureListener { write(null) }
        } else {
            write(null)
        }
    }

    Column(Modifier.fillMaxSize().background(AmaBlack).statusBarsPadding()) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_alt_arrow_left_linear), "Close", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text("Edit profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (saving) {
                CircularProgressIndicator(color = AmaCrimson, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    "Save",
                    color = if (name.isBlank()) AmaTextMuted else AmaCrimson,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable { if (name.isNotBlank()) save() }.padding(6.dp),
                )
            }
        }

        // Avatar
        Box(Modifier.fillMaxWidth().padding(top = 18.dp), contentAlignment = Alignment.Center) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier.size(96.dp).clip(CircleShape).background(AmaCrimson)
                        .padding(2.5.dp).clip(CircleShape).background(Color(0xFF11151D))
                        .clickable { picker.launch("image/*") },
                ) {
                    val model: Any? = pickedUri ?: currentPhoto.ifEmpty { null }
                    if (model != null) {
                        AsyncImage(model, "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                    }
                }
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(AmaCrimson).border(2.dp, AmaBlack, CircleShape)
                        .clickable { picker.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_plus), "Change photo", tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text("Tap to change photo", color = AmaTextSecondary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

        // Fields
        Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
            FieldLabel("Display name")
            EditField(name, { name = it.take(40) }, "Your name", singleLine = true)
            Spacer(Modifier.height(18.dp))
            FieldLabel("Bio")
            EditField(bio, { bio = it.take(160) }, "Tell the brotherhood about you", singleLine = false)
            Spacer(Modifier.height(5.dp))
            Text("${bio.length}/160", color = AmaTextSecondary, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
        }

        Spacer(Modifier.weight(1f))

        // Logout (reuses the host's existing CometChat logout path)
        Box(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 26.dp)) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .border(1.dp, AmaHairline, RoundedCornerShape(12.dp))
                    .clickable { onLogout() }.padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Log out", color = Color(0xFFFF6B6B), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = AmaTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun EditField(value: String, onChange: (String) -> Unit, placeholder: String, singleLine: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = AmaTextMuted, fontSize = 14.sp) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AmaCrimson,
            unfocusedBorderColor = AmaHairline,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = AmaCrimson,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
    )
}
