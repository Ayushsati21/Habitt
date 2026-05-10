package com.example.habit.data.ai

import com.example.habit.data.HabitLibrary
import com.example.habit.data.model.HabitCategory
import java.util.UUID

/**
 * Offline fallback: keyword + library mapping when Claude is unavailable.
 */
object LocalRoutineAnalyzer {
    private data class Rule(
        val keywords: List<String>,
        val detection: AiHabitDetection,
    )

    private val rules: List<Rule> = HabitLibrary.all.map { habit ->
        val keys = buildList {
            add(habit.name.lowercase())
            add(habit.id.replace('_', ' '))
            when (habit.id) {
                "late_night_scroll" -> {
                    add("instagram"); add("tiktok"); add("reels"); add("youtube"); add("scroll"); add("phone in bed")
                }
                "smoking" -> {
                    add("cigarette"); add("smoke"); add("tobacco")
                }
                "junk_food" -> {
                    add("chips"); add("junk"); add("fast food"); add("binge")
                }
                "sedentary" -> {
                    add("desk"); add("sitting"); add("9 hours"); add("10 hours")
                }
                "sleep_deprivation" -> {
                    add("4 hours sleep"); add("5 hours"); add("insomnia")
                }
                "caffeine_overload" -> {
                    add("coffee"); add("energy drink"); add("caffeine")
                }
                "gaming_addiction" -> {
                    add("gaming"); add("games all night")
                }
                "sleep_posture_stomach" -> {
                    add("sleep on my stomach"); add("stomach sleeper"); add("face down"); add("prone sleep")
                }
                "sleep_neck_twist" -> {
                    add("neck bent"); add("awkward pillow"); add("head tilted"); add("neck kink")
                }
                "mouth_breathing_sleep" -> {
                    add("mouth open"); add("mouth breathing"); add("snore"); add("dry mouth sleep")
                }
                "fast_eating" -> {
                    add("eat fast"); add("eating quickly"); add("inhale food"); add("speed eating")
                }
                "distracted_eating" -> {
                    add("eating while"); add("tv dinner"); add("phone while eating"); add("scroll while eating")
                }
                "poor_chewing" -> {
                    add("barely chew"); add("swallow big bites"); add("wolf down"); add("few chews")
                }
                "slouching_seated" -> {
                    add("slouch"); add("hunched"); add("rounded shoulders"); add("lean on desk")
                }
                "jaw_clenching_stress" -> {
                    add("clench jaw"); add("grind teeth"); add("tmj"); add("tight jaw")
                }
                else -> {}
            }
        }
        Rule(
            keywords = keys.distinct(),
            detection = AiHabitDetection(
                title = habit.name,
                category = habit.category,
                severity = habit.severity,
                personalisedHarm = "Based on your routine, this pattern tracks toward: ${habit.keyStat}",
                quitBenefit = habit.quitBenefit,
                confidence = 0.72f,
                libraryMatchId = habit.id,
            ),
        )
    }

    fun analyze(routineText: String): List<AiHabitDetection> {
        val text = routineText.lowercase()
        if (text.isBlank()) return emptyList()
        val hits = linkedMapOf<String, AiHabitDetection>()
        for (rule in rules) {
            val matched = rule.keywords.any { it.isNotBlank() && text.contains(it) }
            if (matched) {
                val id = rule.detection.libraryMatchId ?: rule.detection.title
                hits.putIfAbsent(id, rule.detection)
            }
        }
        return hits.values
            .sortedWith(compareByDescending<AiHabitDetection> { it.severity }.thenByDescending { it.confidence })
            .take(8)
    }

    fun newPendingId(): String = UUID.randomUUID().toString()
}
