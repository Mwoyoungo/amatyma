package com.lokaleza.amatyma.social

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

/**
 * Full-screen story viewer for one author. Respects Android safe areas: progress
 * bars + header sit below the status bar; the footer above the gesture nav. Tap
 * right to advance, left to go back, hold to pause, back/finish to dismiss.
 */
@Composable
fun StoryViewer(authorUid: String, onClose: () -> Unit) {
    val me = FirebaseAuth.getInstance().currentUser?.uid
    val isMine = authorUid == me
    val stories = if (isMine) Stories.myStories else (Stories.ringFor(authorUid)?.stories ?: emptyList())

    if (stories.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    var index by remember(authorUid) { mutableStateOf(0) }
    val story = stories.getOrNull(index)
    if (story == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    var paused by remember { mutableStateOf(false) }
    var progress by remember(authorUid, index) { mutableStateOf(0f) }
    val durationMs = if (story.type == "video") 15000L else 5000L

    LaunchedEffect(authorUid, index) { Stories.markSeen(story.id) }

    LaunchedEffect(authorUid, index, paused) {
        if (paused) return@LaunchedEffect
        var elapsed = (progress * durationMs).toLong()
        while (elapsed < durationMs) {
            delay(40)
            elapsed += 40
            progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
        }
        if (index < stories.lastIndex) index++ else onClose()
    }

    BackHandler { onClose() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // ── Content ──
        when (story.type) {
            "text" -> Box(
                Modifier.fillMaxSize().background(parseColor(story.bgColor)).padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(story.text, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            "video" -> StoryVideo(story.hlsUrl, paused, Modifier.fillMaxSize())
            else -> if (story.imageUrl.isNotEmpty()) {
                AsyncImage(story.imageUrl, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
        }

        // Top + bottom scrims so the controls/caption stay readable
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(150.dp)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent))))

        // ── Tap zones + hold-to-pause (below the top controls in z-order) ──
        Box(Modifier.fillMaxSize().pointerInput(authorUid, stories.size) {
            detectTapGestures(
                onPress = { paused = true; tryAwaitRelease(); paused = false },
                onTap = { offset ->
                    if (offset.x < size.width * 0.32f) {
                        if (index > 0) index--
                    } else {
                        if (index < stories.lastIndex) index++ else onClose()
                    }
                },
            )
        })

        // ── Top controls ──
        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) {
            // Segmented progress
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                stories.forEachIndexed { i, _ ->
                    val seg = when { i < index -> 1f; i == index -> progress; else -> 0f }
                    Box(Modifier.weight(1f).height(2.5.dp).clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxWidth(seg).fillMaxHeight().clip(RoundedCornerShape(99.dp)).background(Color.White))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StoryAvatarRing(story.authorAvatar, 34.dp)
                Spacer(Modifier.width(10.dp))
                Text(story.authorName.ifEmpty { "Brother" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(timeAgo(story.createdAtMillis), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Caption for image/video stories
        if (story.type != "text" && story.text.isNotEmpty()) {
            Text(
                story.text,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = if (isMine) 64.dp else 24.dp),
            )
        }

        // ── Footer: delete own story ──
        if (isMine) {
            Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp)) {
                Box(
                    Modifier.clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.45f))
                        .clickable { Stories.delete(story.id); onClose() }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                ) {
                    Text("Delete", color = Color(0xFFFF6B6B), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StoryVideo(hlsUrl: String, paused: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exo = remember(hlsUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(hlsUrl))
            volume = 1f
            playWhenReady = true
            prepare()
        }
    }
    LaunchedEffect(paused) { exo.playWhenReady = !paused }
    DisposableEffect(hlsUrl) { onDispose { exo.release() } }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exo
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
    )
}

@Composable
private fun StoryAvatarRing(url: String, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(0xFF2A2F3A))) {
        if (url.isNotEmpty()) {
            AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
        }
    }
}

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF1A1F2B)
}

private fun timeAgo(millis: Long): String {
    if (millis <= 0L) return ""
    val diff = System.currentTimeMillis() - millis
    val mins = diff / 60000
    return when {
        mins < 1 -> "now"
        mins < 60 -> "${mins}m"
        else -> "${mins / 60}h"
    }
}
