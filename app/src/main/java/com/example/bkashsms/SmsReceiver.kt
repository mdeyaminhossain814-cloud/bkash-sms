package com.example.bkashsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // একই sender-এর multi-part SMS জোড়া লাগানো
        val grouped = LinkedHashMap<String, StringBuilder>()
        for (p in parts) {
            val from = p.originatingAddress ?: continue
            grouped.getOrPut(from) { StringBuilder() }.append(p.messageBody ?: "")
        }
        val prefs = context.getSharedPreferences("cfg", Context.MODE_PRIVATE)
        val base = prefs.getString("url", "")?.trim()?.trimEnd('/') ?: ""
        val secret = prefs.getString("secret", "") ?: ""
        if (base.isEmpty() || secret.isEmpty()) return

        val pending = goAsync()
        Thread {
            try {
                for ((from, body) in grouped) {
                    if (!from.contains("bkash", ignoreCase = true)) continue   // শুধু bKash-এর SMS
                    post("$base/sms", from, body.toString(), secret)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun post(url: String, from: String, msg: String, secret: String) {
        val json = JSONObject().put("from", from).put("message", msg).put("secret", secret).toString()
        repeat(4) { attempt ->               // fail করলে ৪ বার চেষ্টা
            try {
                val c = URL(url).openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.connectTimeout = 10000
                c.readTimeout = 10000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(json.toByteArray()) }
                val code = c.responseCode
                c.disconnect()
                if (code in 200..299) return
            } catch (_: Exception) { }
            try { Thread.sleep(3000L * (attempt + 1)) } catch (_: InterruptedException) { return }
        }
    }
}
