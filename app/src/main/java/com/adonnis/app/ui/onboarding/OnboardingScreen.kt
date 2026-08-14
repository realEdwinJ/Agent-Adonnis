package com.adonnis.app.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adonnis.app.ai.Prompts
import com.adonnis.app.ui.components.GradientButton

/**
 * 5-step animated onboarding flow with full validation, OpenRouter API key
 * testing, paste-in timetable description, module difficulty ratings,
 * and data persistence via OnboardingViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            OnboardingTopBar(
                currentStep = viewModel.currentStep,
                totalSteps = viewModel.totalSteps,
                onBack = { viewModel.previousStep() }
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                currentStep = viewModel.currentStep,
                totalSteps = viewModel.totalSteps,
                isStepValid = viewModel.isStepValid(),
                isTestingKey = viewModel.isTestingKey,
                onNext = {
                    if (viewModel.currentStep < viewModel.totalSteps - 1) {
                        viewModel.nextStep()
                    } else {
                        viewModel.saveAndComplete()
                        onComplete()
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = viewModel.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "step_transition",
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp)
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        0 -> ApiKeyStep(viewModel)
                        1 -> NameStep(viewModel)
                        2 -> TimetableStep(viewModel)
                        3 -> GoalsStep(viewModel)
                        4 -> ConfirmationStep(viewModel)
                    }
                }
            }
        }
    }
}

// ── Top Bar ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingTopBar(
    currentStep: Int,
    totalSteps: Int,
    onBack: () -> Unit
) {
    TopAppBar(
        title = { Text("Set Up Your Assistant") },
        navigationIcon = {
            if (currentStep > 0) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            // Step indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(end = 16.dp)
            ) {
                for (i in 0 until totalSteps) {
                    Box(
                        modifier = Modifier
                            .size(if (i == currentStep) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (i <= currentStep)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
    )
}

// ── Bottom Bar ──────────────────────────────────────────────────────────

@Composable
private fun OnboardingBottomBar(
    currentStep: Int,
    totalSteps: Int,
    isStepValid: Boolean,
    isTestingKey: Boolean,
    onNext: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step ${currentStep + 1} of $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            GradientButton(
                onClick = onNext,
                enabled = isStepValid && !isTestingKey
            ) {
                if (isTestingKey) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (currentStep == totalSteps - 1) "Let's Go!"
                    else if (currentStep == 0) "Test & Next"
                    else "Next"
                )
            }
        }
    }
}

// ── Step 0: API Key (OpenRouter) ────────────────────────────────────────

@Composable
private fun ApiKeyStep(viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        Text("🔑", style = MaterialTheme.typography.displayMedium)

        Text(
            text = "Enter Your OpenRouter API Key",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "One key for hundreds of AI models (OpenAI, Claude, DeepSeek and more).\nYour key is stored encrypted on your device and is never shared.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = viewModel.apiKey,
            onValueChange = {
                viewModel.apiKey = it
                viewModel.apiKeyValid = false
                viewModel.apiKeyError = null
            },
            label = { Text("OpenRouter API Key") },
            placeholder = { Text("sk-or-v1-...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = viewModel.apiKeyError != null,
            supportingText = {
                if (viewModel.apiKeyError != null) {
                    Text(
                        text = viewModel.apiKeyError!!,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (viewModel.apiKeyValid) {
                    Text(
                        text = "✓ Key is valid!",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.testApiKey() }),
            trailingIcon = {
                if (viewModel.apiKeyValid) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Valid",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        // Test button (shows when API key is entered but not tested)
        if (viewModel.apiKey.isNotBlank() && !viewModel.apiKeyValid) {
            OutlinedButton(
                onClick = { viewModel.testApiKey() },
                enabled = !viewModel.isTestingKey,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.isTestingKey) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Testing...")
                } else {
                    Icon(Icons.Outlined.NetworkCheck, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Test Connection")
                }
            }
        }

        Text(
            text = "Get a free key at openrouter.ai/keys",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Step 1: Names ───────────────────────────────────────────────────────

@Composable
private fun NameStep(viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        Text("👤", style = MaterialTheme.typography.displayMedium)

        Text(
            text = "Tell Me About Yourself",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = viewModel.userName,
            onValueChange = { viewModel.userName = it },
            label = { Text("Your Name") },
            placeholder = { Text("e.g., Alex") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = viewModel.userNameError != null,
            supportingText = { viewModel.userNameError?.let { Text(it) } },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = viewModel.agentName,
            onValueChange = { viewModel.agentName = it },
            label = { Text("Assistant Name") },
            placeholder = { Text("e.g., Adonnis") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = viewModel.agentNameError != null,
            supportingText = {
                if (viewModel.agentNameError != null) {
                    Text(viewModel.agentNameError!!)
                } else {
                    Text("This is what your AI assistant will be called")
                }
            },
            leadingIcon = { Icon(Icons.Outlined.SmartToy, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        // Preview card
        if (viewModel.userName.isNotBlank() && viewModel.agentName.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Nice to meet you, ${viewModel.userName}!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "I am ${viewModel.agentName}, your AI life planner.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// ── Step 2: Timetable (paste AI-generated description) ──────────────────

@Composable
private fun TimetableStep(viewModel: OnboardingViewModel) {
    val clipboard = LocalClipboardManager.current
    var copiedPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(copiedPrompt) {
        if (copiedPrompt) {
            kotlinx.coroutines.delay(2000)
            copiedPrompt = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Text("📚", style = MaterialTheme.typography.displayMedium)

        Text(
            text = "Your School Timetable",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Use any AI chatbot to turn your timetable photo into text, then paste it here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // ── How it works ─────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InstructionStep(1, "Open ChatGPT, Claude or Gemini on your computer (free is fine).")
                InstructionStep(2, "Upload a photo of your timetable and paste the prompt below into it.")
                InstructionStep(3, "Copy the AI's text description and paste it into the box at the bottom.")
            }
        }

        // ── Copyable prompt ──────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prompt to give the other AI",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(Prompts.TIMETABLE_DESCRIPTION_PROMPT))
                            copiedPrompt = true
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            if (copiedPrompt) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (copiedPrompt) "Copied!" else "Copy")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = Prompts.TIMETABLE_DESCRIPTION_PROMPT,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // ── Paste area ───────────────────────────────────────────
        OutlinedTextField(
            value = viewModel.timetableText,
            onValueChange = { viewModel.timetableText = it },
            label = { Text("Paste the AI's timetable description") },
            placeholder = {
                Text(
                    "Monday 09:00-10:00 Mathematics Room 12\n" +
                    "Monday 10:30-12:00 Physics Lab 3\n" +
                    "Tuesday 09:00-11:00 English...\n" +
                    "(one line per class)"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            maxLines = 10
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── Module difficulty ratings ────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rate Your Modules by Difficulty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.addModuleDifficulty() }) {
                Icon(Icons.Filled.AddCircle, contentDescription = "Add module", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Text(
            text = "Rank your modules from hardest to easiest — 1 = your hardest module, 2 = second hardest, and so on. This helps me schedule study time when you're most focused.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (viewModel.moduleDifficulties.isEmpty()) {
            TextButton(onClick = { viewModel.addModuleDifficulty() }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add a module")
            }
        }

        viewModel.moduleDifficulties.forEachIndexed { index, module ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = module.name,
                        onValueChange = { viewModel.updateModuleDifficultyName(index, it) },
                        label = { Text("Module") },
                        placeholder = { Text("e.g. Advanced Math") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = module.rankText,
                        onValueChange = { viewModel.updateModuleDifficultyRank(index, it) },
                        label = { Text("Rank") },
                        placeholder = { Text("1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(88.dp)
                    )
                    IconButton(onClick = { viewModel.removeModuleDifficulty(index) }) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (viewModel.moduleDifficulties.isNotEmpty()) {
            TextButton(onClick = { viewModel.addModuleDifficulty() }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add another module")
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
            modifier = Modifier.size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Step 3: Goals & Sleep ───────────────────────────────────────────────

@Composable
private fun GoalsStep(viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        Text("🎯", style = MaterialTheme.typography.displayMedium)

        Text(
            text = "Your Goals & Schedule",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "What are your biggest goals right now?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = viewModel.goal1,
            onValueChange = { viewModel.goal1 = it },
            label = { Text("Goal #1") },
            placeholder = { Text("e.g., Get an A in Math") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.EmojiEvents, contentDescription = null) }
        )
        OutlinedTextField(
            value = viewModel.goal2,
            onValueChange = { viewModel.goal2 = it },
            label = { Text("Goal #2 (optional)") },
            placeholder = { Text("e.g., Learn guitar") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.TrackChanges, contentDescription = null) }
        )
        OutlinedTextField(
            value = viewModel.goal3,
            onValueChange = { viewModel.goal3 = it },
            label = { Text("Goal #3 (optional)") },
            placeholder = { Text("e.g., Run 3x per week") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Stars, contentDescription = null) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Sleep Schedule",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.wakeUpTime,
                onValueChange = { viewModel.wakeUpTime = it },
                label = { Text("Wake up") },
                placeholder = { Text("6:30 AM") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.WbSunny, contentDescription = null) }
            )
            OutlinedTextField(
                value = viewModel.bedTime,
                onValueChange = { viewModel.bedTime = it },
                label = { Text("Bedtime") },
                placeholder = { Text("10:00 PM") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.NightsStay, contentDescription = null) }
            )
        }

        OutlinedTextField(
            value = viewModel.subjects,
            onValueChange = { viewModel.subjects = it },
            label = { Text("Subjects / Classes") },
            placeholder = { Text("e.g., Math, English, Science") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.School, contentDescription = null) }
        )
    }
}

// ── Step 4: Confirmation ────────────────────────────────────────────────

@Composable
private fun ConfirmationStep(viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Text("🎉", style = MaterialTheme.typography.displayMedium)

        Text(
            text = "Almost Ready!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Here's everything I've set up for you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryRow(icon = "🔑", label = "API Key", value = "✓ Connected")
                HorizontalDivider()
                SummaryRow(icon = "👤", label = "Your Name", value = viewModel.userName)
                HorizontalDivider()
                SummaryRow(icon = "🤖", label = "Assistant", value = viewModel.agentName)
                HorizontalDivider()
                SummaryRow(
                    icon = "📚",
                    label = "Timetable",
                    value = "✓ ${viewModel.timetableText.lines().size} lines"
                )
                HorizontalDivider()
                SummaryRow(
                    icon = "📊",
                    label = "Module Difficulty",
                    value = if (viewModel.moduleDifficulties.any { it.name.isNotBlank() })
                        "${viewModel.moduleDifficulties.count { it.name.isNotBlank() }} rated"
                    else "Not set"
                )
                HorizontalDivider()
                SummaryRow(
                    icon = "🎯",
                    label = "Goals",
                    value = "${viewModel.getGoalsList().size} set"
                )
                if (viewModel.wakeUpTime.isNotBlank() || viewModel.bedTime.isNotBlank()) {
                    HorizontalDivider()
                    SummaryRow(
                        icon = "😴",
                        label = "Sleep",
                        value = "${viewModel.wakeUpTime} → ${viewModel.bedTime}"
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "What happens next?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${viewModel.agentName} will analyze your timetable and start planning your days. You can chat, set alarms, and build a routine together!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SummaryRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
