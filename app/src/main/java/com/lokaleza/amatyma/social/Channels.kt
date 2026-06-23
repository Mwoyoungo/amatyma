package com.lokaleza.amatyma.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.lokaleza.amatyma.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lokaleza.amatyma.social.ui.theme.AmaBlack
import com.lokaleza.amatyma.social.ui.theme.AmaCrimson
import com.lokaleza.amatyma.social.ui.theme.AmaTextSecondary

private data class Channel(val name: String, val icon: Int, val blurb: String)

private val CHANNELS = listOf(
    Channel("Business", R.drawable.ic_case_round_bold, "Hustle, deals & accountability"),
    Channel("Sports", R.drawable.ic_basketball_bold, "Training, teams & competition"),
    Channel("Wellness", R.drawable.ic_meditation_bold, "Discipline, mindset & health"),
    Channel("Political", R.drawable.ic_users_group_rounded_bold, "Debate & community"),
    Channel("Technology", R.drawable.ic_cpu_bold, "Building & the future"),
)

@Composable
fun ChannelsScreen(onOpenChannel: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AmaBlack).statusBarsPadding()) {
        Text(
            "Channels",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 2.dp),
        )
        Text(
            "Find your brotherhood by topic",
            color = AmaTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 18.dp, bottom = 12.dp),
        )
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            CHANNELS.forEach { channel -> ChannelRow(channel) { onOpenChannel(channel.name) } }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ChannelRow(channel: Channel, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(AmaCrimson),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(channel.icon), channel.name, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(channel.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(channel.blurb, color = AmaTextSecondary, fontSize = 12.5.sp)
        }
        Icon(painterResource(R.drawable.ic_alt_arrow_right_linear), null, tint = AmaTextSecondary, modifier = Modifier.size(22.dp))
    }
}
