package com.adonnis.app.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adonnis.app.ui.components.CopyButton
import com.adonnis.app.ui.components.GradientIconButton
import com.adonnis.app.ui.components.MarkdownText
import com.adonnis.app.util.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main chat screen — fully wired to ChatViewModel.
 * Features: persistent message history, typing indicator, character limit,
 * clear chat dialog, error/retry, and auto-scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    bottomBar: @Composable () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isTyping by viewModel.isTyping.collectAsStateWithLifecycle()
    val agentName by viewModel.agentName.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }

    // Auto-scroll to bottom when new messages arrive
    val previousMessageCount = remember { mutableStateOf(messages.size) }
    LaunchedEffect(messages.size) {
        if (messages.size > previousMessageCount.value && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
        previousMessageCount.value = messages.size
    }

    // Offline monitoring
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnline by networkMonitor.observe().collectAsState(initial = true)

    // Debounce ref to prevent rapid sends
    var sendJob by remember { mutableStateOf<Job?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = agentName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isTyping) "Thinking..." else "AI Life Planner",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isTyping)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    // Overflow menu for clear chat
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Chat") },
                                onClick = {
                                    showMenu = false
                                    showClearDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Offline Banner ────────────────────────────────────
                AnimatedVisibility(visible = !isOnline) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "You're offline. Some features may be limited.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // ── Message List ──────────────────────────────────────
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }

                    // Typing indicator
                    if (isTyping) {
                        item(key = "typing") {
                            TypingIndicator()
                        }
                    }

                    // Error message with retry
                    if (errorMessage != null && !isTyping) {
                        item(key = "error") {
                            ErrorCard(
                                message = errorMessage!!,
                                onRetry = { viewModel.retryLastMessage() },
                                onDismiss = { /* error auto-clears on next send */ }
                            )
                        }
                    }

                    // Bottom spacer for scroll
                    item(key = "bottom_spacer") {
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // ── Input Bar ─────────────────────────────────────────
                ChatInputBar(
                    text = inputText,
                    onTextChange = { if (it.length <= ChatViewModel.MAX_MESSAGE_LENGTH) inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            // Debounce: prevent rapid duplicate sends
                            sendJob?.cancel()
                            sendJob = scope.launch {
                                delay(300)
                                viewModel.sendMessage(inputText.trim())
                                inputText = ""
                                listState.animateScrollToItem(messages.size + 1)
                            }
                        }
                    },
                    isProcessing = isTyping
                )
            }

            // ── Empty state overlay ───────────────────────────────────
            if (messages.isEmpty() && !isTyping) {
                EmptyChatOverlay(agentName)
            }
        }
    }

    // ── Clear Chat Dialog ─────────────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
            title = { Text("Clear Chat?") },
            text = { Text("This will delete all conversation history. Your plans and data will not be affected.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChat()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Clear All Messages") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Input Bar ───────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Attachment button
                FilledTonalIconButton(
                    onClick = { /* TODO: attach in Phase 5 */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = "Attach",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Text field
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message your assistant...") },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    enabled = !isProcessing,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend() }),
                    supportingText = if (text.length > ChatViewModel.MAX_MESSAGE_LENGTH * 0.8) {
                        { Text("${text.length}/${ChatViewModel.MAX_MESSAGE_LENGTH}") }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Send button
                GradientIconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank() && !isProcessing,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Message Bubble ──────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: ChatMessageUi) {
    when (message.role) {
        ChatMessageRole.USER -> UserBubble(message)
        ChatMessageRole.AGENT -> AgentBubble(message)
        ChatMessageRole.SYSTEM -> SystemMessage(message)
    }
}

@Composable
private fun UserBubble(message: ChatMessageUi) {
    var expanded by remember { mutableStateOf(false) }
    val isLong = message.content.length > 500

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = 20.dp, bottomEnd = 4.dp
            ),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = if (isLong && !expanded) message.content.take(500) + "..." else message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    overflow = TextOverflow.Ellipsis
                )
                if (isLong) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (expanded) "Show less" else "Continue reading",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
        CopyButton(
            text = message.content,
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}

@Composable
private fun AgentBubble(message: ChatMessageUi) {
    var expanded by remember { mutableStateOf(false) }
    val isLong = message.content.length > 500

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Avatar
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.SmartToy,
                    contentDescription = "Assistant",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 4.dp, topEnd = 20.dp,
                    bottomStart = 20.dp, bottomEnd = 20.dp
                ),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    MarkdownText(
                        text = if (isLong && !expanded) markdownPreview(message.content) else message.content,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isLong) {
                        TextButton(
                            onClick = { expanded = !expanded },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (expanded) "Show less" else "Continue reading",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            CopyButton(
                text = message.content,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun SystemMessage(message: ChatMessageUi) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// ── Typing Indicator ────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp), // align with agent bubble text
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────

/**
 * Truncates Markdown at a line/word boundary so collapsed messages don't
 * split a code block or formatting token (e.g. mid-`**bold**`).
 */
private fun markdownPreview(content: String, limit: Int = 500): String {
    if (content.length <= limit) return content
    val newline = content.lastIndexOf('\n', limit)
    val space = content.lastIndexOf(' ', limit)
    val cut = maxOf(newline, space)
    return content.substring(0, if (cut > 0) cut else limit).trimEnd() + "\n…"
}

// ── Error Card ──────────────────────────────────────────────────────────

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("Retry", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

// ── Empty State ─────────────────────────────────────────────────────────

@Composable
private fun EmptyChatOverlay(agentName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Chat with $agentName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your AI life planner is ready.\nAsk about your schedule, set goals, or just say hello!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
