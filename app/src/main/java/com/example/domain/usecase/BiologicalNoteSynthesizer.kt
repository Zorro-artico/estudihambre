package com.example.domain.usecase

import com.example.domain.model.DailyLog
import com.example.domain.model.MealEntry
import com.example.domain.model.MealSlot
import com.example.domain.model.WeightEntry
import kotlin.math.roundToInt

class BiologicalNoteSynthesizer {

    fun synthesizeClosingNote(
        dailyLog: DailyLog,
        meals: List<MealEntry>,
        latestWeight: WeightEntry?
    ): String {
        val totalIntake = dailyLog.errorBudgetKcal + meals.sumOf { it.caloriesKcal }
        val totalProtein = meals.sumOf { it.proteinG.toDouble() }.roundToInt()
        val deficit = dailyLog.estimatedExpenditureKcal - totalIntake

        val mealSummary = if (meals.isEmpty()) {
            "Sin comidas registradas (solo margen base +${dailyLog.errorBudgetKcal} kcal)."
        } else {
            meals.joinToString("; ") { meal ->
                val portionText = if (meal.batchServings > 1) " (1/${meal.batchServings} olla)" else ""
                "${meal.slot.labelEs}: ${meal.title}$portionText (${meal.caloriesKcal} kcal, ${meal.proteinG.roundToInt()}g P)"
            }
        }

        val coreSlots = listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER)
        val presentSlots = meals.map { it.slot }.toSet()
        val omittedSlots = coreSlots.filter { it !in presentSlots }
        val omittedText = if (omittedSlots.isNotEmpty()) {
            " | Omitido: ${omittedSlots.joinToString(", ") { it.labelEs }}"
        } else {
            ""
        }

        val allAlerts = meals.flatMap { it.alertTags }.distinct()
        val diagnosis = buildList {
            when {
                totalIntake < dailyLog.floorKcal -> {
                    val gap = dailyLog.floorKcal - totalIntake
                    add("Brecha vs piso (-$gap kcal, deuda catabólica activa)")
                }
                totalIntake > dailyLog.ceilingKcal -> {
                    val over = totalIntake - dailyLog.ceilingKcal
                    add("Sobre techo (+$over kcal, posible recarga de glucógeno)")
                }
                else -> add("Rango óptimo cumplido (déficit $deficit kcal)")
            }

            if (totalProtein < dailyLog.minProteinG.roundToInt()) {
                add("Proteína baja ($totalProtein/${dailyLog.minProteinG.roundToInt()}g)")
            } else {
                add("Piso proteico asegurado ($totalProtein g)")
            }

            if (allAlerts.any { it.contains("sodio", ignoreCase = true) }) {
                add("Alerta de sodio (posible retención hídrica mañana)")
            } else if (latestWeight != null && latestWeight.isWhooshDetected) {
                add("Limpieza de sodio / descarga hídrica (whoosh)")
            }
        }.joinToString(" · ")

        return "$mealSummary$omittedText | Diagnóstico: $diagnosis"
    }
}
