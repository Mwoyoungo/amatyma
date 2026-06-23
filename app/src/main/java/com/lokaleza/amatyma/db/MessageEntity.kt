package com.lokaleza.amatyma.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Last 50 messages per conversation.
 * Used to paint the chat screen instantly before CometChat loads from server.
 * We deliberately keep this small — full history lives in CometChat's own store.
 */
@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId", "timestamp"])]
)
data class MessageEntity(
    @PrimaryKey val messageId: Long,
    val conversationId: String,   // matches ConversationEntity.conversationId
    val senderUid: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,          // epoch millis
    val sentByMe: Boolean,
    val status: String            // "sent" | "delivered" | "read" | "pending"
)
