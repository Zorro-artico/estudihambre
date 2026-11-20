package com.example.domain.usecase

import com.example.domain.model.BiologicalSex
import com.example.domain.model.DailyLog
import com.example.domain.model.MealEntry
import com.example.domain.model.MetabolicBuffStatus
import com.example.domain.model.OptimalRangeMetrics
import com.example.domain.model.UserProfile
import kotlin.math.max
import kotlin.math.roundToInt

class MetabolicRangeAndDebtUseCase {

    fun calculateEstimatedTdeeKcal(profile: UserProfile): Int {
        // Mifflin-St Jeor BMR
        val sexOffset = when (profile.sex) {
            BiologicalSex.MALE -> 5.0
            BiologicalSex.FEMALE -> -161.0
        }
        val bmr = (10.0 * profile.currentWeightKg) +
            (6.25 * profile.heightCm) -
            (5.0 * profile.ageYears) +
            sexOffset
        val baseDaily = (bmr * profile.activityLevel.multiplier).roundToInt()
        val exerciseAvgDaily = (profile.strengthTraining.exerciseBonusKcal * 0.65).roundToInt()
        return (baseDaily + exerciseAvgDaily + profile.routineBonusKcal + profile.calibratedTdeeAdjustmentKcal)
            .coerceAtLeast(1200)
    }

    fun calculateOptimalFloorAndCeiling(
        profile: UserProfile,
        tdeeKcal: Int,
        carryoverDebtKcal: Int
    ): Triple<Int, Int, Float> {
        val minProteinG = (profile.currentWeightKg * profile.targetProteinPerKg).toFloat()
        // Exam survival mode grants +80 kcal late-night study snack cushion on ceiling
        val examCushion = if (profile.examSurvivalMode) 80 else 0
        // Ceiling = TDEE - desiredDeficit (maximum intake allowed to still hit target deficit)
        val ceilingKcal = (tdeeKcal - profile.desiredDeficitKcal + examCushion).coerceAtLeast(1100)

        // Anti-catabolic floor: guarantees enough energy alongside minimum protein & prevents >32% deficit
        val proteinEnergyFloor = (minProteinG * 4f).roundToInt() + 650
        val percentageSafeFloor = (tdeeKcal * 0.68).roundToInt()
        val sexMinFloor = if (profile.sex == BiologicalSex.MALE) 1400 else 1200

        var baseFloor = max(sexMinFloor, max(percentageSafeFloor, proteinEnergyFloor))
        if (carryoverDebtKcal > 0 && !profile.examSurvivalMode) {
            // If catabolic debt carries over, tighten floor upward slightly toward ceiling to encourage recovery
            baseFloor = minOf(ceilingKcal - 50, baseFloor + (carryoverDebtKcal / 4).coerceAtMost(200))
        }
        val floorKcal = baseFloor.coerceAtMost(ceilingKcal)
        return Triple(floorKcal, ceilingKcal, minProteinG)
    }

    fun computeMetrics(
        dailyLog: DailyLog,
        meals: List<MealEntry>
    ): OptimalRangeMetrics {
        val mealsKcal = meals.sumOf { it.caloriesKcal }
        val totalIntake = dailyLog.errorBudgetKcal + mealsKcal
        val totalProtein = meals.sumOf { it.proteinG.toDouble() }.toFloat()

        val missingToFloor = (dailyLog.floorKcal - totalIntake).coerceAtLeast(0)
        val remainingBeforeCeiling = (dailyLog.ceilingKcal - totalIntake).coerceAtLeast(0)
        val projectedDeficit = dailyLog.estimatedExpenditureKcal - totalIntake

        val status = when {
            dailyLog.catabolicDebtCarryoverKcal > 0 && totalIntake < dailyLog.floorKcal ->
                MetabolicBuffStatus.MALEFICIO_CATABOLICO
            totalIntake > dailyLog.ceilingKcal ->
                MetabolicBuffStatus.EXCEDENTE
            totalIntake < dailyLog.floorKcal ->
                MetabolicBuffStatus.FORJA_ACTIVA
            totalProtein >= dailyLog.minProteinG ->
                MetabolicBuffStatus.ELIXIR_ANABOLICO
            else ->
                MetabolicBuffStatus.ESCUDO_TECHO
        }

        return OptimalRangeMetrics(
            estimatedTdeeKcal = dailyLog.estimatedExpenditureKcal,
            floorKcal = dailyLog.floorKcal,
            ceilingKcal = dailyLog.ceilingKcal,
            minProteinG = dailyLog.minProteinG,
            totalIntakeKcal = totalIntake,
            totalProteinG = totalProtein,
            missingToFloorKcal = missingToFloor,
            remainingBeforeCeilingKcal = remainingBeforeCeiling,
            projectedDeficitKcal = projectedDeficit,
            catabolicDebtCarryoverKcal = dailyLog.catabolicDebtCarryoverKcal,
            statusBadge = status
        )
    }

    /**
     * Computes catabolic debt carried into the next day when a closed day finishes below floorKcal.
     */
    fun computeNextDayDebtCarryover(closedDay: DailyLog, dayMeals: List<MealEntry>): Int {
        val totalIntake = closedDay.errorBudgetKcal + dayMeals.sumOf { it.caloriesKcal }
        val shortfall = (closedDay.floorKcal - totalIntake).coerceAtLeast(0)
        return if (shortfall > 0) {
            closedDay.catabolicDebtCarryoverKcal + shortfall
        } else {
            // Closing safely inside optimal range clears previous catabolic debt
            0
        }
    }
}
