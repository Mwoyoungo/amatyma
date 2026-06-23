package com.lokaleza.amatyma.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    /** Latest 50 messages for a conversation, oldest first (natural chat order). */
    @Query("""
        SELECT * FROM messages
        WHERE conversationId = :conversationId
        ORDER BY timestamp DESC
        LIMIT 50
    """)
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    /** One-shot read for immediate pre-paint — no coroutine needed on the call site. */
    @Query("""
        SELECT * FROM messages
        WHERE conversationId = :conversationId
        ORDER BY timestamp DESC
        LIMIT 50
    """)
    suspend fun getMessages(conversationId: String): List<MessageEntity>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    /** Keep only the latest 50 per conversation — prevents unbounded growth. */
    @Query("""
        DELETE FROM messages
        WHERE conversationId = :conversationId
        AND messageId NOT IN (
            SELECT messageId FROM messages
            WHERE conversationId = :conversationId
            ORDER BY timestamp DESC
            LIMIT 50
        )
    """)
    suspend fun trimToLimit(conversationId: String)
}
