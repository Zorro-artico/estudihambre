package com.example.domain.usecase

import com.example.domain.model.DailyLog
import com.example.domain.model.MealEntry
import com.example.domain.model.WeightEntry
import kotlin.math.abs
import kotlin.math.roundToInt

class DryWeightAndWaterFilterUseCase {

    companion object {
        const val KCAL_PER_KG_TISSUE = 7700.0
        const val OUTLIER_24H_DELTA_KG = 3.0
        const val WHOOSH_DROP_GRAMS = 600
    }

    fun requiresOutlierConfirmation(
        newWeightKg: Double,
        newTimestampMillis: Long,
        existingWeights: List<WeightEntry>
    ): Pair<Boolean, Double?> {
        val recent = existingWeights
            .filter { abs(newTimestampMillis - it.recordedAtEpochMillis) <= 24L * 3600L * 1000L }
            .maxByOrNull { it.recordedAtEpochMillis }
            ?: existingWeights.maxByOrNull { it.recordedAtEpochMillis }

        if (recent == null) return false to null
        val delta = newWeightKg - recent.weightKg
        val within24h = abs(newTimestampMillis - recent.recordedAtEpochMillis) <= 36L * 3600L * 1000L
        return (within24h && abs(delta) >= OUTLIER_24H_DELTA_KG) to delta
    }

    /**
     * Recomputes movingAverage7dKg, theoreticalDryWeightKg, waterRetentionGrams, and whoosh flags
     * across chronological weigh-ins.
     */
    fun recalculateSeries(
        weightsAsc: List<WeightEntry>,
        dailyLogs: List<DailyLog>,
        mealsByDate: Map<String, List<MealEntry>>,
        fallbackBaseWeightKg: Double
    ): List<WeightEntry> {
        if (weightsAsc.isEmpty()) return emptyList()
        val sortedWeights = weightsAsc.sortedBy { it.logicalDate }
        val baseWeight = sortedWeights.first().weightKg.takeIf { it > 0 } ?: fallbackBaseWeightKg
        val firstDate = sortedWeights.first().logicalDate

        val logsAsc = dailyLogs.sortedBy { it.logicalDate }

        val result = mutableListOf<WeightEntry>()
        sortedWeights.forEachIndexed { index, entry ->
            val windowStart = (index - 6).coerceAtLeast(0)
            val movingAvg = sortedWeights
                .subList(windowStart, index + 1)
                .map { it.weightKg }
                .average()

            // Cumulative net deficit up to entry.logicalDate (starting from first weigh-in date)
            val cumulativeDeficit = logsAsc
                .filter { it.logicalDate >= firstDate && it.logicalDate <= entry.logicalDate }
                .sumOf { log ->
                    val mealsKcal = mealsByDate[log.logicalDate].orEmpty().sumOf { it.caloriesKcal }
                    val intake = log.errorBudgetKcal + mealsKcal
                    log.estimatedExpenditureKcal - intake
                }

            val theoreticalDry = baseWeight - (cumulativeDeficit / KCAL_PER_KG_TISSUE)
            val waterGrams = ((entry.weightKg - theoreticalDry) * 1000.0).roundToInt()

            val prevWater = result.lastOrNull()?.waterRetentionGrams
            val prevWeight = result.lastOrNull()?.weightKg
            val isWhoosh = prevWater != null && prevWeight != null &&
                (prevWater - waterGrams) >= WHOOSH_DROP_GRAMS &&
                (prevWeight - entry.weightKg) >= 0.6

            result.add(
                entry.copy(
                    movingAverage7dKg = movingAvg,
                    theoreticalDryWeightKg = theoreticalDry,
                    waterRetentionGrams = waterGrams,
                    isWhooshDetected = isWhoosh
                )
            )
        }
        return result
    }
}
