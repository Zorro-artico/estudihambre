package com.example.domain.model

import kotlinx.serialization.Serializable

enum class BiologicalSex(val labelEs: String) {
    MALE("Masculino"),
    FEMALE("Femenino")
}

enum class DailyActivityLevel(
    val labelEs: String,
    val multiplier: Double,
    val descriptionEs: String
) {
    SEDENTARY("Sedentario (estudio/escritorio)", 1.25, "Mayoría del día sentado, caminatas breves"),
    LIGHT("Ligero (campus / mandados)", 1.375, "Caminata diaria moderada entre clases o trabajo"),
    MODERATE("Activo (movimiento constante)", 1.55, "Trabajo de pie, bicicleta o alta actividad diaria"),
    HIGH("Muy activo (alta demanda física)", 1.725, "Jornadas físicas exigentes todos los días")
}

enum class StrengthTrainingMode(
    val labelEs: String,
    val exerciseBonusKcal: Int,
    val minProteinPerKg: Float,
    val catabolicRiskFactor: Float
) {
    NONE("Sin entrenamiento de fuerza", 0, 1.4f, 1.30f),
    LIGHT_HYPERTROPHY("Fuerza 2-3 días/sem", 180, 1.7f, 1.0f),
    INTENSE_STRENGTH("Fuerza / Hipertrofia 4-6 días/sem", 300, 2.0f, 0.85f)
}

enum class ContainerShape(val labelEs: String, val formulaLabel: String) {
    CYLINDRICAL("Cilíndrico", "V = π · r² · h"),
    RECTANGULAR("Rectangular", "V = l · a · h"),
    HEMISPHERICAL("Semiesférico", "V = (2/3) · π · r³")
}

enum class SyncStatus {
    SYNCED,
    PENDING_SYNC,
    FAILED
}

enum class MealSlot(val labelEs: String) {
    BREAKFAST("Desayuno"),
    LUNCH("Comida"),
    DINNER("Cena"),
    SNACK("Colación / Madrugada")
}

data class UserProfile(
    val id: Int = 1,
    val currentWeightKg: Double = 74.0,
    val targetWeightKg: Double = 68.0,
    val heightCm: Double = 172.0,
    val ageYears: Int = 22,
    val sex: BiologicalSex = BiologicalSex.MALE,
    val activityLevel: DailyActivityLevel = DailyActivityLevel.LIGHT,
    val strengthTraining: StrengthTrainingMode = StrengthTrainingMode.LIGHT_HYPERTROPHY,
    val trainingRoutineNotes: String = "Pesas 3 días por semana + caminata al campus",
    val dailyRoutineNotes: String = "Caminata diaria al campus y clases presenciales",
    val routineAdaptationSummary: String = "",
    val routineBonusKcal: Int = 0,
    val isSoundEnabled: Boolean = true,
    val targetProteinPerKg: Float = 1.8f,
    val desiredDeficitKcal: Int = 450,
    val baseErrorBudgetKcal: Int = 136,
    val selectedGeminiModel: String = "gemini-3.8-flash",
    val calibratedTdeeAdjustmentKcal: Int = 0,
    val biweeklyBudget: Double = 1200.0,
    val examSurvivalMode: Boolean = false
)

data class ContainerItem(
    val id: Long = 0,
    val name: String,
    val shape: ContainerShape,
    val radiusCm: Double = 0.0,
    val heightCm: Double = 0.0,
    val lengthCm: Double = 0.0,
    val widthCm: Double = 0.0,
    val volumeMl: Double = 0.0,
    val isFavorite: Boolean = false
)

data class DailyLog(
    val logicalDate: String, // YYYY-MM-DD
    val dayNumber: Int,
    val estimatedExpenditureKcal: Int,
    val errorBudgetKcal: Int,
    val floorKcal: Int,
    val ceilingKcal: Int,
    val minProteinG: Float,
    val catabolicDebtCarryoverKcal: Int = 0,
    val isClosed: Boolean = false,
    val biologicalNote: String = "",
    val waterGlasses: Int = 0,
    val waterTargetGlasses: Int = 8
)

data class MealEntry(
    val id: Long = 0,
    val logicalDate: String,
    val slot: MealSlot = MealSlot.LUNCH,
    val title: String,
    val description: String,
    val caloriesKcal: Int,
    val proteinG: Float,
    val alertTags: List<String> = emptyList(),
    val summary: String = "",
    val containerId: Long? = null,
    val containerName: String? = null,
    val fillPercent: Int? = null,
    val occupiedVolumeMl: Double? = null,
    val batchServings: Int = 1,
    val estimatedCost: Double? = null,
    val localPhotoPath: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val isFavorite: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

data class WeightEntry(
    val id: Long = 0,
    val logicalDate: String,
    val weightKg: Double,
    val movingAverage7dKg: Double = weightKg,
    val theoreticalDryWeightKg: Double = weightKg,
    val waterRetentionGrams: Int = 0,
    val isWhooshDetected: Boolean = false,
    val outlierConfirmed: Boolean = false,
    val recordedAtEpochMillis: Long = System.currentTimeMillis()
)

data class SyncQueueItem(
    val id: Long = 0,
    val mealEntryId: Long,
    val targetLogicalDate: String,
    val promptContext: String,
    val localPhotoPath: String?,
    val batchServings: Int = 1,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

enum class EstimationScenario(val labelEs: String) {
    LOWER("Menor al común"),
    AVERAGE("Promedio"),
    HIGHER("Mayor al común")
}

@Serializable
data class GeminiNutritionResponse(
    val calorias_kcal: Int,
    val proteina_g: Float,
    val etiquetas_alerta: List<String>,
    val resumen_breve: String,
    val calorias_menor: Int = (calorias_kcal * 0.82).toInt(),
    val proteina_menor: Float = proteina_g * 0.88f,
    val calorias_mayor: Int = (calorias_kcal * 1.20).toInt(),
    val proteina_mayor: Float = proteina_g * 1.12f,
    val trampas_ocultas: List<String> = emptyList(),
    val sugerencia_ahorro_kcal: String = ""
)

data class OptimalRangeMetrics(
    val estimatedTdeeKcal: Int,
    val floorKcal: Int,
    val ceilingKcal: Int,
    val minProteinG: Float,
    val totalIntakeKcal: Int,
    val totalProteinG: Float,
    val missingToFloorKcal: Int,
    val remainingBeforeCeilingKcal: Int,
    val projectedDeficitKcal: Int,
    val catabolicDebtCarryoverKcal: Int,
    val statusBadge: MetabolicBuffStatus
)

enum class MetabolicBuffStatus(val titleEs: String, val subtitleEs: String) {
    ELIXIR_ANABOLICO("Elixir Anabólico", "Rango óptimo y piso proteico cubiertos"),
    FORJA_ACTIVA("En Construcción", "Falta energía para alcanzar el piso seguro"),
    ESCUDO_TECHO("Cerca del Techo", "Margen de déficit ajustado; cuida la densidad"),
    MALEFICIO_CATABOLICO("Alerta Catabólica", "Déficit excesivo o deuda heredada activa"),
    EXCEDENTE("Sobre el Techo", "Superó el límite de déficit configurado")
}

data class MasterTableRow(
    val dayNumber: Int,
    val logicalDate: String,
    val intakeKcal: Int,
    val expenditureKcal: Int,
    val deficitKcal: Int,
    val weightKg: Double?,
    val proteinG: Float,
    val biologicalNote: String,
    val isClosed: Boolean,
    val pendingSyncCount: Int
)

data class MasterTableIntervalStats(
    val dayCount: Int,
    val totalIntakeKcal: Int,
    val avgDailyIntakeKcal: Double,
    val totalExpenditureKcal: Int,
    val netDeficitKcal: Int,
    val avgDailyDeficitKcal: Double,
    val theoreticalFatOxidizedGrams: Double,
    val realScaleDeltaKg: Double?
)

data class CalibrationSuggestion(
    val consecutiveDaysAnalyzed: Int,
    val theoreticalWeightDeltaKg: Double,
    val realMovingAverageDeltaKg: Double,
    val dailyDiscrepancyKcal: Int,
    val suggestedTdeeDeltaKcal: Int,
    val summaryEs: String
)

data class DeficitProjectionPoint(
    val weekIndex: Int,
    val projectedWeightKg: Double,
    val cumulativeDeficitKcal: Int
)

data class DeficitRiskAssessment(
    val isDangerous: Boolean,
    val riskLevelLabel: String,
    val warnings: List<String>
)

@Serializable
data class RoutineAdaptationResponse(
    val actividad_sugerida: String,
    val entrenamiento_sugerido: String,
    val kcal_ajuste_diario: Int,
    val resumen_adaptacion: String
)

enum class WeightAchievement(
    val id: String,
    val titleEs: String,
    val subtitleEs: String,
    val thresholdPct: Int
) {
    FIRST_DROP("first_drop", "¡Logro conseguido!", "Primer descenso en báscula", 1),
    PROGRESS_25("progress_25", "¡Logro conseguido!", "25% del camino recorrido", 25),
    PROGRESS_50("progress_50", "¡Logro conseguido!", "Mitad de la travesía (50%)", 50),
    PROGRESS_75("progress_75", "¡Logro conseguido!", "Recta final (75%)", 75),
    ALMOST_THERE_90("almost_90", "¡Logro conseguido!", "A un paso de la cima (90%)", 90),
    IDEAL_WEIGHT_REACHED("ideal_weight", "¡Logro conseguido!", "Logro conseguido, tu peso ideal", 100)
}

enum class HabitAchievement(
    val id: String,
    val titleEs: String,
    val subtitleEs: String
) {
    CHEF_QUINCENA("chef_quincena", "¡Logro culinario!", "Chef de Quincena (7d en presupuesto)"),
    PROTEIN_MASTER("protein_master", "¡Logro culinario!", "Maestro de la Proteína (3d seguidos)"),
    WATER_HUNTER("water_hunter", "¡Logro de hábito!", "Cazador de Agua (3d hidratado)"),
    NO_DEBT_WARRIOR("no_debt_warrior", "¡Logro metabólico!", "Cero Deuda Catabólica (5 días)")
}

data class AchievementBanner(
    val titleEs: String,
    val subtitleEs: String,
    val isIdealWeight: Boolean = false
)

data class WeightCelebrationEvent(
    val isNewLowerWeight: Boolean,
    val unlockedAchievement: WeightAchievement?,
    val isIdealWeight: Boolean,
    val progressPercent: Int
)

data class RepeatedMealHabit(
    val slot: MealSlot,
    val meal: MealEntry,
    val repeatCount: Int,
    val alreadyLoggedToday: Boolean = false
)

data class QuincenaBudgetSummary(
    val budgetAmount: Double,
    val spentLast14Days: Double,
    val remainingAmount: Double,
    val avgProteinPerCurrency: Double,
    val topValueMeals: List<MealEntry>,
    val isOverBudget: Boolean
)

data class MealComparisonResult(
    val optionATitle: String,
    val optionA: GeminiNutritionResponse,
    val optionBTitle: String,
    val optionB: GeminiNutritionResponse,
    val recommendedChoice: String, // "A" o "B"
    val comparisonSummaryEs: String
)

data class PantryRecipeSuggestion(
    val nombre: String,
    val ingredientes_usados: String,
    val pasos_rapidos: String,
    val calorias_kcal: Int,
    val proteina_g: Float,
    val costo_nivel: String
)

data class WeeklyCheatBudgetPlan(
    val extraKcal: Int,
    val spreadDays: Int,
    val dailyCompensationKcal: Int,
    val compensatedDailyTargetKcal: Int,
    val keepsDebtZero: Boolean,
    val projectedWeeklyDeficitKcal: Int,
    val recommendationEs: String
)
