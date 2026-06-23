package com.lokaleza.amatyma.db

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.lokaleza.amatyma.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private val onClick: (ConversationEntity) -> Unit
) : ListAdapter<ConversationEntity, ConversationAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.ivAvatar)
        val name: TextView = view.findViewById(R.id.tvName)
        val lastMessage: TextView = view.findViewById(R.id.tvLastMessage)
        val time: TextView = view.findViewById(R.id.tvTime)
        val unread: TextView = view.findViewById(R.id.tvUnread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.name.text = item.name
        holder.lastMessage.text = item.lastMessage.ifEmpty { "No messages yet" }
        holder.time.text = formatTime(item.lastMessageTime)

        if (item.unreadCount > 0) {
            holder.unread.visibility = View.VISIBLE
            holder.unread.text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString()
        } else {
            holder.unread.visibility = View.GONE
        }

        holder.avatar.load(item.avatarUrl) {
            transformations(CircleCropTransformation())
            placeholder(R.drawable.ic_default_avatar)
            error(R.drawable.ic_default_avatar)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    private fun formatTime(epochMillis: Long): String {
        if (epochMillis == 0L) return ""
        val date = Date(epochMillis)
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { time = date }

        return when {
            now.get(Calendar.DATE) == then.get(Calendar.DATE) ->
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            now.get(Calendar.WEEK_OF_YEAR) == then.get(Calendar.WEEK_OF_YEAR) ->
                SimpleDateFormat("EEE", Locale.getDefault()).format(date)
            else ->
                SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ConversationEntity>() {
            override fun areItemsTheSame(a: ConversationEntity, b: ConversationEntity) =
                a.conversationId == b.conversationId
            override fun areContentsTheSame(a: ConversationEntity, b: ConversationEntity) =
                a == b
        }
    }
}
