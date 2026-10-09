package com.example.chatbot

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.sqrt

class Entry(var q: String, var a: String)

class Brain(private val file: File) {
    val pairs = mutableListOf<Entry>()
    val said = mutableListOf<String>()

    init { load() }

    fun findBest(msg: String): Pair<Int?, Double> {
        if (pairs.isEmpty()) return Pair(null, 0.0)
        val g = grams(msg)
        val scores = pairs.map { cosine(g, grams(it.q)) }
        val top = scores.max()
        val close = scores.indices.filter { scores[it] >= top - 0.05 }
        return Pair(close.random(), top)
    }

    fun babble(sentences: List<String>, maxWords: Int = 10): String? {
        if (sentences.size < 3) return null
        val chain = HashMap<String, MutableList<String>>()
        for (s in sentences) {
            val words = listOf("<S>") + s.split(" ").filter { it.isNotEmpty() } + "<E>"
            for (i in 0 until words.size - 1)
                chain.getOrPut(words[i]) { mutableListOf() }.add(words[i + 1])
        }
        var word = "<S>"
        val out = mutableListOf<String>()
        for (k in 0 until maxWords) {
            word = chain[word]?.random() ?: break
            if (word == "<E>") break
            out.add(word)
        }
        return out.joinToString(" ").ifEmpty { null }
    }

    fun save() {
        val o = JSONObject()
        o.put("pairs", JSONArray().apply {
            pairs.forEach { put(JSONObject().put("q", it.q).put("a", it.a)) }
        })
        o.put("said", JSONArray(said))
        file.writeText(o.toString(2))
    }

    private fun load() {
        if (!file.exists()) return
        val o = JSONObject(file.readText())
        val arr = o.getJSONArray("pairs")
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            pairs.add(Entry(p.getString("q"), p.getString("a")))
        }
        val s = o.getJSONArray("said")
        for (i in 0 until s.length()) said.add(s.getString(i))
    }

    // ---------- معالجة النص ----------
    private fun normalize(text: String): String {
        var t = text.lowercase().trim()
        t = t.replace(Regex("[\u064B-\u0652\u0640]"), "")
        t = t.replace(Regex("[إأآ]"), "ا")
        t = t.replace("ى", "ي").replace("ة", "ه")
        t = t.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        return t.replace(Regex("\\s+"), " ").trim()
    }

    private fun grams(text: String, n: Int = 3): Map<String, Int> {
        val t = " ${normalize(text)} "
        val m = HashMap<String, Int>()
        for (i in 0..maxOf(t.length - n, 0)) {
            val g = t.substring(i, minOf(i + n, t.length))
            m[g] = (m[g] ?: 0) + 1
        }
        return m
    }

    private fun cosine(a: Map<String, Int>, b: Map<String, Int>): Double {
        var num = 0.0
        for ((g, v) in a) b[g]?.let { num += v.toDouble() * it }
        val den = sqrt(a.values.sumOf { it.toDouble() * it }) *
                  sqrt(b.values.sumOf { it.toDouble() * it })
        return if (den == 0.0) 0.0 else num / den
    }
}
