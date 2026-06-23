package com.lokaleza.amatyma.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    /** Emits a new list every time any conversation row changes — drives the UI reactively. */
    @Query("SELECT * FROM conversations ORDER BY lastMessageTime DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    /** Upsert: insert or replace if conversationId already exists. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE conversationId = :id")
    suspend fun clearUnread(id: String)

    @Query("UPDATE conversations SET lastMessage = :text, lastMessageTime = :time WHERE conversationId = :id")
    suspend fun updateLastMessage(id: String, text: String, time: Long)

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun count(): Int
}
