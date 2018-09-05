package com.kdragon.android.chatengine

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import com.beust.klaxon.Klaxon
import com.kdragon.android.chatengine.models.*
import com.kdragon.android.utils.asyncExec
import com.kdragon.android.utils.random
import kotlinx.android.synthetic.main.chat_view.*
import okhttp3.*
import java.io.IOException
import java.util.*
import kotlinx.android.synthetic.main.message_received.text_message_body as receivedMessageText
import kotlinx.android.synthetic.main.message_sent.text_message_body as sentMessageText

internal const val tag = "CEApp"

class ChatActivity : AppCompatActivity() {
    private val messageList = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageListAdapter
    private val httpClient = OkHttpClient.Builder()
            .build()
    private val klaxon = Klaxon()
    private var sessionID = genSessionID()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.chat_view)

        messageAdapter = MessageListAdapter(applicationContext, messageList)
        messageRecycler.layoutManager = LinearLayoutManager(this)
        messageRecycler.adapter = messageAdapter

        chatboxSendButton.isEnabled = false
        chatboxSendButton.setOnClickListener {
            if (chatboxText.text.isBlank() || chatboxText.text.length > 100) return@setOnClickListener

            val message = chatboxText.text.toString().trim()
            chatboxText.text.clear()

            asyncExec {
                messageList.new(MessageSender.USER, message)
                sendMessage(message)
            }
        }

        chatboxText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                chatboxSendButton.isEnabled = s!!.isNotEmpty() && s.length <= 100
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        chatboxText.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEND) {
                chatboxSendButton.performClick()
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }

        chatboxText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                chatboxSendButton.performClick()
                return@setOnKeyListener true
            }

            return@setOnKeyListener false
        }

        chatboxText.requestFocus()

        if (savedInstanceState?.isEmpty != false) {
            messageList.new(MessageSender.BOT, greetings.random())
        }
    }

    private fun sendMessage(msg: String) {
        val apiRequest = APIRequest(query = msg, session = sessionID)
        val request = Request.Builder()
                .url("https://chatengine.xyz/api/ask")
                .post(RequestBody.create(jsonType, klaxon.toJsonString(apiRequest)))
                .auth()
                .build()

        httpClient.newCall(request)
                .enqueue(object : Callback {
                    override fun onFailure(call: Call?, e: IOException?) {
                        Log.e(tag, "Failed to send message", e)
                        messageList.new(MessageSender.INTERNAL, getString(R.string.error_internet))
                    }


                    override fun onResponse(call: Call?, httpResponse: Response?) {
                        if (httpResponse?.isSuccessful != true) {
                            val errorMessage = "${httpResponse?.code() ?: -1} ${httpResponse?.message() ?: "Unknown"}: ${httpResponse?.body()?.string() ?: "No response body"}"

                            if (httpResponse?.code() != 429) {
                                Log.e(tag, "Unsuccessful status code from API: $errorMessage")
                            }

                            val message = when (httpResponse?.code() ?: -1) {
                                413 -> R.string.too_long
                                429 -> R.string.too_fast
                                500 -> R.string.server_error
                                502 -> R.string.server_error
                                503 -> R.string.server_error
                                else -> R.string.error
                            }

                            messageList.new(MessageSender.INTERNAL, getString(message))
                            return
                        }

                        val response = klaxon.parse<APIResponse>(httpResponse.body()?.byteStream() ?: "{}".byteInputStream())
                        if (response?.success != true) {
                            Log.e(tag, "Unsuccessful response from API: ${response?.let {klaxon.toJsonString(it)} ?: "Invalid data"}")
                            messageList.new(MessageSender.BOT, "${getString(R.string.error)}: ${response?.error ?: "unknown"}")

                            return
                        }

                        messageList.new(MessageSender.BOT, response.response)
                    }
                })
    }

    private fun genSessionID(): String {
        return sessionPrefix +
                UUID.randomUUID().toString().replace("-", "").take(16)
    }

    private external fun authenticate(request: Request.Builder)

    private fun Request.Builder.auth(): Request.Builder {
        authenticate(this)
        return this
    }

    private fun MutableList<Message>.new(sender: MessageSender, message: String, time: Date = Date()) {
        add(Message(sender, message, time))

        runOnUiThread {
            messageAdapter.notifyDataSetChanged()
            messageRecycler.smoothScrollToPosition(size - 1)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState!!.putString("session", sessionID)
        outState.putSerializable("messages", MessageList(messageList))
    }

    override fun onRestoreInstanceState(savedState: Bundle?) {
        super.onRestoreInstanceState(savedState)

        sessionID = savedState?.getString("session") ?: genSessionID()

        val serializedList = savedState?.getSerializable("messages") as MessageList?
        messageList.addAll(serializedList?.messages ?: listOf())
        messageRecycler.scrollToPosition(messageList.size - 1)
    }

    companion object {
        private const val sessionPrefix = "droid_"
        private val jsonType = MediaType.parse("application/json; charset=utf-8")
        private val greetings = listOf(
                "Hey!",
                "Hey there!",
                "Heya!",
                "Greetings, partner.",
                "Hello!",
                "Hello, friend.",
                "Hello, human.")

        init {
            System.loadLibrary("chatauth")
        }
    }
}
