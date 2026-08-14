package com.adonnis.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenRouter API client.
 *
 * OpenRouter is a unified gateway: one API key gives access to hundreds of
 * models (OpenAI, Anthropic, Google, Meta, DeepSeek, ...) with automatic
 * failover between providers. This removes the model-retirement / 503 /
 * key-format pain we had with calling Google's Gemini API directly.
 *
 * Auth is the standard `Authorization: Bearer <key>` header (keys start
 * with `sk-or-v1-`). Everything is done with plain HttpURLConnection +
 * org.json, so there is no SDK dependency to deprecate.
 */
class OpenRouterClient(
    val apiKey: String,
    val model: String = DEFAULT_MODEL
) {

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1"
        private const val CHAT_URL = "$BASE_URL/chat/completions"

        /**
         * Endpoint used to validate an API key without spending any credits.
         * GET with `Authorization: Bearer <key>` — 200 means valid.
         */
        private const val AUTH_URL = "$BASE_URL/auth/key"

        /**
         * The OpenRouter model used for all requests by default.
         * Public so other screens can show it.
         *
         * `openrouter/auto` is OpenRouter's automatic router: it picks the
         * best available model for the request and automatically fails over
         * when a provider is down or overloaded — which is exactly the kind
         * of pain (retired models, 503 spikes) that motivated switching.
         *
         * The model is configurable in Settings; any valid OpenRouter model
         * ID works (e.g. `deepseek/deepseek-chat-v3-0324:free` for a free
         * model, `anthropic/claude-3.5-sonnet`, etc.).
         */
        const val DEFAULT_MODEL = "openrouter/auto"

        private const val TIMEOUT_MS = 30000

        /**
         * Validate an OpenRouter API key by calling the /auth/key endpoint.
         * Returns (isValid, errorMessage). Cheap — costs no credits and does
         * not depend on any model being available.
         */
        suspend fun validateKey(key: String): Pair<Boolean, String> =
            withContext(Dispatchers.IO) {
                val connection = URL(AUTH_URL).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", "Bearer $key")
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        // Also surface free-tier / limit info if available
                        val body = connection.inputStream.bufferedReader().readText()
                        val limit = try {
                            val data = JSONObject(body).optJSONObject("data")
                            data?.optString("limit", null)?.takeIf { it.isNotBlank() }
                        } catch (_: Exception) { null }
                        return@withContext if (limit == null) {
                            Pair(true, "")
                        } else {
                            Pair(true, "Valid! Credit limit: $limit")
                        }
                    }

                    val errorBody = try {
                        connection.errorStream?.bufferedReader()?.readText() ?: ""
                    } catch (_: Exception) { "" }

                    val message = when (responseCode) {
                        401 -> "API key rejected: ${extractError(errorBody)}"
                        402 -> "Account has no credits. Top up at openrouter.ai."
                        429 -> "Rate limited. Try again later."
                        else -> "HTTP $responseCode: ${extractError(errorBody)}"
                    }
                    Pair(false, message)
                } catch (e: java.net.UnknownHostException) {
                    Pair(false, "No internet connection. Check your network.")
                } catch (e: java.net.SocketTimeoutException) {
                    Pair(false, "Connection timed out. Check your network.")
                } catch (e: Exception) {
                    Pair(false, "Could not reach OpenRouter: ${e.message}")
                } finally {
                    connection.disconnect()
                }
            }

        /** Pull just the human-readable `error.message` from an API error body. */
        internal fun extractError(body: String): String {
            if (body.isBlank()) return "(no details)"
            return try {
                val json = JSONObject(body)
                json.optJSONObject("error")?.optString("message", body.take(200))
                    ?: body.take(200)
            } catch (_: Exception) {
                body.take(200)
            }
        }
    }

    /**
     * Send a chat message with full history and a system instruction.
     * [history] uses roles "user"/"assistant" (OpenAI-compatible).
     */
    suspend fun sendMessage(
        systemInstruction: String,
        history: List<AIMessage>,
        userMessage: String,
        jsonMode: Boolean = false,
        maxTokens: Int = 4096
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(CHAT_URL).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.setRequestProperty("HTTP-Referer", "https://adonnis.app")
                connection.setRequestProperty("X-Title", "Adonnis")
                connection.doOutput = true
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS

                // Build OpenAI-style messages array
                val messages = JSONArray().apply {
                    if (systemInstruction.isNotBlank()) {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", systemInstruction)
                        })
                    }
                    for (msg in history) {
                        put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                }

                val requestBody = JSONObject().apply {
                    put("model", model)
                    put("messages", messages)
                    put("temperature", if (jsonMode) 0.3f else 0.7f)
                    put("max_tokens", maxTokens)
                    put("top_p", 0.95f)
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(requestBody.toString())
                writer.flush()
                writer.close()

                // Read response
                val responseCode = connection.responseCode
                val responseBody = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    val errorBody = try {
                        connection.errorStream?.bufferedReader()?.readText() ?: ""
                    } catch (_: Exception) { "" }
                    return@withContext Result.failure(
                        OpenRouterApiException(responseCode, errorBody)
                    )
                }

                // Parse response — content may be a String or an array of parts
                val root = JSONObject(responseBody)
                val choices = root.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).optJSONObject("message")
                    val content = message?.opt("content")
                    val text = when (content) {
                        null -> ""
                        is String -> content
                        is JSONArray -> buildString {
                            for (i in 0 until content.length()) {
                                val part = content.optJSONObject(i)
                                append(part?.optString("text", "") ?: "")
                            }
                        }
                        else -> content.toString()
                    }
                    if (text.isNotBlank()) {
                        return@withContext Result.success(text)
                    }
                }

                // Check for refusal (content may be blocked by provider policies)
                val refusal = choices?.optJSONObject(0)?.optJSONObject("message")
                    ?.optString("refusal", null)
                    ?: root.optJSONObject("error")?.optString("message", null)
                if (!refusal.isNullOrBlank()) {
                    return@withContext Result.failure(
                        Exception("Content blocked: $refusal")
                    )
                }

                Result.failure(Exception("AI returned an empty response"))
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Single-turn generation (no chat history).
     */
    suspend fun generateContent(
        systemInstruction: String,
        prompt: String,
        jsonMode: Boolean = false,
        maxTokens: Int = 4096
    ): Result<String> = sendMessage(
        systemInstruction = systemInstruction,
        history = emptyList(),
        userMessage = prompt,
        jsonMode = jsonMode,
        maxTokens = maxTokens
    )

    /**
     * Retry a block with exponential backoff.
     */
    suspend fun <T> retryWithBackoff(
        maxRetries: Int = 2,
        initialDelayMs: Long = 1000,
        block: suspend () -> Result<T>
    ): Result<T> {
        var lastError: Throwable? = null
        var delayMs = initialDelayMs

        repeat(maxRetries + 1) { attempt ->
            val result = block()
            if (result.isSuccess) return result
            lastError = result.exceptionOrNull()
            if (attempt < maxRetries) {
                delay(delayMs)
                delayMs *= 2
            }
        }

        return Result.failure(
            lastError ?: Exception("Request failed after $maxRetries retries")
        )
    }
}

/**
 * A single message in the OpenAI-compatible chat format.
 * Roles: "user" or "assistant".
 */
data class AIMessage(
    val role: String,  // "user" or "assistant"
    val content: String
)

/**
 * Exception with HTTP status code and error body from the OpenRouter API.
 */
class OpenRouterApiException(
    val statusCode: Int,
    val errorBody: String
) : Exception("OpenRouter API returned HTTP $statusCode: ${OpenRouterClient.extractError(errorBody)}") {
    companion object {
        private fun extractError(body: String): String =
            OpenRouterClient.extractError(body)
    }
}
