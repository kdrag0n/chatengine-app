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
import android.widget.Toast
import com.beust.klaxon.Klaxon
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.khronodragon.android.chatengine.models.*
import com.khronodragon.android.utils.ImageUtils
import com.khronodragon.android.utils.TimeUtils
import com.khronodragon.android.utils.asyncExec
import com.khronodragon.android.utils.random
import kotlinx.android.synthetic.main.chat_view.*
import kotlinx.android.synthetic.main.message_received.text_message_body as receivedMessageText
import kotlinx.android.synthetic.main.message_sent.text_message_body as sentMessageText
import okhttp3.*
import java.io.IOException
import java.util.*
import kotlin.concurrent.thread

internal const val tag = "CEApp"

class ChatActivity : AppCompatActivity(), RewardedVideoAdListener {
    private val messageList = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageListAdapter
    private val httpClient = OkHttpClient.Builder()
            .build()
    private val klaxon = Klaxon()
    private var sessionID = genSessionID()
    private lateinit var videoAd: RewardedVideoAd

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
            chatboxText.text.clear()

            thread {
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

        if (resources.getBoolean(R.bool.isPhone)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }

        if (savedInstanceState?.isEmpty != false) {
            messageList.new(MessageSender.BOT, greetings.random())
        }

        asyncExec {
            httpClient.newCall(Request.Builder()
                    .url("https://chatengine.xyz/api/ask")
                    .post(RequestBody.create(jsonType, "{}"))
                    .build())
                    .enqueue(object : Callback {
                        override fun onFailure(call: Call?, e: IOException?) {}
                        override fun onResponse(call: Call?, response: Response?) {}
                    })
        }

        MobileAds.initialize(this, "ca-app-pub-9177446456735791~6980584126")
        videoAd = MobileAds.getRewardedVideoAdInstance(this)
        videoAd.rewardedVideoAdListener = this
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
            messageAdapter.notifyDataSetChanged()
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

    private fun loadVideoAd() {
        videoAd.loadAd("ca-app-pub-3940256099942544/5224354917", AdRequest.Builder().build())
    }

    override fun onRewardedVideoAdLoaded() {
        if (videoAd.isLoaded) {
            videoAd.show()
        }
    }

    override fun onPause() {
        super.onPause()
        videoAd.pause(this)
    }

    override fun onResume() {
        super.onResume()
        videoAd.resume(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        videoAd.destroy(this)
    }

    override fun onRewardedVideoAdFailedToLoad(p0: Int) {
        Toast.makeText(this, "Ad failed to load", Toast.LENGTH_SHORT).show()
    }

    override fun onRewarded(p0: RewardItem?) {
        Toast.makeText(this, "Thanks for watching an ad!", Toast.LENGTH_SHORT).show()
    }

    override fun onRewardedVideoAdClosed() {}
    override fun onRewardedVideoAdLeftApplication() {}
    override fun onRewardedVideoAdOpened() {}
    override fun onRewardedVideoStarted() {}

    companion object {
        private const val sessionPrefix = "andyOfcA1_"
        private val jsonType = MediaType.parse("application/json; charset=utf-8")
        private val greetings = listOf(
                "Hey!",
                "Hey there!",
                "Heya!",
                "I'm kind of busy right now, but hi.",
                "Greetings, partner.",
                "Isn't it a beautiful day?",
                "Hello!",
                "Hello, friend.",
                "Hello, human.")

        init {
            System.loadLibrary("chatauth")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}
