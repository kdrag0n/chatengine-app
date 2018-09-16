package com.kdrag0n.chathive

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import com.kdrag0n.chathive.models.Message
import com.kdrag0n.chathive.models.MessageSender
import com.kdragon.android.chatengine.R

class MessageListAdapter(private val context: Context, private val messages: List<Message>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var lastPosition = -1

    override fun getItemCount() = messages.size
    override fun getItemViewType(position: Int) = messages[position].sender.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            MessageSender.BOT.ordinal ->
                ReceivedMessageHolder(inflater.inflate(R.layout.message_received, parent, false))

            MessageSender.USER.ordinal ->
                SentMessageHolder(inflater.inflate(R.layout.message_sent, parent, false))

            MessageSender.INTERNAL.ordinal ->
                InternalMessageHolder(inflater.inflate(R.layout.message_internal, parent, false))

            else -> error("Invalid message sender")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is ReceivedMessageHolder -> holder.bind(message)
            is SentMessageHolder -> holder.bind(message)
            is InternalMessageHolder -> holder.bind(message)
        }

        if (!message.hasAnimated) {
            animate(holder.itemView, holder, position)
        }
    }

    private fun animate(view: View, holder: RecyclerView.ViewHolder, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)

            view.startAnimation(animation)
            lastPosition = position

            when (holder) {
                is ReceivedMessageHolder -> holder.message.hasAnimated = true
                is SentMessageHolder -> holder.message.hasAnimated = true
                is InternalMessageHolder -> holder.message.hasAnimated = true
            }
        }
    }
}