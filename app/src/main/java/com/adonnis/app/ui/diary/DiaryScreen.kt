package com.adonnis.app.ui.diary

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adonnis.app.data.local.entity.DiaryEntryEntity
import com.adonnis.app.ui.components.CopyButton
import com.adonnis.app.ui.components.GradientButton
import com.adonnis.app.ui.components.MarkdownText

/**
 * Diary screen with entry history, live AI-powered diary session,
 * and weekly insight generation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    viewModel: DiaryViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val isInSession by viewModel.isInSession.collectAsStateWithLifecycle()
    val sessionMessages by viewModel.sessionMessages.collectAsStateWithLifecycle()
    val isSessionLoading by viewModel.isSessionLoading.collectAsStateWithLifecycle()
    val selectedEntry by viewModel.selectedEntry.collectAsStateWithLifecycle()
    val weeklyInsight by viewModel.weeklyInsight.collectAsStateWithLifecycle()
    val isGeneratingInsight by viewModel.isGeneratingInsight.collectAsStateWithLifecycle()

    if (selectedEntry != null) {
        DiaryDetailScreen(
            entry = selectedEntry!!,
            viewModel = viewModel
        )
    } else if (isInSession) {
        DiarySessionScreen(
            messages = sessionMessages,
            isLoading = isSessionLoading,
            onSend = { viewModel.continueSession(it) },
            onSave = { viewModel.endSession() },
            onCancel = { viewModel.cancelSession() }
        )
    } else {
        DiaryListScreen(
            entries = entries,
            weeklyInsight = weeklyInsight,
            isGeneratingInsight = isGeneratingInsight,
            onStartEntry = { viewModel.startNewEntry() },
            onSelectEntry = { viewModel.selectEntry(it) },
            onGenerateInsight = { viewModel.generateWeeklyInsight() },
            onDismissInsight = { viewModel.dismissInsight() },
            onBack = onBack,
            bottomBar = bottomBar
        )
    }
}

// ── List Screen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryListScreen(
    entries: List<DiaryEntryEntity>,
    weeklyInsight: String?,
    isGeneratingInsight: Boolean,
    onStartEntry: () -> Unit,
    onSelectEntry: (Long) -> Unit,
    onGenerateInsight: () -> Unit,
    onDismissInsight: () -> Unit,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val todayEntry = entries.firstOrNull { it.date == java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Diary") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onStartEntry) {
                        Icon(Icons.Outlined.EditNote, contentDescription = "New Entry")
                    }
                }
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Weekly insight card
            if (weeklyInsight != null) {
                item(key = "insight") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✨ Weekly Insight",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                CopyButton(text = weeklyInsight)
                                IconButton(onClick = onDismissInsight) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Dismiss")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            MarkdownText(
                                text = weeklyInsight,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Start entry card
            item(key = "start") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (todayEntry != null)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (todayEntry != null) "📝 Today's Entry Complete" else "🌙 Start Today's Entry",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (todayEntry != null)
                                "You've already written about today. Tap to view or edit."
                            else
                                "Chat with your assistant to reflect on your day. This helps improve your future plans!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        GradientButton(
                            onClick = {
                                if (todayEntry != null) onSelectEntry(todayEntry.id)
                                else onStartEntry()
                            }
                        ) {
                            Icon(
                                if (todayEntry != null) Icons.Outlined.Visibility else Icons.Outlined.EditNote,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (todayEntry != null) "View Today's Entry" else "Start Today's Entry")
                        }
                    }
                }
            }

            // Past entries header
            if (entries.isNotEmpty()) {
                item(key = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Past Entries",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = onGenerateInsight,
                            enabled = !isGeneratingInsight
                        ) {
                            if (isGeneratingInsight) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(if (isGeneratingInsight) "Analyzing..." else "Get Weekly Insight")
                        }
                    }
                }
            }

            // Entry list
            items(entries, key = { it.id }) { entry ->
                DiaryEntryCard(
                    entry = entry,
                    onClick = { onSelectEntry(entry.id) }
                )
            }

            // Empty state
            if (entries.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📖", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No diary entries yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Session Screen ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiarySessionScreen(
    messages: List<DiarySessionMessage>,
    isLoading: Boolean,
    onSend: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's Diary") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = onSave) {
                        Text("Save & Finish", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item(key = "guide") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            text = "Your assistant will guide you through today's reflection. Answer naturally — you can mention events, feelings, goals, or anything on your mind.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                items(messages, key = { "${it.role}_${it.content.take(20)}" }) { msg ->
                    SessionBubble(msg)
                }

                if (isLoading) {
                    item(key = "loading") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "● ● ●",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }

            // Input bar
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Tell me about your day...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                onSend(inputText.trim())
                                inputText = ""
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSend(inputText.trim())
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionBubble(msg: DiarySessionMessage) {
    val isUser = msg.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ) {
            val bubblePadding = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            if (isUser) {
                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = bubblePadding
                )
            } else {
                MarkdownText(
                    text = msg.content,
                    modifier = bubblePadding,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        CopyButton(
            text = msg.content,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// ── Detail Screen ───────────────────────────────────────────────────────

@Composable
private fun DiaryDetailScreen(
    entry: DiaryEntryEntity,
    viewModel: DiaryViewModel
) {
    val parsedGoals = remember(entry) {
        parseStringArray(entry.goalsJson)
    }
    val parsedEvents = remember(entry) {
        parseJsonEvents(entry.futureEventsJson)
    }

    val fullEntryText = remember(entry, parsedGoals, parsedEvents) {
        buildString {
            appendLine(viewModel.formatDate(entry.date))
            entry.moodEmoji?.let { appendLine("Mood: $it") }
            appendLine()
            appendLine(entry.content)
            if (parsedGoals.isNotEmpty()) {
                appendLine()
                appendLine("🎯 Goals")
                parsedGoals.forEach { appendLine("  • $it") }
            }
            if (parsedEvents.isNotEmpty()) {
                appendLine()
                appendLine("📅 Upcoming Events")
                parsedEvents.forEach { appendLine("  • ${it.first}: ${it.second}") }
            }
            entry.sentiment?.let {
                val label = when {
                    it > 0.3f -> "😊 Positive"
                    it < -0.3f -> "😔 Challenging"
                    else -> "😐 Balanced"
                }
                appendLine()
                appendLine("Sentiment: $label")
            }
        }.trimEnd()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.clearSelection() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                Spacer(Modifier.width(4.dp))
                Text("Back to Diary")
            }
            CopyButton(text = fullEntryText)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = viewModel.formatDate(entry.date),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (entry.moodEmoji != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Mood: ${entry.moodEmoji}",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(Modifier.height(16.dp))
            MarkdownText(
                text = entry.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (parsedGoals.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "🎯 Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                for (goal in parsedGoals) {
                    Text(
                        text = "  • $goal",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            // Future events section
            if (parsedEvents.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "📅 Upcoming Events",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                for ((date, event) in parsedEvents) {
                    Text(
                        text = "  • $date: $event",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            if (entry.sentiment != null) {
                Spacer(Modifier.height(16.dp))
                val sentimentLabel = when {
                    entry.sentiment!! > 0.3f -> "😊 Positive"
                    entry.sentiment!! < -0.3f -> "😔 Challenging"
                    else -> "😐 Balanced"
                }
                Text(
                    text = "Sentiment: $sentimentLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Entry Card ──────────────────────────────────────────────────────────

@Composable
private fun DiaryEntryCard(
    entry: DiaryEntryEntity,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Date indicator
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val parts = entry.date.split("-")
                    if (parts.size == 3) {
                        Text(
                            text = parts[2].toIntOrNull()?.toString() ?: parts[2],
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getMonthAbbreviation(parts[1].toIntOrNull()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.snippet.takeIf { it.isNotBlank() } ?: entry.content.take(80),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.moodEmoji != null) {
                        Text(entry.moodEmoji!!, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!entry.goalsJson.isNullOrBlank()) {
                        Text(
                            text = "🎯",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!entry.futureEventsJson.isNullOrBlank()) {
                        Text(
                            text = "📅",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun getMonthAbbreviation(month: Int?): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
    7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
    else -> "?"
}

private fun parseStringArray(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) { emptyList() }
}

private fun parseJsonEvents(json: String?): List<Pair<String, String>> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            obj.optString("date", "?") to obj.optString("event", "")
        }
    } catch (_: Exception) { emptyList() }
}
