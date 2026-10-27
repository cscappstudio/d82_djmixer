package com.audioeffect.djmixer.music.mixer

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.audioeffect.djmixer.music.mixer.core.utils.SystemUtil

class ActivityMusicGuide : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SystemUtil.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_guide)

        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btn_got_it).setOnClickListener {
            finish()
        }

        val txtTitle = findViewById<TextView>(R.id.txt_guide_title)
        val titleHtml = "How to <font color='#38BDF8'>add Music</font> to Your Phone"
        txtTitle.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(titleHtml, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(titleHtml)
        }

        val txtCard1 = findViewById<TextView>(R.id.txt_card1_desc)
        val card1Html = "Open a music app or website and download songs in <font color='#38BDF8'>.mp3</font> or <font color='#38BDF8'>.wav</font> format."
        txtCard1.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(card1Html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(card1Html)
        }

        val txtCard2 = findViewById<TextView>(R.id.txt_card2_desc)
        val card2Html = "Connect your phone to a computer, then copy music files to the <font color='#A855F7'>Music</font> or <font color='#A855F7'>Downloads</font> folder."
        txtCard2.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(card2Html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(card2Html)
        }
    }
}
