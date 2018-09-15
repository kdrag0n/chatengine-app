package com.kdrag0n.chathive.database

import android.arch.persistence.room.TypeConverter
import com.kdrag0n.chathive.models.MessageSender
import java.util.*

class Converters {
    companion object {
        @TypeConverter @JvmStatic fun dateFromTimestamp(value: Long?): Date? {
            return Date(value ?: return null)
        }

        @TypeConverter @JvmStatic fun dateToTimestamp(date: Date?): Long? {
            return date?.time
        }

        @TypeConverter @JvmStatic fun senderFromString(value: String?): MessageSender? {
            return MessageSender.valueOf(value ?: return null)
        }

        @TypeConverter @JvmStatic fun senderToString(sender: MessageSender?): String? {
            return sender?.name
        }
    }
}