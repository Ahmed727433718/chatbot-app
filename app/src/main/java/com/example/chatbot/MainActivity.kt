package com.example.chatbot

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {
    private val history = mutableListOf<Pair<String, String>>()
    private var pending = false
    private lateinit var chat: TextView
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var send: Button

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        Api.apiKey = prefs.getString("key", "") ?: ""

        chat = TextView(this).apply {
            textSize = 16f
            setPadding(24, 24, 24, 24)
            setTextIsSelectable(true)
        }
        scroll = ScrollView(this).apply { addView(chat) }
        input = EditText(this).apply { hint = "اكتب رسالتك..." }
        send = Button(this).apply { text = "إرسال" }
        send.setOnClickListener { onSend() }

        val newChat = Button(this).apply { text = "محادثة جديدة" }
        newChat.setOnClickListener {
            if (!pending) {
                history.clear()
                render()
            }
        }

        val copy = Button(this).apply { text = "نسخ آخر رد" }
        copy.setOnClickListener {
            val last = history.lastOrNull { it.first == "bot" }?.second
            if (last != null) {
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("reply", last))
                Toast.makeText(this, "تم النسخ", Toast.LENGTH_SHORT).show()
            }
        }

        val keyBtn = Button(this).apply { text = "المفتاح" }
        keyBtn.setOnClickListener {
            val field = EditText(this).apply { hint = "الصق المفتاح هنا" }
            AlertDialog.Builder(this)
                .setTitle("مفتاح Gemini")
                .setView(field)
                .setPositiveButton("حفظ") { _, _ ->
                    val k = field.text.toString().trim()
                    prefs.edit().putString("key", k).apply()
                    Api.apiKey = k
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(newChat)
            addView(copy)
            addView(keyBtn)
        }
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(send)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            addView(top)
            addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(bottom)
        }
        setContentView(root)
        render()
    }

    private fun onSend() {
        val msg = input.text.toString().trim()
        if (msg.isEmpty() || pending) return
        if (Api.apiKey.isEmpty()) {
            Toast.makeText(this, "اضغط زر المفتاح وأدخل مفتاح Gemini أولاً", Toast.LENGTH_LONG).show()
            return
        }
        input.setText("")

        history.add("user" to msg)
        pending = true
        send.isEnabled = false
        render()

        val snapshot = history.takeLast(20).dropWhile { it.first != "user" }.toList()

        Thread {
            val answer = try {
                Api.ask(snapshot)
            } catch (e: Exception) {
                "صار خطأ: ${e.message}"
            }
            runOnUiThread {
                history.add("bot" to answer)
                pending = false
                send.isEnabled = true
                render()
            }
        }.start()
    }

    private fun render() {
        val sb = StringBuilder()
        if (history.isEmpty()) {
            sb.append("بوت: أهلين! اضغط زر المفتاح أولاً، وبعدين اكتب رسالتك.\n\n")
        }
        for ((role, text) in history) {
            sb.append(if (role == "user") "أنت: " else "بوت: ")
            sb.append(text).append("\n\n")
        }
        if (pending) sb.append("بوت: يفكّر...")
        chat.text = sb.toString()
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }
}
