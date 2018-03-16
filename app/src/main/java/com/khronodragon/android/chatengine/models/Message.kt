package com.khronodragon.android.chatengine.models

import java.io.Serializable
import java.util.Date

data class Message(val sender: MessageSender, val text: String, val createdAt: Date,
                   var hasAnimated: Boolean = false): Serializable