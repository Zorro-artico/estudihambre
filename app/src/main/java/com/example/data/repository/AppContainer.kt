package com.example.data.repository

import android.content.Context
import com.example.data.excel.ExcelManager
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.remote.GeminiNutritionService
import com.example.data.remote.OpenFoodFactsService
import com.example.domain.usecase.BiologicalNoteSynthesizer
import com.example.domain.usecase.CheatMealPlannerUseCase
import com.example.domain.usecase.ContainerGeometryUseCase
import com.example.domain.usecase.DeficitProjectionUseCase
import com.example.domain.usecase.DryWeightAndWaterFilterUseCase
import com.example.domain.usecase.MasterTableStatsUseCase
import com.example.domain.usecase.MetabolicRangeAndDebtUseCase
import com.example.domain.usecase.RepeatedMealHabitUseCase
import com.example.domain.usecase.StudentBudgetAndHydrationUseCase
import com.example.domain.usecase.ThermodynamicCalibrationUseCase
import com.example.domain.usecase.WeightAchievementUseCase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val prefs = PreferencesManager(appContext)

    val geometryUseCase = ContainerGeometryUseCase()
    val metabolicUseCase = MetabolicRangeAndDebtUseCase()
    val dryWeightUseCase = DryWeightAndWaterFilterUseCase()
    val calibrationUseCase = ThermodynamicCalibrationUseCase()
    val noteSynthesizer = BiologicalNoteSynthesizer()
    val deficitProjectionUseCase = DeficitProjectionUseCase()
    val masterTableStatsUseCase = MasterTableStatsUseCase()
    val weightAchievementUseCase = WeightAchievementUseCase()
    val budgetAndHydrationUseCase = StudentBudgetAndHydrationUseCase()
    val repeatedMealHabitUseCase = RepeatedMealHabitUseCase()
    val cheatMealPlannerUseCase = CheatMealPlannerUseCase()

    val repository = NutriRepository(
        appContext = appContext,
        db = db,
        prefs = prefs,
        gemini = GeminiNutritionService(),
        openFoodFacts = OpenFoodFactsService(),
        geometryUseCase = geometryUseCase,
        metabolicUseCase = metabolicUseCase,
        dryWeightUseCase = dryWeightUseCase,
        calibrationUseCase = calibrationUseCase,
        noteSynthesizer = noteSynthesizer,
        deficitProjectionUseCase = deficitProjectionUseCase,
        masterTableStatsUseCase = masterTableStatsUseCase,
        weightAchievementUseCase = weightAchievementUseCase,
        budgetAndHydrationUseCase = budgetAndHydrationUseCase,
        repeatedMealHabitUseCase = repeatedMealHabitUseCase,
        cheatMealPlannerUseCase = cheatMealPlannerUseCase,
        excelManager = ExcelManager()
    )
}
