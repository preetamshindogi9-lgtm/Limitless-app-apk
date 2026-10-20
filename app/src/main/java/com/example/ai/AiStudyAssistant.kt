package com.example.ai

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Calendar
import java.util.Locale

data class AiSummaryResult(
    val summary: String,
    val revisionPoints: List<String>,
    val practiceQuestions: List<QuizQuestion>,
    val keywords: List<String>
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class StudyRecommendation(
    val title: String,
    val message: String,
    val recommendedFocusMinutes: Int,
    val suggestedAppLimitChanges: String,
    val urgencyLevel: UrgencyLevel
)

enum class UrgencyLevel {
    NORMAL,
    RECOMMENDED,
    HIGH_ALERT
}

class AiStudyAssistant(private val context: Context) {

    private val generativeModel by lazy {
        try {
            Firebase.ai.generativeModel(
                modelName = "gemini-3.8-flash",
                generationConfig = generationConfig {
                    temperature = 0.4f
                }
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun summarizeAndExtract(
        title: String,
        content: String,
        subject: String
    ): AiSummaryResult = withContext(Dispatchers.IO) {
        if (content.isBlank()) {
            return@withContext AiSummaryResult(
                summary = "Empty note provided.",
                revisionPoints = emptyList(),
                practiceQuestions = emptyList(),
                keywords = emptyList()
            )
        }

        // Try AI generation if model is accessible
        try {
            val model = generativeModel
            if (model != null) {
                val prompt = """
                    You are an expert academic study assistant for a university student.
                    Subject: $subject
                    Topic: $title
                    Material:
                    $content

                    Provide a concise summary (max 3 sentences), followed by 3-5 bulleted revision points, and 2 multiple-choice practice questions with 4 options and the correct answer index (0-3).
                    Format clearly with sections:
                    [SUMMARY]
                    (summary text)
                    [REVISION_POINTS]
                    - point 1
                    - point 2
                    [PRACTICE_QUESTIONS]
                    Q: question text
                    A: opt0 | opt1 | opt2 | opt3
                    C: 0
                    E: explanation
                """.trimIndent()

                val response = withTimeout(15_000L) {
                    model.generateContent(prompt)
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    val parsed = parseAiResponse(text, content)
                    if (parsed.summary.isNotBlank()) {
                        return@withContext parsed
                    }
                }
            }
        } catch (_: Exception) {
            // Gracefully fall back to local rule-based NLP extraction
        }

        // Local Rule-Based NLP & Concept Extraction
        generateLocalNlpSummary(title, content, subject)
    }

    private fun parseAiResponse(aiText: String, originalContent: String): AiSummaryResult {
        var summary = ""
        val revisionPoints = mutableListOf<String>()
        val questions = mutableListOf<QuizQuestion>()

        val lines = aiText.lines()
        var currentSection = ""

        var currentQ = ""
        var currentOpts = listOf<String>()
        var currentCorrect = 0

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.startsWith("[SUMMARY]")) {
                currentSection = "SUMMARY"
                continue
            } else if (line.startsWith("[REVISION_POINTS]")) {
                currentSection = "REVISION"
                continue
            } else if (line.startsWith("[PRACTICE_QUESTIONS]")) {
                currentSection = "QUESTIONS"
                continue
            }

            when (currentSection) {
                "SUMMARY" -> {
                    if (line.isNotBlank()) {
                        summary = if (summary.isEmpty()) line else "$summary $line"
                    }
                }
                "REVISION" -> {
                    if (line.startsWith("-") || line.startsWith("•") || line.startsWith("*")) {
                        revisionPoints.add(line.trimStart('-', '•', '*', ' '))
                    } else if (line.isNotBlank()) {
                        revisionPoints.add(line)
                    }
                }
                "QUESTIONS" -> {
                    if (line.startsWith("Q:")) {
                        currentQ = line.removePrefix("Q:").trim()
                    } else if (line.startsWith("A:")) {
                        currentOpts = line.removePrefix("A:").split("|").map { it.trim() }
                    } else if (line.startsWith("C:")) {
                        currentCorrect = line.removePrefix("C:").trim().toIntOrNull() ?: 0
                    } else if (line.startsWith("E:") && currentQ.isNotBlank()) {
                        val exp = line.removePrefix("E:").trim()
                        if (currentOpts.size >= 2) {
                            questions.add(
                                QuizQuestion(
                                    question = currentQ,
                                    options = currentOpts,
                                    correctIndex = currentCorrect.coerceIn(0, currentOpts.size - 1),
                                    explanation = exp
                                )
                            )
                        }
                        currentQ = ""
                    }
                }
            }
        }

        val keywords = extractKeywords(originalContent)
        if (summary.isBlank()) {
            summary = originalContent.take(180) + "..."
        }
        return AiSummaryResult(
            summary = summary,
            revisionPoints = if (revisionPoints.isNotEmpty()) revisionPoints else extractKeySentences(originalContent),
            practiceQuestions = if (questions.isNotEmpty()) questions else generateFallbackQuestions(originalContent),
            keywords = keywords
        )
    }

    private fun generateLocalNlpSummary(title: String, content: String, subject: String): AiSummaryResult {
        val sentences = content.split(Regex("[.!?]\\s+")).filter { it.isNotBlank() }
        val keywords = extractKeywords(content)

        // Select top 2 most informative sentences
        val summaryText = if (sentences.size <= 2) {
            content.trim()
        } else {
            val scored = sentences.map { s ->
                val score = keywords.count { kw -> s.contains(kw, ignoreCase = true) }
                Pair(s, score)
            }.sortedByDescending { it.second }
            "${scored.firstOrNull()?.first ?: sentences[0]}. ${scored.getOrNull(1)?.first ?: sentences[1]}."
        }

        // Generate Revision Points
        val revisionPoints = mutableListOf<String>()
        sentences.take(5).forEach { s ->
            val cleaned = s.trim().replace("\n", " ")
            if (cleaned.length in 15..160) {
                revisionPoints.add("Key concept: $cleaned")
            }
        }
        if (revisionPoints.isEmpty()) {
            revisionPoints.add("Review the primary definitions and formulas in $title.")
            revisionPoints.add("Practice active recall by testing yourself on: ${keywords.take(4).joinToString(", ")}.")
        }

        // Practice Questions
        val questions = generateFallbackQuestions(content)

        return AiSummaryResult(
            summary = summaryText,
            revisionPoints = revisionPoints,
            practiceQuestions = questions,
            keywords = keywords
        )
    }

    private fun extractKeywords(text: String): List<String> {
        val stopWords = setOf(
            "the", "and", "is", "in", "to", "of", "a", "that", "it", "with",
            "as", "for", "on", "was", "at", "by", "an", "be", "this", "which",
            "or", "from", "are", "have", "has", "not", "but", "can", "they",
            "we", "you", "use", "when", "into", "their", "more", "each"
        )
        val words = text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 3 && it !in stopWords }

        val frequency = mutableMapOf<String, Int>()
        for (w in words) {
            frequency[w] = (frequency[w] ?: 0) + 1
        }
        return frequency.entries
            .sortedByDescending { it.value }
            .take(6)
            .map { it.key.replaceFirstChar { char -> char.uppercase() } }
    }

    private fun extractKeySentences(content: String): List<String> {
        return content.split("\n")
            .filter { it.trim().length in 10..150 }
            .take(4)
    }

    private fun generateFallbackQuestions(content: String): List<QuizQuestion> {
        val keywords = extractKeywords(content)
        val q1 = if (keywords.isNotEmpty()) {
            QuizQuestion(
                question = "What is the primary topic addressed in this note?",
                options = listOf(
                    keywords.first(),
                    "Unrelated General Theory",
                    "Network Architecture",
                    "Historical Analysis"
                ),
                correctIndex = 0,
                explanation = "The notes emphasize ${keywords.first()} as a central academic topic."
            )
        } else {
            QuizQuestion(
                question = "What is the optimal study method for these concepts?",
                options = listOf(
                    "Spaced repetition & practice problem solving",
                    "Passive re-reading before exam",
                    "Ignoring definitions",
                    "Memorizing without understanding"
                ),
                correctIndex = 0,
                explanation = "Spaced repetition and active problem-solving provide the highest retention."
            )
        }

        val q2 = QuizQuestion(
            question = "Which strategy minimizes distraction during study sessions?",
            options = listOf(
                "Leaving social media notifications on",
                "Setting strict app time limits & using Focus Shield",
                "Studying while watching short video clips",
                "Skipping breaks entirely"
            ),
            correctIndex = 1,
            explanation = "Pre-committing to app limits and using focused study intervals prevents cognitive switching."
        )

        return listOf(q1, q2)
    }

    /**
     * Predictive Analytics & Study Recommendations
     * Analyzes time of day, total screen time, app limits exceeded, and study minutes.
     */
    fun analyzeUsageAndRecommend(
        usedMinutes: Int,
        totalLimitMinutes: Int,
        studyMinutesToday: Int,
        exceededAppsCount: Int
    ): StudyRecommendation {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // Predictive peak distraction window (Evening 6:00 PM - 10:00 PM)
        val isPeakDistractionHours = hour in 18..22

        return when {
            exceededAppsCount > 0 -> {
                StudyRecommendation(
                    title = "Limit Exceeded: Focus Shield Activated",
                    message = "You reached your daily quota on $exceededAppsCount app(s). Switch gears to revision notes or a quick 20-minute focus session.",
                    recommendedFocusMinutes = 20,
                    suggestedAppLimitChanges = "Keep your limit locked until tomorrow morning.",
                    urgencyLevel = UrgencyLevel.HIGH_ALERT
                )
            }
            isPeakDistractionHours && usedMinutes > (totalLimitMinutes * 0.7) -> {
                StudyRecommendation(
                    title = "Evening Distraction Prediction Alert",
                    message = "Usage patterns show peak social media browsing between 7:00 PM and 9:30 PM. We recommend starting your study block now.",
                    recommendedFocusMinutes = 35,
                    suggestedAppLimitChanges = "Tighten YouTube & Instagram limits by 10 minutes tonight.",
                    urgencyLevel = UrgencyLevel.RECOMMENDED
                )
            }
            studyMinutesToday < 30 -> {
                StudyRecommendation(
                    title = "Daily Study Target Incomplete",
                    message = "You have logged $studyMinutesToday mins of study today. Try a short 25-minute Pomodoro block to maintain your streak.",
                    recommendedFocusMinutes = 25,
                    suggestedAppLimitChanges = "Maintain active limits.",
                    urgencyLevel = UrgencyLevel.NORMAL
                )
            }
            else -> {
                StudyRecommendation(
                    title = "Optimal Productivity Balance",
                    message = "Great work! You have logged $studyMinutesToday mins of study and kept screen time under control.",
                    recommendedFocusMinutes = 15,
                    suggestedAppLimitChanges = "Current limits are working effectively.",
                    urgencyLevel = UrgencyLevel.NORMAL
                )
            }
        }
    }
}
