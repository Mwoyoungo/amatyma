package com.lokaleza.amatyma.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lokaleza.amatyma.R
import com.lokaleza.amatyma.social.ui.theme.AmaBlack
import com.lokaleza.amatyma.social.ui.theme.AmaCrimson
import com.lokaleza.amatyma.social.ui.theme.AmaPurple

private enum class RingState { UNSEEN, SEEN, NONE }

/** Stories tray: your own bubble first, then followed authors with active stories. */
@Composable
fun StoryTray(user: SocialUser?, onCreate: () -> Unit, onOpenStory: (String) -> Unit) {
    val myStories = Stories.myStories
    val rings = Stories.rings

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StoryBubble(
            avatar = user?.avatarUrl ?: "",
            label = "You",
            ringState = when {
                myStories.isEmpty() -> RingState.NONE
                Stories.myAllSeen -> RingState.SEEN
                else -> RingState.UNSEEN
            },
            showAdd = true,
            onClick = { if (myStories.isNotEmpty()) onOpenStory(user?.uid ?: "") else onCreate() },
            onAdd = onCreate,
        )
        rings.forEach { ring ->
            StoryBubble(
                avatar = ring.authorAvatar,
                label = ring.authorName.ifEmpty { "Brother" },
                ringState = if (ring.allSeen) RingState.SEEN else RingState.UNSEEN,
                showAdd = false,
                onClick = { onOpenStory(ring.authorUid) },
                onAdd = {},
            )
        }
    }
}

@Composable
private fun StoryBubble(
    avatar: String,
    label: String,
    ringState: RingState,
    showAdd: Boolean,
    onClick: () -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        Modifier.width(72.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            val ring: Brush = when (ringState) {
                RingState.UNSEEN -> Brush.linearGradient(listOf(AmaCrimson, AmaPurple))
                RingState.SEEN -> SolidColor(Color.White.copy(alpha = 0.25f))
                RingState.NONE -> SolidColor(Color.White.copy(alpha = 0.12f))
            }
            Box(
                Modifier.size(70.dp).clip(CircleShape).background(ring)
                    .padding(2.5.dp).clip(CircleShape).background(Color(0xFF11151D)),
            ) {
                if (avatar.isNotEmpty()) {
                    AsyncImage(avatar, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                }
            }
            if (showAdd) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(AmaCrimson).border(2.5.dp, AmaBlack, CircleShape).clickable { onAdd() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_plus), "Add story", tint = Color.White, modifier = Modifier.size(11.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color(0xFFEFF2F8), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
