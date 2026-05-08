package com.hermes.agent.agent

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

// ══════════════════════════════════════════════════════════════
//  HermesAgent — AI Brain
//  Claude API को use करता है Hindi commands समझने के लिए
// ══════════════════════════════════════════════════════════════

class HermesAgent(private val context: Context) {

    companion object {
        private const val TAG = "HermesAgent"
        private const val CLAUDE_API_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-opus-4-5"
    }

    private val client = OkHttpClient.Builder().build()
    private val gson = Gson()
    private val phoneController = PhoneController(context)
    private val rootController = RootController(context)

    // System Prompt — Agent की personality और capabilities
    private val systemPrompt = """
        तुम "Hermes" हो — एक powerful Hindi AI Agent जो Android phone को control करता है।
        
        तुम्हारी capabilities:
        1. CALL — किसी को call करना: call_contact(name/number)
        2. SMS — message भेजना: send_sms(number, message)
        3. WIFI — WiFi on/off: toggle_wifi(on/off)  
        4. BLUETOOTH — BT on/off: toggle_bluetooth(on/off)
        5. FLASHLIGHT — torch on/off: toggle_flashlight(on/off)
        6. VOLUME — volume control: set_volume(0-100)
        7. SCREENSHOT — screenshot लेना: take_screenshot()
        8. APP_OPEN — app खोलना: open_app(package_name)
        9. SHELL — root command चलाना: run_shell(command)
        10. BRIGHTNESS — brightness set: set_brightness(0-255)
        11. ALARM — alarm set करना: set_alarm(hour, minute, label)
        12. NOTIFICATION_READ — notifications पढ़ना: read_notifications()
        
        Response format (हमेशा JSON में):
        {
          "reply": "user को Hindi में क्या बोलना है",
          "action": "action_name या null",
          "params": {"key": "value"} या {}
        }
        
        Rules:
        - हमेशा Hindi में बात करो
        - Action clear और precise होना चाहिए
        - अगर कुछ समझ नहीं आया तो clarification मांगो
        - Dangerous commands के लिए confirm करो
    """.trimIndent()

    // ──────────────────────────────────────────────
    //  Main entry: user command process करो
    // ──────────────────────────────────────────────
    suspend fun processCommand(userInput: String): AgentResponse {
        return withContext(Dispatchers.IO) {
            try {
                val aiResponse = callClaudeAPI(userInput)
                executeAction(aiResponse)
                aiResponse
            } catch (e: Exception) {
                Log.e(TAG, "Error processing command", e)
                AgentResponse(
                    reply = "माफ करना, कुछ error आ गई: ${e.message}",
                    action = null,
                    params = emptyMap()
                )
            }
        }
    }

    // ──────────────────────────────────────────────
    //  Claude API Call
    // ──────────────────────────────────────────────
    private suspend fun callClaudeAPI(userInput: String): AgentResponse {
        val requestBody = ClaudeRequest(
            model = MODEL,
            maxTokens = 1024,
            system = systemPrompt,
            messages = listOf(
                ClaudeMessage(role = "user", content = userInput)
            )
        )

        val json = gson.toJson(requestBody)
        val body = json.toRequestBody("application/json".toMediaType())

        // API Key — Settings से लेना होगा
        val apiKey = SettingsManager.getApiKey(context)

        val request = Request.Builder()
            .url(CLAUDE_API_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response")

        if (!response.isSuccessful) {
            throw IOException("API Error ${response.code}: $responseBody")
        }

        val claudeResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)
        val textContent = claudeResponse.content.firstOrNull { it.type == "text" }?.text
            ?: throw IOException("No text response from API")

        // JSON parse करो
        return try {
            // Clean JSON (backticks हटाओ अगर हों)
            val cleanJson = textContent
                .replace("```json", "")
                .replace("```", "")
                .trim()
            gson.fromJson(cleanJson, AgentResponse::class.java)
        } catch (e: Exception) {
            // अगर JSON parse न हो तो plain text response
            AgentResponse(reply = textContent, action = null, params = emptyMap())
        }
    }

    // ──────────────────────────────────────────────
    //  Action Execute करो
    // ──────────────────────────────────────────────
    private fun executeAction(response: AgentResponse) {
        when (response.action) {
            "call_contact" -> {
                val number = response.params["number"] ?: response.params["name"] ?: return
                phoneController.makeCall(number)
            }
            "send_sms" -> {
                val number = response.params["number"] ?: return
                val message = response.params["message"] ?: return
                phoneController.sendSMS(number, message)
            }
            "toggle_wifi" -> {
                val state = response.params["state"] ?: "toggle"
                rootController.toggleWifi(state == "on")
            }
            "toggle_bluetooth" -> {
                val state = response.params["state"] ?: "toggle"
                rootController.toggleBluetooth(state == "on")
            }
            "toggle_flashlight" -> {
                val state = response.params["state"] ?: "on"
                phoneController.toggleFlashlight(state == "on")
            }
            "set_volume" -> {
                val level = response.params["level"]?.toIntOrNull() ?: 50
                phoneController.setVolume(level)
            }
            "take_screenshot" -> {
                rootController.takeScreenshot()
            }
            "open_app" -> {
                val pkg = response.params["package"] ?: return
                phoneController.openApp(pkg)
            }
            "run_shell" -> {
                val cmd = response.params["command"] ?: return
                rootController.runShellCommand(cmd)
            }
            "set_brightness" -> {
                val level = response.params["level"]?.toIntOrNull() ?: 128
                rootController.setBrightness(level)
            }
            "set_alarm" -> {
                val hour = response.params["hour"]?.toIntOrNull() ?: return
                val minute = response.params["minute"]?.toIntOrNull() ?: 0
                val label = response.params["label"] ?: "Hermes Alarm"
                phoneController.setAlarm(hour, minute, label)
            }
            "read_notifications" -> {
                phoneController.readNotifications()
            }
            null -> {
                // सिर्फ conversation, कोई action नहीं
                Log.d(TAG, "No action, just chat response")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  Data Classes
// ══════════════════════════════════════════════════════════════

data class AgentResponse(
    val reply: String,
    val action: String?,
    val params: Map<String, String>
)

data class ClaudeRequest(
    val model: String,
    @SerializedName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<ClaudeMessage>
)

data class ClaudeMessage(
    val role: String,
    val content: String
)

data class ClaudeResponse(
    val content: List<ContentBlock>
)

data class ContentBlock(
    val type: String,
    val text: String?
)
