package com.kdrag0n.chathive.models

import java.io.Serializable

data class MessageList(val messages: MutableList<Message>) : Serializable {
    companion object {
        @JvmStatic private val serialVersionUID: Long = 1024L
    }
}