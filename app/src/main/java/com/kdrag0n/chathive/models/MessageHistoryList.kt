package com.kdrag0n.chathive.models

import java.io.Serializable

data class MessageHistoryList(val v: MutableList<List<Message>>) : Serializable {
    companion object {
        @JvmStatic private val serialVersionUID: Long = 1024
    }
}