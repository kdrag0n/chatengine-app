package com.kdrag0n.chathive.models

import java.io.Serializable
import java.util.*

data class Message(val sender: MessageSender, val text: String, val createdAt: Date,
                   var hasAnimated: Boolean = false): Serializable {
    companion object {
        @JvmStatic private val serialVersionUID: Long = 1024
    }
}