package com.hermes.agent.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hermes.agent.R
import com.hermes.agent.agent.HermesAgent
import com.hermes.agent.agent.SettingsManager
import com.hermes.agent.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.Locale

// ══════════════════════════════════════════════════════════════
//  MainActivity — Chat Interface + Voice Input
// ══════════════════════════════════════════════════════════════

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var hermesAgent: HermesAgent
    private lateinit var chatAdapter: ChatAdapter
    private var tts: TextToSpeech? = null
    private var isListening = false

    companion object {
        private const val TAG = "MainActivity"
        private const val SPEECH_REQUEST = 100
    }

    // ──────────────────────────────────────────────
    //  Permissions Request
    // ──────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "✅ सभी permissions मिल गईं!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "⚠️ कुछ permissions नहीं मिलीं", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // API Key check
        if (SettingsManager.getApiKey(this).isEmpty()) {
            showApiKeyDialog()
        }

        setupUI()
        setupAgent()
        setupTTS()
        requestPermissions()
        showWelcomeMessage()
    }

    private fun setupUI() {
        // RecyclerView setup
        chatAdapter = ChatAdapter()
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).also {
                it.stackFromEnd = true
            }
            adapter = chatAdapter
        }

        // Send button
        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.etInput.text?.clear()
            }
        }

        // Voice button
        binding.btnVoice.setOnClickListener {
            startVoiceInput()
        }

        // Settings button
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Enter key से send
        binding.etInput.setOnEditorActionListener { _, _, _ ->
            binding.btnSend.performClick()
            true
        }
    }

    private fun setupAgent() {
        hermesAgent = HermesAgent(this)
    }

    private fun setupTTS() {
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN") // Hindi TTS
            Log.d(TAG, "TTS initialized")
        }
    }

    // ──────────────────────────────────────────────
    //  Message भेजो
    // ──────────────────────────────────────────────
    private fun sendMessage(text: String) {
        // User message add करो
        chatAdapter.addMessage(ChatMessage(text, ChatMessage.TYPE_USER))
        scrollToBottom()

        // Loading show करो
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSend.isEnabled = false
        binding.btnVoice.isEnabled = false

        // Agent process करे
        lifecycleScope.launch {
            try {
                val response = hermesAgent.processCommand(text)

                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSend.isEnabled = true
                    binding.btnVoice.isEnabled = true

                    // Agent response add करो
                    chatAdapter.addMessage(ChatMessage(response.reply, ChatMessage.TYPE_AGENT))
                    scrollToBottom()

                    // TTS से बोलो
                    speakResponse(response.reply)

                    // Action notification
                    response.action?.let { action ->
                        val actionMsg = "⚙️ Action: $action"
                        chatAdapter.addMessage(ChatMessage(actionMsg, ChatMessage.TYPE_SYSTEM))
                        scrollToBottom()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSend.isEnabled = true
                    binding.btnVoice.isEnabled = true
                    chatAdapter.addMessage(
                        ChatMessage("❌ Error: ${e.message}", ChatMessage.TYPE_SYSTEM)
                    )
                    scrollToBottom()
                }
            }
        }
    }

    // ──────────────────────────────────────────────
    //  🎤 Voice Input
    // ──────────────────────────────────────────────
    private fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")  // Hindi
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "हिंदी में बोलिए...")
        }

        try {
            startActivityForResult(intent, SPEECH_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated but needed for speech result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.firstOrNull()?.let { spokenText ->
                binding.etInput.setText(spokenText)
                sendMessage(spokenText)
                binding.etInput.text?.clear()
            }
        }
    }

    // ──────────────────────────────────────────────
    //  🔊 TTS Response
    // ──────────────────────────────────────────────
    private fun speakResponse(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hermes_response")
    }

    // ──────────────────────────────────────────────
    //  Welcome Message
    // ──────────────────────────────────────────────
    private fun showWelcomeMessage() {
        val welcome = "नमस्ते! मैं Hermes हूं 🤖\n\nमैं आपका AI Phone Assistant हूं। आप मुझसे हिंदी में बात कर सकते हैं।\n\nकुछ examples:\n• \"WiFi बंद करो\"\n• \"Ankit को call करो\"\n• \"Volume 50% करो\"\n• \"Screenshot लो\"\n• \"सुबह 7 बजे alarm लगाओ\""
        chatAdapter.addMessage(ChatMessage(welcome, ChatMessage.TYPE_AGENT))
    }

    private fun showApiKeyDialog() {
        startActivity(Intent(this, SettingsActivity::class.java))
        Toast.makeText(this, "पहले API Key set करें", Toast.LENGTH_LONG).show()
    }

    private fun scrollToBottom() {
        binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }

    private fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        permissionLauncher.launch(perms)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
