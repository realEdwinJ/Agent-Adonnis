package com.adonnis.app.ui.planner

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adonnis.app.ui.components.GradientButton

/**
 * 3-day rolling planner with AI-generated schedule, color-coded timeline,
 * day statistics, and tap-to-expand block details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    viewModel: PlannerViewModel = viewModel()
) {
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val blocks by viewModel.currentBlocks.collectAsStateWithLifecycle()
    val stats by viewModel.dayStats.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generationError by viewModel.generationError.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("3-Day Plan", style = MaterialTheme.typography.titleMedium)
                        if (isGenerating) {
                            Text(
                                "Generating...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.generatePlans() },
                        enabled = !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Regenerate")
                        }
                    }
                }
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Day Tabs ──────────────────────────────────────────────
            TabRow(selectedTabIndex = selectedDay) {
                viewModel.dayLabels.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedDay == index,
                        onClick = { viewModel.selectDay(index) },
                        text = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(label, fontWeight = FontWeight.Medium)
                                Text(
                                    text = viewModel.detailLabels[index],
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────
            if (plans.isEmpty() && !isGenerating) {
                EmptyPlannerState(onGenerate = { viewModel.generatePlans() })
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // Error banner
                    if (generationError != null) {
                        item(key = "error") {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
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
                                        text = generationError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.generatePlans() }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }

                    // Summary card
                    if (blocks.isNotEmpty()) {
                        item(key = "summary") {
                            DaySummaryCard(stats, viewModel)
                        }
                    }

                    // Time blocks
                    if (blocks.isEmpty() && !isGenerating) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No plan for this day yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Tap refresh to generate a new plan",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else {
                        items(blocks, key = { "${selectedDay}_${it.timeRange}_${it.title}" }) { block ->
                            TimeBlockCard(block = block)
                        }
                    }

                    // Bottom spacer
                    item(key = "spacer") { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Day Summary Card ────────────────────────────────────────────────────

@Composable
private fun DaySummaryCard(stats: PlannerViewModel.DayStats, viewModel: PlannerViewModel) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Day Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = viewModel.formatHours(stats.totalMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("📚 Study", viewModel.formatHours(stats.studyMinutes))
                StatItem("☕ Break", viewModel.formatHours(stats.breakMinutes))
                StatItem("🍽️ Meals", viewModel.formatHours(stats.mealMinutes))
                StatItem("💪 Exercise", viewModel.formatHours(stats.exerciseMinutes))
                StatItem("😴 Sleep", viewModel.formatHours(stats.sleepMinutes))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label.substringAfter(" "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

// ── Time Block Card ─────────────────────────────────────────────────────

@Composable
private fun TimeBlockCard(block: TimeBlockUi) {
    var expanded by remember { mutableStateOf(false) }
    val bgColor = if (block.color == Color.Unspecified) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        block.color.copy(alpha = 0.15f)
    }
    val textColor = if (block.color == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        block.color
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = textColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = block.timeRange,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Duration badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = textColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${block.durationMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(textColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Expandable note
            AnimatedVisibility(visible = expanded && block.note != null) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = block.note ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ── Empty State ─────────────────────────────────────────────────────────

@Composable
private fun EmptyPlannerState(onGenerate: () -> Unit) {
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
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No Plans Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your AI will create a personalized 3-day plan\nbased on your timetable and goals.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            GradientButton(onClick = onGenerate) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Generate My First Plan")
            }
        }
    }
}
