package com.example

import com.example.data.excel.ExcelManager
import com.example.domain.model.BiologicalSex
import com.example.domain.model.ContainerShape
import com.example.domain.model.DailyActivityLevel
import com.example.domain.model.DailyLog
import com.example.domain.model.HabitAchievement
import com.example.domain.model.MasterTableRow
import com.example.domain.model.MealEntry
import com.example.domain.model.MealSlot
import com.example.domain.model.StrengthTrainingMode
import com.example.domain.model.UserProfile
import com.example.domain.model.WeightEntry
import com.example.domain.model.WeightAchievement
import com.example.domain.usecase.BiologicalNoteSynthesizer
import com.example.domain.usecase.CheatMealPlannerUseCase
import com.example.domain.usecase.ContainerGeometryUseCase
import com.example.domain.usecase.DryWeightAndWaterFilterUseCase
import com.example.domain.usecase.FoodEmojiMapper
import com.example.domain.usecase.MetabolicRangeAndDebtUseCase
import com.example.domain.usecase.RepeatedMealHabitUseCase
import com.example.domain.usecase.StudentBudgetAndHydrationUseCase
import com.example.domain.usecase.ThermodynamicCalibrationUseCase
import com.example.domain.usecase.WeightAchievementUseCase
import kotlin.math.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainAndExcelUnitTest {

    @Test
    fun containerGeometry_computesExactVolumes() {
        val geom = ContainerGeometryUseCase()
        val cyl = geom.calculateTotalVolumeMl(ContainerShape.CYLINDRICAL, radiusCm = 5.0, heightCm = 10.0, 0.0, 0.0)
        assertEquals(PI * 25.0 * 10.0, cyl, 0.01)

        val rect = geom.calculateTotalVolumeMl(ContainerShape.RECTANGULAR, 0.0, heightCm = 6.0, lengthCm = 14.0, widthCm = 14.0)
        assertEquals(1176.0, rect, 0.01)

        val hemi = geom.calculateTotalVolumeMl(ContainerShape.HEMISPHERICAL, radiusCm = 6.0, 0.0, 0.0, 0.0)
        assertEquals((2.0 / 3.0) * PI * 216.0, hemi, 0.01)

        assertEquals(588.0, geom.calculateOccupiedVolumeMl(rect, 50), 0.01)
    }

    @Test
    fun metabolicDebt_carriesOverWhenClosedBelowFloor_andClearsWhenOptimal() {
        val useCase = MetabolicRangeAndDebtUseCase()
        val profile = UserProfile(
            currentWeightKg = 75.0,
            heightCm = 175.0,
            ageYears = 23,
            sex = BiologicalSex.MALE,
            activityLevel = DailyActivityLevel.LIGHT,
            strengthTraining = StrengthTrainingMode.LIGHT_HYPERTROPHY,
            desiredDeficitKcal = 500,
            baseErrorBudgetKcal = 136
        )
        val tdee = useCase.calculateEstimatedTdeeKcal(profile)
        val tdeeWithRoutine = useCase.calculateEstimatedTdeeKcal(profile.copy(routineBonusKcal = 180))
        assertEquals(tdee + 180, tdeeWithRoutine)

        val (floor, ceiling, minProt) = useCase.calculateOptimalFloorAndCeiling(profile, tdee, 0)
        assertTrue(floor <= ceiling)

        val day1 = DailyLog("2026-11-10", 1, tdee, 136, floor, ceiling, minProt, catabolicDebtCarryoverKcal = 0, isClosed = true)
        val lowMeals = listOf(
            MealEntry(logicalDate = "2026-11-10", slot = MealSlot.LUNCH, title = "Maruchan", description = "", caloriesKcal = 350, proteinG = 8f)
        )
        val debt = useCase.computeNextDayDebtCarryover(day1, lowMeals)
        assertEquals(floor - (136 + 350), debt)

        val day2 = day1.copy(logicalDate = "2026-11-11", dayNumber = 2, catabolicDebtCarryoverKcal = debt)
        val adequateMeals = listOf(
            MealEntry(logicalDate = "2026-11-11", slot = MealSlot.LUNCH, title = "Pollo y arroz", description = "", caloriesKcal = floor, proteinG = 140f)
        )
        val cleared = useCase.computeNextDayDebtCarryover(day2, adequateMeals)
        assertEquals(0, cleared)
    }

    @Test
    fun dryWeight_andOutlierCheck_workAccurately() {
        val filter = DryWeightAndWaterFilterUseCase()
        val existing = listOf(
            WeightEntry(id = 1, logicalDate = "2026-11-01", weightKg = 75.0, recordedAtEpochMillis = 1_000_000L)
        )
        val (needsConfirm, delta) = filter.requiresOutlierConfirmation(78.5, 1_000_000L + 3600_000L, existing)
        assertTrue(needsConfirm)
        assertEquals(3.5, delta ?: 0.0, 0.01)

        val (safeConfirm, _) = filter.requiresOutlierConfirmation(74.6, 1_000_000L + 3600_000L, existing)
        assertFalse(safeConfirm)

        val log1 = DailyLog("2026-11-01", 1, 2400, 100, 1500, 1900, 130f)
        val log2 = DailyLog("2026-11-02", 2, 2400, 100, 1500, 1900, 130f)
        val meals = mapOf(
            "2026-11-01" to listOf(MealEntry(logicalDate = "2026-11-01", title = "A", description = "", caloriesKcal = 1500, proteinG = 120f)),
            "2026-11-02" to listOf(MealEntry(logicalDate = "2026-11-02", title = "B", description = "", caloriesKcal = 1500, proteinG = 120f))
        )
        val weights = listOf(
            WeightEntry(id = 1, logicalDate = "2026-11-01", weightKg = 75.0),
            WeightEntry(id = 2, logicalDate = "2026-11-02", weightKg = 75.4)
        )
        val series = filter.recalculateSeries(weights, listOf(log1, log2), meals, 75.0)
        // Cumulative deficit = (2400-1600)*2 = 1600 kcal -> 1600 / 7700 = 0.2078 kg lost theoretically
        assertEquals(75.0 - (1600.0 / 7700.0), series.last().theoreticalDryWeightKg, 0.001)
        assertTrue(series.last().waterRetentionGrams > 500)
    }

    @Test
    fun thermodynamicCalibration_detectsSystematicDriftOver14Days() {
        val calib = ThermodynamicCalibrationUseCase()
        val logs = (1..15).map { d ->
            val date = String.format("2026-11-%02d", d)
            DailyLog(date, d, estimatedExpenditureKcal = 2400, errorBudgetKcal = 100, floorKcal = 1500, ceilingKcal = 1900, minProteinG = 130f)
        }
        val meals = logs.associate { log ->
            log.logicalDate to listOf(MealEntry(logicalDate = log.logicalDate, title = "M", description = "", caloriesKcal = 1800, proteinG = 130f))
        }
        // Theoretical deficit = 500/day * 15 = 7500 kcal (~0.97 kg loss). Suppose real MA dropped by 2.5 kg (user burns way more)
        val weights = listOf(
            WeightEntry(logicalDate = "2026-11-01", weightKg = 75.0, movingAverage7dKg = 75.0),
            WeightEntry(logicalDate = "2026-11-05", weightKg = 74.2, movingAverage7dKg = 74.4),
            WeightEntry(logicalDate = "2026-11-10", weightKg = 73.1, movingAverage7dKg = 73.5),
            WeightEntry(logicalDate = "2026-11-15", weightKg = 72.4, movingAverage7dKg = 72.5)
        )
        val suggestion = calib.evaluateCalibration(weights, logs, meals)
        assertNotNull(suggestion)
        assertTrue((suggestion?.suggestedTdeeDeltaKcal ?: 0) > 200)
    }

    @Test
    fun excelManager_roundTrip_preservesAll8Columns() {
        val excel = ExcelManager()
        val rows = listOf(
            MasterTableRow(1, "2026-11-18", 1680, 2300, 620, 74.25, 138.5f, "Torta & pollo | Rango óptimo", true, 0),
            MasterTableRow(2, "2026-11-19", 1740, 2300, 560, null, 142.0f, "Sopa instantánea + huevo", false, 0)
        )
        val bytes = excel.buildWorkbookBytes(rows)
        val parsed = excel.parseWorkbookBytes(bytes)
        assertEquals(2, parsed.size)
        assertEquals(1, parsed[0].dayNumber)
        assertEquals("2026-11-18", parsed[0].logicalDate)
        assertEquals(1680, parsed[0].intakeKcal)
        assertEquals(2300, parsed[0].expenditureKcal)
        assertEquals(620, parsed[0].deficitKcal)
        assertEquals(74.25, parsed[0].weightKg ?: 0.0, 0.001)
        assertEquals(138.5f, parsed[0].proteinG, 0.05f)
        assertEquals("Torta & pollo | Rango óptimo", parsed[0].biologicalNote)
        assertEquals(null, parsed[1].weightKg)
    }

    @Test
    fun biologicalNoteSynthesizer_includesOmittedSlotsAndDiagnosis() {
        val synth = BiologicalNoteSynthesizer()
        val log = DailyLog("2026-11-19", 1, 2300, 136, 1550, 1850, 130f)
        val meals = listOf(
            MealEntry(logicalDate = "2026-11-19", slot = MealSlot.LUNCH, title = "Torta de huevo", description = "", caloriesKcal = 620, proteinG = 28f, alertTags = listOf("exceso de sodio"))
        )
        val note = synth.synthesizeClosingNote(log, meals, null)
        assertTrue(note.contains("Omitido: Desayuno, Cena"))
        assertTrue(note.contains("Brecha vs piso"))
        assertTrue(note.contains("Alerta de sodio"))
    }

    @Test
    fun weightAchievements_unlockProgressiveMilestonesAndIdealWeight() {
        val useCase = WeightAchievementUseCase()
        val start = WeightEntry(id = 1, logicalDate = "2026-11-01", weightKg = 80.0, recordedAtEpochMillis = 100L)
        val unlocked = mutableSetOf<String>()

        // 1. Small initial drop -> FIRST_DROP
        val e1 = useCase.evaluateNewWeight(
            newWeightKg = 79.4,
            targetWeightKg = 70.0,
            previousWeightsDesc = listOf(start),
            alreadyUnlockedIds = unlocked
        )
        assertTrue(e1.isNewLowerWeight)
        assertFalse(e1.isIdealWeight)
        assertEquals(WeightAchievement.FIRST_DROP, e1.unlockedAchievement)
        unlocked += e1.unlockedAchievement!!.id

        // 2. Same or higher weight -> not lower, no achievement
        val w1 = WeightEntry(id = 2, logicalDate = "2026-11-02", weightKg = 79.4, recordedAtEpochMillis = 200L)
        val eSame = useCase.evaluateNewWeight(79.6, 70.0, listOf(w1, start), unlocked)
        assertFalse(eSame.isNewLowerWeight)
        assertNull(eSame.unlockedAchievement)

        // 3. 25%, 50%, 75%, 90% progression
        val e25 = useCase.evaluateNewWeight(77.5, 70.0, listOf(w1, start), unlocked)
        assertTrue(e25.isNewLowerWeight)
        assertEquals(25, e25.progressPercent)
        assertEquals(WeightAchievement.PROGRESS_25, e25.unlockedAchievement)
        unlocked += e25.unlockedAchievement!!.id

        val e50 = useCase.evaluateNewWeight(75.0, 70.0, listOf(w1.copy(weightKg = 77.5), start), unlocked)
        assertEquals(WeightAchievement.PROGRESS_50, e50.unlockedAchievement)
        unlocked += e50.unlockedAchievement!!.id

        val e75 = useCase.evaluateNewWeight(72.5, 70.0, listOf(w1.copy(weightKg = 75.0), start), unlocked)
        assertEquals(WeightAchievement.PROGRESS_75, e75.unlockedAchievement)
        unlocked += e75.unlockedAchievement!!.id

        val e90 = useCase.evaluateNewWeight(71.0, 70.0, listOf(w1.copy(weightKg = 72.5), start), unlocked)
        assertEquals(WeightAchievement.ALMOST_THERE_90, e90.unlockedAchievement)
        unlocked += e90.unlockedAchievement!!.id

        // 4. Ideal weight reached -> IDEAL_WEIGHT_REACHED with exact subtitle
        val eIdeal = useCase.evaluateNewWeight(70.0, 70.0, listOf(w1.copy(weightKg = 71.0), start), unlocked)
        assertTrue(eIdeal.isIdealWeight)
        assertTrue(eIdeal.isNewLowerWeight)
        assertEquals(100, eIdeal.progressPercent)
        assertEquals(WeightAchievement.IDEAL_WEIGHT_REACHED, eIdeal.unlockedAchievement)
        assertEquals("Logro conseguido, tu peso ideal", eIdeal.unlockedAchievement?.subtitleEs)

        // 5. Weight-gain goal progress check
        val gainSeriesDesc = listOf(
            WeightEntry(id = 2, logicalDate = "2026-11-10", weightKg = 64.0),
            WeightEntry(id = 1, logicalDate = "2026-11-01", weightKg = 60.0)
        )
        assertEquals(50, useCase.computeCurrentProgressPercent(gainSeriesDesc, 64.0, 68.0))
    }

    @Test
    fun foodEmojiMapper_tagsStudentMealsAndAvoidsDuplicates() {
        assertEquals("🍜 Maruchan de camarón", FoodEmojiMapper.formatTitleWithEmoji("Maruchan de camarón"))
        assertEquals("🥖 Torta de jamón", FoodEmojiMapper.formatTitleWithEmoji("Torta de jamón"))
        assertEquals("🍔 Hamburguesa doble", FoodEmojiMapper.formatTitleWithEmoji("🍔 Hamburguesa doble"))
        assertEquals("Platillo misterioso", FoodEmojiMapper.formatTitleWithEmoji("Platillo misterioso"))
    }

    @Test
    fun repeatedMealHabit_preselectsFrequentSlotMealsAndMarksToday() {
        val useCase = RepeatedMealHabitUseCase()
        val history = listOf(
            MealEntry(id = 1, logicalDate = "2026-11-17", slot = MealSlot.BREAKFAST, title = "🍳 Huevos con frijoles", description = "", caloriesKcal = 420, proteinG = 26f, createdAtEpochMillis = 1000L),
            MealEntry(id = 2, logicalDate = "2026-11-18", slot = MealSlot.BREAKFAST, title = "Huevos con frijoles", description = "", caloriesKcal = 420, proteinG = 26f, createdAtEpochMillis = 2000L),
            MealEntry(id = 3, logicalDate = "2026-11-18", slot = MealSlot.LUNCH, title = "🍜 Maruchan", description = "", caloriesKcal = 380, proteinG = 8f, createdAtEpochMillis = 3000L)
        )
        val beforeToday = useCase.computeHabitsBySlot(history, emptyList())
        val breakfastHabit = beforeToday[MealSlot.BREAKFAST]
        assertNotNull(breakfastHabit)
        assertEquals(2, breakfastHabit?.repeatCount)
        assertFalse(breakfastHabit?.alreadyLoggedToday ?: true)

        val suggested = useCase.pickSuggestedHabitForHour(beforeToday, hourOfDay = 9)
        assertEquals(MealSlot.BREAKFAST, suggested?.slot)

        val afterBreakfast = useCase.computeHabitsBySlot(
            history,
            listOf(MealEntry(id = 4, logicalDate = "2026-11-19", slot = MealSlot.BREAKFAST, title = "🍳 Huevos con frijoles", description = "", caloriesKcal = 420, proteinG = 26f))
        )
        assertTrue(afterBreakfast[MealSlot.BREAKFAST]?.alreadyLoggedToday == true)
    }

    @Test
    fun hydrationBudgetAndCheatPlanner_computeStudentMetrics() {
        val hydrationBudget = StudentBudgetAndHydrationUseCase()
        val saltyMeal = MealEntry(
            logicalDate = "2026-11-19",
            title = "🍜 Maruchan",
            description = "",
            caloriesKcal = 380,
            proteinG = 8f,
            alertTags = listOf("exceso de sodio"),
            estimatedCost = 20.0
        )
        assertEquals(11, hydrationBudget.computeWaterTargetGlasses(listOf(saltyMeal), examSurvivalMode = true))
        assertEquals(8, hydrationBudget.computeWaterTargetGlasses(emptyList(), examSurvivalMode = false))

        val budgetSummary = hydrationBudget.computeQuincenaBudgetSummary(
            UserProfile(biweeklyBudget = 1000.0),
            listOf(saltyMeal.copy(proteinG = 30f, estimatedCost = 50.0))
        )
        assertEquals(50.0, budgetSummary.spentLast14Days, 0.01)
        assertEquals(950.0, budgetSummary.remainingAmount, 0.01)
        assertFalse(budgetSummary.isOverBudget)

        val cheatPlanner = CheatMealPlannerUseCase()
        val safePlan = cheatPlanner.simulateWeeklyCheatCompensation(
            profile = UserProfile(desiredDeficitKcal = 450),
            estimatedTdeeKcal = 2300,
            floorKcal = 1550,
            ceilingKcal = 1850,
            extraCravingKcal = 600,
            spreadDays = 5
        )
        assertTrue(safePlan.keepsDebtZero)
        assertEquals(120, safePlan.dailyCompensationKcal)
        assertEquals(1730, safePlan.compensatedDailyTargetKcal)

        val harshPlan = cheatPlanner.simulateWeeklyCheatCompensation(
            profile = UserProfile(desiredDeficitKcal = 450),
            estimatedTdeeKcal = 2300,
            floorKcal = 1550,
            ceilingKcal = 1700,
            extraCravingKcal = 900,
            spreadDays = 2
        )
        assertFalse(harshPlan.keepsDebtZero)
    }
}
