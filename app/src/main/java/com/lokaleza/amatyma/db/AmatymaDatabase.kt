package com.lokaleza.amatyma.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AmatymaDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile private var INSTANCE: AmatymaDatabase? = null

        fun get(context: Context): AmatymaDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AmatymaDatabase::class.java,
                    "amatyma_chat.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
