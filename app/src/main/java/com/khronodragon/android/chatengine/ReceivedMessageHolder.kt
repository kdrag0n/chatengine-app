package com.khronodragon.android.chatengine

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.message_received.view.*

// private val?
class ReceivedMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val messageText: TextView = itemView.findViewById(R.id.text_message_body)
    val timeText: TextView = itemView.findViewById(R.id.text_message_time)
    val nameText: TextView = itemView.findViewById(R.id.text_message_name)
    val profileImage: ImageView = itemView.findViewById(R.id.image_message_profile)

    fun bind(message: Message) {
        messageText.text = message.text
        timeText.text =
    }
}