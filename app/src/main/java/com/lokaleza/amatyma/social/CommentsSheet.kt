package com.lokaleza.amatyma.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.lokaleza.amatyma.R
import com.lokaleza.amatyma.social.ui.theme.AmaCrimson
import com.lokaleza.amatyma.social.ui.theme.AmaHairline
import com.lokaleza.amatyma.social.ui.theme.AmaTextSecondary

private val SHEET_BG = Color(0xFF12141A)

private fun docToComment(c: com.google.firebase.firestore.DocumentSnapshot): Comment = Comment(
    id = c.id,
    authorUid = c.getString("authorUid") ?: "",
    authorName = c.getString("authorName") ?: "",
    authorHandle = c.getString("authorHandle") ?: "",
    authorAvatar = c.getString("authorAvatar") ?: "",
    text = c.getString("text") ?: "",
    likeCount = (c.getLong("likeCount") ?: 0L).toInt(),
    timeMillis = c.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
    replyCount = (c.getLong("replyCount") ?: 0L).toInt(),
)

/** Live comments for a single post (used by the bottom sheet). */
class CommentsState(private val postId: String) {
    var comments by mutableStateOf<List<Comment>>(emptyList()); private set
    val count: Int get() = comments.size

    private val db = FirebaseFirestore.getInstance()
    private var registration: ListenerRegistration? = null

    fun start() {
        registration = db.collection("videoPosts").document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) comments = snap.documents.map { docToComment(it) }
            }
    }

    fun add(user: SocialUser?, text: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        db.collection("videoPosts").document(postId).collection("comments").add(
            mapOf(
                "authorUid" to uid,
                "authorName" to (user?.name ?: ""),
                "authorHandle" to (user?.handle ?: ""),
                "authorAvatar" to (user?.avatarUrl ?: ""),
                "text" to trimmed,
                "likeCount" to 0,
                "createdAt" to FieldValue.serverTimestamp(),
            )
        )
    }

    fun addReply(commentId: String, user: SocialUser?, text: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        db.collection("videoPosts").document(postId).collection("comments").document(commentId)
            .collection("replies").add(
                mapOf(
                    "authorUid" to uid,
                    "authorName" to (user?.name ?: ""),
                    "authorHandle" to (user?.handle ?: ""),
                    "authorAvatar" to (user?.avatarUrl ?: ""),
                    "text" to trimmed,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
    }

    fun stop() {
        registration?.remove()
        registration = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(postId: String, user: SocialUser?, onDismiss: () -> Unit) {
    val state = remember(postId) { CommentsState(postId) }
    DisposableEffect(postId) {
        state.start()
        onDispose { state.stop() }
    }
    var text by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<Comment?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SHEET_BG,
        contentColor = Color.White,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.25f)))
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f)) {
            Text(
                "${state.count} comments",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(AmaHairline))

            if (state.comments.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No comments yet — start the conversation.", color = AmaTextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    items(state.comments, key = { it.id }) { c ->
                        SheetCommentRow(postId, c) { replyTarget = it }
                    }
                }
            }

            // Composer — rides above BOTH the keyboard and the gesture nav bar.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
            ) {
                if (replyTarget != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.04f)).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Replying to ${replyTarget!!.authorName.ifEmpty { "Brother" }}", color = AmaTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("✕", color = AmaTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { replyTarget = null })
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SheetAvatar(user?.avatarUrl ?: "", 32.dp)
                    Spacer(Modifier.width(9.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                if (replyTarget != null) "Reply to ${replyTarget!!.authorName.ifEmpty { "Brother" }}…" else "Add a comment…",
                                color = AmaTextSecondary, fontSize = 14.sp,
                            )
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(AmaCrimson),
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    val canSend = text.isNotBlank()
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (canSend) AmaCrimson else Color.White.copy(alpha = 0.08f))
                            .clickable(enabled = canSend) {
                                val target = replyTarget
                                if (target != null) state.addReply(target.id, user, text) else state.add(user, text)
                                text = ""
                                replyTarget = null
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_plain_2_bold), "Send", tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetCommentRow(postId: String, c: Comment, onReply: (Comment) -> Unit) {
    var expanded by remember(c.id) { mutableStateOf(false) }
    var replies by remember(c.id) { mutableStateOf<List<Comment>>(emptyList()) }

    DisposableEffect(postId, c.id, expanded) {
        if (!expanded) return@DisposableEffect onDispose { }
        val reg = FirebaseFirestore.getInstance()
            .collection("videoPosts").document(postId).collection("comments").document(c.id)
            .collection("replies").orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ -> if (snap != null) replies = snap.documents.map { docToComment(it) } }
        onDispose { reg.remove() }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SheetAvatar(c.authorAvatar, 36.dp)
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(c.authorName.ifEmpty { "Brother" }, color = AmaTextSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(c.text, color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(commentTime(c.timeMillis), color = AmaTextSecondary, fontSize = 11.sp)
                    Spacer(Modifier.width(16.dp))
                    Text("Reply", color = AmaTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onReply(c) })
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_heart_linear), "Like", tint = AmaTextSecondary, modifier = Modifier.size(16.dp))
                if (c.likeCount > 0) {
                    Text("${c.likeCount}", color = AmaTextSecondary, fontSize = 10.sp)
                }
            }
        }

        if (c.replyCount > 0) {
            Row(
                modifier = Modifier.padding(start = 47.dp, top = 8.dp).clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(22.dp).height(1.dp).background(AmaHairline))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (expanded) "Hide replies" else "View ${c.replyCount} ${if (c.replyCount == 1) "reply" else "replies"}",
                    color = AmaTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (expanded) {
            Column(modifier = Modifier.padding(start = 47.dp, top = 4.dp)) {
                replies.forEach { ReplyRow(it) }
            }
        }
    }
}

@Composable
private fun ReplyRow(r: Comment) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        SheetAvatar(r.authorAvatar, 28.dp)
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(r.authorName.ifEmpty { "Brother" }, color = AmaTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(r.text, color = Color.White, fontSize = 13.5.sp)
            Spacer(Modifier.height(4.dp))
            Text(commentTime(r.timeMillis), color = AmaTextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SheetAvatar(url: String, size: Dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(AmaCrimson).padding(1.6.dp).clip(CircleShape).background(Color(0xFF11151D)),
    ) {
        if (url.isNotEmpty()) {
            AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
        }
    }
}

private fun commentTime(millis: Long): String {
    if (millis <= 0L) return ""
    val diff = System.currentTimeMillis() - millis
    val m = diff / 60_000
    val h = m / 60
    val d = h / 24
    return when {
        d > 0 -> "${d}d"
        h > 0 -> "${h}h"
        m > 0 -> "${m}m"
        else -> "now"
    }
}
