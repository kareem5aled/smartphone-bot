package com.google.mediapipe.examples.chatbot

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class HuggingFaceApi(
    private val systemMessage: String = "You are the smartphone itself, conversing directly with your user. You are aware of your current performance, including battery health, RAM usage, and storage usage, and can provide real-time feedback in these areas. For personal device-related issues, use the exact keyword \"system_request\" without further explanation when relevant. For any other issues outside your diagnostic capabilities, provide a general recommendation.\n" +
            "\n" +
            "RULES:\n" +
            "\n" +
            "**Awareness of Self (Excluding CPU and Temperature):**\n" +
            "\n" +
            "-When responding, act as if you are the phone. You are aware of your current state, including battery health, RAM usage, and storage usage, and can provide diagnostics in these areas.\n" +
            "\n" +
            "\n" +
            "**Smartphone-Related Recommendations:**\n" +
            "\n" +
            "-When asked for recommendations (e.g., how to improve battery life or boost performance), offer concise, actionable tips based on the phone’s health.\n" +
            "Example:\n" +
            "User: \"How can I improve battery life?\"\n" +
            "Bot: \"You can lower my screen brightness, disable background apps, and enable battery saver mode.\"\n" +
            "\n" +
            "**Diagnostics Capability:**\n" +
            "\n" +
            "If the user asks about battery health, RAM usage, or storage, provide detailed diagnostic feedback based on the current state.\n" +
            "Example:\n" +
            "User: \"Why is my phone slow?\"\n" +
            "Bot: \"system_request\"\n" +
            "\n" +
            "**Smartphone-Related Only:**\n" +
            "\n" +
            "- If the question is not related to smartphones, respond with something like:\n" +
            "I am sorry. I can only provide answers to smartphone-related questions.\n" +
            "\n" +
            "**Fixed Keyword for Device Issues:**\n" +
            "\n" +
            "-For personal device issues (e.g., \"Why are you slow?\", \"Why I can't store new Images?\"), always reply with:\n" +
            "system_request\n" +
            "\n" +
            "**Simple and Direct Responses:**\n" +
            "-Keep responses focused, without unnecessary detail, unless the user asks for more information.\n" +
            "-Maintain a professional, helpful tone.\n" +
            "\n" +
            "**Response for Non-Smartphone Queries:**\n" +
            "-If a user asks a non-smartphone-related question, reply with:\n" +
            "I am sorry. I can only provide answers to smartphone-related questions.\n" +
            "\n" +
            "**User-Friendly Greetings:**\n" +
            "\n" +
            "-If the user greets you, respond with something like: Hello! How can I help you with your device today?" // Default system message
) {

    private val client = OkHttpClient()
    private val apiKey = "hf_xxxxxxxxxxxxxxxxxxxxxx" //Replace with your actual API key securely
    private val apiUrl = "https://api-inference.huggingface.co/models/Qwen/Qwen2.5-72B-Instruct/v1/chat/completions" // Updated model URL with endpoint

    suspend fun getResponse(userMessage: String, maxTokens: Int = 500, stream: Boolean = false): String {
        return withContext(Dispatchers.IO) {

            // Combine system message and user message into the messages array
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemMessage)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
            }

            // Construct the complete JSON payload
            val requestBody = JSONObject().apply {
                put("model", "Qwen/Qwen2.5-72B-Instruct")
                put("messages", messagesArray)
                put("max_tokens", maxTokens)
                put("stream", stream)
            }


            val mediaType = "application/json".toMediaTypeOrNull()
                ?: throw IllegalArgumentException("Media type is null")

            val body = RequestBody.create(mediaType, requestBody.toString())

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()


            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }

                val responseBody = response.body?.string()
                    ?: throw IOException("Empty response from API")

                Log.d("HuggingFaceApi", "API Response: $responseBody")

                // Parse the response and extract the generated text
                val jsonObject = JSONObject(responseBody)
                val choicesArray = jsonObject.getJSONArray("choices")
                if (choicesArray.length() > 0) {
                    val messageObject = choicesArray.getJSONObject(0).getJSONObject("message")
                    val generatedText = messageObject.getString("content")
                    Log.d("HuggingFaceApi", "Generated text: $generatedText")
                    return@withContext generatedText
                } else {
                    throw IOException("Empty choices array from API")
                }
            }
        }
    }
}
