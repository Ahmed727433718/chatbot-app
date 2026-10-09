package com.example.chatbot

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel

object Api {
    var apiKey: String = "local"

    private const val SEQ = 10
    private const val D = 2
    private const val PLUS = 10
    private const val MINUS = 11
    private const val EQ = 12
    private const val BOS = 13
    private const val VOCAB = 14

    private var interp: Interpreter? = null

    fun init(ctx: Context) {
        if (interp != null) return
        val fd = ctx.assets.openFd("math_float16.tflite")
        val buf = FileInputStream(fd.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        val ip = Interpreter(buf)
        ip.resizeInput(0, intArrayOf(1, SEQ))
        ip.allocateTensors()
        interp = ip
    }

    fun ask(history: List<Pair<String, String>>): String {
        val msg = history.lastOrNull { it.first == "user" }?.second
            ?: return "اكتب مسألة مثل 12+7="
        val m = Regex("""\s*(\d+)\s*([+\-])\s*(\d+)\s*=?\s*""").matchEntire(msg)
            ?: return "أنا بحسب الجمع والطرح بس، مثل: 12+7="
        val a = m.groupValues[1].toInt()
        val b = m.groupValues[3].toInt()
        val sub = m.groupValues[2] == "-"
        if (a > 99 || b > 99) return "الأرقام لازم تكون بين 0 و 99"
        return try {
            infer(a, b, sub)
        } catch (e: Exception) {
            "خطأ بالنموذج: ${e.message}"
        }
    }

    private fun infer(a: Int, b: Int, sub: Boolean): String {
        val ip = interp ?: return "النموذج ما اتحمّل"
        val seq = IntArray(SEQ)
        var pos = 0
        fun put(t: Int) { seq[pos++] = t }

        put(BOS)
        for (k in 0 until D) put((a / p10(k)) % 10)
        put(if (sub) MINUS else PLUS)
        for (k in 0 until D) put((b / p10(k)) % 10)
        put(EQ)

        val out = IntArray(D + 2)
        for (i in 0 until D + 2) {
            val res = Array(1) { Array(SEQ) { FloatArray(VOCAB) } }
            ip.run(arrayOf(seq), res)
            val logits = res[0][pos - 1]
            val range = if (i == 0) listOf(PLUS, MINUS) else (0..9).toList()
            var best = range[0]
            for (t in range) if (logits[t] > logits[best]) best = t
            out[i] = best
            if (pos < SEQ) seq[pos] = best
            pos++
        }

        val sign = if (out[0] == MINUS) -1 else 1
        var value = 0
        for (k in 0..D) value += out[1 + k] * p10(k)
        return (sign * value).toString()
    }

    private fun p10(k: Int): Int {
        var r = 1
        repeat(k) { r *= 10 }
        return r
    }
}
