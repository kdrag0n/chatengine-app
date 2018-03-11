package com.khronodragon.android.chatengine.models

import java.time.Instant

data class Message(val sender: MessageSender, val text: String, val createdAt: Instant)