package com.khronodragon.android.chatengine

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.khronodragon.android.chatengine.models.Message
import com.khronodragon.android.utils.formatAsTime

class InternalMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val messageText: TextView = itemView.findViewById(R.id.text_message_body)
    private val timeText: TextView = itemView.findViewById(R.id.text_message_time)
    lateinit var message: Message

    fun bind(message: Message) {
        messageText.text = message.text
        timeText.text = message.createdAt.formatAsTime()
        this.message = message
    }
}