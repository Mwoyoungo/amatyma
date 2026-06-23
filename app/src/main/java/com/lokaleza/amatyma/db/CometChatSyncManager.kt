package com.lokaleza.amatyma.db

import android.content.Context
import android.util.Log
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.TypingIndicator
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit

/**
 * Keeps our Room database in sync with CometChat.
 * Call [start] once after CometChat login succeeds.
 */
class CometChatSyncManager(context: Context) {

    private val repo = ChatRepository(context)

    companion object {
        private const val TAG = "CometChatSync"
        private const val LISTENER_ID = "amatyma_sync_listener"
        private const val PAGE_SIZE = 30
    }

    fun start() {
        registerMessageListener()
        fetchAndCacheConversations()
    }

    fun stop() {
        CometChat.removeMessageListener(LISTENER_ID)
    }

    // ─── Initial fetch ────────────────────────────────────────────────────────

    private fun fetchAndCacheConversations() {
        val request = ConversationsRequest.ConversationsRequestBuilder()
            .setLimit(PAGE_SIZE)
            .build()

        request.fetchNext(object : CometChat.CallbackListener<List<Conversation>>() {
            override fun onSuccess(conversations: List<Conversation>?) {
                if (!conversations.isNullOrEmpty()) {
                    Log.d(TAG, "Fetched ${conversations.size} conversations → Room")
                    repo.saveConversations(conversations)
                }
            }

            override fun onError(e: CometChatException?) {
                // Network unavailable — Room already has whatever was cached last session.
                Log.w(TAG, "Conversation fetch failed (offline?): ${e?.message}")
            }
        })
    }

    // ─── Real-time listener ───────────────────────────────────────────────────

    private fun registerMessageListener() {
        CometChat.addMessageListener(LISTENER_ID, object : CometChat.MessageListener() {

            override fun onTextMessageReceived(message: TextMessage?) {
                message ?: return
                val myUid = CometChatUIKit.getLoggedInUser()?.uid ?: return
                val conversationId = conversationIdFrom(message, myUid)
                repo.saveMessage(message, conversationId)
                // Update the conversation's last message preview in Room
                repo.updateConversationLastMessage(
                    conversationId = conversationId,
                    text = message.text ?: "",
                    time = message.sentAt * 1000L
                )
                Log.d(TAG, "New text message cached → $conversationId")
            }

            override fun onMediaMessageReceived(message: MediaMessage?) {
                message ?: return
                val myUid = CometChatUIKit.getLoggedInUser()?.uid ?: return
                val conversationId = conversationIdFrom(message, myUid)
                repo.updateConversationLastMessage(
                    conversationId = conversationId,
                    text = "[${message.type}]",
                    time = message.sentAt * 1000L
                )
            }

            override fun onCustomMessageReceived(message: CustomMessage?) {
                message ?: return
                val myUid = CometChatUIKit.getLoggedInUser()?.uid ?: return
                val conversationId = conversationIdFrom(message, myUid)
                repo.updateConversationLastMessage(
                    conversationId = conversationId,
                    text = "[message]",
                    time = message.sentAt * 1000L
                )
            }

            override fun onTypingStarted(indicator: TypingIndicator?) { /* no-op */ }
            override fun onTypingEnded(indicator: TypingIndicator?) { /* no-op */ }
        })
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun conversationIdFrom(message: BaseMessage, myUid: String): String {
        return if (message.receiverType == CometChatConstants.RECEIVER_TYPE_GROUP) {
            message.receiverUid
        } else {
            // 1-to-1: store under the OTHER person's UID
            if (message.sender?.uid == myUid) message.receiverUid
            else message.sender?.uid ?: ""
        }
    }
}
