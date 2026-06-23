package com.lokaleza.amatyma.db

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lokaleza.amatyma.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessagePreviewAdapter(
    private val isGroupChat: Boolean = false
) : RecyclerView.Adapter<MessagePreviewAdapter.ViewHolder>() {

    // Messages come in descending order from Room — we reverse for display
    private var messages: List<MessageEntity> = emptyList()

    fun submitList(list: List<MessageEntity>) {
        messages = list.reversed()  // show oldest at top, newest at bottom
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (messages[position].sentByMe) TYPE_MINE else TYPE_THEIRS

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == TYPE_MINE)
            R.layout.item_message_mine else R.layout.item_message_theirs
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        holder.text.text = msg.text
        holder.time.text = formatTime(msg.timestamp)

        // Show sender name in group chats for incoming messages
        holder.sender?.let {
            it.visibility = if (isGroupChat && !msg.sentByMe) View.VISIBLE else View.GONE
            it.text = msg.senderName
        }
    }

    override fun getItemCount() = messages.size

    private fun formatTime(epochMillis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))

    inner class ViewHolder(view: View, viewType: Int) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.tvText)
        val time: TextView = view.findViewById(R.id.tvTime)
        val sender: TextView? = if (viewType == TYPE_THEIRS) view.findViewById(R.id.tvSender) else null
    }

    companion object {
        private const val TYPE_MINE = 0
        private const val TYPE_THEIRS = 1
    }
}
