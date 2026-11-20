package com.example.domain.usecase

import com.example.domain.model.MealEntry
import com.example.domain.model.MealSlot
import com.example.domain.model.RepeatedMealHabit

class RepeatedMealHabitUseCase {

    /**
     * Groups recent meals by MealSlot and normalized base title (ignoring leading emoji)
     * so repeated Breakfast/Lunch/Dinner/Snack choices are preselected for 1-tap confirmation.
     */
    fun computeHabitsBySlot(
        allMealsDesc: List<MealEntry>,
        todayMeals: List<MealEntry>
    ): Map<MealSlot, RepeatedMealHabit> {
        val result = mutableMapOf<MealSlot, RepeatedMealHabit>()
        val todayKeys = todayMeals.map { "${it.slot.name}:${normalizeKey(it.title)}" }.toSet()

        for (slot in MealSlot.entries) {
            val slotMeals = allMealsDesc.filter { it.slot == slot && it.caloriesKcal > 0 }
            if (slotMeals.isEmpty()) continue

            val grouped = slotMeals.groupBy { normalizeKey(it.title) }
            // Prefer meals eaten >= 2 times, otherwise fallback to the most recent meal in this slot
            val bestRepeated = grouped.values
                .filter { it.size >= 2 }
                .maxByOrNull { list -> list.size * 1000L + (list.maxOfOrNull { it.createdAtEpochMillis } ?: 0L) / 1_000_000L }

            val candidateList = bestRepeated ?: listOf(slotMeals.first())
            val representative = candidateList.first()
            val candidateKey = "${slot.name}:${normalizeKey(representative.title)}"

            // Still expose it in the slot map, marking whether it's already logged today
            if (candidateKey !in todayKeys || bestRepeated != null) {
                result[slot] = RepeatedMealHabit(
                    slot = slot,
                    meal = representative,
                    repeatCount = candidateList.size,
                    alreadyLoggedToday = candidateKey in todayKeys
                )
            }
        }
        return result
    }

    fun pickSuggestedHabitForHour(
        habitsBySlot: Map<MealSlot, RepeatedMealHabit>,
        hourOfDay: Int
    ): RepeatedMealHabit? {
        val preferredSlot = when (hourOfDay) {
            in 5..11 -> MealSlot.BREAKFAST
            in 12..17 -> MealSlot.LUNCH
            in 18..22 -> MealSlot.DINNER
            else -> MealSlot.SNACK
        }
        val primary = habitsBySlot[preferredSlot]
        if (primary != null && !primary.alreadyLoggedToday && primary.repeatCount >= 2) {
            return primary
        }
        // Fallback to any repeated habit not yet logged today
        return habitsBySlot.values.firstOrNull { !it.alreadyLoggedToday && it.repeatCount >= 2 }
            ?: habitsBySlot[preferredSlot]?.takeIf { !it.alreadyLoggedToday }
    }

    private fun normalizeKey(title: String): String {
        val noEmoji = title.trim().dropWhile { ch ->
            val cp = ch.code
            cp > 127 || ch == ' '
        }
        return noEmoji.lowercase().replace(Regex("\\s+"), " ").trim()
    }
}
