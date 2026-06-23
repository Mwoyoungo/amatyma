package com.lokaleza.amatyma.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.lokaleza.amatyma.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.lokaleza.amatyma.social.ui.theme.AmaCrimson

/**
 * Immersive TikTok/Reels-style viewer: full-screen vertical pager over the feed
 * videos. The visible page autoplays with sound + loop; swipe up/down to move.
 */
@Composable
fun ReelsScreen(
    startPostId: String,
    user: SocialUser?,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit = {},
) {
    val vm: VideoFeedViewModel = viewModel()
    val posts = vm.posts.filter { it.type == "video" && it.hlsUrl.isNotEmpty() }
    val pagerState = rememberPagerState(initialPage = 0) { posts.size }
    var commentsPostId by remember { mutableStateOf<String?>(null) }

    var jumped by remember { mutableStateOf(false) }
    LaunchedEffect(posts) {
        if (!jumped && posts.isNotEmpty()) {
            val idx = posts.indexOfFirst { it.postId == startPostId }
            if (idx >= 0) pagerState.scrollToPage(idx)
            jumped = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (posts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…", color = Color.White, fontSize = 14.sp)
            }
        } else {
            VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                ReelPage(
                    post = posts[page],
                    currentUid = user?.uid,
                    isActive = page == pagerState.currentPage,
                    onOpenComments = { id -> commentsPostId = id },
                    onOpenUser = onOpenUser,
                )
            }
        }

        // Back
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 14.dp, top = 8.dp)
                .size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f))
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_alt_arrow_left_linear), "Back", tint = Color.White, modifier = Modifier.size(20.dp))
        }

        commentsPostId?.let { id ->
            CommentsSheet(postId = id, user = user, onDismiss = { commentsPostId = null })
        }
    }
}

@Composable
private fun ReelPage(post: VideoPost, currentUid: String?, isActive: Boolean, onOpenComments: (String) -> Unit, onOpenUser: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Poster as the base layer so swapping to the player never flashes black.
        if (post.posterUrl.isNotEmpty()) {
            AsyncImage(post.posterUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (isActive) {
            ReelVideoPlayer(post.hlsUrl, modifier = Modifier.fillMaxSize())
        }

        // Bottom gradient so the @username/caption + rail stay readable over bright video.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))),
        )

        // Right action rail (TikTok-style: author avatar + follow badge on top)
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 10.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.padding(bottom = 4.dp)) {
                Box(modifier = Modifier.clickable { onOpenUser(post.authorUid) }) {
                    ReelAvatar(post.authorAvatar, 48.dp)
                }
                if (post.authorUid.isNotEmpty() && post.authorUid != currentUid && !Following.isFollowing(post.authorUid)) {
                    Box(
                        modifier = Modifier.offset(y = 11.dp).size(22.dp).clip(CircleShape).background(AmaCrimson)
                            .border(2.dp, Color.Black, CircleShape).clickable { Following.toggle(post.authorUid) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_plus), "Follow", tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                }
            }
            RailItem(
                R.drawable.ic_heart_bold,
                post.kudosCount.toString(),
                tint = if (Likes.isLiked(post.postId)) AmaCrimson else Color.White,
            ) { Likes.toggle(post.postId) }
            RailItem(R.drawable.ic_chat_round_line_linear, post.commentCount.toString()) { onOpenComments(post.postId) }
            RailItem(R.drawable.ic_bookmark_linear, "Save") { }
            RailItem(R.drawable.ic_plain_2_bold, "Share") { }
        }

        // Bottom-left: @username + caption (avatar + follow live on the rail)
        Column(
            modifier = Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 14.dp, end = 86.dp, bottom = 28.dp),
        ) {
            Text(
                post.authorHandle.ifEmpty { post.authorName.ifEmpty { "Amatyma" } },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.clickable { onOpenUser(post.authorUid) },
            )
            if (post.caption.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(post.caption, color = Color.White, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ReelVideoPlayer(hlsUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exo = remember(hlsUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(hlsUrl))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 1f
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(hlsUrl) { onDispose { exo.release() } }

    Box(modifier = modifier.clickable { exo.playWhenReady = !exo.playWhenReady }) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exo
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
        )
    }
}

@Composable
private fun RailItem(icon: Int, label: String, tint: Color = Color.White, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape).clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), label, tint = tint, modifier = Modifier.size(30.dp))
        }
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReelAvatar(url: String, size: Dp) {
    Box(
        modifier = Modifier
            .size(size).clip(CircleShape).background(AmaCrimson)
            .padding(1.6.dp).clip(CircleShape).background(Color(0xFF11151D)),
    ) {
        if (url.isNotEmpty()) {
            AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
        }
    }
}
