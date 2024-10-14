package com.google.mediapipe.examples.chatbot

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ChatViewModel(
    private val inferenceModel: InferenceModel,
    private val deviceHealth: DeviceHealth,
    private val intentClassifier: IntentClassifier
) : ViewModel() {

    private val huggingFaceApi = HuggingFaceApi()

    private val _uiState: MutableStateFlow<GemmaUiState> = MutableStateFlow(GemmaUiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _textInputEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isTextInputEnabled: StateFlow<Boolean> = _textInputEnabled.asStateFlow()


    private val _isResponseGenerating: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isResponseGenerating: StateFlow<Boolean> = _isResponseGenerating.asStateFlow()

    init {
        // Add a welcoming message from the model when the ViewModel is initialized
        viewModelScope.launch(Dispatchers.IO) {
            val welcomeMessage = "Hello! I'm here to assist with mobile advice. If you want to know system info, just type 'sysinfo'."
            streamSystemMessage(welcomeMessage)
        }
    }

    private val _isOnlineMode = MutableStateFlow(false)
    val isOnlineMode: StateFlow<Boolean> = _isOnlineMode.asStateFlow()

    fun toggleOnlineMode() {
        _isOnlineMode.update { !it }
    }

    fun sendMessage(userMessage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Input Validation
            if (userMessage.isBlank()) {
                streamSystemMessage("Please enter a valid question.")
                return@launch
            }

            if (userMessage.length > 500) {
                streamSystemMessage("Your question is too long. Please shorten it and try again.")
                return@launch
            }

            // Always add the user's message to the UI
            _uiState.value.addMessage(userMessage, USER_PREFIX)

            if (_isOnlineMode.value) {
                // Online mode is enabled, communicate with Hugging Face API
                handleOnlineModeRequest(userMessage)
            } else {
                // Classify the intent of the user's message
                val intent = intentClassifier.classifyIntent(userMessage)

                when (intent) {
                    IntentType.DEVICE_STATUS_INQUIRY -> {
                        // Generate and stream device diagnostic response
                        val diagnosticResponse = generateDeviceDiagnosticResponse()
                        streamSystemMessage(diagnosticResponse)
                    }

                    IntentType.GENERAL_ADVICE_REQUEST -> {
                        // Handle general advice using LLM
                        handleGeneralAdviceRequest(userMessage)
                    }

                    IntentType.OUT_OF_SCOPE -> {
                        // Stream out-of-scope message
                        streamSystemMessage("I'm sorry, but that question is out of scope.")
                    }

                }
            }
        }
    }
    private fun setResponseGenerating(isGenerating: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            _isResponseGenerating.value = isGenerating
        }
    }
    private suspend fun handleOnlineModeRequest(userMessage: String) {
        // Create a loading message
        var currentMessageId: String? = _uiState.value.createLoadingMessage()
        setResponseGenerating(true)
        try {
            // Send the message to the Hugging Face API
            val response = huggingFaceApi.getResponse(userMessage)
            if (response == "system_request"){
                    // If response is "system_request", generate and stream the device diagnostic response
                    val diagnosticResponse = generateDeviceDiagnosticResponse()
                    // Stream the diagnostic response
                    streamSystemMessage(diagnosticResponse,currentMessageId)
                    currentMessageId = null // Clear the current message ID as we handled the response
            } else{
                // Stream the response character by character
                val delayBetweenChars = 30L // Adjustable delay between characters
                for (char in response) {
                    withContext(Dispatchers.Main) {
                        currentMessageId?.let {
                            _uiState.value.appendMessage(it, char.toString())
                        }
                    }
                    delay(delayBetweenChars)
                }
                // Indicate that streaming is done
                withContext(Dispatchers.Main) {
                    currentMessageId?.let {
                        _uiState.value.appendMessage(it, "", done = true)
                    }
                    currentMessageId = null
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                currentMessageId?.let {
                    // Update the existing loading message with the error and mark as done
                    _uiState.value.appendMessage(it, "An error occurred while communicating with the online Model. Please, make sure you have active internet connection.", done = true)
                    currentMessageId = null
                }
            }
        } finally {
            setResponseGenerating(false)
        }
    }

    private suspend fun streamSystemMessage(message: String, messageId: String? = null) {
        val id = messageId ?: withContext(Dispatchers.Main) {
            _uiState.value.createLoadingMessage()
        }
        setResponseGenerating(true)
        try {
            val delayBetweenChars = 30L // Adjustable delay between characters
            for (char in message) {
                withContext(Dispatchers.Main) {
                    _uiState.value.appendMessage(id, char.toString())
                }
                delay(delayBetweenChars)
            }
            // Indicate that streaming is done
            withContext(Dispatchers.Main) {
                _uiState.value.appendMessage(id, "", done = true)
            }
        } catch (e: Exception) {
            // Handle exceptions if necessary
            withContext(Dispatchers.Main) {
                _uiState.value.addMessage("An error occurred while streaming the message.", MODEL_PREFIX)
            }
        } finally {
            setResponseGenerating(false)
        }
    }

    private suspend fun handleGeneralAdviceRequest(userMessage: String) {
        // Create a loading message (e.g., showing a spinner)
        var currentMessageId: String? = _uiState.value.createLoadingMessage()
        setResponseGenerating(true)
        try {
            // Send the message to the LLM
            inferenceModel.generateResponseAsync(userMessage)

            // Collect partial results from the LLM
            inferenceModel.partialResults
                .collectIndexed { index, (partialResult, done) ->
                    currentMessageId?.let {
                        if (index == 0) {
                            _uiState.value.appendFirstMessage(it, partialResult)
                        } else {
                            _uiState.value.appendMessage(it, partialResult, done)
                        }
                        if (done) {
                            setResponseGenerating(false)
                            currentMessageId = null
                        }
                    }
                }
        } catch (e: Exception) {
            _uiState.value.addMessage(e.localizedMessage ?: "Unknown Error", MODEL_PREFIX)
        } finally {
            setResponseGenerating(false)
        }
    }

    private fun generateDeviceDiagnosticResponse(): String {
        val builder = StringBuilder()
        builder.append("--System Information--\n\n")
        // RAM Status
        if (deviceHealth.isMemoryLow()) {
            builder.append("🔴 **Memory Alert**: Your device is running low on memory. Consider closing some background apps to improve performance.\n\n")
        } else {
            builder.append("🟢 **Memory Status**: Sufficient memory available.\n\n")
        }

        // Detailed RAM Usage
        val memoryStatus = deviceHealth.getMemoryStatus()
        builder.append("$memoryStatus\n\n")

        // Battery Status
        val batteryLevel = deviceHealth.getBatteryLevel()
        val batteryHealth = deviceHealth.getBatteryHealth()

        if (deviceHealth.isBatteryLow()) {
            builder.append("🔴 **Battery Low**: Your battery level is at $batteryLevel%. Consider charging your device to ensure uninterrupted usage.\n\n")
        } else {
            builder.append("🟢 **Battery Level**: $batteryLevel%.\n\n")
        }

        if (!deviceHealth.isCharging()) {
            builder.append("⚠️ **Charging Status**: Your device is not charging. If you're experiencing battery drain, consider enabling battery-saving modes or checking your charging cable.\n\n")
        }

        // Battery Health
        val batteryHealthStatus = when (batteryHealth) {
            "Good" -> "🟢 **Battery Health**: $batteryHealth."
            else -> "🔴 **Battery Health**: $batteryHealth."
        }
        builder.append("$batteryHealthStatus\n\n")
        val topApps = deviceHealth.getTopUsedAppsFromEvents()
        if (topApps.isNotEmpty()) {
            builder.append("📊 **Top Used Apps (Estimated Battery Consumers):**\n")
            for ((index, app) in topApps.withIndex()) {

                builder.append("${index + 1}. ${app.appName}\n")
            }
            builder.append("\n")
        } else {
            builder.append("No app usage data available.\n\n")
        }

        // Storage Status
        val storageStatus = deviceHealth.getStorageStatus()
        if (deviceHealth.isStorageLow()) {
            builder.append("🔴 **Storage Alert**: $storageStatus\n\n")
        } else {
            builder.append("🟢 **Storage Status**: $storageStatus\n\n")
        }

        // Provide General Recommendations if any alerts
        if (builder.contains("🔴") || builder.contains("⚠️")) {
            builder.append("**Recommendations:**\n")
            if (builder.contains("Memory Alert")) {
                builder.append("- Close unused background apps.\n")
            }
            if (builder.contains("Battery Low")) {
                builder.append("- Charge your device regularly and avoid complete discharges.\n")
            }
            if (builder.contains("Storage Alert")) {
                builder.append("- Uninstall unnecessary apps and delete unwanted files.\n")
            }
            if (builder.contains("Charging Status")) {
                builder.append("- Enable battery-saving modes or check your charging accessories.\n")
            }
        } else {
            builder.append("Your device is running smoothly! Keep up the good work.\n")
        }

        return builder.toString()
    }



    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val inferenceModel = InferenceModel.getInstance(context)
                val deviceHealth = DeviceHealth(context)
                val intentClassifier = IntentClassifier()
                return ChatViewModel(inferenceModel, deviceHealth, intentClassifier) as T
            }
        }
    }
}
