package com.khronodragon.android.chatengine

import android.content.pm.ActivityInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import com.beust.klaxon.Klaxon
import com.google.firebase.analytics.FirebaseAnalytics
import com.khronodragon.android.chatengine.models.*
import com.khronodragon.android.utils.ImageUtils
import com.khronodragon.android.utils.TimeUtils
import kotlinx.android.synthetic.main.chat_view.*
import kotlinx.android.synthetic.main.message_received.text_message_body as receivedMessageText
import kotlinx.android.synthetic.main.message_sent.text_message_body as sentMessageText
import okhttp3.*
import java.io.IOException
import java.util.*

internal const val tag = "CEApp"

class ChatActivity : AppCompatActivity() {
    private val messageList = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageListAdapter
    private val httpClient = OkHttpClient.Builder()
            .build()
    private val klaxon = Klaxon()
    private lateinit var analytics: FirebaseAnalytics
    private var sessionID = genSessionID()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TimeUtils.dateFormat = DateFormat.getTimeFormat(applicationContext)
        ImageUtils.resources = resources

        setContentView(R.layout.chat_view)

        messageAdapter = MessageListAdapter(applicationContext, messageList)
        messageRecycler.layoutManager = LinearLayoutManager(this)
        messageRecycler.adapter = messageAdapter

        chatboxSendButton.isEnabled = false
        chatboxSendButton.setOnClickListener {
            if (chatboxText.text.isBlank() || chatboxText.text.length > 100) return@setOnClickListener

            val message = chatboxText.text.toString().trim()
            messageList.new(MessageSender.USER, message)
            sendMessage(message)
            chatboxText.text.clear()
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

        if (resources.getBoolean(R.bool.isPhone)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }

        try {
            analytics = FirebaseAnalytics.getInstance(this)
            analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
        } catch (e: Exception) {}
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
                        // TODO: warning icon
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
                                302 -> R.string.outdated
                                307 -> R.string.outdated
                                400 -> R.string.outdated
                                401 -> R.string.outdated_corrupt
                                404 -> R.string.outdated
                                405 -> R.string.outdated
                                410 -> R.string.service_gone
                                413 -> R.string.too_long
                                429 -> R.string.too_fast
                                500 -> R.string.server_error
                                501 -> R.string.no_mod
                                502 -> R.string.server_down
                                503 -> R.string.server_down
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

        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "send_message")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Send Message")
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "text")
        analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
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

        runOnUiThread({
            messageAdapter.notifyItemInserted(size - 1)
            messageRecycler.scrollToPosition(size - 1)
        })
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState!!.putString("session", sessionID)
        outState.putSerializable("messages", MessageList(messageList))
    }

    override fun onRestoreInstanceState(savedState: Bundle?) {
        super.onRestoreInstanceState(savedState)

        sessionID = savedState!!.getString("session")

        val serializedList = savedState.getSerializable("messages") as MessageList
        messageList.addAll(serializedList.messages)
        messageRecycler.scrollToPosition(messageList.size - 1)
    }

    companion object {
        private val jsonType = MediaType.parse("application/json; charset=utf-8")
        private const val sessionPrefix = "andyOfcA1_"

        init {
            System.loadLibrary("chatauth")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}
