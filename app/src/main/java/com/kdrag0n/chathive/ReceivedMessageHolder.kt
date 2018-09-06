package com.kdrag0n.chathive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.kdrag0n.chathive.models.Message
import com.kdrag0n.utils.formatAsTime
import com.kdragon.android.chatengine.R

class ReceivedMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val messageText: TextView = itemView.findViewById(R.id.text_message_body)
    private val timeText: TextView = itemView.findViewById(R.id.text_message_time)
    lateinit var message: Message

    fun bind(message: Message) {
        messageText.text = message.text
        timeText.text = message.createdAt.formatAsTime(DateFormat.getTimeFormat(itemView.context))
        this.message = message

        messageText.setOnLongClickListener {
            val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Message Text", messageText.text)

            clipboard.primaryClip = clip
            Toast.makeText(itemView.context, "Text copied", Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }
    }
}