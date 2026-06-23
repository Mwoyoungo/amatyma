package com.lokaleza.amatyma.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.lokaleza.amatyma.R
import com.lokaleza.amatyma.social.ui.theme.AmaBlack
import com.lokaleza.amatyma.social.ui.theme.AmaCrimson
import com.lokaleza.amatyma.social.ui.theme.AmaCrimsonBorder
import com.lokaleza.amatyma.social.ui.theme.AmaCrimsonTint
import com.lokaleza.amatyma.social.ui.theme.AmaHairline
import com.lokaleza.amatyma.social.ui.theme.AmaPurple
import com.lokaleza.amatyma.social.ui.theme.AmaRose
import com.lokaleza.amatyma.social.ui.theme.AmaTextSecondary

private val TOPICS = listOf("All", "Business", "Sports", "Wellness", "Political", "Technology")
private const val TAB_FOR_YOU = "For You"
private const val TAB_FOLLOWING = "Following"
private const val DEFAULT_RATIO = 0.5625f // 9:16 portrait until the poster loads
private val ACTION_GRAY = Color(0xFFC8CED9)

@Composable
fun FeedScreen(
    user: SocialUser?,
    onOpenChat: () -> Unit = {},
    onOpenPost: (String) -> Unit = {},
    onOpenUser: (String) -> Unit = {},
    selectedTopic: String = "All",
    onSelectTopic: (String) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onCreateStory: () -> Unit = {},
    onOpenStory: (String) -> Unit = {},
    listState: LazyListState,
) {
    val feedVm: VideoFeedViewModel = viewModel()
    val allPosts = feedVm.posts
    var feedMode by rememberSaveable { mutableStateOf(TAB_FOR_YOU) }

    val base = if (feedMode == TAB_FOLLOWING) allPosts.filter { Following.isFollowing(it.authorUid) } else allPosts
    val posts = if (feedMode == TAB_FOLLOWING || selectedTopic == "All") base
        else base.filter { it.topic.equals(selectedTopic, ignoreCase = true) }

    // The video closest to the viewport centre autoplays (muted). Post items carry
    // a String key (postId); the header/chips/story items don't, so they're skipped.
    val activePostId by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - center) }
                ?.key as? String
        }
    }

    // One scrolling list: the header/chips/stories are list content, so they
    // scroll away. statusBarsPadding keeps it below the notch (edge-to-edge).
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(AmaBlack)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 100.dp), // clears the floating bottom bar
    ) {
        item { CompactHeader(onOpenChat, onOpenSearch) }
        item { FeedTabs(feedMode) { feedMode = it } }
        if (feedMode == TAB_FOR_YOU) item { TopicChips(selectedTopic, onSelectTopic) }
        item { StoryTray(user, onCreateStory, onOpenStory) }

        if (posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 36.dp),
                    ) {
                        val (title, sub) = when {
                            feedMode == TAB_FOLLOWING && Following.ids.isEmpty() ->
                                "Not following anyone yet" to "Follow brothers and their posts land here."
                            feedMode == TAB_FOLLOWING ->
                                "All caught up" to "No new posts from the brothers you follow."
                            else -> "No posts yet" to "Be the first to share with the brotherhood."
                        }
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(sub, color = AmaTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(posts, key = { it.postId }) { post ->
                VideoCard(post, user?.uid, onOpenPost, onOpenUser, isActive = post.postId == activePostId)
            }
        }
    }
}

// ─── Compact header (scrolls away) ───────────────────────────────────────────

@Composable
private fun CompactHeader(onOpenChat: () -> Unit, onOpenSearch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(AmaCrimson), contentAlignment = Alignment.Center) {
            Text("A", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        }
        Spacer(Modifier.width(9.dp))
        Text("Amatyma", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderIcon(R.drawable.ic_chat_round_line_linear, onClick = onOpenChat)
            HeaderIcon(R.drawable.ic_magnifer_linear, onClick = onOpenSearch)
            HeaderIcon(R.drawable.ic_bell_linear)
        }
    }
}

@Composable
private fun HeaderIcon(icon: Int, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(32.dp).clip(CircleShape).background(AmaCrimsonTint)
            .border(1.dp, AmaCrimsonBorder, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), null, tint = AmaRose, modifier = Modifier.size(16.dp))
    }
}

// ─── Topic chips ─────────────────────────────────────────────────────────────

@Composable
private fun TopicChips(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = 16.dp, end = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        TOPICS.forEach { topic -> TopicChip(topic, selected = topic == selected) { onSelect(topic) } }
    }
}

@Composable
private fun TopicChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) AmaCrimson else AmaBlack)
            .border(1.dp, if (selected) Color.Transparent else AmaHairline, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            color = if (selected) Color.White else Color(0xFFEEF0F6),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

// ─── For You / Following tabs ────────────────────────────────────────────────

@Composable
private fun FeedTabs(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        FeedTab(TAB_FOR_YOU, selected == TAB_FOR_YOU) { onSelect(TAB_FOR_YOU) }
        Spacer(Modifier.width(26.dp))
        FeedTab(TAB_FOLLOWING, selected == TAB_FOLLOWING) { onSelect(TAB_FOLLOWING) }
    }
}

@Composable
private fun FeedTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            label,
            color = if (selected) Color.White else AmaTextSecondary,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .size(width = 20.dp, height = 2.5.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (selected) AmaCrimson else Color.Transparent),
        )
    }
}

// ─── Stories ─────────────────────────────────────────────────────────────────

// ─── Post card (scaled, full-bleed media) ────────────────────────────────────

@Composable
private fun VideoCard(post: VideoPost, currentUid: String?, onOpenPost: (String) -> Unit, onOpenUser: (String) -> Unit, isActive: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).clickable { onOpenUser(post.authorUid) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeedAvatar(post.authorAvatar, 46.dp)
                Spacer(Modifier.width(11.dp))
                Column {
                    Text(post.authorName.ifEmpty { "Amatyma" }, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(post.authorHandle.ifEmpty { post.topic }, color = AmaPurple, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (post.authorUid.isNotEmpty() && post.authorUid != currentUid) {
                val following = Following.isFollowing(post.authorUid)
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (following) Color.Transparent else AmaCrimson)
                        .border(1.dp, if (following) AmaHairline else Color.Transparent, RoundedCornerShape(999.dp))
                        .clickable { Following.toggle(post.authorUid) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        if (following) "Following" else "Follow",
                        color = if (following) AmaTextSecondary else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                    )
                }
            }
        }

        if (post.type == "text") {
            TextPostBody(post)
        } else {
            if (post.caption.isNotEmpty()) {
                Text(
                    post.caption,
                    color = Color(0xFFE3E6EC),
                    fontSize = 15.5.sp,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                )
            } else {
                Spacer(Modifier.height(12.dp))
            }
            if (post.type == "image") ImageCarousel(post) else PosterMedia(post, isActive, onOpenPost)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            LikeAction(post)
            ActionItem(R.drawable.ic_chat_round_line_linear, post.commentCount.toString())
            Spacer(Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_plain_2_bold), "Share", tint = ACTION_GRAY, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ImageCarousel(post: VideoPost) {
    val urls = post.imageUrls.ifEmpty { listOfNotNull(post.posterUrl.ifEmpty { null }) }
    if (urls.isEmpty()) return
    val pagerState = rememberPagerState { urls.size }
    Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF0B0E15))) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Image(
                painter = rememberAsyncImagePainter(urls[page]),
                contentDescription = post.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (urls.size > 1) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    .clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("${pagerState.currentPage + 1}/${urls.size}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(urls.size) { i ->
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (i == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.4f)))
                }
            }
        }
    }
}

@Composable
private fun TextPostBody(post: VideoPost) {
    if (post.bgColor.isNotEmpty()) {
        val c = runCatching { Color(android.graphics.Color.parseColor(post.bgColor)) }.getOrDefault(Color(0xFF1A1F2B))
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp)).background(c).padding(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(post.caption, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    } else {
        Text(
            post.caption,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 14.dp),
        )
    }
}

@Composable
private fun PosterMedia(post: VideoPost, isActive: Boolean, onOpenPost: (String) -> Unit) {
    val painter = rememberAsyncImagePainter(post.posterUrl)
    val ratio = run {
        val state = painter.state
        if (state is AsyncImagePainter.State.Success) {
            val s = state.painter.intrinsicSize
            if (s.width > 0f && s.height > 0f) (s.width / s.height).coerceIn(0.5f, 1.91f) else DEFAULT_RATIO
        } else DEFAULT_RATIO
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .background(Color(0xFF0B0E15))
            .clickable { onOpenPost(post.postId) },
        contentAlignment = Alignment.Center,
    ) {
        // Poster is ALWAYS the base layer so activating the video never flashes black.
        Image(painter = painter, contentDescription = post.caption, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (isActive && post.hlsUrl.isNotEmpty()) {
            InlineVideo(post.hlsUrl, Modifier.matchParentSize())
        } else {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.42f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_play_bold), "Play", tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
        // Tap the card for sound (opens Reels).
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_volume_loud_linear), "Sound", tint = Color.White, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun InlineVideo(hlsUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exo = remember(hlsUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(hlsUrl))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f // muted in-feed; sound is in Reels
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(hlsUrl) { onDispose { exo.release() } }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exo
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                // Transparent shutter so the poster behind shows until the first
                // frame decodes — no black flash on activation.
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
    )
}

@Composable
private fun ActionItem(icon: Int, count: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(painterResource(icon), null, tint = ACTION_GRAY, modifier = Modifier.size(22.dp))
        Text(count, color = ACTION_GRAY, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LikeAction(post: VideoPost) {
    val liked = Likes.isLiked(post.postId)
    Row(
        modifier = Modifier.clickable { Likes.toggle(post.postId) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            painterResource(if (liked) R.drawable.ic_heart_bold else R.drawable.ic_heart_linear),
            "Like",
            tint = if (liked) AmaCrimson else ACTION_GRAY,
            modifier = Modifier.size(22.dp),
        )
        Text(post.kudosCount.toString(), color = ACTION_GRAY, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FeedAvatar(url: String, size: Dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(AmaCrimson).padding(1.8.dp).clip(CircleShape).background(Color(0xFF11151D)),
    ) {
        if (url.isNotEmpty()) {
            AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
        }
    }
}
