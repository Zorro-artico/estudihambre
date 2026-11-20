package com.example.domain.usecase

import com.example.domain.model.CalibrationSuggestion
import com.example.domain.model.DailyLog
import com.example.domain.model.MealEntry
import com.example.domain.model.WeightEntry
import kotlin.math.abs
import kotlin.math.roundToInt

class ThermodynamicCalibrationUseCase {

    companion object {
        const val MIN_CALIBRATION_DAYS = 14
        const val SIGNIFICANT_DAILY_DRIFT_KCAL = 80
    }

    fun evaluateCalibration(
        weightsAsc: List<WeightEntry>,
        dailyLogsAsc: List<DailyLog>,
        mealsByDate: Map<String, List<MealEntry>>
    ): CalibrationSuggestion? {
        if (weightsAsc.size < 4 || dailyLogsAsc.size < MIN_CALIBRATION_DAYS) return null

        val recentLogs = dailyLogsAsc.takeLast(21)
        val startDate = recentLogs.first().logicalDate
        val endDate = recentLogs.last().logicalDate

        val startWeight = weightsAsc.firstOrNull { it.logicalDate >= startDate } ?: return null
        val endWeight = weightsAsc.lastOrNull { it.logicalDate <= endDate } ?: return null
        if (startWeight.logicalDate == endWeight.logicalDate) return null

        val daysBetween = recentLogs.size.coerceAtLeast(MIN_CALIBRATION_DAYS)
        val cumulativeDeficit = recentLogs.sumOf { log ->
            val mealsKcal = mealsByDate[log.logicalDate].orEmpty().sumOf { it.caloriesKcal }
            log.estimatedExpenditureKcal - (log.errorBudgetKcal + mealsKcal)
        }

        val theoreticalDeltaKg = -(cumulativeDeficit / DryWeightAndWaterFilterUseCase.KCAL_PER_KG_TISSUE)
        val realMaDeltaKg = endWeight.movingAverage7dKg - startWeight.movingAverage7dKg

        // Positive discrepancy means user lost MORE weight than theory (actual TDEE is higher)
        // Negative discrepancy means user lost LESS weight than theory (actual TDEE is lower or error budget too low)
        val discrepancyKg = theoreticalDeltaKg - realMaDeltaKg
        val dailyDiscrepancyKcal = ((discrepancyKg * DryWeightAndWaterFilterUseCase.KCAL_PER_KG_TISSUE) / daysBetween).roundToInt()

        if (abs(dailyDiscrepancyKcal) < SIGNIFICANT_DAILY_DRIFT_KCAL) return null

        val directionLabel = if (dailyDiscrepancyKcal > 0) {
            "Tu pérdida real supera la teórica: tu gasto diario real es ~+${dailyDiscrepancyKcal} kcal/día mayor."
        } else {
            "Tu pérdida real es menor a la teórica: conviene ajustar el gasto en ${dailyDiscrepancyKcal} kcal/día o subir el margen de error."
        }

        return CalibrationSuggestion(
            consecutiveDaysAnalyzed = daysBetween,
            theoreticalWeightDeltaKg = theoreticalDeltaKg,
            realMovingAverageDeltaKg = realMaDeltaKg,
            dailyDiscrepancyKcal = dailyDiscrepancyKcal,
            suggestedTdeeDeltaKcal = dailyDiscrepancyKcal,
            summaryEs = directionLabel
        )
    }
}
