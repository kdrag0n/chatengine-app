package com.kdragon.android.chatengine.models

import java.io.Serializable

data class MessageList(val messages: MutableList<Message>) : Serializable