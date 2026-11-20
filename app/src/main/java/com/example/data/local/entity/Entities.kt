package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.domain.model.BiologicalSex
import com.example.domain.model.ContainerItem
import com.example.domain.model.ContainerShape
import com.example.domain.model.DailyActivityLevel
import com.example.domain.model.DailyLog
import com.example.domain.model.MealEntry
import com.example.domain.model.MealSlot
import com.example.domain.model.StrengthTrainingMode
import com.example.domain.model.SyncQueueItem
import com.example.domain.model.SyncStatus
import com.example.domain.model.UserProfile
import com.example.domain.model.WeightEntry
import org.json.JSONArray

class RoomConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val arr = JSONArray()
        value.forEach { arr.put(it) }
        return arr.toString()
    }

    @TypeConverter
    fun toStringList(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    val heightCm: Double,
    val ageYears: Int,
    val sex: String,
    val activityLevel: String,
    val strengthTraining: String,
    val trainingRoutineNotes: String,
    val dailyRoutineNotes: String = "",
    val routineAdaptationSummary: String = "",
    val routineBonusKcal: Int = 0,
    val isSoundEnabled: Boolean = true,
    val targetProteinPerKg: Float,
    val desiredDeficitKcal: Int,
    val baseErrorBudgetKcal: Int,
    val selectedGeminiModel: String,
    val calibratedTdeeAdjustmentKcal: Int,
    val biweeklyBudget: Double = 1200.0,
    val examSurvivalMode: Boolean = false
) {
    fun toDomain() = UserProfile(
        id = id,
        currentWeightKg = currentWeightKg,
        targetWeightKg = targetWeightKg,
        heightCm = heightCm,
        ageYears = ageYears,
        sex = BiologicalSex.valueOf(sex),
        activityLevel = DailyActivityLevel.valueOf(activityLevel),
        strengthTraining = StrengthTrainingMode.valueOf(strengthTraining),
        trainingRoutineNotes = trainingRoutineNotes,
        dailyRoutineNotes = dailyRoutineNotes,
        routineAdaptationSummary = routineAdaptationSummary,
        routineBonusKcal = routineBonusKcal,
        isSoundEnabled = isSoundEnabled,
        targetProteinPerKg = targetProteinPerKg,
        desiredDeficitKcal = desiredDeficitKcal,
        baseErrorBudgetKcal = baseErrorBudgetKcal,
        selectedGeminiModel = selectedGeminiModel,
        calibratedTdeeAdjustmentKcal = calibratedTdeeAdjustmentKcal,
        biweeklyBudget = biweeklyBudget,
        examSurvivalMode = examSurvivalMode
    )

    companion object {
        fun fromDomain(p: UserProfile) = UserProfileEntity(
            id = p.id,
            currentWeightKg = p.currentWeightKg,
            targetWeightKg = p.targetWeightKg,
            heightCm = p.heightCm,
            ageYears = p.ageYears,
            sex = p.sex.name,
            activityLevel = p.activityLevel.name,
            strengthTraining = p.strengthTraining.name,
            trainingRoutineNotes = p.trainingRoutineNotes,
            dailyRoutineNotes = p.dailyRoutineNotes,
            routineAdaptationSummary = p.routineAdaptationSummary,
            routineBonusKcal = p.routineBonusKcal,
            isSoundEnabled = p.isSoundEnabled,
            targetProteinPerKg = p.targetProteinPerKg,
            desiredDeficitKcal = p.desiredDeficitKcal,
            baseErrorBudgetKcal = p.baseErrorBudgetKcal,
            selectedGeminiModel = p.selectedGeminiModel,
            calibratedTdeeAdjustmentKcal = p.calibratedTdeeAdjustmentKcal,
            biweeklyBudget = p.biweeklyBudget,
            examSurvivalMode = p.examSurvivalMode
        )
    }
}

@Entity(tableName = "containers")
data class ContainerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val shape: String,
    val radiusCm: Double,
    val heightCm: Double,
    val lengthCm: Double,
    val widthCm: Double,
    val volumeMl: Double,
    val isFavorite: Boolean
) {
    fun toDomain() = ContainerItem(
        id = id,
        name = name,
        shape = ContainerShape.valueOf(shape),
        radiusCm = radiusCm,
        heightCm = heightCm,
        lengthCm = lengthCm,
        widthCm = widthCm,
        volumeMl = volumeMl,
        isFavorite = isFavorite
    )

    companion object {
        fun fromDomain(c: ContainerItem) = ContainerEntity(
            id = c.id,
            name = c.name,
            shape = c.shape.name,
            radiusCm = c.radiusCm,
            heightCm = c.heightCm,
            lengthCm = c.lengthCm,
            widthCm = c.widthCm,
            volumeMl = c.volumeMl,
            isFavorite = c.isFavorite
        )
    }
}

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val logicalDate: String,
    val dayNumber: Int,
    val estimatedExpenditureKcal: Int,
    val errorBudgetKcal: Int,
    val floorKcal: Int,
    val ceilingKcal: Int,
    val minProteinG: Float,
    val catabolicDebtCarryoverKcal: Int,
    val isClosed: Boolean,
    val biologicalNote: String,
    val waterGlasses: Int = 0,
    val waterTargetGlasses: Int = 8
) {
    fun toDomain() = DailyLog(
        logicalDate = logicalDate,
        dayNumber = dayNumber,
        estimatedExpenditureKcal = estimatedExpenditureKcal,
        errorBudgetKcal = errorBudgetKcal,
        floorKcal = floorKcal,
        ceilingKcal = ceilingKcal,
        minProteinG = minProteinG,
        catabolicDebtCarryoverKcal = catabolicDebtCarryoverKcal,
        isClosed = isClosed,
        biologicalNote = biologicalNote,
        waterGlasses = waterGlasses,
        waterTargetGlasses = waterTargetGlasses
    )

    companion object {
        fun fromDomain(d: DailyLog) = DailyLogEntity(
            logicalDate = d.logicalDate,
            dayNumber = d.dayNumber,
            estimatedExpenditureKcal = d.estimatedExpenditureKcal,
            errorBudgetKcal = d.errorBudgetKcal,
            floorKcal = d.floorKcal,
            ceilingKcal = d.ceilingKcal,
            minProteinG = d.minProteinG,
            catabolicDebtCarryoverKcal = d.catabolicDebtCarryoverKcal,
            isClosed = d.isClosed,
            biologicalNote = d.biologicalNote,
            waterGlasses = d.waterGlasses,
            waterTargetGlasses = d.waterTargetGlasses
        )
    }
}

@Entity(tableName = "meal_entries")
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logicalDate: String,
    val slot: String,
    val title: String,
    val description: String,
    val caloriesKcal: Int,
    val proteinG: Float,
    val alertTags: List<String>,
    val summary: String,
    val containerId: Long?,
    val containerName: String?,
    val fillPercent: Int?,
    val occupiedVolumeMl: Double?,
    val batchServings: Int,
    val estimatedCost: Double?,
    val localPhotoPath: String?,
    val syncStatus: String,
    val isFavorite: Boolean,
    val createdAtEpochMillis: Long
) {
    fun toDomain() = MealEntry(
        id = id,
        logicalDate = logicalDate,
        slot = runCatching { MealSlot.valueOf(slot) }.getOrDefault(MealSlot.LUNCH),
        title = title,
        description = description,
        caloriesKcal = caloriesKcal,
        proteinG = proteinG,
        alertTags = alertTags,
        summary = summary,
        containerId = containerId,
        containerName = containerName,
        fillPercent = fillPercent,
        occupiedVolumeMl = occupiedVolumeMl,
        batchServings = batchServings,
        estimatedCost = estimatedCost,
        localPhotoPath = localPhotoPath,
        syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.SYNCED),
        isFavorite = isFavorite,
        createdAtEpochMillis = createdAtEpochMillis
    )

    companion object {
        fun fromDomain(m: MealEntry) = MealEntryEntity(
            id = m.id,
            logicalDate = m.logicalDate,
            slot = m.slot.name,
            title = m.title,
            description = m.description,
            caloriesKcal = m.caloriesKcal,
            proteinG = m.proteinG,
            alertTags = m.alertTags,
            summary = m.summary,
            containerId = m.containerId,
            containerName = m.containerName,
            fillPercent = m.fillPercent,
            occupiedVolumeMl = m.occupiedVolumeMl,
            batchServings = m.batchServings,
            estimatedCost = m.estimatedCost,
            localPhotoPath = m.localPhotoPath,
            syncStatus = m.syncStatus.name,
            isFavorite = m.isFavorite,
            createdAtEpochMillis = m.createdAtEpochMillis
        )
    }
}

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logicalDate: String,
    val weightKg: Double,
    val movingAverage7dKg: Double,
    val theoreticalDryWeightKg: Double,
    val waterRetentionGrams: Int,
    val isWhooshDetected: Boolean,
    val outlierConfirmed: Boolean,
    val recordedAtEpochMillis: Long
) {
    fun toDomain() = WeightEntry(
        id = id,
        logicalDate = logicalDate,
        weightKg = weightKg,
        movingAverage7dKg = movingAverage7dKg,
        theoreticalDryWeightKg = theoreticalDryWeightKg,
        waterRetentionGrams = waterRetentionGrams,
        isWhooshDetected = isWhooshDetected,
        outlierConfirmed = outlierConfirmed,
        recordedAtEpochMillis = recordedAtEpochMillis
    )

    companion object {
        fun fromDomain(w: WeightEntry) = WeightEntryEntity(
            id = w.id,
            logicalDate = w.logicalDate,
            weightKg = w.weightKg,
            movingAverage7dKg = w.movingAverage7dKg,
            theoreticalDryWeightKg = w.theoreticalDryWeightKg,
            waterRetentionGrams = w.waterRetentionGrams,
            isWhooshDetected = w.isWhooshDetected,
            outlierConfirmed = w.outlierConfirmed,
            recordedAtEpochMillis = w.recordedAtEpochMillis
        )
    }
}

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealEntryId: Long,
    val targetLogicalDate: String,
    val promptContext: String,
    val localPhotoPath: String?,
    val batchServings: Int,
    val retryCount: Int,
    val lastError: String?,
    val createdAtEpochMillis: Long
) {
    fun toDomain() = SyncQueueItem(
        id = id,
        mealEntryId = mealEntryId,
        targetLogicalDate = targetLogicalDate,
        promptContext = promptContext,
        localPhotoPath = localPhotoPath,
        batchServings = batchServings,
        retryCount = retryCount,
        lastError = lastError,
        createdAtEpochMillis = createdAtEpochMillis
    )

    companion object {
        fun fromDomain(s: SyncQueueItem) = SyncQueueEntity(
            id = s.id,
            mealEntryId = s.mealEntryId,
            targetLogicalDate = s.targetLogicalDate,
            promptContext = s.promptContext,
            localPhotoPath = s.localPhotoPath,
            batchServings = s.batchServings,
            retryCount = s.retryCount,
            lastError = s.lastError,
            createdAtEpochMillis = s.createdAtEpochMillis
        )
    }
}
