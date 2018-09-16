package com.kdrag0n.chathive.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import java.io.Serializable
import java.util.*

@Entity(tableName = "messages")
data class Message(@PrimaryKey(autoGenerate = true) var id: Long = 0,
                                val sender: MessageSender,
                                val text: String,
                                val createdAt: Date): Serializable {
    @Ignore var hasAnimated = false

    companion object {
        @JvmStatic private val serialVersionUID: Long = 1024L
    }
}