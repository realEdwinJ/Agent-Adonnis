package com.adonnis.app.ui.alarm

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adonnis.app.AdonnisApplication
import com.adonnis.app.alarm.ChallengeState
import com.adonnis.app.ai.MathEquationEngine
import kotlin.math.sin
import kotlin.random.Random

/**
 * Full-screen math challenge alarm with BODMAS equations,
 * progression tracking, confetti celebration, and snooze.
 */
@Composable
fun AlarmChallengeScreen(
    alarmId: Long,
    equations: List<MathEquationEngine.Equation>,
    currentIndex: Int,
    solvedCount: Int,
    totalEquations: Int,
    snoozeCount: Int,
    isComplete: Boolean,
    isLoading: Boolean,
    onAnswerSubmitted: (String) -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    var answerText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))))
    ) {
        when {
            isLoading -> LoadingState()
            isComplete -> CompleteState(solvedCount, snoozeCount, onDismiss)
            else -> ChallengeState(
                equations = equations,
                currentIndex = currentIndex,
                solvedCount = solvedCount,
                totalEquations = totalEquations,
                snoozeCount = snoozeCount,
                answerText = answerText,
                onAnswerChange = { answerText = it; showError = false },
                onSubmit = {
                    if (answerText.isNotBlank()) {
                        val isCorrect = checkAnswer(equations, currentIndex, answerText)
                        if (isCorrect) {
                            onAnswerSubmitted(answerText)
                            answerText = ""
                            showError = false
                        } else {
                            showError = true
                        }
                    }
                },
                onSnooze = onSnooze,
                pulseScale = pulseScale
            )
        }
    }
}

// ── Check Answer ─────────────────────────────────────────────────────

private fun checkAnswer(
    equations: List<MathEquationEngine.Equation>,
    currentIndex: Int,
    answerText: String
): Boolean {
    val answer = answerText.trim().toIntOrNull() ?: return false
    return currentIndex < equations.size && answer == equations[currentIndex].answer
}

// ── Loading State ────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFE94560), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Loading challenge...", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

// ── Complete State ───────────────────────────────────────────────────

@Composable
private fun CompleteState(solvedCount: Int, snoozeCount: Int, onDismiss: () -> Unit) {
    // Confetti particles
    val particles = remember { generateConfettiParticles() }
    val animProgress = rememberInfiniteTransition(label = "confetti")
    particles.forEachIndexed { i, particle ->
        val offset by animProgress.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(particle.durationMs), RepeatMode.Restart),
            label = "confetti_$i"
        )
        particle.currentOffset = offset
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Confetti canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val x = p.startX + sin(p.currentOffset * Math.PI.toFloat() * p.wobbleFreq) * 100f
                val y = p.startY - p.currentOffset * size.height * 1.2f
                drawCircle(
                    color = p.color,
                    radius = p.size,
                    center = Offset(x, y),
                    alpha = (1f - p.currentOffset * 0.5f).coerceIn(0f, 1f)
                )
            }
        }

        // Content
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎉", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "YOU'RE AWAKE!",
                fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Solved $solvedCount equations",
                fontSize = 18.sp, color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Start Your Day! 🌅", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Self-contained overload for navigation destinations (NavGraph).
 * Manages its own [ChallengeState] internally, matching the expected
 * signature for alarm routes that are navigated to from AlarmScreen.
 */
@Composable
fun AlarmChallengeScreen(
    alarmId: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as AdonnisApplication
    val challengeState = remember(alarmId) { ChallengeState(alarmId, app) }

    val equations by challengeState.equations.collectAsState()
    val currentIndex by challengeState.currentIndex.collectAsState()
    val solvedCount by challengeState.solvedCount.collectAsState()
    val totalEquations by challengeState.totalEquations.collectAsState()
    val snoozeCount by challengeState.snoozeCount.collectAsState()
    val isComplete by challengeState.isComplete.collectAsState()
    val isLoading by challengeState.isLoading.collectAsState()

    AlarmChallengeScreen(
        alarmId = alarmId,
        equations = equations,
        currentIndex = currentIndex,
        solvedCount = solvedCount,
        totalEquations = totalEquations,
        snoozeCount = snoozeCount,
        isComplete = isComplete,
        isLoading = isLoading,
        onAnswerSubmitted = { answer -> challengeState.submitAnswer(answer) },
        onSnooze = { /* Snooze handled only from MathChallengeActivity */ },
        onDismiss = {
            challengeState.onMorningGreeting()
            onDismiss()
        }
    )
}

private data class ConfettiParticle(
    val startX: Float, val startY: Float,
    val color: Color, val size: Float,
    val durationMs: Int, val wobbleFreq: Float,
    var currentOffset: Float = 0f
)

private fun generateConfettiParticles(): List<ConfettiParticle> {
    val colors = listOf(Color(0xFFE94560), Color(0xFF2ECC71), Color(0xFF3498DB),
        Color(0xFFF1C40F), Color(0xFF9B59B6), Color(0xFF1ABC9C))
    return (0 until 40).map {
        ConfettiParticle(
            startX = Random.nextFloat() * 1000f,
            startY = 1000f,
            color = colors.random(),
            size = Random.nextFloat() * 8f + 4f,
            durationMs = Random.nextInt(1500, 3000),
            wobbleFreq = Random.nextFloat() * 3f + 1f
        )
    }
}

// ── Active Challenge State ───────────────────────────────────────────

@Composable
private fun ChallengeState(
    equations: List<MathEquationEngine.Equation>,
    currentIndex: Int,
    solvedCount: Int,
    totalEquations: Int,
    snoozeCount: Int,
    answerText: String,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSnooze: () -> Unit,
    pulseScale: Float
) {
    val progress = if (totalEquations > 0) solvedCount.toFloat() / totalEquations else 0f

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Wake up header
        Text("⏰", fontSize = 56.sp)
        Text(
            "WAKE UP!",
            fontSize = 42.sp, fontWeight = FontWeight.Bold,
            color = Color(0xFFE94560),
            modifier = Modifier.scale(pulseScale)
        )
        Spacer(Modifier.height(20.dp))

        // Progress
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp).background(
                Color.White.copy(alpha = 0.1f), RoundedCornerShape(5.dp)
            ),
            color = Color(0xFFE94560),
            trackColor = Color.Transparent
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$solvedCount / $totalEquations solved",
            color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp
        )
        Spacer(Modifier.height(32.dp))

        // Current equation card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Solve to dismiss", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (currentIndex < equations.size) "${equations[currentIndex].question} = ?" else "Complete!",
                    fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    color = Color.White, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))

                // Answer input + submit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = answerText,
                        onValueChange = onAnswerChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Answer", color = Color.White.copy(alpha = 0.3f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE94560),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        isError = answerText.isNotBlank() && answerText.toIntOrNull() == null,
                        supportingText = {
                            if (answerText.isNotBlank() && answerText.toIntOrNull() == null) {
                                Text("Enter a number", color = Color(0xFFE94560))
                            }
                        }
                    )
                    Button(
                        onClick = onSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94560)),
                        modifier = Modifier.height(56.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        Text("Submit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Snooze section
        val canSnooze = snoozeCount < 3
        if (canSnooze) {
            TextButton(onClick = onSnooze) {
                Text(
                    "Snooze 5 min ($snoozeCount/3)",
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            Text(
                "No more snoozes — solve to escape!",
                color = Color(0xFFE94560).copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}
