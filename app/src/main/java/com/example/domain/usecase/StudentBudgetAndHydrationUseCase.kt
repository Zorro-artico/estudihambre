package com.example.domain.usecase

import com.example.domain.model.DailyLog
import com.example.domain.model.HabitAchievement
import com.example.domain.model.MealEntry
import com.example.domain.model.QuincenaBudgetSummary
import com.example.domain.model.UserProfile
import kotlin.math.roundToInt

class StudentBudgetAndHydrationUseCase {

    fun computeWaterTargetGlasses(meals: List<MealEntry>, examSurvivalMode: Boolean): Int {
        val hasHighSodium = meals.any { meal ->
            meal.alertTags.any { tag ->
                tag.contains("sodio", ignoreCase = true) ||
                    tag.contains("sal", ignoreCase = true) ||
                    tag.contains("maruchan", ignoreCase = true)
            }
        }
        val base = 8
        val sodiumExtra = if (hasHighSodium) 2 else 0
        val examExtra = if (examSurvivalMode) 1 else 0
        return base + sodiumExtra + examExtra
    }

    fun computeHydrationStreak(logsDesc: List<DailyLog>): Int {
        var streak = 0
        for (log in logsDesc) {
            if (log.waterGlasses >= log.waterTargetGlasses.coerceAtLeast(1)) {
                streak++
            } else if (streak > 0 || log.isClosed) {
                break
            }
        }
        return streak
    }

    fun computeQuincenaBudgetSummary(
        profile: UserProfile,
        recent14DayMeals: List<MealEntry>
    ): QuincenaBudgetSummary {
        val mealsWithCost = recent14DayMeals.filter { (it.estimatedCost ?: 0.0) > 0.0 }
        val totalSpent = mealsWithCost.sumOf { it.estimatedCost ?: 0.0 }
        val budget = profile.biweeklyBudget.coerceAtLeast(1.0)
        val remaining = (budget - totalSpent).coerceAtLeast(0.0)
        val totalProteinOnBudget = mealsWithCost.sumOf { it.proteinG.toDouble() }
        val avgProteinPerCurrency = if (totalSpent > 0.0) totalProteinOnBudget / totalSpent else 0.0
        val bestEfficiencyMeals = mealsWithCost
            .filter { it.proteinG > 0f }
            .distinctBy { it.title }
            .sortedByDescending { it.proteinG / (it.estimatedCost ?: 1.0) }
            .take(3)

        return QuincenaBudgetSummary(
            budgetAmount = budget,
            spentLast14Days = totalSpent,
            remainingAmount = remaining,
            avgProteinPerCurrency = avgProteinPerCurrency,
            topValueMeals = bestEfficiencyMeals,
            isOverBudget = totalSpent > budget
        )
    }

    fun evaluateHabitAchievements(
        logsAsc: List<DailyLog>,
        mealsByDate: Map<String, List<MealEntry>>,
        budgetSummary: QuincenaBudgetSummary,
        alreadyUnlockedIds: Set<String>
    ): List<HabitAchievement> {
        val newlyUnlocked = mutableListOf<HabitAchievement>()

        // 1. Chef de Quincena: >= 7 logged days and within biweekly budget with cost entries
        if (HabitAchievement.CHEF_QUINCENA.id !in alreadyUnlockedIds) {
            val daysLogged = logsAsc.size
            if (daysLogged >= 7 && !budgetSummary.isOverBudget && budgetSummary.spentLast14Days > 0.0) {
                newlyUnlocked += HabitAchievement.CHEF_QUINCENA
            }
        }

        // 2. Maestro de la Proteína: 3 consecutive days hitting minProteinG
        if (HabitAchievement.PROTEIN_MASTER.id !in alreadyUnlockedIds) {
            var consecutiveProtein = 0
            var hit = false
            for (log in logsAsc) {
                val protein = mealsByDate[log.logicalDate].orEmpty().sumOf { it.proteinG.toDouble() }
                if (protein >= log.minProteinG) {
                    consecutiveProtein++
                    if (consecutiveProtein >= 3) {
                        hit = true
                        break
                    }
                } else {
                    consecutiveProtein = 0
                }
            }
            if (hit) newlyUnlocked += HabitAchievement.PROTEIN_MASTER
        }

        // 3. Cazador de Agua: 3 days meeting water target (or streak >= 3)
        if (HabitAchievement.WATER_HUNTER.id !in alreadyUnlockedIds) {
            var streak = 0
            var hit = false
            for (log in logsAsc) {
                if (log.waterGlasses >= log.waterTargetGlasses.coerceAtLeast(1)) {
                    streak++
                    if (streak >= 3) {
                        hit = true
                        break
                    }
                } else {
                    streak = 0
                }
            }
            if (hit) newlyUnlocked += HabitAchievement.WATER_HUNTER
        }

        // 4. Cero Deuda Catabólica: 5 closed days without carryover debt and intake >= floor
        if (HabitAchievement.NO_DEBT_WARRIOR.id !in alreadyUnlockedIds) {
            val cleanClosedDays = logsAsc.count { log ->
                val intake = log.errorBudgetKcal + mealsByDate[log.logicalDate].orEmpty().sumOf { it.caloriesKcal }
                log.isClosed && log.catabolicDebtCarryoverKcal == 0 && intake >= log.floorKcal
            }
            if (cleanClosedDays >= 5) {
                newlyUnlocked += HabitAchievement.NO_DEBT_WARRIOR
            }
        }

        return newlyUnlocked
    }
}
