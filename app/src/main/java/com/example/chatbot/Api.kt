package com.example.chatbot

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Api {
    const val KEY = "AQ.Ab8RN6IX5hCK_0tGfxQh2oiA-kVCIX55myelVVOH9du4mKg7QQ"
    const val MODEL = "gemini-2.0-flash"

    fun ask(text: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$KEY")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        val body = JSONObject().put(
            "contents", JSONArray().put(
                JSONObject().put(
                    "parts", JSONArray().put(JSONObject().put("text", text))
                )
            )
        )
        conn.outputStream.use { it.write(body.toString().toByteArray()) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val resp = stream.bufferedReader().readText()
        if (code !in 200..299) return "خطأ $code: $resp"

        return JSONObject(resp)
            .getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts").getJSONObject(0)
            .getString("text")
    }
}
