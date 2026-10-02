package com.discountworld.prototype

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.discountworld.easypaisasdk.activities.DiscoveryActivity

class MainActivity : AppCompatActivity() {
    companion object {
        // Base64 encoded API key: YTU1Mjg0YzYtZjQ3MC00ZTA3LWE2ZDMtOGQ3ZTBiNWIzYzEz
        private const val API_KEY = "YTU1Mjg0YzYtZjQ3MC00ZTA3LWE2ZDMtOGQ3ZTBiNWIzYzEz"
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

            val intent = packageManager.getLaunchIntentForPackage("com.discountworld.dwapp") ?: Intent().apply {
                setClassName("com.discountworld.dwapp", "com.discountworld.dwapp.activities.MainActivity")
            }

            intent.putExtra("unique_id", uniqueId)
            intent.putExtra("customer_tier", selectedTier)
            intent.putExtra("api_key", API_KEY)

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