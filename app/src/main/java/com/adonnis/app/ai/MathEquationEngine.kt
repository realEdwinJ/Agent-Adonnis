package com.adonnis.app.ai

/**
 * Generates BODMAS math equations for the alarm challenge.
 * Uses the AI client when available, falls back to a hardcoded bank
 * so the alarm always works even without an API connection.
 */
object MathEquationEngine {

    data class Equation(val question: String, val answer: Int)

    /**
     * Get 10 equations at the given difficulty.
     * Primarily uses the AI client when [aiClient] is available,
     * but falls back to the hardcoded bank for reliability.
     */
    suspend fun generate(
        difficulty: String = "medium",
        aiClient: OpenRouterClient? = null
    ): List<Equation> {
        // Try the AI client first
        if (aiClient != null) {
            try {
                val result = aiClient.generateContent(
                    systemInstruction = "You are a math equation generator. Output ONLY valid JSON.",
                    prompt = Prompts.mathEquations(difficulty),
                    jsonMode = true
                )
                result.onSuccess { response ->
                    val parsed = ResponseParser.parseMathEquations(response)
                    parsed.onSuccess { equations ->
                        if (equations.size >= 10) {
                            return equations.map { Equation(it.question, it.answer) }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Fallback: hardcoded bank
        return fallbackEquations(difficulty).take(10)
    }

    private fun fallbackEquations(difficulty: String): List<Equation> = when (difficulty) {
        "easy" -> listOf(
            Equation("5 + 3 × 2", 11),
            Equation("12 - 4 ÷ 2", 10),
            Equation("3 × 4 + 6", 18),
            Equation("15 - 3 × 2", 9),
            Equation("8 ÷ 2 + 5", 9),
            Equation("7 + 6 - 3", 10),
            Equation("4 × 5 - 8", 12),
            Equation("20 - 6 ÷ 3", 18),
            Equation("9 + 7 - 4", 12),
            Equation("6 × 3 - 7", 11),
            Equation("14 ÷ 2 + 3", 10),
            Equation("5 × 4 - 6", 14)
        )
        "hard" -> listOf(
            Equation("12 + 8 × (6 - 2) ÷ 4", 20),
            Equation("(15 - 3) × 2 + 8 ÷ 4", 26),
            Equation("7 × 3 + 4² - 6", 31),
            Equation("(20 + 4) ÷ 6 × 5 - 3", 17),
            Equation("9 × 3 - 16 ÷ 4 + 5", 28),
            Equation("5² - 3 × 4 + 8 ÷ 2", 17),
            Equation("(18 - 6) ÷ 3 × 5 + 2", 22),
            Equation("8 × 4 - 12 ÷ 3 + 7", 36),
            Equation("6² ÷ 4 + 5 × 3 - 2", 22),
            Equation("(30 - 5) ÷ 5 × 4 + 3", 23),
            Equation("14 + 6 × (8 - 3) ÷ 2", 29),
            Equation("4 × 7 - 18 ÷ 3 + 2²", 28)
        )
        else -> listOf( // medium
            Equation("12 + 8 × 3 - 4", 32),
            Equation("(15 - 6) × 2 + 5", 23),
            Equation("7 × 4 - 18 ÷ 3", 22),
            Equation("20 - 3 × 4 + 6", 14),
            Equation("(8 + 4) × 3 - 10", 26),
            Equation("9 × 3 - 15 ÷ 5", 24),
            Equation("25 - (4 + 6) × 2", 5),
            Equation("6 × 5 - 12 ÷ 4", 27),
            Equation("(18 + 6) ÷ 4 × 3", 18),
            Equation("30 - 4 × 5 + 8", 18),
            Equation("14 - 2 × 3 + 7", 15),
            Equation("4 × 6 - 18 ÷ 2", 15)
        )
    }
}
