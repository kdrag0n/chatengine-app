package com.kdrag0n.chathive

import android.arch.persistence.room.Room
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import com.kdrag0n.chathive.database.AppDatabase
import com.kdrag0n.chathive.models.Message
import com.kdrag0n.chathive.models.MessageList
import com.kdrag0n.chathive.models.MessageSender
import com.kdrag0n.utils.asyncExec
import com.kdrag0n.utils.random
import com.kdragon.android.chatengine.R
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlinx.android.synthetic.main.message_received.text_message_body as receivedMessageText
import kotlinx.android.synthetic.main.message_sent.text_message_body as sentMessageText

class MainActivity : AppCompatActivity() {
    private val messageList = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageListAdapter
    private lateinit var db: AppDatabase
    private lateinit var prefs: SharedPreferences
    private val httpClient by lazy {
        OkHttpClient.Builder().build()
    }
    private var sessionID = genSessionID()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar_main as Toolbar?)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "chat").build()
        prefs = getSharedPreferences("chathive", Context.MODE_PRIVATE)

        messageAdapter = MessageListAdapter(applicationContext, messageList)

        messageRecycler.layoutManager = LinearLayoutManager(this)
        messageRecycler.adapter = messageAdapter

        messageRecycler.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                messageRecycler.scrollToPosition(messageAdapter.itemCount - 1)
            }
        }

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

        asyncExec {
            if (prefs.getBoolean("saveHistory", true)) {
                readDbHistory()
                if (messageList.size > 0) {
                    messageList[messageList.size - 1].hasAnimated = true // prevent animation of last message
                }

                runOnUiThread {
                    messageAdapter.notifyDataSetChanged()
                    messageRecycler.scrollToPosition(messageAdapter.itemCount - 1)
                }
            }

            if (messageList.isEmpty()) {
                messageList.new(MessageSender.BOT, greetings.random())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.actions, menu ?: return true)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu?.findItem(R.id.historyOpt)?.isChecked = prefs.getBoolean("saveHistory", true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.clearOpt -> with (AlertDialog.Builder(this, R.style.DialogTheme)) {
                setTitle(R.string.clear_history_confirm)
                setMessage(R.string.clear_history_confirm_desc)

                setNegativeButton(android.R.string.no) { _, _ -> }
                setPositiveButton(android.R.string.yes) { _, _ ->
                    messageList.clear()
                    clearDbHistory()
                    messageList.new(MessageSender.BOT, greetings.random())
                }

                show()
            }
            R.id.aboutOpt -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.historyOpt -> {
                item.isChecked = !item.isChecked

                prefs.edit().putBoolean("saveHistory", item.isChecked).apply()
                if (item.isChecked) { // turn on
                    writeDbHistory()
                } else { // turn off
                    clearDbHistory()
                }
            }
        }

        return true
    }

    private fun readDbHistory() {
        messageList.addAll(db.messageDao().loadAllMessages())
    }

    private fun clearDbHistory() {
        asyncExec {
            db.clearAllTables()
        }
    }

    private fun writeDbHistory() {
        asyncExec {
            db.messageDao().insertMessages(messageList)
        }
    }

    private fun sendMessage(msg: String) {
        val apiRequest = JSONObject().also {
            it.put("query", msg)
            it.put("session", sessionID)
        }

        val request = Request.Builder()
                .url("https://chatengine.xyz/api/ask")
                .post(RequestBody.create(jsonType, apiRequest.toString()))
                .auth()
                .build()

        try {
            val resp = httpClient.newCall(request)
                    .execute()

            if (!resp.isSuccessful) {
                val message = when (resp.code()) {
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

            try {
                val jsonResp = JSONObject(resp.body()?.string() ?: "{}")

                if (!jsonResp.getBoolean("success")) {
                    messageList.new(MessageSender.BOT, getString(R.string.error_api, jsonResp.getString("error")))
                    return
                }

                messageList.new(MessageSender.BOT, jsonResp.getString("response"))
            } catch (e: JSONException) {
                // invalid response - server error
                messageList.new(MessageSender.BOT, getString(R.string.error_response))
            }
        } catch (e: Exception) {
            messageList.new(MessageSender.INTERNAL, getString(R.string.error_internet))
        }
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
        add(Message(sender = sender, text = message, createdAt = time))

        runOnUiThread {
            messageAdapter.notifyDataSetChanged()
            messageRecycler.scrollToPosition(messageAdapter.itemCount - 1)
        }

        // save history
        if (prefs.getBoolean("saveHistory", true)) {
            writeDbHistory()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putString("session", sessionID)
        outState?.putSerializable("messages", MessageList(messageList))
    }

    override fun onRestoreInstanceState(savedState: Bundle?) {
        super.onRestoreInstanceState(savedState)

        sessionID = savedState?.getString("session") ?: genSessionID()

        val serializedList = savedState?.getSerializable("messages") as MessageList?
        messageList.addAll(serializedList?.messages ?: listOf())
        messageAdapter.notifyDataSetChanged()
        messageRecycler.scrollToPosition(messageAdapter.itemCount - 1)
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
