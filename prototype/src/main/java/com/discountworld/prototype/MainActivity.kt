package com.discountworld.prototype

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.discountworld.easypaisasdk.activities.DiscoveryActivity
import com.discountworld.dwapp.activities.MainActivity as DwMainActivity

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class MainActivity : AppCompatActivity() {
    companion object {
        // Raw API Key (converted to Base64 dynamically)
        private const val API_KEY = "bbf4304e-8ffa-448b-bc72-35f333072ac8"
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun getBase64ApiKey(): String {
        val originalBytes = API_KEY.toByteArray(Charsets.UTF_8)
        return Base64.encode(originalBytes)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etProtoCnic = findViewById<EditText>(R.id.etProtoCnic)

        fun formatCnicText(raw: String): String {
            val digits = raw.filter { it.isDigit() }.take(13)
            val sb = StringBuilder()
            for (i in digits.indices) {
                sb.append(digits[i])
                if ((i == 4 || i == 11) && i != digits.lastIndex) {
                    sb.append('-')
                }
            }
            return sb.toString()
        }

        etProtoCnic?.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating || s == null) return

                val currentStr = s.toString()
                val formatted = formatCnicText(currentStr)

                if (formatted != currentStr) {
                    isUpdating = true
                    s.replace(0, s.length, formatted)
                    try {
                        etProtoCnic.setSelection(formatted.length)
                    } catch (_: Exception) { }
                    isUpdating = false
                }
            }
        })

        findViewById<Button>(R.id.btnDwApp)?.setOnClickListener {
            val rawInput = etProtoCnic?.text?.toString()?.trim() ?: ""
            if (rawInput.isEmpty()) {
                android.widget.Toast.makeText(this, "Please enter CNIC", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val cnic = rawInput

            val intent = packageManager.getLaunchIntentForPackage("com.discountworld.dwapp")
                ?: Intent(this, DwMainActivity::class.java)

            intent.putExtra("cnic", cnic)
            intent.putExtra("api_key", getBase64ApiKey())

            try {
                startActivity(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "DW App not found: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btn)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, DiscoveryActivity::class.java))
        }
    }
}
