package com.kdrag0n.chathive.models

import java.io.Serializable
import java.util.*

data class MessageHistoryList(val entries: MutableList<Entry>) : Serializable {
    data class Entry(var name: String, val time: Date, val messages: List<Message>) : Serializable
}