package com.kdrag0n.chathive

import android.arch.persistence.room.Room
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import com.kdrag0n.chathive.database.AppDatabase
import com.kdrag0n.chathive.models.Message
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
import kotlin.concurrent.thread
import kotlinx.android.synthetic.main.message_received.text_message_body as receivedMessageText
import kotlinx.android.synthetic.main.message_sent.text_message_body as sentMessageText

class MainActivity : AppCompatActivity() {
    private val messageList = LinkedList<Message>()
    private val dbWriterLock = java.lang.Object()
    private lateinit var messageAdapter: MessageListAdapter
    private lateinit var db: AppDatabase
    private lateinit var prefs: SharedPreferences
    private var recyclerAtBottom = true
    private var dbPosWritten = 0
    private var dbWriterTask: Thread? = null
    private val httpClient by lazy {
        OkHttpClient.Builder().build()
    }
    private var sessionID = genSessionID()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar_main as Toolbar?)

        messageAdapter = MessageListAdapter(applicationContext, messageList)

        messageRecycler.layoutManager = LinearLayoutManager(this)
        messageRecycler.adapter = messageAdapter

        messageRecycler.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (recyclerAtBottom) {
                messageRecycler.scrollToPosition(messageAdapter.itemCount - 1)
            }
        }
        messageRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                recyclerAtBottom = !recyclerView.canScrollVertically(1)
            }
        })

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
            db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "chat").build()
            prefs = getSharedPreferences("chathive", Context.MODE_PRIVATE)

            if (prefs.getBoolean("saveHistory", true)) {
                readDbHistory()

                runOnUiThread {
                    messageAdapter.notifyDataSetChanged()
                    messageRecycler.scrollToPosition(messageAdapter.itemCount - 1)
                }
            }

            if (messageList.isEmpty()) {
                messageList.new(MessageSender.BOT, greetings.random())
            }

            if (prefs.getBoolean("saveHistory", true)) {
                // start the writer task
                dbWriterTask = dbWriter()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized && prefs.getBoolean("saveHistory", true)) {
            // start the writer task
            dbWriterTask = dbWriter()
        }
    }

    override fun onPause() {
        super.onPause()
        stopWriter()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWriter() // if sometimes onPause doesn't get called...?
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
                    dbPosWritten = 0
                    clearDbHistory()
                    messageList.new(MessageSender.BOT, greetings.random())
                }

                show()
            }
            R.id.aboutOpt -> showAboutActivity()
            R.id.contactOpt -> contactDev()
            R.id.historyOpt -> {
                item.isChecked = !item.isChecked

                prefs.edit().putBoolean("saveHistory", item.isChecked).apply()
                if (item.isChecked) { // turn on
                    dbWriterTask = dbWriter()
                    writeDbHistory()
                } else { // turn off
                    clearDbHistory()
                    stopWriter()
                }
            }
        }

        return true
    }

    private fun readDbHistory() {
        for (message in db.messageDao().loadAllMessages()) {
            message.hasAnimated = true
            messageList.add(message)
        }
    }

    private fun clearDbHistory() {
        asyncExec {
            db.clearAllTables()
        }
    }

    private fun writeDbHistory() {
        asyncExec {
            synchronized(dbWriterLock) {
                dbWriterLock.notify()
            }
        }
    }

    private fun stopWriter() {
        if (dbWriterTask != null && dbWriterTask!!.isAlive) {
            dbWriterTask!!.interrupt()
        }

        dbWriterTask = null
    }

    private fun dbWriter(): Thread {
        return thread(isDaemon = true) {
            var loop = true

            while (loop) {
                try {
                    synchronized(dbWriterLock) {
                        dbWriterLock.wait()
                    }
                } catch (_: InterruptedException) {
                    // do our last write, then exit
                    loop = false
                }

                val end = messageList.size - 1
                if (end > dbPosWritten) {
                    db.messageDao().insertMessages(messageList.slice(dbPosWritten..end))
                    dbPosWritten = end + 1
                }
            }
        }
    }

    internal fun contactDev(extra: String = "", ctx: Context = this) {
        val addr = getString(R.string.contact_mail).replace(" (at) ", "@")

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("mailto:$addr$extra")))
        } catch (e: ActivityNotFoundException) {
            errorDialog(getString(R.string.error_mailto_handler, addr), ctx = ctx)
        }
    }

    private fun errorDialog(message: String, ctx: Context = this) {
        runOnUiThread {
            with (android.app.AlertDialog.Builder(ctx, R.style.DialogTheme)) {
                setTitle(R.string.error_generic)
                setMessage(message)
                setPositiveButton(android.R.string.ok) { _, _ -> }
                setCancelable(false)
                show()
            }
        }
    }

    private fun showAboutActivity() {
        AboutActivity.mainParent = this

        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
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

    private fun LinkedList<Message>.new(sender: MessageSender, message: String, time: Date = Date()) {
        add(Message(sender = sender, text = message, createdAt = time))

        runOnUiThread {
            messageAdapter.notifyDataSetChanged()
            if (recyclerAtBottom) {
                messageRecycler.scrollToPosition(messageAdapter.itemCount - 1)
            }
        }

        // save history
        if (prefs.getBoolean("saveHistory", true)) {
            writeDbHistory()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putString("session", sessionID)
    }

    override fun onRestoreInstanceState(savedState: Bundle?) {
        super.onRestoreInstanceState(savedState)

        sessionID = savedState?.getString("session") ?: genSessionID()

        asyncExec {
            readDbHistory()

            runOnUiThread {
                messageAdapter.notifyDataSetChanged()
                messageRecycler.scrollToPosition(messageAdapter.itemCount - 1)
            }
        }
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
