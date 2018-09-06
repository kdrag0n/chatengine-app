package com.kdragon.android.chatengine

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        about_version.summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        about_version.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                startActivity(intent)
            }
        }

        about_author.summary = R.string.author_nick.string()
        about_author.uri = R.string.website_uri

        about_telegram.removeSummary()
        about_telegram.uri = R.string.telegram_uri

        about_email.removeSummary()
        about_email.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + R.string.contact_mail.string().replace(" (at) ", "@"))))
        }
    }

    private fun Int.string(vararg fmt: Any): String {
        return resources.getString(this, *fmt)
    }
}
