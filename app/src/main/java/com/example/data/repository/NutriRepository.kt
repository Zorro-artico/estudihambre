package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.example.data.excel.ExcelManager
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.local.entity.ContainerEntity
import com.example.data.local.entity.DailyLogEntity
import com.example.data.local.entity.MealEntryEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WeightEntryEntity
import com.example.data.remote.BarcodeProductLookupResult
import com.example.data.remote.GeminiNutritionService
import com.example.data.remote.OpenFoodFactsService
import com.example.domain.model.BiologicalSex
import com.example.domain.model.CalibrationSuggestion
import com.example.domain.model.ContainerItem
import com.example.domain.model.ContainerShape
import com.example.domain.model.DailyLog
import com.example.domain.model.GeminiNutritionResponse
import com.example.domain.model.HabitAchievement
import com.example.domain.model.MasterTableRow
import com.example.domain.model.MealComparisonResult
import com.example.domain.model.MealEntry
import com.example.domain.model.MealSlot
import com.example.domain.model.PantryRecipeSuggestion
import com.example.domain.model.QuincenaBudgetSummary
import com.example.domain.model.RepeatedMealHabit
import com.example.domain.model.RoutineAdaptationResponse
import com.example.domain.model.SyncQueueItem
import com.example.domain.model.SyncStatus
import com.example.domain.model.UserProfile
import com.example.domain.model.WeightCelebrationEvent
import com.example.domain.model.WeightEntry
import com.example.domain.usecase.BiologicalNoteSynthesizer
import com.example.domain.usecase.CheatMealPlannerUseCase
import com.example.domain.usecase.ContainerGeometryUseCase
import com.example.domain.usecase.DeficitProjectionUseCase
import com.example.domain.usecase.DryWeightAndWaterFilterUseCase
import com.example.domain.usecase.FoodEmojiMapper
import com.example.domain.usecase.MasterTableStatsUseCase
import com.example.domain.usecase.MetabolicRangeAndDebtUseCase
import com.example.domain.usecase.RepeatedMealHabitUseCase
import com.example.domain.usecase.StudentBudgetAndHydrationUseCase
import com.example.domain.usecase.ThermodynamicCalibrationUseCase
import com.example.domain.usecase.WeightAchievementUseCase
import com.example.ui.widget.DailyCauldronWidgetProvider
import com.example.util.DateUtils
import com.example.util.ImageStorageHelper
import com.example.worker.GeminiSyncWorker
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class NutriRepository(
    private val appContext: Context,
    private val db: AppDatabase,
    private val prefs: PreferencesManager,
    val gemini: GeminiNutritionService,
    val openFoodFacts: OpenFoodFactsService,
    val geometryUseCase: ContainerGeometryUseCase,
    val metabolicUseCase: MetabolicRangeAndDebtUseCase,
    val dryWeightUseCase: DryWeightAndWaterFilterUseCase,
    val calibrationUseCase: ThermodynamicCalibrationUseCase,
    val noteSynthesizer: BiologicalNoteSynthesizer,
    val deficitProjectionUseCase: DeficitProjectionUseCase,
    val masterTableStatsUseCase: MasterTableStatsUseCase,
    val weightAchievementUseCase: WeightAchievementUseCase,
    val budgetAndHydrationUseCase: StudentBudgetAndHydrationUseCase,
    val repeatedMealHabitUseCase: RepeatedMealHabitUseCase,
    val cheatMealPlannerUseCase: CheatMealPlannerUseCase,
    val excelManager: ExcelManager
) {
    private val profileDao = db.userProfileDao()
    private val dailyLogDao = db.dailyLogDao()
    private val mealDao = db.mealDao()
    private val containerDao = db.containerDao()
    private val weightDao = db.weightDao()
    private val syncQueueDao = db.syncQueueDao()

    val currencySymbol: Flow<String> = prefs.currencySymbol
    val unlockedAchievements: Flow<Set<String>> = prefs.unlockedAchievements

    val profile: Flow<UserProfile> = profileDao.observeProfile().map { entity ->
        entity?.toDomain() ?: UserProfile()
    }

    val containers: Flow<List<ContainerItem>> = containerDao.observeAllContainers().map { list ->
        list.map { it.toDomain() }
    }

    val favoriteMeals: Flow<List<MealEntry>> = mealDao.observeFavoriteMeals().map { list ->
        list.map { it.toDomain() }
    }

    val allMealsDesc: Flow<List<MealEntry>> = mealDao.observeAllMeals().map { list ->
        list.map { it.toDomain() }
    }

    val allLogsDesc: Flow<List<DailyLog>> = dailyLogDao.observeAllLogsDesc().map { list ->
        list.map { it.toDomain() }
    }

    val weightsDesc: Flow<List<WeightEntry>> = weightDao.observeAllWeightsDesc().map { list ->
        list.map { it.toDomain() }
    }

    val syncQueue: Flow<List<SyncQueueItem>> = syncQueueDao.observeQueue().map { list ->
        list.map { it.toDomain() }
    }

    val quincenaBudgetSummary: Flow<QuincenaBudgetSummary> = combine(
        profile,
        allMealsDesc
    ) { prof, meals ->
        val cutoffDate = DateUtils.shiftLogicalDate(DateUtils.todayLogicalDate(), -13)
        val recentMeals = meals.filter { it.logicalDate >= cutoffDate }
        budgetAndHydrationUseCase.computeQuincenaBudgetSummary(prof, recentMeals)
    }

    val hydrationStreak: Flow<Int> = allLogsDesc.map { logs ->
        budgetAndHydrationUseCase.computeHydrationStreak(logs)
    }

    val masterTableRows: Flow<List<MasterTableRow>> = combine(
        dailyLogDao.observeAllLogsDesc(),
        mealDao.observeAllMeals(),
        weightDao.observeAllWeightsDesc(),
        syncQueueDao.observeQueue()
    ) { logs, meals, weights, queue ->
        val mealsByDate = meals.map { it.toDomain() }.groupBy { it.logicalDate }
        val weightByDate = weights.map { it.toDomain() }.associateBy { it.logicalDate }
        val pendingByDate = queue.groupBy { it.targetLogicalDate }

        logs.map { entity ->
            val log = entity.toDomain()
            val dayMeals = mealsByDate[log.logicalDate].orEmpty()
            val intake = log.errorBudgetKcal + dayMeals.sumOf { it.caloriesKcal }
            val protein = dayMeals.sumOf { it.proteinG.toDouble() }.toFloat()
            val deficit = log.estimatedExpenditureKcal - intake
            MasterTableRow(
                dayNumber = log.dayNumber,
                logicalDate = log.logicalDate,
                intakeKcal = intake,
                expenditureKcal = log.estimatedExpenditureKcal,
                deficitKcal = deficit,
                weightKg = weightByDate[log.logicalDate]?.weightKg,
                proteinG = protein,
                biologicalNote = log.biologicalNote,
                isClosed = log.isClosed,
                pendingSyncCount = pendingByDate[log.logicalDate]?.size ?: 0
            )
        }
    }

    fun observeDailyLog(logicalDate: String): Flow<DailyLog?> {
        return dailyLogDao.observeLogByDate(logicalDate).map { it?.toDomain() }
    }

    fun observeMealsForDate(logicalDate: String): Flow<List<MealEntry>> {
        return mealDao.observeMealsForDate(logicalDate).map { list -> list.map { it.toDomain() } }
    }

    fun observeHabitsBySlot(logicalDate: String): Flow<Map<MealSlot, RepeatedMealHabit>> {
        return allMealsDesc.map { all ->
            val todayMeals = all.filter { it.logicalDate == logicalDate }
            repeatedMealHabitUseCase.computeHabitsBySlot(all, todayMeals)
        }
    }

    suspend fun ensureDailyLogExists(logicalDate: String): DailyLog {
        val existing = dailyLogDao.getLogByDateOnce(logicalDate)
        if (existing != null) return existing.toDomain()

        val currentProfile = profileDao.getProfileOnce()?.toDomain() ?: UserProfile().also {
            profileDao.upsertProfile(UserProfileEntity.fromDomain(it))
        }
        val prevLog = dailyLogDao.getPreviousLog(logicalDate)?.toDomain()
        val carryoverDebt = if (prevLog != null && prevLog.isClosed) {
            val prevMeals = mealDao.getMealsForDateOnce(prevLog.logicalDate).map { it.toDomain() }
            metabolicUseCase.computeNextDayDebtCarryover(prevLog, prevMeals)
        } else {
            prevLog?.catabolicDebtCarryoverKcal ?: 0
        }

        val maxDay = dailyLogDao.getMaxDayNumber() ?: 0
        val tdee = metabolicUseCase.calculateEstimatedTdeeKcal(currentProfile)
        val (floor, ceiling, minProt) = metabolicUseCase.calculateOptimalFloorAndCeiling(
            profile = currentProfile,
            tdeeKcal = tdee,
            carryoverDebtKcal = carryoverDebt
        )

        val created = DailyLog(
            logicalDate = logicalDate,
            dayNumber = maxDay + 1,
            estimatedExpenditureKcal = tdee,
            errorBudgetKcal = currentProfile.baseErrorBudgetKcal,
            floorKcal = floor,
            ceilingKcal = ceiling,
            minProteinG = minProt,
            catabolicDebtCarryoverKcal = carryoverDebt,
            isClosed = false,
            biologicalNote = "",
            waterGlasses = 0,
            waterTargetGlasses = budgetAndHydrationUseCase.computeWaterTargetGlasses(emptyList(), currentProfile.examSurvivalMode)
        )
        dailyLogDao.upsertLog(DailyLogEntity.fromDomain(created))
        refreshTodayWidgetSnapshot()
        return created
    }

    suspend fun saveProfile(updated: UserProfile, updateOpenTodaySnapshot: Boolean = true) {
        profileDao.upsertProfile(UserProfileEntity.fromDomain(updated))
        if (updateOpenTodaySnapshot) {
            val today = DateUtils.todayLogicalDate()
            val todayLog = dailyLogDao.getLogByDateOnce(today)?.toDomain()
            if (todayLog != null && !todayLog.isClosed) {
                val tdee = metabolicUseCase.calculateEstimatedTdeeKcal(updated)
                val (floor, ceiling, minProt) = metabolicUseCase.calculateOptimalFloorAndCeiling(
                    profile = updated,
                    tdeeKcal = tdee,
                    carryoverDebtKcal = todayLog.catabolicDebtCarryoverKcal
                )
                val todayMeals = mealDao.getMealsForDateOnce(today).map { it.toDomain() }
                val waterTarget = budgetAndHydrationUseCase.computeWaterTargetGlasses(todayMeals, updated.examSurvivalMode)
                dailyLogDao.upsertLog(
                    DailyLogEntity.fromDomain(
                        todayLog.copy(
                            estimatedExpenditureKcal = tdee,
                            errorBudgetKcal = updated.baseErrorBudgetKcal,
                            floorKcal = floor,
                            ceilingKcal = ceiling,
                            minProteinG = minProt,
                            waterTargetGlasses = waterTarget
                        )
                    )
                )
            }
        }
        refreshTodayWidgetSnapshot()
    }

    suspend fun toggleExamSurvivalMode(enabled: Boolean) {
        val current = profileDao.getProfileOnce()?.toDomain() ?: UserProfile()
        saveProfile(current.copy(examSurvivalMode = enabled), updateOpenTodaySnapshot = true)
    }

    suspend fun updateWaterGlasses(logicalDate: String, delta: Int): List<HabitAchievement> {
        val log = ensureDailyLogExists(logicalDate)
        val nextCount = (log.waterGlasses + delta).coerceIn(0, 24)
        dailyLogDao.upsertLog(DailyLogEntity.fromDomain(log.copy(waterGlasses = nextCount)))
        refreshTodayWidgetSnapshot()
        return evaluateAndUnlockHabitAchievements()
    }

    private suspend fun refreshWaterTargetForDate(logicalDate: String) {
        val log = dailyLogDao.getLogByDateOnce(logicalDate)?.toDomain() ?: return
        val prof = profileDao.getProfileOnce()?.toDomain() ?: UserProfile()
        val meals = mealDao.getMealsForDateOnce(logicalDate).map { it.toDomain() }
        val target = budgetAndHydrationUseCase.computeWaterTargetGlasses(meals, prof.examSurvivalMode)
        if (target != log.waterTargetGlasses) {
            dailyLogDao.upsertLog(DailyLogEntity.fromDomain(log.copy(waterTargetGlasses = target)))
        }
    }

    suspend fun evaluateAndUnlockHabitAchievements(): List<HabitAchievement> {
        val prof = profileDao.getProfileOnce()?.toDomain() ?: UserProfile()
        val logsAsc = dailyLogDao.getAllLogsAscOnce().map { it.toDomain() }
        val allMeals = mealDao.getAllMealsOnce().map { it.toDomain() }
        val mealsByDate = allMeals.groupBy { it.logicalDate }
        val cutoffDate = DateUtils.shiftLogicalDate(DateUtils.todayLogicalDate(), -13)
        val budgetSummary = budgetAndHydrationUseCase.computeQuincenaBudgetSummary(
            prof,
            allMeals.filter { it.logicalDate >= cutoffDate }
        )
        val alreadyUnlocked = prefs.getUnlockedAchievementsOnce()
        val newlyUnlocked = budgetAndHydrationUseCase.evaluateHabitAchievements(
            logsAsc = logsAsc,
            mealsByDate = mealsByDate,
            budgetSummary = budgetSummary,
            alreadyUnlockedIds = alreadyUnlocked
        )
        newlyUnlocked.forEach { prefs.unlockAchievement(it.id) }
        return newlyUnlocked
    }

    suspend fun setCurrencySymbol(symbol: String) {
        prefs.setCurrencySymbol(symbol)
    }

    suspend fun saveContainer(container: ContainerItem) {
        val computedVolume = geometryUseCase.calculateTotalVolumeMl(
            shape = container.shape,
            radiusCm = container.radiusCm,
            heightCm = container.heightCm,
            lengthCm = container.lengthCm,
            widthCm = container.widthCm
        )
        containerDao.insertContainer(
            ContainerEntity.fromDomain(container.copy(volumeMl = computedVolume))
        )
    }

    suspend fun addStudentContainerPresets() {
        val presets = listOf(
            ContainerItem(
                name = "Vaso Sopa Instantánea",
                shape = ContainerShape.CYLINDRICAL,
                radiusCm = 4.6,
                heightCm = 10.5,
                isFavorite = true
            ),
            ContainerItem(
                name = "Tupper Estudiantil Mediano",
                shape = ContainerShape.RECTANGULAR,
                lengthCm = 14.0,
                widthCm = 14.0,
                heightCm = 6.0,
                isFavorite = true
            ),
            ContainerItem(
                name = "Bowl Hondo",
                shape = ContainerShape.HEMISPHERICAL,
                radiusCm = 7.5,
                isFavorite = false
            )
        )
        presets.forEach { saveContainer(it) }
    }

    suspend fun toggleContainerFavorite(container: ContainerItem) {
        containerDao.insertContainer(
            ContainerEntity.fromDomain(container.copy(isFavorite = !container.isFavorite))
        )
    }

    suspend fun deleteContainer(id: Long) {
        containerDao.deleteContainerById(id)
    }

    suspend fun lookupBarcodeProduct(barcode: String): Result<BarcodeProductLookupResult> {
        return openFoodFacts.lookupBarcode(barcode)
    }

    suspend fun simulateMealWithGemini(
        description: String,
        photoPath: String?,
        container: ContainerItem?,
        fillPercent: Int?,
        batchServings: Int
    ): GeminiNutritionResponse {
        val profile = profileDao.getProfileOnce()?.toDomain() ?: UserProfile()
        val occupiedMl = if (container != null && fillPercent != null) {
            geometryUseCase.calculateOccupiedVolumeMl(container.volumeMl, fillPercent)
        } else null

        val prompt = gemini.buildMealPrompt(
            description = description,
            containerName = container?.name,
            fillPercent = fillPercent,
            occupiedVolumeMl = occupiedMl,
            batchServings = batchServings,
            dailyRoutineNotes = profile.dailyRoutineNotes,
            trainingNotes = profile.trainingRoutineNotes
        )

        val bitmap: Bitmap? = photoPath?.let { ImageStorageHelper.loadBitmapForAi(it) }
        return try {
            gemini.analyzeMeal(
                modelName = profile.selectedGeminiModel,
                promptText = prompt,
                photoBitmap = bitmap
            )
        } finally {
            bitmap?.recycle()
        }
    }

    suspend fun compareMealsAAndBWithGemini(
        optionADesc: String,
        optionAPhotoPath: String?,
        optionBDesc: String,
        optionBPhotoPath: String?,
        remainingBeforeCeilingKcal: Int,
        missingProteinG: Float
    ): MealComparisonResult {
        val profile = profileDao.getProfileOnce()?.toDomain() ?: UserProfile()
        val bitmapA = optionAPhotoPath?.let { ImageStorageHelper.loadBitmapForAi(it) }
        val bitmapB = optionBPhotoPath?.let { ImageStorageHelper.loadBitmapForAi(it) }
        return try {
            gemini.compareMealsAAndB(
                modelName = profile.selectedGeminiModel,
                optionADesc = optionADesc,
                optionABitmap = bitmapA,
                optionBDesc = optionBDesc,
                optionBBitmap = bitmapB,
                remainingBeforeCeilingKcal = remainingBeforeCeilingKcal,
                missingProteinG = missingProteinG
            )
        } finally {
            bitmapA?.recycle()
            bitmapB?.recycle()
        }
    }

    suspend fun generatePantryRecipesWithGemini(
        ingredientsText: String,
        fridgePhotoPath: String?,
        remainingBeforeCeilingKcal: Int,
        missingProteinG: Float
    ): List<PantryRecipeSuggestion> {
        val profile = profileDao.getProfileOnce()?.toDomain() ?: UserProfile()
        val bitmap = fridgePhotoPath?.let { ImageStorageHelper.loadBitmapForAi(it) }
        return try {
            gemini.generatePantryRecipes(
                modelName = profile.selectedGeminiModel,
                ingredientsText = ingredientsText,
                fridgeBitmap = bitmap,
                remainingBeforeCeilingKcal = remainingBeforeCeilingKcal,
                missingProteinG = missingProteinG
            )
        } finally {
            bitmap?.recycle()
        }
    }

    suspend fun confirmAnalyzedMeal(
        logicalDate: String,
        slot: MealSlot,
        title: String,
        description: String,
        caloriesKcal: Int,
        proteinG: Float,
        alertTags: List<String>,
        summary: String,
        container: ContainerItem?,
        fillPercent: Int?,
        batchServings: Int,
        estimatedCost: Double?,
        isFavorite: Boolean,
        tempPhotoPathToDelete: String?
    ): List<HabitAchievement> {
        ensureDailyLogExists(logicalDate)
        val occupiedMl = if (container != null && fillPercent != null) {
            geometryUseCase.calculateOccupiedVolumeMl(container.volumeMl, fillPercent)
        } else null

        val rawTitle = title.ifBlank { description.take(40).ifBlank { "Comida registrada" } }
        val titledWithEmoji = FoodEmojiMapper.formatTitleWithEmoji(rawTitle, description)

        val entry = MealEntry(
            logicalDate = logicalDate,
            slot = slot,
            title = titledWithEmoji,
            description = description,
            caloriesKcal = caloriesKcal,
            proteinG = proteinG,
            alertTags = alertTags,
            summary = summary,
            containerId = container?.id,
            containerName = container?.name,
            fillPercent = fillPercent,
            occupiedVolumeMl = occupiedMl,
            batchServings = batchServings.coerceAtLeast(1),
            estimatedCost = estimatedCost,
            localPhotoPath = null,
            syncStatus = SyncStatus.SYNCED,
            isFavorite = isFavorite
        )
        mealDao.insertMeal(MealEntryEntity.fromDomain(entry))
        ImageStorageHelper.deleteLocalPhotoIfOwned(appContext, tempPhotoPathToDelete)
        refreshWaterTargetForDate(logicalDate)
        refreshTodayWidgetSnapshot()
        return evaluateAndUnlockHabitAchievements()
    }

    suspend fun enqueueOfflineMeal(
        logicalDate: String,
        slot: MealSlot,
        title: String,
        description: String,
        container: ContainerItem?,
        fillPercent: Int?,
        batchServings: Int,
        estimatedCost: Double?,
        isFavorite: Boolean,
        internalPhotoPath: String?
    ) {
        ensureDailyLogExists(logicalDate)
        val profile = profileDao.getProfileOnce()?.toDomain() ?: UserProfile()
        val occupiedMl = if (container != null && fillPercent != null) {
            geometryUseCase.calculateOccupiedVolumeMl(container.volumeMl, fillPercent)
        } else null

        val rawTitle = title.ifBlank { description.take(36).ifBlank { "Comida pendiente" } }
        val titledWithEmoji = FoodEmojiMapper.formatTitleWithEmoji(rawTitle, description)

        val mealId = mealDao.insertMeal(
            MealEntryEntity.fromDomain(
                MealEntry(
                    logicalDate = logicalDate,
                    slot = slot,
                    title = titledWithEmoji,
                    description = description,
                    caloriesKcal = 0,
                    proteinG = 0f,
                    alertTags = listOf("pendiente de análisis IA"),
                    summary = "En cola offline para procesarse al recuperar red.",
                    containerId = container?.id,
                    containerName = container?.name,
                    fillPercent = fillPercent,
                    occupiedVolumeMl = occupiedMl,
                    batchServings = batchServings.coerceAtLeast(1),
                    estimatedCost = estimatedCost,
                    localPhotoPath = internalPhotoPath,
                    syncStatus = SyncStatus.PENDING_SYNC,
                    isFavorite = isFavorite
                )
            )
        )

        val prompt = gemini.buildMealPrompt(
            description = description,
            containerName = container?.name,
            fillPercent = fillPercent,
            occupiedVolumeMl = occupiedMl,
            batchServings = batchServings,
            dailyRoutineNotes = profile.dailyRoutineNotes,
            trainingNotes = profile.trainingRoutineNotes
        )

        syncQueueDao.enqueue(
            SyncQueueEntity.fromDomain(
                SyncQueueItem(
                    mealEntryId = mealId,
                    targetLogicalDate = logicalDate,
                    promptContext = prompt,
                    localPhotoPath = internalPhotoPath,
                    batchServings = batchServings.coerceAtLeast(1)
                )
            )
        )
        GeminiSyncWorker.enqueue(appContext)
        refreshTodayWidgetSnapshot()
    }

    suspend fun retrySyncQueue() {
        GeminiSyncWorker.enqueue(appContext)
    }

    suspend fun updateMeal(meal: MealEntry) {
        val formatted = meal.copy(
            title = FoodEmojiMapper.formatTitleWithEmoji(meal.title, meal.description)
        )
        mealDao.updateMeal(MealEntryEntity.fromDomain(formatted))
        refreshWaterTargetForDate(meal.logicalDate)
        refreshTodayWidgetSnapshot()
    }

    suspend fun reassignMealToPreviousDay(meal: MealEntry) {
        val targetDate = DateUtils.previousLogicalDate(meal.logicalDate)
        ensureDailyLogExists(targetDate)
        mealDao.updateMeal(MealEntryEntity.fromDomain(meal.copy(logicalDate = targetDate)))
        refreshWaterTargetForDate(meal.logicalDate)
        refreshWaterTargetForDate(targetDate)
        refreshTodayWidgetSnapshot()
    }

    suspend fun deleteMeal(meal: MealEntry) {
        syncQueueDao.deleteByMealId(meal.id)
        ImageStorageHelper.deleteLocalPhotoIfOwned(appContext, meal.localPhotoPath)
        mealDao.deleteMealById(meal.id)
        refreshWaterTargetForDate(meal.logicalDate)
        refreshTodayWidgetSnapshot()
    }

    suspend fun quickAddFavoriteMeal(favorite: MealEntry, targetLogicalDate: String): List<HabitAchievement> {
        ensureDailyLogExists(targetLogicalDate)
        val titled = FoodEmojiMapper.formatTitleWithEmoji(favorite.title, favorite.description)
        mealDao.insertMeal(
            MealEntryEntity.fromDomain(
                favorite.copy(
                    id = 0,
                    logicalDate = targetLogicalDate,
                    title = titled,
                    syncStatus = SyncStatus.SYNCED,
                    localPhotoPath = null,
                    createdAtEpochMillis = System.currentTimeMillis()
                )
            )
        )
        refreshWaterTargetForDate(targetLogicalDate)
        refreshTodayWidgetSnapshot()
        return evaluateAndUnlockHabitAchievements()
    }

    suspend fun quickAcceptRepeatedHabit(habit: RepeatedMealHabit, targetLogicalDate: String): List<HabitAchievement> {
        ensureDailyLogExists(targetLogicalDate)
        val titled = FoodEmojiMapper.formatTitleWithEmoji(habit.meal.title, habit.meal.description)
        mealDao.insertMeal(
            MealEntryEntity.fromDomain(
                habit.meal.copy(
                    id = 0,
                    logicalDate = targetLogicalDate,
                    slot = habit.slot,
                    title = titled,
                    syncStatus = SyncStatus.SYNCED,
                    localPhotoPath = null,
                    createdAtEpochMillis = System.currentTimeMillis()
                )
            )
        )
        refreshWaterTargetForDate(targetLogicalDate)
        refreshTodayWidgetSnapshot()
        return evaluateAndUnlockHabitAchievements()
    }

    suspend fun closeDay(logicalDate: String): Pair<DailyLog, List<HabitAchievement>> {
        val log = ensureDailyLogExists(logicalDate)
        val meals = mealDao.getMealsForDateOnce(logicalDate).map { it.toDomain() }
        val latestWeight = weightDao.getAllWeightsAscOnce().lastOrNull { it.logicalDate <= logicalDate }?.toDomain()
        val note = noteSynthesizer.synthesizeClosingNote(log, meals, latestWeight)
        val closed = log.copy(isClosed = true, biologicalNote = note)
        dailyLogDao.upsertLog(DailyLogEntity.fromDomain(closed))
        refreshTodayWidgetSnapshot()
        val unlocked = evaluateAndUnlockHabitAchievements()
        return closed to unlocked
    }

    suspend fun reopenDay(logicalDate: String) {
        val log = dailyLogDao.getLogByDateOnce(logicalDate)?.toDomain() ?: return
        dailyLogDao.upsertLog(DailyLogEntity.fromDomain(log.copy(isClosed = false)))
        refreshTodayWidgetSnapshot()
    }

    suspend fun adaptRoutineWithGemini(
        dailyRoutineText: String,
        exerciseRoutineText: String,
        weightKg: Double,
        ageYears: Int,
        sex: BiologicalSex,
        modelName: String
    ): RoutineAdaptationResponse {
        return gemini.adaptRoutine(
            modelName = modelName,
            dailyRoutineText = dailyRoutineText,
            exerciseRoutineText = exerciseRoutineText,
            weightKg = weightKg,
            ageYears = ageYears,
            sex = sex
        )
    }

    suspend fun recordWeight(
        logicalDate: String,
        weightKg: Double,
        outlierConfirmed: Boolean
    ): WeightCelebrationEvent {
        ensureDailyLogExists(logicalDate)
        val previousWeightsDesc = weightDao.getAllWeightsAscOnce().map { it.toDomain() }.reversed()
        val profile = profileDao.getProfileOnce()?.toDomain() ?: UserProfile()
        val alreadyUnlocked = prefs.getUnlockedAchievementsOnce()

        val celebration = weightAchievementUseCase.evaluateNewWeight(
            newWeightKg = weightKg,
            targetWeightKg = profile.targetWeightKg,
            previousWeightsDesc = previousWeightsDesc,
            alreadyUnlockedIds = alreadyUnlocked
        )
        celebration.unlockedAchievement?.let { unlocked ->
            prefs.unlockAchievement(unlocked.id)
        }

        val newEntry = WeightEntry(
            logicalDate = logicalDate,
            weightKg = weightKg,
            outlierConfirmed = outlierConfirmed,
            recordedAtEpochMillis = System.currentTimeMillis()
        )
        weightDao.insertWeight(WeightEntryEntity.fromDomain(newEntry))
        recalculateAllWeights()

        saveProfile(profile.copy(currentWeightKg = weightKg), updateOpenTodaySnapshot = true)
        return celebration
    }

    suspend fun deleteWeight(id: Long) {
        weightDao.deleteWeightById(id)
        recalculateAllWeights()
    }

    private suspend fun recalculateAllWeights() {
        val weightsAsc = weightDao.getAllWeightsAscOnce().map { it.toDomain() }
        if (weightsAsc.isEmpty()) return
        val logsAsc = dailyLogDao.getAllLogsAscOnce().map { it.toDomain() }
        val mealsByDate = mealDao.getAllMealsOnce().map { it.toDomain() }.groupBy { it.logicalDate }
        val fallbackWeight = profileDao.getProfileOnce()?.currentWeightKg ?: weightsAsc.first().weightKg

        val recalculated = dryWeightUseCase.recalculateSeries(
            weightsAsc = weightsAsc,
            dailyLogs = logsAsc,
            mealsByDate = mealsByDate,
            fallbackBaseWeightKg = fallbackWeight
        )
        weightDao.upsertWeights(recalculated.map { WeightEntryEntity.fromDomain(it) })
    }

    suspend fun computeCalibrationSuggestion(): CalibrationSuggestion? {
        val weightsAsc = weightDao.getAllWeightsAscOnce().map { it.toDomain() }
        val logsAsc = dailyLogDao.getAllLogsAscOnce().map { it.toDomain() }
        val mealsByDate = mealDao.getAllMealsOnce().map { it.toDomain() }.groupBy { it.logicalDate }
        return calibrationUseCase.evaluateCalibration(weightsAsc, logsAsc, mealsByDate)
    }

    suspend fun applyCalibrationAdjustment(deltaKcal: Int) {
        val current = profileDao.getProfileOnce()?.toDomain() ?: UserProfile()
        saveProfile(
            current.copy(calibratedTdeeAdjustmentKcal = current.calibratedTdeeAdjustmentKcal + deltaKcal),
            updateOpenTodaySnapshot = true
        )
    }

    suspend fun exportExcel(uri: Uri, rows: List<MasterTableRow>): Result<Int> {
        return excelManager.exportToUri(appContext, uri, rows)
    }

    suspend fun importExcel(uri: Uri): Result<Int> {
        val result = excelManager.importFromUri(appContext, uri)
        return result.map { importedRows ->
            val currentProfile = profileDao.getProfileOnce()?.toDomain() ?: UserProfile()
            importedRows.forEach { row ->
                val (floor, ceiling, minProt) = metabolicUseCase.calculateOptimalFloorAndCeiling(
                    profile = currentProfile,
                    tdeeKcal = row.expenditureKcal,
                    carryoverDebtKcal = 0
                )
                val log = DailyLog(
                    logicalDate = row.logicalDate,
                    dayNumber = row.dayNumber,
                    estimatedExpenditureKcal = row.expenditureKcal,
                    errorBudgetKcal = 0,
                    floorKcal = floor,
                    ceilingKcal = ceiling,
                    minProteinG = minProt,
                    catabolicDebtCarryoverKcal = 0,
                    isClosed = true,
                    biologicalNote = row.biologicalNote
                )
                dailyLogDao.upsertLog(DailyLogEntity.fromDomain(log))

                mealDao.deleteMealsForDate(row.logicalDate)
                if (row.intakeKcal > 0 || row.proteinG > 0f) {
                    mealDao.insertMeal(
                        MealEntryEntity.fromDomain(
                            MealEntry(
                                logicalDate = row.logicalDate,
                                slot = MealSlot.LUNCH,
                                title = "Registro importado (.xlsx)",
                                description = row.biologicalNote.ifBlank { "Importado desde Tabla Maestra" },
                                caloriesKcal = row.intakeKcal,
                                proteinG = row.proteinG,
                                summary = row.biologicalNote,
                                syncStatus = SyncStatus.SYNCED
                            )
                        )
                    )
                }

                if (row.weightKg != null) {
                    weightDao.insertWeight(
                        WeightEntryEntity.fromDomain(
                            WeightEntry(
                                logicalDate = row.logicalDate,
                                weightKg = row.weightKg,
                                outlierConfirmed = true
                            )
                        )
                    )
                }
            }
            recalculateAllWeights()
            refreshTodayWidgetSnapshot()
            importedRows.size
        }
    }

    private suspend fun refreshTodayWidgetSnapshot() {
        val today = DateUtils.todayLogicalDate()
        val log = dailyLogDao.getLogByDateOnce(today)?.toDomain() ?: return
        val meals = mealDao.getMealsForDateOnce(today).map { it.toDomain() }
        val metrics = metabolicUseCase.computeMetrics(log, meals)
        DailyCauldronWidgetProvider.saveSnapshotAndRefresh(
            context = appContext,
            remainingBeforeCeilingKcal = metrics.remainingBeforeCeilingKcal,
            missingProteinG = (metrics.minProteinG - metrics.totalProteinG).coerceAtLeast(0f).roundToInt(),
            statusTitle = metrics.statusBadge.titleEs,
            waterGlasses = log.waterGlasses,
            waterTarget = log.waterTargetGlasses
        )
    }

    fun isNetworkConnected(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
