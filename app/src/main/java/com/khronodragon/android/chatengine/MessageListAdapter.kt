package com.khronodragon.android.chatengine

import android.content.Context
import com.khronodragon.android.chatengine.models.Message
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import com.khronodragon.android.chatengine.models.MessageSender

class MessageListAdapter(private val context: Context, private val messages: List<Message>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var lastPosition: Int = -1

    override fun getItemCount() = messages.size
    override fun getItemViewType(position: Int) = messages[position].sender.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            MessageSender.BOT.ordinal ->
                ReceivedMessageHolder(inflater.inflate(R.layout.message_received, parent, false))

            MessageSender.USER.ordinal ->
                SentMessageHolder(inflater.inflate(R.layout.message_sent, parent, false))

            else -> throw IllegalArgumentException("Invalid message sender")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is ReceivedMessageHolder -> holder.bind(message)
            is SentMessageHolder -> holder.bind(message)
        }

        animate(holder.itemView, holder, position)
    }

    private fun animate(view: View, holder: RecyclerView.ViewHolder, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(context, when (holder) {
                is ReceivedMessageHolder -> android.R.anim.slide_in_left
                is SentMessageHolder -> android.R.anim.fade_in
                else -> error("Invalid ViewHolder")
            })
            view.startAnimation(animation)
            lastPosition = position
        }
    }
}