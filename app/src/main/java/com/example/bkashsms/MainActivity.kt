package com.example.bkashsms

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("cfg", Context.MODE_PRIVATE)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        fun label(t: String) = TextView(this).apply { text = t; textSize = 14f; setPadding(0, pad, 0, 0) }

        val url = EditText(this).apply {
            hint = "http://YOUR_SERVER_IP:8080"
            setText(prefs.getString("url", ""))
            inputType = InputType.TYPE_TEXT_VARIATION_URI
        }
        val secret = EditText(this).apply {
            hint = "SMS_SECRET (bot এর .env এর সাথে হুবহু এক)"
            setText(prefs.getString("secret", ""))
        }
        val status = TextView(this).apply { textSize = 13f; setPadding(0, pad, 0, 0) }

        val save = Button(this).apply { text = "Save" }
        val test = Button(this).apply { text = "Test connection" }

        root.addView(TextView(this).apply { text = "bKash SMS → Bot"; textSize = 22f })
        root.addView(label("Bot server URL"))
        root.addView(url)
        root.addView(label("Secret"))
        root.addView(secret)
        root.addView(save)
        root.addView(test)
        root.addView(status)
        setContentView(root)

        fun refresh() {
            val ok = Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
            status.text = if (ok) "✅ SMS permission দেওয়া আছে। bKash SMS এলে নিজে নিজে bot-এ যাবে।"
            else "❌ SMS permission নেই। App info → Permissions → SMS চালু করুন।"
        }

        save.setOnClickListener {
            prefs.edit().putString("url", url.text.toString().trim()).putString("secret", secret.text.toString().trim()).apply()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }
        test.setOnClickListener {
            val base = url.text.toString().trim().trimEnd('/')
            status.text = "Testing..."
            Thread {
                val msg = try {
                    val c = URL("$base/").openConnection() as HttpURLConnection
                    c.connectTimeout = 8000; c.readTimeout = 8000
                    val r = c.responseCode
                    c.disconnect()
                    if (r == 200) "✅ Bot server পাওয়া গেছে" else "⚠️ Server answered HTTP $r"
                } catch (e: Exception) { "❌ Connect হয়নি: ${e.message}" }
                runOnUiThread { status.text = msg }
            }.start()
        }

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), 1)
        }
        refresh()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        recreate()
    }
}
