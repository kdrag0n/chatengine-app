package com.kdrag0n.chathive.models

enum class MessageSender {
    INTERNAL, BOT, USER;

    companion object {
        @JvmStatic private val serialVersionUID: Long = 1024
    }
}