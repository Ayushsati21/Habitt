package com.example.habit.data.ai

import com.example.habit.data.HabitLibrary
import com.example.habit.data.model.HabitCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Calls Anthropic Messages API. Pass an empty API key to skip network calls (app uses local keyword analysis).
 *
 * Docs: https://docs.anthropic.com/en/api/messages
 */
class ClaudeHabitAnalyzer(
    private val apiKey: String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyze(routineDescription: String): Result<List<AiHabitDetection>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No API key"))
        }
        if (routineDescription.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        runCatching {
            val libraryIndex = HabitLibrary.all.joinToString("\n") { "${it.id} — ${it.name} (${it.category.name})" }
            val system = """
                You are a health behaviour analyst for the Habit app. The user describes their daily routine.
                Identify harmful or risky habits, including very small "micro-habits" (sleep posture, mouth breathing,
                fast eating, distracted eating, slouching, jaw clenching, etc.) when clearly implied.
                For each habit, return one JSON object in a JSON ARRAY with keys:
                habit_name, category, severity (1-3), personalised_harm (short, references their wording),
                quit_benefit (one practical sentence), confidence (0.0-1.0), existing_library_match (string id or null).
                Categories must be one of: Diet, Digital, Substance, Sleep, Physical, Social, Mental, Financial.
                Only include items with confidence >= 0.6.
                Map to an existing_library_match id when it clearly fits this catalog (use exact id or null for custom):
                $libraryIndex
                Return JSON only — no markdown fences, no commentary. Max 8 items; prefer higher severity & confidence.
            """.trimIndent()

            val body = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 2048)
                put("system", system)
                put(
                    "messages",
                    JSONArray().put(
                        JSONObject().put("role", "user").put(
                            "content",
                            "Routine description:\n${routineDescription.trim()}",
                        ),
                    ),
                )
            }

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("Claude HTTP ${response.code}: ${respBody.take(500)}")
                }
                val root = JSONObject(respBody)
                val text = root.getJSONArray("content").getJSONObject(0).getString("text")
                parseDetectionsJson(text)
            }
        }
    }

    private fun parseDetectionsJson(raw: String): List<AiHabitDetection> {
        var t = raw.trim()
        if (t.startsWith("```")) {
            t = t.removePrefix("```json").removePrefix("```").trim()
            if (t.endsWith("```")) t = t.removeSuffix("```").trim()
        }
        val arr = JSONArray(t)
        val out = ArrayList<AiHabitDetection>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = o.optString("habit_name")
            if (name.isBlank()) continue
            val category = parseCategory(o.optString("category"))
            val severity = o.optInt("severity", 1).coerceIn(1, 3)
            val harm = o.optString("personalised_harm")
            if (harm.isBlank()) continue
            val benefit = o.optString("quit_benefit").ifBlank {
                "Small daily tweaks add up — track one clean day at a time."
            }
            val conf = o.optDouble("confidence", 0.0).toFloat().coerceIn(0f, 1f)
            if (conf < 0.6f) continue
            val libMatch = o.optString("existing_library_match").trim().takeIf { it.isNotBlank() && !it.equals("null", true) }
            val validatedLib = libMatch?.takeIf { id -> HabitLibrary.byId(id) != null }
            out.add(
                AiHabitDetection(
                    title = name,
                    category = category,
                    severity = severity,
                    personalisedHarm = harm,
                    quitBenefit = benefit,
                    confidence = conf,
                    libraryMatchId = validatedLib,
                ),
            )
        }
        return out
            .sortedWith(compareByDescending<AiHabitDetection> { it.severity }.thenByDescending { it.confidence })
            .take(8)
    }

    private fun parseCategory(raw: String): HabitCategory {
        val n = raw.trim()
        if (n.isEmpty()) return HabitCategory.Mental
        runCatching { HabitCategory.valueOf(n) }.getOrNull()?.let { return it }
        return when (n.lowercase()) {
            "diet" -> HabitCategory.Diet
            "digital" -> HabitCategory.Digital
            "substance" -> HabitCategory.Substance
            "sleep" -> HabitCategory.Sleep
            "physical" -> HabitCategory.Physical
            "social" -> HabitCategory.Social
            "mental" -> HabitCategory.Mental
            "financial" -> HabitCategory.Financial
            else -> HabitCategory.Mental
        }
    }

    companion object {
        private const val MODEL = "claude-3-5-haiku-20241022"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
