package com.adonnis.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adonnis.app.AdonnisApplication
import com.adonnis.app.ai.ChatHistoryFormatter
import com.adonnis.app.ai.OpenRouterApiException
import com.adonnis.app.ai.OpenRouterClient
import com.adonnis.app.ai.Prompts
import com.adonnis.app.data.local.entity.ChatMessageEntity
import com.adonnis.app.data.local.entity.ReminderEntity
import com.adonnis.app.util.DateParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject

/**
 * ViewModel for the main chat screen.
 * Manages message history, sends messages via OpenRouterClient,
 * handles loading/error states with retry, and injects situational context.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AdonnisApplication
    private val chatRepository = app.chatRepository
    private val contextBuilder = ContextBuilder(
        userRepository = app.userRepository,
        planRepository = app.planRepository,
        diaryRepository = app.diaryRepository,
        memoryRepository = app.memoryRepository
    )

    /** Lazily initialized AI client (needs API key from prefs) */
    private var aiClient: OpenRouterClient? = null

    /**
     * Returns an OpenRouter client built from the CURRENT stored API key
     * and model. Re-reads both on every call and recreates the client when
     * they change (e.g. the user updated them in Settings) — this is what
     * fixes the "valid in Settings but chat says invalid" staleness bug.
     */
    private fun currentClient(): OpenRouterClient? {
        val apiKey = app.userRepository.getApiKey()
        if (apiKey.isBlank()) {
            aiClient = null
            return null
        }
        val model = app.preferencesManager.openRouterModel
        if (aiClient == null || aiClient?.apiKey != apiKey || aiClient?.model != model) {
            aiClient = OpenRouterClient(apiKey, model)
        }
        return aiClient
    }

    // ── Message List ─────────────────────────────────────────────────

    private val _messages = MutableStateFlow<List<ChatMessageUi>>(emptyList())
    val messages: StateFlow<List<ChatMessageUi>> = _messages.asStateFlow()

    // ── Typing Indicator ─────────────────────────────────────────────

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    // ── Agent Name (from user prefs) ─────────────────────────────────

    private val _agentName = MutableStateFlow("Adonnis")
    val agentName: StateFlow<String> = _agentName.asStateFlow()

    // ── Error / Retry ───────────────────────────────────────────────

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── API Status ──────────────────────────────────────────────────

    private val _apiReady = MutableStateFlow(false)
    val apiReady: StateFlow<Boolean> = _apiReady.asStateFlow()

    // ── Limits ─────────────────────────────────────────────────────

    companion object {
        const val MAX_MESSAGE_LENGTH = 4096
        const val HISTORY_LIMIT = 50
        const val MAX_RETRIES = 2
        const val RETRY_DELAY_MS = 1000L
    }

    // ── Init ─────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            // Load agent name
            _agentName.value = app.userRepository.getAgentName()

            // Initialize AI client
            _apiReady.value = currentClient() != null

            // Load message history from Room
            chatRepository.getRecentMessages(HISTORY_LIMIT).collect { entities ->
                val uiMessages = entities.map { it.toUi() }
                _messages.value = uiMessages

                // Show welcome message if chat is empty
                if (uiMessages.isEmpty()) {
                    showWelcomeMessage()
                }
            }
        }
    }

    private suspend fun showWelcomeMessage() {
        val name = app.userRepository.getUserName()
        val agent = _agentName.value
        val greeting = if (name.isNotBlank()) {
            "Hello $name! I'm $agent, your AI life planner. I have your timetable and goals. " +
                    "What would you like to do today? You can ask about your plan, set reminders, " +
                    "or just say hi!"
        } else {
            "Hello! I'm $agent, your AI life planner. " +
                    "Tell me about yourself and I'll help organize your day!"
        }

        val entity = ChatMessageEntity(
            role = "agent",
            content = greeting,
            messageType = "text"
        )
        val id = chatRepository.saveMessage(entity)
        _messages.value = _messages.value + entity.toUi(id)
    }

    // ── Send Message ─────────────────────────────────────────────────

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || trimmed.length > MAX_MESSAGE_LENGTH) return

        viewModelScope.launch {
            // Save & show user message
            val userEntity = ChatMessageEntity(role = "user", content = trimmed)
            chatRepository.saveMessage(userEntity)

            // Show typing indicator
            _isTyping.value = true
            _errorMessage.value = null

            try {
                // Build context and call the AI
                val response = callAiWithRetry(trimmed)

                response.onSuccess { reply ->
                    // Auto-create a reminder if the AI mentioned a future
                    // event (hidden ⏰REMINDER tag), then strip the tag from
                    // the message the user sees.
                    val (cleanReply, reminderInfo) = extractReminder(reply)
                    if (reminderInfo != null) {
                        createReminderFromChat(reminderInfo)
                    }
                    val agentEntity = ChatMessageEntity(role = "agent", content = cleanReply)
                    chatRepository.saveMessage(agentEntity)
                }.onFailure { error ->
                    handleAiError(error, trimmed)
                }
            } catch (e: Exception) {
                handleAiError(e, trimmed)
            } finally {
                _isTyping.value = false
            }
        }
    }

    /**
     * Call the AI with retry logic (exponential backoff).
     * Falls back to a graceful error message if all retries fail.
     */
    private suspend fun callAiWithRetry(userMessage: String): Result<String> {
        val client = currentClient()
        if (client == null) {
            return Result.failure(Exception("API key not configured. Set it in Settings."))
        }

        val context = contextBuilder.buildContext()
        val systemPrompt = Prompts.chatPersona(
            agentName = _agentName.value,
            userName = app.userRepository.getUserName(),
            context = context
        )

        // Get chat history from Room and format it for the AI
        val allMessages = chatRepository.getAllMessagesOnce()
        val aiHistory = ChatHistoryFormatter.format(allMessages)
        val fixedHistory = ChatHistoryFormatter.validateAndFix(aiHistory)

        return client.retryWithBackoff(
            maxRetries = MAX_RETRIES,
            initialDelayMs = RETRY_DELAY_MS
        ) {
            client.sendMessage(
                systemInstruction = systemPrompt,
                history = fixedHistory,
                userMessage = userMessage,
                jsonMode = false
            )
        }
    }

    /**
     * Handle an AI error — show a helpful message in the chat.
     * Distinguishes real auth failures (401/403) from transient issues
     * (no credits, rate limits, provider overload, unknown model) and
     * network problems, so users aren't told their key is invalid when it isn't.
     */
    private suspend fun handleAiError(error: Throwable, originalMessage: String) {
        val message = when {
            error is OpenRouterApiException && error.statusCode in 401..403 ->
                "Your API key seems to be invalid. Go to Settings to update it."
            error is OpenRouterApiException && error.statusCode == 402 ->
                "Your OpenRouter account has no credits left. Top up at openrouter.ai."
            error is OpenRouterApiException && error.statusCode == 404 ->
                "The selected AI model doesn't exist. Try a different model in Settings."
            error is OpenRouterApiException && error.statusCode == 429 ->
                "The AI provider is rate-limiting us right now. Please wait a moment and try again."
            error is OpenRouterApiException && error.statusCode in 500..599 ->
                "The AI provider is having issues right now. Please try again in a moment."
            error.message?.contains("API key not configured", ignoreCase = true) == true ->
                "Your API key isn't set yet. Go to Settings to add it."
            error.message?.contains("network", ignoreCase = true) == true ||
                    error.message?.contains("timeout", ignoreCase = true) == true ||
                    error.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                "I'm having trouble connecting to the internet. Please check your connection and try again."
            error.message?.contains("SAFETY", ignoreCase = true) == true ||
                    error.message?.contains("Content blocked", ignoreCase = true) == true ->
                "I couldn't process that request due to content safety filters. Please try rephrasing."
            else ->
                "Sorry, something went wrong: ${error.message?.take(100)}"
        }

        val errorEntity = ChatMessageEntity(
            role = "system",
            content = message,
            messageType = "system"
        )
        chatRepository.saveMessage(errorEntity)
        _errorMessage.value = message
    }

    // ── Auto-Reminders from Chat ─────────────────────────────────────

    /**
     * Extract an AI-emitted reminder tag from a reply.
     * Returns the cleaned text and the parsed reminder info (or null).
     */
    private fun extractReminder(reply: String): Pair<String, ReminderInfo?> {
        // [\s\S] matches across newlines — the AI may wrap the JSON over
        // multiple lines, and a multiline tag must still be parsed AND stripped.
        val match = Regex("⏰REMINDER:\\{([\\s\\S]*?)\\}").find(reply)
        if (match == null) return reply.trim() to null

        // Always strip the tag from the visible reply, even if parsing fails,
        // so a malformed tag never shows as raw text in chat.
        val clean = reply.replace(match.value, "").replace("\n\n\n", "\n\n").trim()

        return try {
            val json = JSONObject("{" + match.groupValues[1] + "}")
            val whenText = json.optString("when")
            val info = ReminderInfo(
                title = json.optString("title").ifBlank { "Reminder" },
                whenText = whenText,
                note = json.optString("note")
            )
            clean to (if (whenText.isBlank()) null else info)
        } catch (_: Exception) {
            clean to null
        }
    }

    private suspend fun createReminderFromChat(info: ReminderInfo) {
        val triggerAt = DateParser.parse(info.whenText) ?: return
        if (triggerAt <= System.currentTimeMillis()) return

        // Always learn the event so future planning knows about it — even if
        // the user has turned notifications off.
        app.memoryRepository.remember(
            category = "event",
            content = "Upcoming: ${info.title} on ${DateParser.describe(triggerAt)}" +
                (if (info.note.isNotBlank()) " (${info.note})" else ""),
            source = "chat"
        )

        // Respect the user's Reminders toggle in Settings — but only for
        // the notification, not the learning.
        if (!app.preferencesManager.remindersEnabled) return

        val reminder = ReminderEntity(
            title = info.title,
            description = info.note.ifBlank { "Set from your chat with ${_agentName.value}" },
            dateTime = triggerAt,
            isAutoGenerated = true
        )
        app.reminderRepository.createReminder(reminder)
    }

    private data class ReminderInfo(val title: String, val whenText: String, val note: String)

    // ── Clear Chat ───────────────────────────────────────────────────

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.deleteAll()
            _messages.value = emptyList()
            showWelcomeMessage()
        }
    }

    // ── Retry Last Send ──────────────────────────────────────────────

    fun retryLastMessage() {
        val currentMessages = _messages.value
        val lastUserMessage = currentMessages.lastOrNull { it.role == ChatMessageRole.USER }
            ?: return
        // Remove the failed message and any messages after it
        val idx = currentMessages.indexOf(lastUserMessage)
        _messages.value = currentMessages.take(idx + 1)
        sendMessage(lastUserMessage.content)
    }

    // ── Refresh API Key ──────────────────────────────────────────────

    fun refreshApiKey() {
        viewModelScope.launch {
            _apiReady.value = currentClient() != null
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun ChatMessageEntity.toUi(generatedId: Long = id): ChatMessageUi {
        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        return ChatMessageUi(
            id = generatedId,
            role = when (role) {
                "agent" -> ChatMessageRole.AGENT
                "system" -> ChatMessageRole.SYSTEM
                else -> ChatMessageRole.USER
            },
            content = content,
            messageType = messageType,
            timestamp = dateFormat.format(Date(timestamp))
        )
    }
}

// ── UI Models ───────────────────────────────────────────────────────────

enum class ChatMessageRole { USER, AGENT, SYSTEM }

data class ChatMessageUi(
    val id: Long = 0,
    val role: ChatMessageRole,
    val content: String,
    val messageType: String = "text",
    val timestamp: String = ""
)
