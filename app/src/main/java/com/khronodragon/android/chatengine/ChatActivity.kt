package com.khronodragon.android.chatengine

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import com.khronodragon.android.chatengine.models.Message
import com.khronodragon.android.chatengine.models.MessageSender
import com.khronodragon.android.utils.ImageUtils
import com.khronodragon.android.utils.TimeUtils
import kotlinx.android.synthetic.main.chat_view.*
import java.util.*

internal val tag = "CEApp"

class ChatActivity : AppCompatActivity() {
    private val messageList = mutableListOf<Message>()
    private val messageAdapter = MessageListAdapter(messageList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TimeUtils.dateFormat = DateFormat.getDateFormat(applicationContext)
        ImageUtils.resources = resources

        setContentView(R.layout.chat_view)

        messageRecycler.layoutManager = LinearLayoutManager(this)
        messageRecycler.adapter = messageAdapter

        chatboxSendButton.isEnabled = false
        chatboxSendButton.setOnClickListener({
            if (chatboxText.text.isEmpty()) return@setOnClickListener

            messageList.add(Message(MessageSender.USER, chatboxText.text.toString(), Date()))
            messageAdapter.notifyItemInserted(messageList.size - 1)
            chatboxText.text.clear()
        })

        chatboxText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                chatboxSendButton.isEnabled = s!!.isNotEmpty()
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Example of a call to a native method
        Log.i(tag, stringFromJNI())
    }

    external private fun stringFromJNI(): String

    companion object {
        init {
            System.loadLibrary("chatauth")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}
