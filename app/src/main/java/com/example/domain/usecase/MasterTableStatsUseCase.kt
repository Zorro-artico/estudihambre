package com.example.domain.usecase

import com.example.domain.model.MasterTableIntervalStats
import com.example.domain.model.MasterTableRow

class MasterTableStatsUseCase {

    fun calculateIntervalStats(rows: List<MasterTableRow>): MasterTableIntervalStats {
        if (rows.isEmpty()) {
            return MasterTableIntervalStats(
                dayCount = 0,
                totalIntakeKcal = 0,
                avgDailyIntakeKcal = 0.0,
                totalExpenditureKcal = 0,
                netDeficitKcal = 0,
                avgDailyDeficitKcal = 0.0,
                theoreticalFatOxidizedGrams = 0.0,
                realScaleDeltaKg = null
            )
        }

        val ordered = rows.sortedBy { it.logicalDate }
        val totalIntake = ordered.sumOf { it.intakeKcal }
        val totalExpenditure = ordered.sumOf { it.expenditureKcal }
        val netDeficit = ordered.sumOf { it.deficitKcal }
        val count = ordered.size.toDouble()
        val theoreticalFatGrams = (netDeficit / DryWeightAndWaterFilterUseCase.KCAL_PER_KG_TISSUE) * 1000.0

        val weightsInRange = ordered.mapNotNull { it.weightKg }
        val realDelta = if (weightsInRange.size >= 2) {
            weightsInRange.last() - weightsInRange.first()
        } else {
            null
        }

        return MasterTableIntervalStats(
            dayCount = ordered.size,
            totalIntakeKcal = totalIntake,
            avgDailyIntakeKcal = totalIntake / count,
            totalExpenditureKcal = totalExpenditure,
            netDeficitKcal = netDeficit,
            avgDailyDeficitKcal = netDeficit / count,
            theoreticalFatOxidizedGrams = theoreticalFatGrams,
            realScaleDeltaKg = realDelta
        )
    }
}
