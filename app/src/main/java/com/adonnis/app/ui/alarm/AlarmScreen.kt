package com.adonnis.app.ui.alarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adonnis.app.AdonnisApplication
import com.adonnis.app.data.local.entity.AlarmEntity
import com.adonnis.app.ui.components.GradientButton
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * Alarm setup screen with full configuration:
 * - Time picker (24h)
 * - Day-of-week selector (Sun–Sat chips)
 * - Custom label
 * - Math challenge difficulty (Easy / Medium / Hard)
 * - Strict mode toggle
 * - List of saved alarms
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    onBack: () -> Unit,
    onAlarmTriggered: (Long) -> Unit = {},
    bottomBar: @Composable () -> Unit = {}
) {
    // ── New alarm form state ─────────────────────────────────────────
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableIntStateOf(6) }
    var selectedMinute by remember { mutableIntStateOf(30) }
    var alarmLabel by remember { mutableStateOf("Wake Up!") }
    var selectedDays by remember { mutableStateOf(listOf(1, 2, 3, 4, 5)) } // weekdays
    var mathDifficulty by remember { mutableIntStateOf(1) } // 0=easy, 1=medium, 2=hard
    var strictMode by remember { mutableStateOf(true) }

    // ── Persisted alarms from Room ──────────────────────────────────
    val context = LocalContext.current
    val app = context.applicationContext as AdonnisApplication
    val alarmRepository = app.alarmRepository
    val scope = rememberCoroutineScope()
    val savedAlarms by alarmRepository.getAllAlarms().collectAsStateWithLifecycle(initialValue = emptyList())

    // ── Edit state ──────────────────────────────────────────────────
    // Null = creating a new alarm; non-null = editing an existing one.
    var editingAlarmId by remember { mutableStateOf<Long?>(null) }
    // Preserve the original creation time when editing so an update
    // doesn't clobber it with a fresh timestamp.
    var editingCreatedAt by remember { mutableStateOf(0L) }
    // Prevents duplicate alarms when the save button is double-tapped.
    var isSaving by remember { mutableStateOf(false) }

    // Android 13+ notification permission, requested when saving an alarm.
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // ── Time picker dialog ───────────────────────────────────────────
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onConfirm = { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarms") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── Header ───────────────────────────────────────────────
            item(key = "header") {
                Column {
                    Text(
                        text = "Wake-Up Alarms",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Set alarms with a math challenge — 10 BODMAS equations to prove you're awake.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Time Picker ──────────────────────────────────────────
            item(key = "time") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showTimePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Alarm Time",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("%02d:%02d", selectedHour, selectedMinute),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            Icons.Outlined.AccessTime,
                            contentDescription = "Change time",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── Alarm Label ──────────────────────────────────────────
            item(key = "label") {
                OutlinedTextField(
                    value = alarmLabel,
                    onValueChange = { alarmLabel = it.take(30) },
                    label = { Text("Alarm Label") },
                    placeholder = { Text("e.g., Wake Up!") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Label, contentDescription = null) }
                )
            }

            // ── Day of Week ──────────────────────────────────────────
            item(key = "days") {
                Text(
                    text = "Repeat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    dayNames.forEachIndexed { index, name ->
                        val isSelected = selectedDays.contains(index)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) {
                                    selectedDays - index
                                } else {
                                    selectedDays + index
                                }
                            },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Quick presets
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(
                        onClick = { selectedDays = listOf(1, 2, 3, 4, 5) },
                        label = { Text("Weekdays") }
                    )
                    SuggestionChip(
                        onClick = { selectedDays = listOf(0, 6) },
                        label = { Text("Weekends") }
                    )
                    SuggestionChip(
                        onClick = { selectedDays = (0..6).toList() },
                        label = { Text("Every Day") }
                    )
                }
            }

            // ── Math Difficulty ──────────────────────────────────────
            item(key = "difficulty") {
                Text(
                    text = "⚡ Math Challenge",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("Easy", "Medium", "Hard").forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = mathDifficulty == index,
                            onClick = { mathDifficulty = index },
                            shape = when (index) {
                                0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                                listOf("Easy", "Medium", "Hard").lastIndex -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                                else -> RoundedCornerShape(0.dp)
                            }
                        ) {
                            Text(label)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when (mathDifficulty) {
                        0 -> "Simple 2-operator equations (numbers 1-20)"
                        1 -> "2-3 operator BODMAS with brackets (numbers 1-50)"
                        2 -> "3+ operators, nested brackets, exponents (numbers 1-100)"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Strict Mode ──────────────────────────────────────────
            item(key = "strict") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Strict Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Alarm won't stop until all 10 equations are solved correctly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = strictMode, onCheckedChange = { strictMode = it })
                    }
                }
            }

            // ── Save / Update Button (duplicate-safe) ────────────────
            item(key = "save") {
                GradientButton(
                    onClick = {
                        if (isSaving) return@GradientButton // guard: no double taps
                        isSaving = true
                        // Ask for notification permission before creating the
                        // alarm so its trigger can actually show a notification.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        val daysJson = JSONArray(selectedDays).toString()
                        val difficulty = when (mathDifficulty) {
                            0 -> "easy"
                            1 -> "medium"
                            2 -> "hard"
                            else -> "medium"
                        }
                        val alarm = AlarmEntity(
                            id = editingAlarmId ?: 0L,
                            label = alarmLabel,
                            hour = selectedHour,
                            minute = selectedMinute,
                            daysOfWeekJson = daysJson,
                            mathDifficulty = difficulty,
                            mode = if (strictMode) "strict" else "normal",
                            // Preserve the original timestamp only when
                            // editing; new alarms get the current time.
                            createdAt = if (editingAlarmId != null) editingCreatedAt
                            else System.currentTimeMillis()
                        )
                        scope.launch {
                            try {
                                if (editingAlarmId != null) {
                                    alarmRepository.updateAlarm(alarm)
                                } else {
                                    alarmRepository.createAlarm(alarm)
                                }
                            } finally {
                                // Always reset, even on error, so the button
                                // never gets stuck disabled.
                                editingAlarmId = null
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    Icon(
                        if (editingAlarmId != null) Icons.Filled.Check else Icons.Filled.AddAlarm,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (editingAlarmId != null) "Update Alarm" else "Save Alarm")
                }
                if (editingAlarmId != null) {
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(
                            onClick = {
                                editingAlarmId = null
                                // Restore form defaults for a new alarm
                                selectedHour = 6; selectedMinute = 30
                                alarmLabel = "Wake Up!"
                                selectedDays = listOf(1, 2, 3, 4, 5)
                                mathDifficulty = 1; strictMode = true
                            }
                        ) {
                            Text("Cancel editing")
                        }
                    }
                }
            }

            // ── Saved Alarms List ────────────────────────────────────
            if (savedAlarms.isNotEmpty()) {
                item(key = "saved_header") {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Saved Alarms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(savedAlarms.reversed(), key = { it.id }) { alarm ->
                    SavedAlarmCard(
                        alarm = alarm,
                        onToggle = {
                            scope.launch {
                                alarmRepository.setAlarmEnabled(alarm.id, !alarm.isEnabled)
                            }
                        },
                        onEdit = {
                            // Load this alarm into the form for editing
                            editingAlarmId = alarm.id
                            editingCreatedAt = alarm.createdAt
                            selectedHour = alarm.hour
                            selectedMinute = alarm.minute
                            alarmLabel = alarm.label
                            selectedDays = parseDays(alarm.daysOfWeekJson)
                            mathDifficulty = when (alarm.mathDifficulty) {
                                "easy" -> 0
                                "hard" -> 2
                                else -> 1
                            }
                            strictMode = alarm.mode == "strict"
                        },
                        onDelete = {
                            scope.launch {
                                alarmRepository.deleteAlarm(alarm)
                            }
                        }
                    )
                }
            }

            // ── Bottom Spacer ────────────────────────────────────────
            item(key = "spacer") { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Time Picker Dialog ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timeState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time", fontWeight = FontWeight.Bold) },
        text = {
            TimePicker(
                state = timeState,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(timeState.hour, timeState.minute)
            }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Saved Alarm Card ───────────────────────────────────────────────────

@Composable
private fun SavedAlarmCard(
    alarm: AlarmEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text = buildString {
                        append(alarm.label)
                        append(" · ")
                        append(daysToLabel(alarm.daysOfWeekJson))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Edit + delete actions
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit alarm",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete alarm",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "⚡ ${alarm.mathDifficulty.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (alarm.mode == "strict") {
                    Text(
                        text = "🔒 Strict",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun parseDays(json: String): List<Int> = try {
    val arr = JSONArray(json)
    (0 until arr.length()).map { arr.getInt(it) }
} catch (_: Exception) {
    emptyList()
}

private fun daysToLabel(daysJson: String): String {
    return try {
        val arr = JSONArray(daysJson)
        val names = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val selected = (0 until arr.length()).map { arr.getInt(it) }
        if (selected.size == 7) "Every day"
        else if (selected == listOf(1, 2, 3, 4, 5)) "Weekdays"
        else if (selected == listOf(0, 6)) "Weekends"
        else selected.sorted().map { names[it] }.joinToString(", ")
    } catch (_: Exception) { "Once" }
}
