package com.lokaleza.amatyma.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per conversation (1-to-1 or group).
 * Drives the conversation list UI instantly from local storage,
 * even before CometChat has made any network call.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val conversationId: String,  // UID for users, GUID for groups
    val name: String,
    val avatarUrl: String,
    val lastMessage: String,
    val lastMessageTime: Long,               // epoch millis — used for sorting
    val unreadCount: Int,
    val type: String                         // "user" or "group"
)
