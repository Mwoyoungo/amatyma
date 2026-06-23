package com.lokaleza.amatyma.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lokaleza.amatyma.R
import com.lokaleza.amatyma.social.ui.theme.AmaBlack
import com.lokaleza.amatyma.social.ui.theme.AmaCrimson
import com.lokaleza.amatyma.social.ui.theme.AmaHairline
import com.lokaleza.amatyma.social.ui.theme.AmaPurple
import com.lokaleza.amatyma.social.ui.theme.AmaTextSecondary

private val CREATE_TOPICS = listOf("Business", "Sports", "Wellness", "Political", "Technology")
private val TEXT_BG = listOf("", "#C2185B", "#3949AB", "#00897B", "#6A1B9A", "#E65100", "#37474F")

@Composable
fun CreateScreen(user: SocialUser?, onClose: () -> Unit = {}) {
    var mode by remember { mutableStateOf("video") }     // video | photo | text
    var topic by remember { mutableStateOf(CREATE_TOPICS[0]) }
    var posting by remember { mutableStateOf(false) }

    val vm: CreateVideoViewModel = viewModel()

    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var photoCaption by remember { mutableStateOf("") }

    var text by remember { mutableStateOf("") }
    var bgIdx by remember { mutableStateOf(0) }

    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris -> if (uris.isNotEmpty()) images = uris }

    fun launchImagePicker() =
        pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    val canPost = !posting && when (mode) {
        "video" -> vm.canPost
        "photo" -> images.isNotEmpty()
        "text" -> text.isNotBlank()
        else -> false
    }

    fun doPost() {
        when (mode) {
            "video" -> vm.post(onPosted = onClose)
            "photo" -> {
                posting = true
                Posts.createImage(user, images, photoCaption, topic) { ok -> posting = false; if (ok) onClose() }
            }
            "text" -> {
                posting = true
                Posts.createText(user, text, topic, TEXT_BG[bgIdx]) { ok -> posting = false; if (ok) onClose() }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(AmaBlack).statusBarsPadding().padding(horizontal = 18.dp)) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Cancel", color = AmaTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onClose() })
            Text("New Post", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Box(
                Modifier.clip(RoundedCornerShape(999.dp))
                    .background(if (canPost) AmaCrimson else AmaCrimson.copy(alpha = 0.4f))
                    .clickable(enabled = canPost) { doPost() }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (posting) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(15.dp))
                else Text("Post", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        // Mode selector
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeTab("Video", mode == "video", Modifier.weight(1f)) { mode = "video" }
            ModeTab("Photo", mode == "photo", Modifier.weight(1f)) { mode = "photo" }
            ModeTab("Text", mode == "text", Modifier.weight(1f)) { mode = "text" }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (mode) {
                "video" -> VideoComposer(vm)
                "photo" -> PhotoComposer(images, photoCaption, { photoCaption = it }, topic, { topic = it }, ::launchImagePicker)
                else -> TextComposer(text, { text = it }, bgIdx, { bgIdx = it }, topic, { topic = it })
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(999.dp))
            .background(if (selected) AmaCrimson else Color.White.copy(alpha = 0.06f))
            .border(1.dp, if (selected) Color.Transparent else AmaHairline, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Color.White else AmaTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TopicChipsRow(selected: String, onSelect: (String) -> Unit) {
    Text("TOPIC", color = AmaTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CREATE_TOPICS.forEach { t ->
            val sel = selected == t
            Box(
                Modifier.clip(RoundedCornerShape(999.dp))
                    .background(if (sel) AmaCrimson else AmaBlack)
                    .border(1.dp, if (sel) Color.Transparent else AmaHairline, RoundedCornerShape(999.dp))
                    .clickable { onSelect(t) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(t, color = if (sel) Color.White else Color(0xFFCDD2DB), fontSize = 12.5.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun VideoComposer(vm: CreateVideoViewModel) {
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.onVideoPicked(uri)
    }
    fun launch() = pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))

    if (vm.selectedUri == null) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)).clickable { launch() },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(AmaCrimson), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_videocamera_bold), null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(9.dp))
                Text("tap to add a 30–60s video", color = AmaTextSecondary, fontSize = 12.sp)
            }
        }
    } else {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF11151D)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_play_bold), null, tint = Color.White, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(6.dp))
                Text("${vm.durationSec}s selected", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Change", color = AmaPurple, fontSize = 12.sp, modifier = Modifier.clickable { launch() })
            }
        }
    }
    vm.durationError?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = AmaCrimson, fontSize = 12.sp)
    }
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = vm.caption, onValueChange = { vm.caption = it },
        placeholder = { Text("Share something with the brotherhood…", color = AmaTextSecondary) },
        modifier = Modifier.fillMaxWidth(), minLines = 3,
    )
    Spacer(Modifier.height(16.dp))
    TopicChipsRow(vm.topic) { vm.topic = it }
    if (vm.phase == CreatePhase.UPLOADING) {
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress = { vm.progress }, modifier = Modifier.fillMaxWidth(), color = AmaCrimson)
    }
    vm.message?.let {
        Spacer(Modifier.height(10.dp))
        Text(it, color = if (vm.phase == CreatePhase.ERROR) AmaCrimson else AmaTextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun PhotoComposer(
    images: List<Uri>, caption: String, onCaption: (String) -> Unit,
    topic: String, onTopic: (String) -> Unit, onPick: () -> Unit,
) {
    if (images.isEmpty()) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)).clickable { onPick() },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(AmaCrimson), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_plus), null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(9.dp))
                Text("tap to add photos (up to 10)", color = AmaTextSecondary, fontSize = 12.sp)
            }
        }
    } else {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            images.forEach { uri ->
                AsyncImage(uri, null, contentScale = ContentScale.Crop, modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("${images.size} selected · Change", color = AmaPurple, fontSize = 12.sp, modifier = Modifier.clickable { onPick() })
    }
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = caption, onValueChange = onCaption,
        placeholder = { Text("Add a caption…", color = AmaTextSecondary) },
        modifier = Modifier.fillMaxWidth(), minLines = 2,
    )
    Spacer(Modifier.height(16.dp))
    TopicChipsRow(topic, onTopic)
}

@Composable
private fun TextComposer(
    text: String, onText: (String) -> Unit, bgIdx: Int, onBg: (Int) -> Unit,
    topic: String, onTopic: (String) -> Unit,
) {
    val bg = if (TEXT_BG[bgIdx].isEmpty()) Color(0xFF11151D) else Color(android.graphics.Color.parseColor(TEXT_BG[bgIdx]))
    Box(
        Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(bg).padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = text, onValueChange = onText,
            textStyle = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text("What's on your mind?", color = Color.White.copy(alpha = 0.6f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                inner()
            },
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TEXT_BG.forEachIndexed { i, hex ->
            val c = if (hex.isEmpty()) Color(0xFF11151D) else Color(android.graphics.Color.parseColor(hex))
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(c)
                    .border(if (i == bgIdx) 2.dp else 1.dp, if (i == bgIdx) Color.White else Color.White.copy(alpha = 0.35f), CircleShape)
                    .clickable { onBg(i) },
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    TopicChipsRow(topic, onTopic)
}
