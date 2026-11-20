package com.example.domain.usecase

import com.example.domain.model.WeightAchievement
import com.example.domain.model.WeightCelebrationEvent
import com.example.domain.model.WeightEntry
import kotlin.math.abs
import kotlin.math.roundToInt

class WeightAchievementUseCase {

    fun evaluateNewWeight(
        newWeightKg: Double,
        targetWeightKg: Double,
        previousWeightsDesc: List<WeightEntry>,
        alreadyUnlockedIds: Set<String>
    ): WeightCelebrationEvent {
        val startWeightKg = previousWeightsDesc.lastOrNull()?.weightKg ?: newWeightKg
        val previousMinWeight = previousWeightsDesc.minOfOrNull { it.weightKg }
        val previousLatestWeight = previousWeightsDesc.firstOrNull()?.weightKg

        val isLosingGoal = startWeightKg >= targetWeightKg
        val isNewLower = previousLatestWeight != null && newWeightKg < previousLatestWeight

        val totalDistance = abs(startWeightKg - targetWeightKg)
        val reachedIdeal = if (isLosingGoal) {
            newWeightKg <= targetWeightKg + 0.05
        } else {
            newWeightKg >= targetWeightKg - 0.05
        }

        val progressPct = when {
            reachedIdeal -> 100
            totalDistance < 0.1 -> 0
            isLosingGoal -> {
                val covered = (startWeightKg - newWeightKg).coerceAtLeast(0.0)
                ((covered / totalDistance) * 100.0).roundToInt().coerceIn(0, 99)
            }
            else -> {
                val covered = (newWeightKg - startWeightKg).coerceAtLeast(0.0)
                ((covered / totalDistance) * 100.0).roundToInt().coerceIn(0, 99)
            }
        }

        val candidate = when {
            reachedIdeal && WeightAchievement.IDEAL_WEIGHT_REACHED.id !in alreadyUnlockedIds ->
                WeightAchievement.IDEAL_WEIGHT_REACHED
            progressPct >= 90 && WeightAchievement.ALMOST_THERE_90.id !in alreadyUnlockedIds ->
                WeightAchievement.ALMOST_THERE_90
            progressPct >= 75 && WeightAchievement.PROGRESS_75.id !in alreadyUnlockedIds ->
                WeightAchievement.PROGRESS_75
            progressPct >= 50 && WeightAchievement.PROGRESS_50.id !in alreadyUnlockedIds ->
                WeightAchievement.PROGRESS_50
            progressPct >= 25 && WeightAchievement.PROGRESS_25.id !in alreadyUnlockedIds ->
                WeightAchievement.PROGRESS_25
            (isNewLower || (previousMinWeight != null && newWeightKg < previousMinWeight)) &&
                WeightAchievement.FIRST_DROP.id !in alreadyUnlockedIds ->
                WeightAchievement.FIRST_DROP
            else -> null
        }

        return WeightCelebrationEvent(
            isNewLowerWeight = isNewLower,
            unlockedAchievement = candidate,
            isIdealWeight = reachedIdeal,
            progressPercent = progressPct
        )
    }

    fun computeCurrentProgressPercent(
        weightsDesc: List<WeightEntry>,
        fallbackCurrentKg: Double,
        targetWeightKg: Double
    ): Int {
        val startWeight = weightsDesc.lastOrNull()?.weightKg ?: fallbackCurrentKg
        val currentWeight = weightsDesc.firstOrNull()?.weightKg ?: fallbackCurrentKg
        val isLosingGoal = startWeight >= targetWeightKg
        if (isLosingGoal && currentWeight <= targetWeightKg + 0.05) return 100
        if (!isLosingGoal && currentWeight >= targetWeightKg - 0.05) return 100
        val total = abs(startWeight - targetWeightKg)
        if (total < 0.1) return 0
        val covered = if (isLosingGoal) (startWeight - currentWeight) else (currentWeight - startWeight)
        return ((covered.coerceAtLeast(0.0) / total) * 100.0).roundToInt().coerceIn(0, 99)
    }
}
