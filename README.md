# 🤖 Hermes Agent — Hindi AI Phone Controller

Claude AI से powered Hindi Phone Assistant जो आपके rooted Android phone को control करता है।

---

## ✨ Features

| Feature | Root Required | Description |
|---------|--------------|-------------|
| 📞 Call | ❌ | Contact को call करो |
| 💬 SMS | ❌ | Message भेजो |
| 🔦 Flashlight | ❌ | Torch on/off |
| 🔊 Volume | ❌ | Volume control |
| ⏰ Alarm | ❌ | Alarm set करो |
| 📱 App Open | ❌ | कोई भी app खोलो |
| 📶 WiFi | ✅ | WiFi on/off |
| 🔵 Bluetooth | ✅ | BT on/off |
| 📸 Screenshot | ✅ | Screen capture |
| ☀️ Brightness | ✅ | Brightness control |
| ⚡ Shell | ✅ | Any root command |
| 🖱️ UI Automation | ❌* | Click, scroll, type |

*Accessibility Service permission चाहिए

---

## 📋 Requirements

- Android 8.0+ (API 26+)
- **Rooted phone** (Magisk / KernelSU / SuperSU)
- Claude API Key — [console.anthropic.com](https://console.anthropic.com)
- Android Studio (build करने के लिए)

---

## 🚀 Setup Guide

### Step 1: Project Clone / Download करो
```bash
# Android Studio में open करो
File → Open → HermesAgent folder select करो
```

### Step 2: API Key Add करो
App के Settings में जाकर Claude API Key डालो।

अथवा `local.properties` में:
```
CLAUDE_API_KEY=sk-ant-your-key-here
```

### Step 3: Build करो
```
Build → Generate Signed Bundle/APK → APK → Debug/Release
```

### Step 4: Phone पर Install करो
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 5: Permissions दो
App खोलो और सभी permissions allow करो:
- ✅ Microphone
- ✅ Phone (Calls)
- ✅ Contacts
- ✅ SMS
- ✅ Camera
- ✅ Storage

### Step 6: Special Permissions (Important!)
Settings → Accessibility → Hermes Agent → Enable करो
Settings → Special App Access → Notification Access → Hermes Agent → Enable करो

---

## 💬 Hindi Commands Examples

```
"WiFi बंद करो"
"Bluetooth चालू करो"
"Rahul को call करो"
"98765 को message करो: मैं आ रहा हूं"
"Volume 30 percent करो"
"Screenshot लो"
"Flashlight चालू करो"
"सुबह 7 बजे alarm लगाओ — gym जाना है"
"WhatsApp खोलो"
"Brightness 50 percent करो"
"Battery status बताओ"
"सभी notifications बताओ"
```

---

## 🏗️ Architecture

```
HermesAgent/
├── agent/
│   ├── HermesAgent.kt          ← AI Brain (Claude API)
│   ├── PhoneController.kt      ← Basic phone actions
│   ├── RootController.kt       ← Root (su) commands
│   ├── HermesNotificationListener.kt
│   └── SettingsManager.kt
├── ui/
│   ├── MainActivity.kt         ← Chat UI + Voice input
│   ├── SettingsActivity.kt
│   └── ChatAdapter.kt
└── service/
    ├── HermesAgentService.kt   ← Background service
    ├── HermesAccessibilityService.kt ← UI automation
    └── BootReceiver.kt         ← Auto-start on boot
```

---

## 🔧 Customization

### Custom Commands Add करना
`HermesAgent.kt` के system prompt में नया action add करो:
```
15. CUSTOM — कुछ custom: custom_action(param)
```

फिर `executeAction()` में handle करो:
```kotlin
"custom_action" -> {
    val param = response.params["param"] ?: return
    // आपका code यहाँ
}
```

### Hermes (NousResearch) Model Use करना
अगर local Hermes model use करना है (Ollama के साथ):
```kotlin
// HermesAgent.kt में change करो:
private const val CLAUDE_API_URL = "http://localhost:11434/api/chat"
private const val MODEL = "nous-hermes-2"
```

---

## ⚠️ Important Notes

1. **Root commands careful use करें** — गलत shell command phone brick कर सकती है
2. **API Key safe रखें** — किसी के साथ share मत करो
3. **Accessibility Service** — Battery drain हो सकती है, जरूरत न हो तो disable करो
4. **CALL_PHONE permission** — Direct call होगी, confirmation dialog नहीं

---

## 📝 License
MIT License — Free to use and modify
