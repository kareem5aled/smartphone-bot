// IntentClassifier.kt
package com.google.mediapipe.examples.chatbot

enum class IntentType {
    DEVICE_STATUS_INQUIRY,
    GENERAL_ADVICE_REQUEST,
    OUT_OF_SCOPE
}

class IntentClassifier {

    fun classifyIntent(input: String): IntentType {
        val processedInput = input.lowercase().trim()

        // Check if the input is exactly "sysinfo"
        if (processedInput == "sysinfo") {
            return IntentType.DEVICE_STATUS_INQUIRY
        }

        // Check if the input contains any smartphone-related keywords
        if (KeywordManager.containsSmartphoneKeyword(input)) {
            return IntentType.GENERAL_ADVICE_REQUEST
        }

        // Otherwise, it's out of scope
        return IntentType.OUT_OF_SCOPE
    }
}