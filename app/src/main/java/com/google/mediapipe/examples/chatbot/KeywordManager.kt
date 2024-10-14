package com.google.mediapipe.examples.chatbot

object KeywordManager {
    // Define all your keywords categorized for better organization

    private val greetings = listOf(
        "hi","hello","hola","hey","howdy","greetings","good morning","good afternoon","good evening","good day"
    )
    private val generalMobileTerms = listOf(
        "android", "ios", "iphone", "smartphone", "mobile phone", "cellphone",
        "phone", "device", "cell phone", "handset", "gadget", "tablet", "phablet"
    )

    private val osAndPlatformTerms = listOf(
        "operating system", "os", "play store", "app store", "iOS update",
        "android update", "firmware", "software update", "ota", "rooting",
        "jailbreaking", "beta version"
    )

    private val applicationRelatedTerms = listOf(
        "app", "application", "game", "mobile app", "download", "install",
        "update app", "app permissions", "in-app purchase", "push notifications",
        "background app refresh", "data usage", "app crash", "app settings"
    )

    private val phoneFeaturesAndSettings = listOf(
        "bluetooth", "wifi", "nfc", "hotspot", "airplane mode", "gps",
        "location services", "camera", "selfie", "portrait mode", "night mode",
        "lens", "zoom", "video recording", "slow-motion", "4k"
    )

    private val hardwareAndAccessories = listOf(
        "charger", "charging", "fast charging", "wireless charging", "usb-c",
        "lightning cable", "headphones", "earbuds", "airpods", "display",
        "screen", "battery", "power", "screen protector", "case", "cover",
        "stylus", "headphone jack"
    )

    private val commonMobileFunctionalities = listOf(
        "call", "messaging", "sms", "text", "voicemail", "contacts",
        "address book", "video call", "facetime", "imessage", "google duo",
        "signal", "whatsapp", "telegram", "phone number", "sim card", "dual sim",
        "carrier", "network", "4g", "5g", "lte", "mobile data", "roaming"
    )

    private val securityAndPrivacy = listOf(
        "fingerprint", "face id", "face unlock", "passcode", "pin",
        "lock screen", "unlock", "encryption", "two-factor authentication",
        "vpn", "security settings", "privacy", "data backup", "icloud",
        "google drive", "find my iphone", "find my device", "device tracking"
    )

    private val troubleshootingAndIssues = listOf(
        "battery drain", "overheating", "slow phone", "lagging", "app not working",
        "touchscreen issue", "reboot", "restart", "reset phone", "factory reset",
        "phone not charging", "connectivity issue"
    )

    private val appSpecificTerms = listOf(
        "facebook", "instagram", "twitter", "snapchat", "tiktok", "youtube",
        "spotify", "gmail", "outlook", "zoom", "microsoft teams", "netflix",
        "slack", "telegram", "signal", "uber", "google maps"
    )

    private val accessibilityFeatures = listOf(
        "voiceover", "talkback", "screen reader", "closed captions",
        "text-to-speech", "magnification", "hearing aid compatibility",
        "vibration", "gesture control", "accessibility settings"
    )

    private val mobilePaymentsAndBanking = listOf(
        "mobile payments", "apple pay", "google pay", "samsung pay",
        "contactless payments", "wallet", "banking app", "mobile banking",
        "money transfer", "peer-to-peer payment"
    )

    // Combine all keywords into single lists for efficient searching
    val singleWordKeywords: Set<String> = (
                    greetings+
                    generalMobileTerms +
                    osAndPlatformTerms +
                    applicationRelatedTerms +
                    phoneFeaturesAndSettings +
                    hardwareAndAccessories +
                    commonMobileFunctionalities +
                    securityAndPrivacy +
                    troubleshootingAndIssues +
                    appSpecificTerms +
                    accessibilityFeatures +
                    mobilePaymentsAndBanking
            ).filter { !it.contains(" ") }.toSet()

    val multiWordKeywords: List<String> = (
                    greetings+
                    generalMobileTerms +
                    osAndPlatformTerms +
                    applicationRelatedTerms +
                    phoneFeaturesAndSettings +
                    hardwareAndAccessories +
                    commonMobileFunctionalities +
                    securityAndPrivacy +
                    troubleshootingAndIssues +
                    appSpecificTerms +
                    accessibilityFeatures +
                    mobilePaymentsAndBanking
            ).filter { it.contains(" ") }

    /**
     * Checks if the input contains any of the predefined smartphone-related keywords.
     * @param input The user input string.
     * @return True if any keyword is found, False otherwise.
     */
    fun containsSmartphoneKeyword(input: String): Boolean {
        val processedInput = preprocessInput(input)
        val words = processedInput.split(" ")

        // Check single-word keywords
        for (word in words) {
            if (singleWordKeywords.contains(word)) {
                return true
            }
        }

        // Check multi-word keywords
        for (keyword in multiWordKeywords) {
            if (processedInput.contains(keyword)) {
                return true
            }
        }

        return false
    }

    /**
     * Preprocesses the input string by converting to lowercase, removing punctuation,
     * and trimming whitespace.
     * @param input The raw user input string.
     * @return The processed string.
     */
    private fun preprocessInput(input: String): String {
        return input.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "") // Remove punctuation
            .trim()
    }
}
