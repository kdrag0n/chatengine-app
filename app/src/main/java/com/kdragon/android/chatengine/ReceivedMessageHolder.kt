package com.kdragon.android.chatengine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.format.DateFormat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.kdragon.android.chatengine.models.Message
import com.kdragon.android.utils.displayRoundImage
import com.kdragon.android.utils.formatAsTime
import ru.noties.markwon.Markwon

class ReceivedMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val messageText: TextView = itemView.findViewById(R.id.text_message_body)
    private val timeText: TextView = itemView.findViewById(R.id.text_message_time)
    private val profileImage: ImageView = itemView.findViewById(R.id.image_message_profile)
    lateinit var message: Message

    fun bind(message: Message) {
        Markwon.setMarkdown(messageText, message.text)
        timeText.text = message.createdAt.formatAsTime(DateFormat.getTimeFormat(itemView.context))
        this.message = message

        messageText.setOnLongClickListener {
            val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Message Text", messageText.text)

            clipboard.primaryClip = clip
            Toast.makeText(itemView.context, "Text copied", Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }

        profileImage.displayRoundImage("https://chatengine.xyz/static/img/avatar.jpg")
    }
}