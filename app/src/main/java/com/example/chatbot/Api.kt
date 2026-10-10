package com.example.chatbot

import java.net.HttpURLConnection
import java.net.URL

object Api {
    var apiKey: String = "local"

    private const val SERVER = "http://127.0.0.1:8000/solve"
    private const val KEY = "TEST_API_CHATPYPT"

    fun init(ctx: android.content.Context) {
        // لا حاجة لتحميل نموذج، السيرفر هو اللي يحسب
    }

    fun ask(history: List<Pair<String, String>>): String {
        val msg = history.lastOrNull { it.first == "user" }?.second
            ?: return "اكتب مسألة مثل 12+7="

        val conn = URL(SERVER).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("X-Key", KEY)
            conn.doOutput = true
            val body = org.json.JSONObject().put("expr", msg).toString()
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp = org.json.JSONObject(stream.bufferedReader().readText())
            if (code in 200..299) resp.getString("answer")
            else "خطأ: ${resp.optString("error", "غير معروف")}"
        } catch (e: Exception) {
            "ما قدرت أوصل للسيرفر. تأكد إن Termux شغّال والسيرفر يعمل."
        } finally {
            conn.disconnect()
        }
    }
}
