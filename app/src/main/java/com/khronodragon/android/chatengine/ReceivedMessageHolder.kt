package com.khronodragon.android.chatengine

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.khronodragon.android.chatengine.models.Message
import com.khronodragon.android.utils.displayRoundImage
import com.khronodragon.android.utils.formatAsTime

class ReceivedMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val messageText: TextView = itemView.findViewById(R.id.text_message_body)
    private val timeText: TextView = itemView.findViewById(R.id.text_message_time)
    private val profileImage: ImageView = itemView.findViewById(R.id.image_message_profile)

    fun bind(message: Message) {
        messageText.text = message.text
        timeText.text = message.createdAt.formatAsTime()

        profileImage.displayRoundImage("https://chatengine.xyz/static/img/avatar.jpg")
    }
}