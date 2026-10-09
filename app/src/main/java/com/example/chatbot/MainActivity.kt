package com.example.chatbot

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {
    private lateinit var brain: Brain
    private lateinit var chat: TextView
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        brain = Brain(File(filesDir, "memory.json"))

        chat = TextView(this).apply { textSize = 16f; setPadding(24, 24, 24, 24) }
        scroll = ScrollView(this).apply { addView(chat) }
        input = EditText(this).apply { hint = "اكتب هنا..." }
        val send = Button(this).apply { text = "إرسال" }
        send.setOnClickListener { onSend() }

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(send)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(bottom)
        }
        setContentView(root)
        bot("أهلين! اكتب أي شي وأنا برد عليك.")
    }

    private fun onSend() {
        val msg = input.text.toString().trim()
        input.setText("")
        if (msg.isEmpty()) return

        you(msg)
        bot("...")
        Thread {
            val answer = try { Api.ask(msg) } catch (e: Exception) { "صار خطأ: ${e.message}" }
            runOnUiThread {
                replaceLastBot(answer)
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            }
        }.start()
    }

    private fun you(t: String) { chat.append("أنت: $t\n\n") }
    private fun bot(t: String) { chat.append("بوت: $t\n\n") }

    // يستبدل "..." بالرد الفعلي
    private fun replaceLastBot(t: String) {
        val text = chat.text.toString()
        val idx = text.lastIndexOf("بوت: ...\n\n")
        if (idx >= 0) {
            chat.text = text.substring(0, idx) + "بوت: $t\n\n"
        } else {
            bot(t)
        }
    }
}ز
