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

    private var lastUsed: Int? = null
    private var pendingTeach: String? = null
    private var fixIndex: Int? = null

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
        bot("أهلين! بعرف حاليًا ${brain.pairs.size} رد.")
    }

    private fun onSend() {
        val msg = input.text.toString().trim()
        input.setText("")

        if (msg.isEmpty()) {
            pendingTeach?.let {
                pendingTeach = null
                bot(brain.babble(brain.said.dropLast(1)) ?: "احكيلي أكثر...")
                brain.save()
            }
            return
        }
        you(msg)

        val fix = fixIndex
        val teach = pendingTeach
        when {
            fix != null -> {
                brain.pairs[fix].a = msg
                fixIndex = null
                brain.save()
                bot("تمام، صححتها وما رح أنساها. 🧠")
            }
            teach != null -> {
                brain.pairs.add(Entry(teach, msg))
                lastUsed = brain.pairs.size - 1
                pendingTeach = null
                brain.save()
                bot("تعلمت! 🧠")
            }
            msg == "/ذاكرة" -> bot("بعرف ${brain.pairs.size} رد وسمعت منك ${brain.said.size} جملة.")
            msg == "/غلط" -> {
                val i = lastUsed
                if (i == null || i >= brain.pairs.size) bot("ما في رد أصحّحه لسا.")
                else {
                    bot("لما قلتلي «${brain.pairs[i].q}» رديت «${brain.pairs[i].a}». شو كان المفروض أرد؟")
                    fixIndex = i
                }
            }
            else -> reply(msg)
        }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun reply(msg: String) {
        brain.said.add(msg)
        val (idx, score) = brain.findBest(msg)
        if (idx != null && score >= 0.55) {
            lastUsed = idx
            bot(brain.pairs[idx].a)
        } else {
            lastUsed = null
            pendingTeach = msg
            bot("ما عندي رد على هاي لسا. شو بترد لو حدا قالها؟ (اضغط إرسال فاضي للتخطي)")
        }
        brain.save()
    }

    private fun you(t: String) { chat.append("أنت: $t\n\n") }
    private fun bot(t: String) { chat.append("بوت: $t\n\n") }
}
