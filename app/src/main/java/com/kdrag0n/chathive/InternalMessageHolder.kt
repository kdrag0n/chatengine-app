package com.kdrag0n.chathive

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.kdrag0n.chathive.models.Message
import com.kdragon.android.chatengine.R

class InternalMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val messageText: TextView = itemView.findViewById(R.id.text_message_body)
    lateinit var message: Message

    fun bind(message: Message) {
        messageText.text = message.text
        this.message = message
    }
}