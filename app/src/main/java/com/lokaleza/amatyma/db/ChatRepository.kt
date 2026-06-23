package com.lokaleza.amatyma.db

import android.content.Context
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Single point of contact for local data.
 * Reads come from Room (instant, offline-safe).
 * Writes happen whenever CometChat delivers data.
 */
class ChatRepository(context: Context) {

    private val db = AmatymaDatabase.get(context)
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    // ─── Read ────────────────────────────────────────────────────────────────

    fun observeConversations(): Flow<List<ConversationEntity>> =
        conversationDao.observeAll()

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.observeMessages(conversationId)

    suspend fun getMessagesCached(conversationId: String): List<MessageEntity> =
        messageDao.getMessages(conversationId)

    suspend fun hasConversations(): Boolean = conversationDao.count() > 0

    // ─── Write ───────────────────────────────────────────────────────────────

    /** Called whenever CometChat delivers a fresh conversations list. */
    fun saveConversations(conversations: List<Conversation>) {
        scope.launch {
            val myUid = CometChatUIKit.getLoggedInUser()?.uid ?: return@launch
            val entities = conversations.mapNotNull { it.toEntity(myUid) }
            conversationDao.upsertAll(entities)
        }
    }

    /** Called when a single conversation updates (new message, read receipt, etc.). */
    fun saveConversation(conversation: Conversation) {
        scope.launch {
            val myUid = CometChatUIKit.getLoggedInUser()?.uid ?: return@launch
            conversation.toEntity(myUid)?.let { conversationDao.upsert(it) }
        }
    }

    /** Called when a new message arrives or is sent. */
    fun saveMessage(message: BaseMessage, conversationId: String) {
        scope.launch {
            val myUid = CometChatUIKit.getLoggedInUser()?.uid ?: return@launch
            message.toEntity(conversationId, myUid)?.let {
                messageDao.upsert(it)
                messageDao.trimToLimit(conversationId)
            }
        }
    }

    /** Called when a batch of messages loads for a conversation (on chat open). */
    fun saveMessages(messages: List<BaseMessage>, conversationId: String) {
        scope.launch {
            val myUid = CometChatUIKit.getLoggedInUser()?.uid ?: return@launch
            val entities = messages.mapNotNull { it.toEntity(conversationId, myUid) }
            if (entities.isNotEmpty()) {
                messageDao.upsertAll(entities)
                messageDao.trimToLimit(conversationId)
            }
        }
    }

    fun markAsRead(conversationId: String) {
        scope.launch { conversationDao.clearUnread(conversationId) }
    }

    fun updateConversationLastMessage(conversationId: String, text: String, time: Long) {
        scope.launch { conversationDao.updateLastMessage(conversationId, text, time) }
    }
}

// ─── Mapping helpers ─────────────────────────────────────────────────────────

private fun Conversation.toEntity(myUid: String): ConversationEntity? {
    val with = conversationWith ?: return null
    val (id, name, avatar, type) = when (with) {
        is User -> listOf(with.uid, with.name ?: "", with.avatar ?: "", "user")
        is Group -> listOf(with.guid, with.name ?: "", with.icon ?: "", "group")
        else -> return null
    }
    val last = lastMessage
    val lastText = when {
        last == null -> ""
        last is TextMessage -> last.text ?: ""
        else -> "[${last.type}]"
    }
    return ConversationEntity(
        conversationId = id,
        name = name,
        avatarUrl = avatar,
        lastMessage = lastText,
        lastMessageTime = (last?.sentAt ?: 0L) * 1000L,
        unreadCount = unreadMessageCount,
        type = type
    )
}

private fun BaseMessage.toEntity(conversationId: String, myUid: String): MessageEntity? {
    // Only cache text messages for the pre-paint layer.
    // Media/calls are handled by CometChat's own renderer once it loads.
    if (this !is TextMessage) return null
    val sender = sender ?: return null
    return MessageEntity(
        messageId = id,
        conversationId = conversationId,
        senderUid = sender.uid,
        senderName = sender.name ?: "",
        text = text ?: "",
        timestamp = sentAt * 1000L,
        sentByMe = sender.uid == myUid,
        status = "sent"
    )
}
