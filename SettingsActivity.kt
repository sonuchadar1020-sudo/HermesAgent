package com.hermes.agent.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hermes.agent.agent.SettingsManager
import com.hermes.agent.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Existing API key load करो
        binding.etApiKey.setText(SettingsManager.getApiKey(this))

        binding.btnSave.setOnClickListener {
            val apiKey = binding.etApiKey.text.toString().trim()
            if (apiKey.isEmpty()) {
                Toast.makeText(this, "API Key खाली नहीं हो सकती", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SettingsManager.saveApiKey(this, apiKey)
            Toast.makeText(this, "✅ API Key save हो गई!", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnBack.setOnClickListener { finish() }
    }
}

// ══════════════════════════════════════════════════════════════
//  SettingsManager — SharedPreferences wrapper
// ══════════════════════════════════════════════════════════════
package com.hermes.agent.agent

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "hermes_prefs"
    private const val KEY_API_KEY = "claude_api_key"

    fun saveApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_API_KEY, key).apply()
    }

    fun getApiKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_API_KEY, "") ?: ""
    }
}
