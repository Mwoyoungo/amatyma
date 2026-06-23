package com.lokaleza.amatyma.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lokaleza.amatyma.R
import com.lokaleza.amatyma.social.ui.theme.AmaBlack
import com.lokaleza.amatyma.social.ui.theme.AmaCrimson
import com.lokaleza.amatyma.social.ui.theme.AmaHairline
import com.lokaleza.amatyma.social.ui.theme.AmaTextSecondary

@Composable
fun ProfileScreen(
    uid: String,
    seedUser: SocialUser?,
    onBack: (() -> Unit)?,
    onOpenPost: (String) -> Unit,
    onEditProfile: () -> Unit = {},
) {
    if (uid.isEmpty()) {
        Box(Modifier.fillMaxSize().background(AmaBlack), contentAlignment = Alignment.Center) {
            Text("Loading…", color = Color.White, fontSize = 14.sp)
        }
        return
    }

    val state = remember(uid) { ProfileState(uid) }
    DisposableEffect(uid) {
        state.start(seedUser)
        onDispose { state.stop() }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().background(AmaBlack).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { ProfileHeader(state, onBack, onEditProfile) }
        items(state.posts, key = { it.postId }) { post -> GridCell(post, onOpenPost) }
    }
}

@Composable
private fun ProfileHeader(state: ProfileState, onBack: (() -> Unit)?, onEditProfile: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_alt_arrow_left_linear), "Back", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            } else {
                Spacer(Modifier.size(34.dp))
            }
            Text("Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.size(34.dp))
        }

        // Cover
        Box(modifier = Modifier.fillMaxWidth().height(88.dp).background(AmaCrimson))

        // Identity
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(state.avatar, 76.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(state.name.ifEmpty { "Brother" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                if (state.handle.isNotEmpty()) {
                    Text(state.handle, color = AmaTextSecondary, fontSize = 13.sp)
                }
            }
        }

        // Bio
        if (state.bio.isNotEmpty()) {
            Text(
                state.bio,
                color = Color(0xFFE3E6EC),
                fontSize = 13.5.sp,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 11.dp),
            )
        }

        // Action button
        Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
            if (state.isMe) {
                ActionButton("Edit profile", filled = false) { onEditProfile() }
            } else if (state.iFollow) {
                ActionButton("Following", filled = false) { state.toggle() }
            } else {
                ActionButton("Follow", filled = true) { state.toggle() }
            }
        }

        // Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
        ) {
            Stat("Posts", state.postsCount)
            Stat("Brothers", state.followersCount)
            Stat("Kudos", state.kudosTotal)
        }

        // Tabs (decorative for now)
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("Posts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Media", color = AmaTextSecondary, fontSize = 13.sp)
            Text("Saved", color = AmaTextSecondary, fontSize = 13.sp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AmaHairline))
    }
}

@Composable
private fun RowScope.Stat(label: String, value: Int) {
    Column(modifier = Modifier.weight(1f)) {
        Text("$value", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = AmaTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActionButton(label: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) AmaCrimson else Color.White.copy(alpha = 0.06f))
            .border(1.dp, if (filled) Color.Transparent else AmaHairline, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun GridCell(post: VideoPost, onOpenPost: (String) -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .background(Color(0xFF11151D))
            .clickable { if (post.type == "video") onOpenPost(post.postId) },
        contentAlignment = Alignment.Center,
    ) {
        if (post.type == "text" && post.posterUrl.isEmpty()) {
            val c = runCatching { Color(android.graphics.Color.parseColor(post.bgColor)) }.getOrDefault(Color(0xFF1A1F2B))
            Box(Modifier.fillMaxSize().background(c).padding(8.dp), contentAlignment = Alignment.Center) {
                Text(
                    post.caption, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 4, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                )
            }
        } else if (post.posterUrl.isNotEmpty()) {
            AsyncImage(post.posterUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        when (post.type) {
            "video" -> Icon(painterResource(R.drawable.ic_play_bold), null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
            "image" -> if (post.imageUrls.size > 1) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("${post.imageUrls.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(url: String, size: Dp) {
    Box(
        modifier = Modifier
            .size(size).clip(CircleShape).background(AmaCrimson)
            .padding(2.5.dp).clip(CircleShape).background(Color(0xFF11151D)),
    ) {
        if (url.isNotEmpty()) {
            AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
        }
    }
}
