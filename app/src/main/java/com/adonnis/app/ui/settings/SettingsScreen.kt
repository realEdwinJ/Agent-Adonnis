package com.adonnis.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adonnis.app.ai.Prompts

/**
 * Settings screen — manage API key, profile, alarms, notifications, and data.
 * Fully wired to SettingsViewModel with functional dialogs for every option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onResetOnboarding: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    // Collect all ViewModel state
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val agentName by viewModel.agentName.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val timetable by viewModel.timetable.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val wakeUpTime by viewModel.wakeUpTime.collectAsStateWithLifecycle()
    val bedTime by viewModel.bedTime.collectAsStateWithLifecycle()
    val sleepHours by viewModel.sleepHours.collectAsStateWithLifecycle()
    val morningRoutine by viewModel.morningRoutine.collectAsStateWithLifecycle()
    val model by viewModel.model.collectAsStateWithLifecycle()
    val moduleDifficulties by viewModel.moduleDifficulties.collectAsStateWithLifecycle()

    val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()
    val diaryPromptsEnabled by viewModel.diaryPromptsEnabled.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()

    val showNameDialog by viewModel.showNameDialog.collectAsStateWithLifecycle()
    val showAgentDialog by viewModel.showAgentDialog.collectAsStateWithLifecycle()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsStateWithLifecycle()
    val showTimetableDialog by viewModel.showTimetableDialog.collectAsStateWithLifecycle()
    val showGoalsDialog by viewModel.showGoalsDialog.collectAsStateWithLifecycle()
    val showSleepDialog by viewModel.showSleepDialog.collectAsStateWithLifecycle()
    val showResetDialog by viewModel.showResetDialog.collectAsStateWithLifecycle()
    val showModelDialog by viewModel.showModelDialog.collectAsStateWithLifecycle()
    val showModuleDifficultyDialog by viewModel.showModuleDifficultyDialog.collectAsStateWithLifecycle()
    val showRemindersDialog by viewModel.showRemindersDialog.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── Profile Section ──
            item {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                SettingsCard(
                    title = "Change Name",
                    subtitle = if (userName.isNotBlank()) "Current: $userName" else "Not set",
                    icon = Icons.Outlined.Person,
                    onClick = viewModel::showNameDialog
                )
            }
            item {
                SettingsCard(
                    title = "Rename Assistant",
                    subtitle = "Current: $agentName",
                    icon = Icons.Outlined.SmartToy,
                    onClick = viewModel::showAgentDialog
                )
            }
            item {
                SettingsCard(
                    title = "Update Timetable",
                    subtitle = if (timetable.isNotBlank()) "${timetable.lines().size} lines saved" else "Paste a timetable description",
                    icon = Icons.Outlined.CalendarMonth,
                    onClick = viewModel::showTimetableDialog
                )
            }
            item {
                SettingsCard(
                    title = "Module Difficulty",
                    subtitle = if (moduleDifficulties.any { it.name.isNotBlank() })
                        "${moduleDifficulties.count { it.name.isNotBlank() }} rated (1 = hardest)"
                    else
                        "Rate your modules' difficulty",
                    icon = Icons.Outlined.BarChart,
                    onClick = viewModel::showModuleDifficultyDialog
                )
            }
            item {
                SettingsCard(
                    title = "Update Goals",
                    subtitle = if (goals.isNotEmpty()) "${goals.size} goal(s) set" else "No goals yet",
                    icon = Icons.Outlined.EmojiEvents,
                    onClick = viewModel::showGoalsDialog
                )
            }
            item {
                SettingsCard(
                    title = "Sleep Schedule",
                    subtitle = buildString {
                        if (wakeUpTime.isNotBlank()) append("Wake: $wakeUpTime")
                        if (bedTime.isNotBlank()) append(" | Bed: $bedTime")
                        if (sleepHours.isNotBlank()) append(" | ${sleepHours}h needed")
                        if (isEmpty()) append("Not configured")
                    },
                    icon = Icons.Outlined.Bedtime,
                    onClick = viewModel::showSleepDialog
                )
            }

            // ── API Section ──
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "API & AI",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                SettingsCard(
                    title = "OpenRouter API Key",
                    subtitle = if (apiKey.isNotBlank()) "••••••••" else "Not configured",
                    icon = Icons.Outlined.Key,
                    onClick = viewModel::showApiKeyDialog
                )
            }
            item {
                SettingsCard(
                    title = "AI Model",
                    subtitle = "Current: $model",
                    icon = Icons.Outlined.Memory,
                    onClick = viewModel::showModelDialog
                )
            }

            // ── Appearance Section ──
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                SettingsCard(
                    title = "Dark Mode",
                    subtitle = if (darkMode) "On — dark theme active" else "Off — follows system",
                    icon = if (darkMode) Icons.Filled.DarkMode else Icons.Outlined.LightMode,
                    onClick = viewModel::toggleDarkMode,
                    trailing = {
                        Switch(
                            checked = darkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() }
                        )
                    }
                )
            }

            // ── Notifications Section ──
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                SettingsCard(
                    title = "Reminders",
                    subtitle = "Event and task reminders",
                    icon = Icons.Outlined.Notifications,
                    trailing = {
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = viewModel::setRemindersEnabled
                        )
                    }
                )
            }
            item {
                SettingsCard(
                    title = "Diary Prompts",
                    subtitle = "End-of-day diary reminders",
                    icon = Icons.Outlined.MenuBook,
                    trailing = {
                        Switch(
                            checked = diaryPromptsEnabled,
                            onCheckedChange = viewModel::setDiaryPromptsEnabled
                        )
                    }
                )
            }
            item {
                SettingsCard(
                    title = "Manage Reminders",
                    subtitle = if (reminders.isEmpty())
                        "No upcoming reminders — chat & diary events auto-create them"
                    else
                        "${reminders.size} upcoming reminder(s) — tap to view",
                    icon = Icons.Outlined.AlarmAdd,
                    onClick = viewModel::showRemindersDialog
                )
            }

            // ── Data Section ──
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Data",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                Card(
                    onClick = viewModel::showResetDialog,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reset All Data",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Clear everything and start over",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // ── About ──
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Adonnis — AI Life Planner",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Version 1.0.0",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Powered by OpenRouter AI. Built with Jetpack Compose & Material 3.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Licenses",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "• OpenRouter AI API\n• Jetpack Compose + Material 3\n• Room Database\n• AndroidX Security (EncryptedSharedPreferences)\n• Kotlinx Coroutines",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }
                }
            }

            // Bottom spacer
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────────

    if (showNameDialog) {
        NameEditDialog(
            currentName = userName,
            onSave = viewModel::saveName,
            onDismiss = viewModel::hideNameDialog
        )
    }

    if (showAgentDialog) {
        NameEditDialog(
            currentName = agentName,
            title = "Rename Assistant",
            label = "Assistant name",
            onSave = viewModel::saveAgentName,
            onDismiss = viewModel::hideAgentDialog
        )
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = apiKey,
            onSave = viewModel::saveApiKey,
            onDismiss = viewModel::hideApiKeyDialog,
            onTest = viewModel::testApiKey,
            onKeyChange = viewModel::setPendingApiKey,
            testResult = viewModel.apiKeyTestResult.collectAsStateWithLifecycle().value,
            isTesting = viewModel.isTestingKey.collectAsStateWithLifecycle().value
        )
    }

    if (showTimetableDialog) {
        TimetableDialog(
            currentTimetable = timetable,
            onSave = viewModel::saveTimetable,
            onDismiss = viewModel::hideTimetableDialog
        )
    }

    if (showModelDialog) {
        ModelDialog(
            currentModel = model,
            onSave = viewModel::saveModel,
            onDismiss = viewModel::hideModelDialog
        )
    }

    if (showModuleDifficultyDialog) {
        ModuleDifficultyDialog(
            items = moduleDifficulties,
            onUpdate = viewModel::updateModuleDifficulty,
            onAdd = viewModel::addModuleDifficulty,
            onRemove = viewModel::removeModuleDifficulty,
            onSave = viewModel::saveModuleDifficulties,
            onDismiss = viewModel::hideModuleDifficultyDialog
        )
    }

    if (showGoalsDialog) {
        GoalsDialog(
            currentGoals = goals,
            onSave = viewModel::saveGoals,
            onDismiss = viewModel::hideGoalsDialog
        )
    }

    if (showSleepDialog) {
        SleepDialog(
            currentWakeUp = wakeUpTime,
            currentBed = bedTime,
            currentHours = sleepHours,
            currentRoutine = morningRoutine,
            onSave = viewModel::saveSleepPreferences,
            onDismiss = viewModel::hideSleepDialog
        )
    }

    if (showRemindersDialog) {
        RemindersDialog(
            reminders = reminders,
            formatTime = viewModel::formatReminderTime,
            onDone = viewModel::markReminderDone,
            onDelete = viewModel::deleteReminder,
            onDismiss = viewModel::hideRemindersDialog
        )
    }

    // ── Reset Confirmation ──────────────────────────────────────────
    val navigateToOnboarding by viewModel.navigateToOnboarding.collectAsStateWithLifecycle()
    LaunchedEffect(navigateToOnboarding) {
        if (navigateToOnboarding) onResetOnboarding()
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideResetDialog,
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null) },
            title = { Text("Reset All Data?") },
            text = { Text("This will delete all your data including plans, diary entries, reminders, and settings. You'll be taken back to the setup screen. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = viewModel::resetAllData,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Reset Everything") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideResetDialog) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Reusable Settings Card ───────────────────────────────────────────────

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    onClick: () -> Unit = {},
    trailing: @Composable () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

// ── Name Edit Dialog ────────────────────────────────────────────────────

@Composable
private fun NameEditDialog(
    currentName: String,
    title: String = "Change Name",
    label: String = "Your name",
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── API Key Dialog ──────────────────────────────────────────────────────

@Composable
private fun ApiKeyDialog(
    currentKey: String,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onTest: () -> Unit,
    onKeyChange: (String) -> Unit,
    testResult: SettingsViewModel.ApiKeyTestResult?,
    isTesting: Boolean
) {
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Key, contentDescription = null) },
        title = { Text("OpenRouter API Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Keys start with sk-or-v1-. Get one free at openrouter.ai/keys",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = currentKey,
                    onValueChange = onKeyChange,
                    label = { Text("API Key") },
                    placeholder = { Text("sk-or-v1-...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = if (showKey) "Hide key" else "Show key"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onTest,
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isTesting) "Testing..." else "Test Key")
                    }
                }

                // Test result feedback
                if (testResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (testResult.success)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (testResult.success) Icons.Filled.CheckCircle
                                else Icons.Filled.Error,
                                contentDescription = null,
                                tint = if (testResult.success)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = testResult.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (testResult.success)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = testResult?.success == true
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Timetable Dialog (paste AI-generated description) ───────────────────

@Composable
private fun TimetableDialog(
    currentTimetable: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentTimetable) }
    val clipboard = LocalClipboardManager.current
    var copiedPrompt by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(copiedPrompt) {
        if (copiedPrompt) {
            kotlinx.coroutines.delay(2000)
            copiedPrompt = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
        title = { Text("Update Timetable") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(scrollState)
            ) {
                Text(
                    text = "Turn your timetable photo into text with any AI chatbot, then paste the description here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "1. Open ChatGPT / Claude / Gemini on your computer",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "2. Upload a photo of your timetable",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "3. Paste this prompt into it, then copy the reply",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = Prompts.TIMETABLE_DESCRIPTION_PROMPT,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(Prompts.TIMETABLE_DESCRIPTION_PROMPT))
                                copiedPrompt = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                if (copiedPrompt) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (copiedPrompt) "Copied!" else "Copy prompt")
                        }
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Paste the description here") },
                    placeholder = { Text("Monday 09:00-10:00 Mathematics Room 12\n...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── AI Model Dialog ─────────────────────────────────────────────────────

@Composable
private fun ModelDialog(
    currentModel: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var model by remember { mutableStateOf(currentModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Memory, contentDescription = null) },
        title = { Text("AI Model") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("OpenRouter model ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("openrouter/auto") }
                )
                Text(
                    text = "• openrouter/auto (default) — automatically picks the best model and fails over when a provider is down.\n• Free models exist too, e.g. deepseek/deepseek-chat-v3-0324:free\n• Browse all models at openrouter.ai/models",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (model.isNotBlank()) onSave(model) },
                enabled = model.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Module Difficulty Dialog (1 = hardest) ──────────────────────────────

@Composable
private fun ModuleDifficultyDialog(
    items: List<SettingsViewModel.ModuleDifficultyItem>,
    onUpdate: (Int, String, String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.BarChart, contentDescription = null) },
        title = { Text("Module Difficulty") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Rank your modules from hardest to easiest: 1 = your hardest module, 2 = second hardest, and so on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (items.isEmpty()) {
                    TextButton(onClick = onAdd) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add a module")
                    }
                }

                items.forEachIndexed { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { onUpdate(index, it, item.rankText) },
                            label = { Text("Module") },
                            placeholder = { Text("e.g. Advanced Math") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = item.rankText,
                            onValueChange = { onUpdate(index, item.name, it) },
                            label = { Text("Rank") },
                            placeholder = { Text("1") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(80.dp)
                        )
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (items.isNotEmpty()) {
                    TextButton(onClick = onAdd) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add another module")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Goals Dialog ────────────────────────────────────────────────────────

@Composable
private fun GoalsDialog(
    currentGoals: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    // Ensure at least 3 fields
    val goalsList = remember(currentGoals.size) {
        mutableStateListOf<String>().apply {
            addAll(currentGoals)
            while (size < 3) add("")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.EmojiEvents, contentDescription = null) },
        title = { Text("Update Goals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "What are your top priorities?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                goalsList.forEachIndexed { index, goal ->
                    OutlinedTextField(
                        value = goal,
                        onValueChange = { goalsList[index] = it },
                        label = { Text("Goal ${index + 1}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                when (index) {
                                    0 -> "e.g. Get an A in Chemistry"
                                    1 -> "e.g. Learn to play guitar"
                                    else -> "e.g. Read 12 books this year"
                                }
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(goalsList.filter { it.isNotBlank() }) },
                enabled = goalsList.any { it.isNotBlank() }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Reminders Dialog ─────────────────────────────────────────────

@Composable
private fun RemindersDialog(
    reminders: List<com.adonnis.app.data.local.entity.ReminderEntity>,
    formatTime: (Long) -> String,
    onDone: (Long) -> Unit,
    onDelete: (com.adonnis.app.data.local.entity.ReminderEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.AlarmAdd, contentDescription = null) },
        title = { Text("Upcoming Reminders") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (reminders.isEmpty()) {
                    Text(
                        text = "No upcoming reminders. Mention a future event in chat or diary " +
                            "(e.g. \"dentist at 3pm tomorrow\") and Adonnis will create one automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                reminders.forEach { reminder ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formatTime(reminder.dateTime),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (reminder.isAutoGenerated) {
                                    Text(
                                        text = "🤖 auto",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(onClick = { onDone(reminder.id) }) {
                                Text("Done")
                            }
                            IconButton(onClick = { onDelete(reminder) }) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

// ── Sleep Schedule Dialog ───────────────────────────────────────────────

@Composable
private fun SleepDialog(
    currentWakeUp: String,
    currentBed: String,
    currentHours: String,
    currentRoutine: String,
    onSave: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var wakeUp by remember { mutableStateOf(currentWakeUp) }
    var bed by remember { mutableStateOf(currentBed) }
    var hours by remember { mutableStateOf(currentHours) }
    var routine by remember { mutableStateOf(currentRoutine) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Bedtime, contentDescription = null) },
        title = { Text("Sleep Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = wakeUp,
                    onValueChange = { wakeUp = it },
                    label = { Text("Wake up time") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 7:00 AM") }
                )
                OutlinedTextField(
                    value = bed,
                    onValueChange = { bed = it },
                    label = { Text("Bedtime") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 11:00 PM") }
                )
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Hours of sleep needed") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 8") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = routine,
                    onValueChange = { routine = it },
                    label = { Text("Morning routine") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Gym, meditation, reading") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(wakeUp.trim(), bed.trim(), hours.trim(), routine.trim()) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
