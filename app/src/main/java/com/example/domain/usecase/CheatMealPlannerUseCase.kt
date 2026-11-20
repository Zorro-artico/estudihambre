package com.example.domain.usecase

import com.example.domain.model.UserProfile
import com.example.domain.model.WeeklyCheatBudgetPlan
import kotlin.math.roundToInt

class CheatMealPlannerUseCase {

    fun simulateWeeklyCheatCompensation(
        profile: UserProfile,
        estimatedTdeeKcal: Int,
        floorKcal: Int,
        ceilingKcal: Int,
        extraCravingKcal: Int,
        spreadDays: Int
    ): WeeklyCheatBudgetPlan {
        val safeDays = spreadDays.coerceIn(2, 7)
        val clampedExtra = extraCravingKcal.coerceIn(100, 2500)
        val dailyReduction = (clampedExtra.toDouble() / safeDays).roundToInt()

        val compensatedTargetIntake = ceilingKcal - dailyReduction
        val keepsDebtZero = compensatedTargetIntake >= floorKcal

        val safeMaxDailyReduction = (ceilingKcal - floorKcal).coerceAtLeast(0)
        val recommendedDays = if (dailyReduction > safeMaxDailyReduction && safeMaxDailyReduction > 0) {
            kotlin.math.ceil(clampedExtra.toDouble() / safeMaxDailyReduction).toInt().coerceIn(2, 7)
        } else {
            safeDays
        }

        val weeklyBaselineDeficit = profile.desiredDeficitKcal * 7
        val netWeeklyDeficit = weeklyBaselineDeficit - if (keepsDebtZero) 0 else (clampedExtra - safeMaxDailyReduction * safeDays)

        val message = if (keepsDebtZero) {
            "Compensando -$dailyReduction kcal/día durante $safeDays días te mantienes en $compensatedTargetIntake kcal (sobre tu piso de $floorKcal kcal), protegiendo masa muscular y con déficit semanal intacto."
        } else {
            "Reducir $dailyReduction kcal/día perforaría tu piso seguro ($floorKcal kcal). Repártelo en $recommendedDays días (-${(clampedExtra.toDouble() / recommendedDays).roundToInt()} kcal/día) o disfruta sin compensar de golpe."
        }

        return WeeklyCheatBudgetPlan(
            extraKcal = clampedExtra,
            spreadDays = safeDays,
            dailyCompensationKcal = dailyReduction,
            compensatedDailyTargetKcal = compensatedTargetIntake,
            keepsDebtZero = keepsDebtZero,
            projectedWeeklyDeficitKcal = netWeeklyDeficit,
            recommendationEs = message
        )
    }
}
