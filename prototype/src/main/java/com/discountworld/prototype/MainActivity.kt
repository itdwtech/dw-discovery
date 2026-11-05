package com.discountworld.prototype

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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
        private const val API_KEY = "a55284c6-f470-4e07-a6d3-8d7e0b5b3c13"
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

        findViewById<Button>(R.id.btnDwApp)?.setOnClickListener {
            val uniqueId = findViewById<android.widget.EditText>(R.id.etProtoUniqueId)?.text?.toString()?.trim()?.ifEmpty { "123456789012" } ?: "123456789012"
            val rgTier = findViewById<android.widget.RadioGroup>(R.id.rgProtoTier)
            val selectedTier = when (rgTier?.checkedRadioButtonId) {
                R.id.rbProtoGold -> "Gold"
                R.id.rbProtoSilver -> "Silver"
                R.id.rbProtoBronze -> "Bronze"
                else -> "Gold"
            }

            val intent = packageManager.getLaunchIntentForPackage("com.discountworld.dwapp")
                ?: Intent(this, DwMainActivity::class.java)

            intent.putExtra("unique_id", uniqueId)
            intent.putExtra("customer_tier", selectedTier)
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