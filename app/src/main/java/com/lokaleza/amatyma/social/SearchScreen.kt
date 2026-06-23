package com.lokaleza.amatyma.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.lokaleza.amatyma.R
import com.lokaleza.amatyma.social.ui.theme.AmaBlack
import com.lokaleza.amatyma.social.ui.theme.AmaCrimson
import com.lokaleza.amatyma.social.ui.theme.AmaHairline
import com.lokaleza.amatyma.social.ui.theme.AmaPurple
import com.lokaleza.amatyma.social.ui.theme.AmaTextSecondary

private data class SearchUser(val uid: String, val name: String, val handle: String, val avatar: String)

/** People discovery + post search. Reads the users collection read-only (chat untouched). */
@Composable
fun SearchScreen(
    currentUid: String?,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenPost: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var people by remember { mutableStateOf<List<SearchUser>>(emptyList()) }
    val feedVm: VideoFeedViewModel = viewModel()

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("users").limit(60).get()
            .addOnSuccessListener { snap ->
                people = snap.documents.mapNotNull { d ->
                    if (d.id == currentUid) return@mapNotNull null
                    val name = d.getString("displayName") ?: ""
                    val username = d.getString("username") ?: ""
                    if (name.isEmpty() && username.isEmpty()) return@mapNotNull null
                    SearchUser(
                        uid = d.id,
                        name = name.ifEmpty { username },
                        handle = if (username.isNotEmpty()) "@$username" else "",
                        avatar = d.getString("photoURL") ?: "",
                    )
                }
            }
    }

    val q = query.trim().lowercase()
    val filteredPeople = if (q.isEmpty()) people
        else people.filter { it.name.lowercase().contains(q) || it.handle.lowercase().contains(q) }
    val matchingPosts = if (q.isEmpty()) emptyList()
        else feedVm.posts.filter { it.caption.lowercase().contains(q) || it.authorName.lowercase().contains(q) }

    Column(Modifier.fillMaxSize().background(AmaBlack).statusBarsPadding()) {
        // Search bar
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_alt_arrow_left_linear), "Back", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(4.dp))
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, AmaHairline, RoundedCornerShape(999.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_magnifer_linear), null, tint = AmaTextSecondary, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(9.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search brothers & posts", color = AmaTextSecondary, fontSize = 14.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(AmaCrimson),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            if (filteredPeople.isNotEmpty()) {
                item { SectionLabel(if (q.isEmpty()) "Discover people" else "People") }
                items(filteredPeople, key = { it.uid }) { person ->
                    PersonRow(person, onOpenUser)
                }
            }
            if (matchingPosts.isNotEmpty()) {
                item { SectionLabel("Posts") }
                items(matchingPosts, key = { "p_${it.postId}" }) { post ->
                    PostResultRow(post, onOpenPost)
                }
            }
            if (filteredPeople.isEmpty() && matchingPosts.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (q.isEmpty()) "Find brothers to follow" else "No results for “$query”",
                            color = AmaTextSecondary, fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, color = AmaTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun PersonRow(person: SearchUser, onOpenUser: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onOpenUser(person.uid) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(AmaCrimson).padding(1.8.dp).clip(CircleShape).background(Color(0xFF11151D)),
        ) {
            if (person.avatar.isNotEmpty()) {
                AsyncImage(person.avatar, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(person.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (person.handle.isNotEmpty()) {
                Text(person.handle, color = AmaPurple, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val following = Following.isFollowing(person.uid)
        Box(
            Modifier.clip(RoundedCornerShape(999.dp))
                .background(if (following) Color.Transparent else AmaCrimson)
                .border(1.dp, if (following) AmaHairline else Color.Transparent, RoundedCornerShape(999.dp))
                .clickable { Following.toggle(person.uid) }
                .padding(horizontal = 16.dp, vertical = 7.dp),
        ) {
            Text(
                if (following) "Following" else "Follow",
                color = if (following) AmaTextSecondary else Color.White,
                fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp,
            )
        }
    }
}

@Composable
private fun PostResultRow(post: VideoPost, onOpenPost: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onOpenPost(post.postId) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF11151D))) {
            if (post.posterUrl.isNotEmpty()) {
                AsyncImage(post.posterUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                post.caption.ifEmpty { "Video" }, color = Color.White, fontSize = 14.sp,
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(post.authorName.ifEmpty { "Amatyma" }, color = AmaTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
