package com.lokaleza.amatyma.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.lokaleza.amatyma.social.ui.theme.AmaBlack
import com.lokaleza.amatyma.social.ui.theme.AmaCrimson
import com.lokaleza.amatyma.social.ui.theme.AmaPurple
import com.lokaleza.amatyma.social.ui.theme.AmaTextSecondary

data class AppNotification(
    val id: String,
    val type: String,
    val actorUid: String,
    val actorName: String,
    val actorAvatar: String,
    val postId: String,
    val posterUrl: String,
    val text: String,
    val timeMillis: Long,
    val read: Boolean,
)

class AlertsState {
    var items by mutableStateOf<List<AppNotification>>(emptyList()); private set
    private var registration: ListenerRegistration? = null

    fun start() {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        registration = FirebaseFirestore.getInstance()
            .collection("users").document(me).collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    items = snap.documents.map { d ->
                        AppNotification(
                            id = d.id,
                            type = d.getString("type") ?: "",
                            actorUid = d.getString("actorUid") ?: "",
                            actorName = d.getString("actorName") ?: "",
                            actorAvatar = d.getString("actorAvatar") ?: "",
                            postId = d.getString("postId") ?: "",
                            posterUrl = d.getString("posterUrl") ?: "",
                            text = d.getString("text") ?: "",
                            timeMillis = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                            read = d.getBoolean("read") ?: false,
                        )
                    }
                }
            }
    }

    fun markAllRead() {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val col = FirebaseFirestore.getInstance().collection("users").document(me).collection("notifications")
        items.filter { !it.read }.forEach { col.document(it.id).update("read", true) }
    }

    fun stop() {
        registration?.remove()
        registration = null
    }
}

@Composable
fun NotificationsScreen(
    onOpenUser: (String) -> Unit = {},
    onOpenPost: (String) -> Unit = {},
) {
    val state = remember { AlertsState() }
    DisposableEffect(Unit) {
        state.start()
        onDispose { state.stop() }
    }

    Column(modifier = Modifier.fillMaxSize().background(AmaBlack).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Alerts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(
                "Mark all read",
                color = AmaPurple,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { state.markAllRead() },
            )
        }

        if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No alerts yet", color = AmaTextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                items(state.items, key = { it.id }) { n ->
                    NotifRow(n, onOpenUser, onOpenPost)
                }
            }
        }
    }
}

@Composable
private fun NotifRow(n: AppNotification, onOpenUser: (String) -> Unit, onOpenPost: (String) -> Unit) {
    val message = when (n.type) {
        "follow" -> "${n.actorName.ifEmpty { "Someone" }} started following you"
        "kudos" -> "${n.actorName.ifEmpty { "Someone" }} gave kudos to your post"
        "comment" -> "${n.actorName.ifEmpty { "Someone" }} commented: ${n.text}"
        "reply" -> "${n.actorName.ifEmpty { "Someone" }} replied: ${n.text}"
        "new_post" -> "${n.actorName.ifEmpty { "Someone" }} posted a new video"
        else -> n.actorName
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (!n.read) Color(0x14C693F0) else Color.Transparent)
            .clickable {
                if (n.type == "follow") onOpenUser(n.actorUid)
                else if (n.postId.isNotEmpty()) onOpenPost(n.postId)
            }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(AmaCrimson).padding(1.6.dp).clip(CircleShape).background(Color(0xFF11151D)),
        ) {
            if (n.actorAvatar.isNotEmpty()) {
                AsyncImage(n.actorAvatar, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(message, color = Color(0xFFDFE3EA), fontSize = 13.5.sp)
            Spacer(Modifier.size(3.dp))
            Text(timeAgoShort(n.timeMillis), color = AmaTextSecondary, fontSize = 11.sp)
        }
        if (n.posterUrl.isNotEmpty()) {
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF11151D))) {
                AsyncImage(n.posterUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private fun timeAgoShort(millis: Long): String {
    if (millis <= 0L) return ""
    val diff = System.currentTimeMillis() - millis
    val m = diff / 60_000
    val h = m / 60
    val d = h / 24
    return when {
        d > 0 -> "${d}d ago"
        h > 0 -> "${h}h ago"
        m > 0 -> "${m}m ago"
        else -> "now"
    }
}
