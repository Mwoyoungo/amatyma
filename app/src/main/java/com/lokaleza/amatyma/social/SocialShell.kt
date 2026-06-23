package com.lokaleza.amatyma.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.lokaleza.amatyma.R
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lokaleza.amatyma.social.ui.theme.AmaBlack
import com.lokaleza.amatyma.social.ui.theme.AmaHairline
import com.lokaleza.amatyma.social.ui.theme.AmaTextMuted
import kotlinx.coroutines.launch

private enum class SocialTab(
    val route: String,
    val label: String,
    val iconActive: Int,
    val iconInactive: Int,
) {
    FEED("feed", "Home", R.drawable.ic_home_smile_bold, R.drawable.ic_home_smile_linear),
    CHANNELS("channels", "Channels", R.drawable.ic_hashtag_circle_bold, R.drawable.ic_hashtag_circle_linear),
    ALERTS("notifications", "Alerts", R.drawable.ic_bell_bold, R.drawable.ic_bell_linear),
    PROFILE("profile", "Profile", R.drawable.ic_user_circle_bold, R.drawable.ic_user_circle_linear),
}

private const val ROUTE_CREATE = "create"

@Composable
fun SocialShell(onOpenChat: () -> Unit = {}, onLogout: () -> Unit = {}) {
    val nav = rememberNavController()
    val identityVm: SocialIdentityViewModel = viewModel()
    val feedListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selectedTopic by remember { mutableStateOf("All") }
    LaunchedEffect(identityVm.user) {
        Likes.ensureStarted()
        Following.ensureStarted()
        Stories.ensureStarted()
    }
    var storyAuthor by remember { mutableStateOf<String?>(null) }

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTabRoute = currentRoute == SocialTab.FEED.route ||
        currentRoute == SocialTab.CHANNELS.route ||
        currentRoute == SocialTab.ALERTS.route ||
        currentRoute == SocialTab.PROFILE.route

    var barVisible by remember { mutableStateOf(true) }
    LaunchedEffect(currentRoute) { barVisible = true }

    // Hide the bar when scrolling content up, reveal when scrolling down.
    val nestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -4f) barVisible = false
                else if (available.y > 4f) barVisible = true
                return Offset.Zero
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AmaBlack)) {
        NavHost(
            navController = nav,
            startDestination = SocialTab.FEED.route,
            modifier = Modifier.fillMaxSize().nestedScroll(nestedScroll),
        ) {
            composable(SocialTab.FEED.route) {
                FeedScreen(
                    user = identityVm.user,
                    onOpenChat = onOpenChat,
                    onOpenPost = { postId -> nav.navigate("reels/$postId") },
                    onOpenUser = { uid -> if (uid.isNotEmpty()) nav.navigate("user/$uid") },
                    selectedTopic = selectedTopic,
                    onSelectTopic = { selectedTopic = it },
                    onOpenSearch = { nav.navigate("search") },
                    onCreateStory = { nav.navigate("createStory") },
                    onOpenStory = { uid -> storyAuthor = uid },
                    listState = feedListState,
                )
            }
            composable(SocialTab.CHANNELS.route) {
                ChannelsScreen(onOpenChannel = { topic ->
                    selectedTopic = topic
                    nav.navigate(SocialTab.FEED.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    scope.launch { feedListState.animateScrollToItem(0) }
                })
            }
            composable(ROUTE_CREATE) {
                CreateScreen(user = identityVm.user, onClose = {
                    nav.navigate(SocialTab.FEED.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
            composable(SocialTab.ALERTS.route) {
                NotificationsScreen(
                    onOpenUser = { uid -> if (uid.isNotEmpty()) nav.navigate("user/$uid") },
                    onOpenPost = { id -> nav.navigate("reels/$id") },
                )
            }
            composable(SocialTab.PROFILE.route) {
                ProfileScreen(
                    uid = identityVm.user?.uid ?: "",
                    seedUser = identityVm.user,
                    onBack = null,
                    onOpenPost = { id -> nav.navigate("reels/$id") },
                    onEditProfile = { nav.navigate("editProfile") },
                )
            }
            composable("editProfile") {
                EditProfileScreen(
                    uid = identityVm.user?.uid ?: "",
                    seed = identityVm.user,
                    onClose = { nav.popBackStack() },
                    onLogout = onLogout,
                )
            }
            composable("createStory") {
                CreateStoryScreen(user = identityVm.user, onClose = { nav.popBackStack() })
            }
            composable("search") {
                SearchScreen(
                    currentUid = identityVm.user?.uid,
                    onBack = { nav.popBackStack() },
                    onOpenUser = { uid -> if (uid.isNotEmpty()) nav.navigate("user/$uid") },
                    onOpenPost = { id -> nav.navigate("reels/$id") },
                )
            }
            composable("reels/{postId}") { entry ->
                ReelsScreen(
                    startPostId = entry.arguments?.getString("postId") ?: "",
                    user = identityVm.user,
                    onBack = { nav.popBackStack() },
                    onOpenUser = { uid -> if (uid.isNotEmpty()) nav.navigate("user/$uid") },
                )
            }
            composable("user/{uid}") { entry ->
                ProfileScreen(
                    uid = entry.arguments?.getString("uid") ?: "",
                    seedUser = identityVm.user,
                    onBack = { nav.popBackStack() },
                    onOpenPost = { id -> nav.navigate("reels/$id") },
                )
            }
        }

        AnimatedVisibility(
            visible = isTabRoute && barVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SocialBottomBar(
                nav = nav,
                currentRoute = currentRoute,
                onHomeReselected = {
                    barVisible = true
                    scope.launch { feedListState.animateScrollToItem(0) }
                },
            )
        }

        storyAuthor?.let { author ->
            StoryViewer(author) { storyAuthor = null }
        }
    }
}

@Composable
private fun SocialBottomBar(nav: NavController, currentRoute: String?, onHomeReselected: () -> Unit) {
    fun go(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // background BEFORE navigationBarsPadding → the black fills down to the screen
    // edge (under the gesture bar), while the icons sit above the safe area.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmaBlack)
            .navigationBarsPadding()
            .padding(top = 9.dp, bottom = 8.dp, start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        NavItem(SocialTab.FEED, currentRoute == SocialTab.FEED.route) {
            if (currentRoute == SocialTab.FEED.route) onHomeReselected() else go(SocialTab.FEED.route)
        }
        NavItem(SocialTab.CHANNELS, currentRoute == SocialTab.CHANNELS.route) { go(SocialTab.CHANNELS.route) }
        CreateButton { go(ROUTE_CREATE) }
        NavItem(SocialTab.ALERTS, currentRoute == SocialTab.ALERTS.route) { go(SocialTab.ALERTS.route) }
        NavItem(SocialTab.PROFILE, currentRoute == SocialTab.PROFILE.route) { go(SocialTab.PROFILE.route) }
    }
}

@Composable
private fun RowScope.NavItem(tab: SocialTab, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) Color.White else AmaTextMuted
    Column(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(if (selected) tab.iconActive else tab.iconInactive),
            tab.label,
            tint = color,
            modifier = Modifier.size(24.dp),
        )
        Text(tab.label, color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RowScope.CreateButton(onClick: () -> Unit) {
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AmaBlack)
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_plus), "Create", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}
