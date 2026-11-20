package com.example.domain.usecase

import com.example.domain.model.DeficitProjectionPoint
import com.example.domain.model.DeficitRiskAssessment
import com.example.domain.model.StrengthTrainingMode
import com.example.domain.model.UserProfile
import kotlin.math.roundToInt

class DeficitProjectionUseCase {

    fun projectWeightCurve(
        startWeightKg: Double,
        desiredDailyDeficitKcal: Int,
        weeks: Int = 12
    ): List<DeficitProjectionPoint> {
        return (0..weeks).map { week ->
            val cumulativeDeficit = desiredDailyDeficitKcal * 7 * week
            val projectedWeight = (startWeightKg - (cumulativeDeficit / DryWeightAndWaterFilterUseCase.KCAL_PER_KG_TISSUE))
                .coerceAtLeast(35.0)
            DeficitProjectionPoint(
                weekIndex = week,
                projectedWeightKg = projectedWeight,
                cumulativeDeficitKcal = cumulativeDeficit
            )
        }
    }

    fun assessRisk(
        profile: UserProfile,
        estimatedTdeeKcal: Int,
        desiredDeficitKcal: Int,
        currentProteinG: Float
    ): DeficitRiskAssessment {
        val targetIntake = estimatedTdeeKcal - desiredDeficitKcal
        val deficitRatio = desiredDeficitKcal.toDouble() / estimatedTdeeKcal.coerceAtLeast(1)
        val minProteinG = profile.currentWeightKg * profile.strengthTraining.minProteinPerKg
        val warnings = mutableListOf<String>()

        if (deficitRatio > 0.30 || desiredDeficitKcal >= 900) {
            warnings.add("Déficit agresivo (>30% del gasto): alto riesgo de fatiga metabólica y pérdida de masa magra.")
        }
        if (targetIntake < 1200) {
            warnings.add("Ingesta proyectada inferior a 1,200 kcal/día: compromete micronutrientes y recuperación.")
        }
        if (profile.strengthTraining == StrengthTrainingMode.NONE && desiredDeficitKcal > 450) {
            warnings.add("Sin entrenamiento de fuerza, un déficit superior a 450 kcal eleva el catabolismo muscular.")
        }
        if (currentProteinG > 0f && currentProteinG < minProteinG) {
            warnings.add("Proteína diaria debajo del umbral (${currentProteinG.roundToInt()}g vs ${minProteinG.roundToInt()}g recomendados).")
        }

        val label = when {
            warnings.size >= 2 -> "Peligro Catabólico Alto"
            warnings.size == 1 -> "Precaución Metabólica"
            desiredDeficitKcal <= 0 -> "Mantenimiento / Superávit"
            else -> "Déficit Sostenible"
        }

        return DeficitRiskAssessment(
            isDangerous = warnings.isNotEmpty(),
            riskLevelLabel = label,
            warnings = warnings
        )
    }
}
