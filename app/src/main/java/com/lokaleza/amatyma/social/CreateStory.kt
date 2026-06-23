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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.lokaleza.amatyma.R
import com.lokaleza.amatyma.social.ui.theme.AmaBlack
import com.lokaleza.amatyma.social.ui.theme.AmaCrimson
import com.lokaleza.amatyma.social.ui.theme.AmaHairline
import com.lokaleza.amatyma.social.ui.theme.AmaTextSecondary

private val STORY_COLORS = listOf(
    "#C2185B", "#1A1F2B", "#3949AB", "#00897B", "#6A1B9A", "#E65100", "#2E7D32", "#37474F",
)

/** Create a text-on-color or image story (video lands in 5b). Writes to socialStories. */
@Composable
fun CreateStoryScreen(user: SocialUser?, onClose: () -> Unit) {
    var mode by remember { mutableStateOf("chooser") } // chooser | text | image
    var text by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var colorIdx by remember { mutableStateOf(0) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var posting by remember { mutableStateOf(false) }

    val uid = user?.uid ?: ""

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { imageUri = uri; mode = "image" } else if (mode == "chooser") onClose()
    }

    fun authorFields(): HashMap<String, Any> = hashMapOf(
        "authorUid" to uid,
        "authorName" to (user?.name ?: ""),
        "authorHandle" to (user?.handle ?: ""),
        "authorAvatar" to (user?.avatarUrl ?: ""),
        "createdAt" to FieldValue.serverTimestamp(),
    )

    fun postText() {
        if (posting || uid.isEmpty() || text.isBlank()) return
        posting = true
        val data = authorFields()
        data["type"] = "text"
        data["text"] = text.trim()
        data["bgColor"] = STORY_COLORS[colorIdx]
        FirebaseFirestore.getInstance().collection("socialStories").add(data)
            .addOnSuccessListener { onClose() }
            .addOnFailureListener { posting = false }
    }

    fun postImage() {
        val uri = imageUri ?: return
        if (posting || uid.isEmpty()) return
        posting = true
        val db = FirebaseFirestore.getInstance()
        val doc = db.collection("socialStories").document()
        val ref = FirebaseStorage.getInstance().reference.child("social_stories/$uid/${doc.id}.jpg")
        ref.putFile(uri)
            .continueWithTask { ref.downloadUrl }
            .addOnSuccessListener { url ->
                val data = authorFields()
                data["type"] = "image"
                data["imageUrl"] = url.toString()
                data["text"] = caption.trim()
                doc.set(data)
                    .addOnSuccessListener { onClose() }
                    .addOnFailureListener { posting = false }
            }
            .addOnFailureListener { posting = false }
    }

    Box(Modifier.fillMaxSize().background(AmaBlack)) {
        when (mode) {
            "text" -> TextStoryEditor(
                text = text, onText = { text = it.take(220) },
                color = Color(android.graphics.Color.parseColor(STORY_COLORS[colorIdx])),
                colorIdx = colorIdx, onPickColor = { colorIdx = it },
                posting = posting, onClose = onClose, onPost = ::postText,
            )
            "image" -> ImageStoryEditor(
                uri = imageUri, caption = caption, onCaption = { caption = it.take(160) },
                posting = posting, onClose = onClose, onPost = ::postImage,
            )
            else -> StoryChooser(
                onClose = onClose,
                onText = { mode = "text" },
                onPhoto = { picker.launch("image/*") },
            )
        }
    }
}

@Composable
private fun StoryChooser(onClose: () -> Unit, onText: () -> Unit, onPhoto: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CloseButton(onClose)
            Spacer(Modifier.width(12.dp))
            Text("Add to story", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ChooserCard("Text", R.drawable.ic_chat_round_line_linear, AmaCrimson, Modifier.weight(1f), onText)
            ChooserCard("Photo", R.drawable.ic_videocamera_bold, Color(0xFF3949AB), Modifier.weight(1f), onPhoto)
        }
    }
}

@Composable
private fun ChooserCard(label: String, icon: Int, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(18.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(10.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun TextStoryEditor(
    text: String, onText: (String) -> Unit, color: Color,
    colorIdx: Int, onPickColor: (Int) -> Unit,
    posting: Boolean, onClose: () -> Unit, onPost: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(color)) {
        BasicTextField(
            value = text,
            onValueChange = onText,
            textStyle = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(28.dp),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text("Type something…", color = Color.White.copy(alpha = 0.6f), fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                inner()
            },
        )

        Row(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(14.dp)) { CloseButton(onClose) }

        // Color palette + Share, above the gesture nav bar
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                STORY_COLORS.forEachIndexed { i, hex ->
                    Box(
                        Modifier.size(28.dp).clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(hex)))
                            .border(if (i == colorIdx) 2.dp else 1.dp, if (i == colorIdx) Color.White else Color.White.copy(alpha = 0.4f), CircleShape)
                            .clickable { onPickColor(i) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            ShareBar(enabled = text.isNotBlank(), posting = posting, onPost = onPost)
        }
    }
}

@Composable
private fun ImageStoryEditor(
    uri: Uri?, caption: String, onCaption: (String) -> Unit,
    posting: Boolean, onClose: () -> Unit, onPost: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (uri != null) {
            AsyncImage(uri, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
        Row(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(14.dp)) { CloseButton(onClose) }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.4f)).border(1.dp, AmaHairline, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                BasicTextField(
                    value = caption,
                    onValueChange = onCaption,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = SolidColor(AmaCrimson),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (caption.isEmpty()) Text("Add a caption…", color = AmaTextSecondary, fontSize = 15.sp)
                        inner()
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
            ShareBar(enabled = true, posting = posting, onPost = onPost)
        }
    }
}

@Composable
private fun ShareBar(enabled: Boolean, posting: Boolean, onPost: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (enabled && !posting) AmaCrimson else Color.White.copy(alpha = 0.15f))
            .clickable(enabled = enabled && !posting) { onPost() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (posting) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text("Share to story", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).clickable { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_alt_arrow_left_linear), "Close", tint = Color.White, modifier = Modifier.size(18.dp))
    }
}
