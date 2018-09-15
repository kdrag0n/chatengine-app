package com.kdrag0n.chathive.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kdrag0n.chathive.models.Message

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertMessages(messages: List<Message>)

    @Query("SELECT * FROM messages ORDER BY id ASC")
    fun loadAllMessages(): List<Message>
}