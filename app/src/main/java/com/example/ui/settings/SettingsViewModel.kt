package com.example.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NutriRepository
import com.example.domain.model.BiologicalSex
import com.example.domain.model.DailyActivityLevel
import com.example.domain.model.MasterTableRow
import com.example.domain.model.StrengthTrainingMode
import com.example.domain.model.UserProfile
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profile: UserProfile = UserProfile(),
    val estimatedTdeeKcal: Int = 2200,
    val floorKcal: Int = 1500,
    val ceilingKcal: Int = 1750,
    val minProteinG: Float = 133f,
    val currencySymbol: String = "$",
    val allRowsForExport: List<MasterTableRow> = emptyList(),
    val isAdaptingRoutine: Boolean = false,
    val feedbackMessage: String? = null
)

class SettingsViewModel(private val repository: NutriRepository) : ViewModel() {

    private val isAdaptingRoutine = MutableStateFlow(false)
    private val feedbackMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.profile,
        repository.currencySymbol,
        repository.masterTableRows,
        isAdaptingRoutine,
        feedbackMessage
    ) { profile, currency, rows, adapting, msg ->
        val tdee = repository.metabolicUseCase.calculateEstimatedTdeeKcal(profile)
        val (floor, ceiling, minProt) = repository.metabolicUseCase.calculateOptimalFloorAndCeiling(
            profile = profile,
            tdeeKcal = tdee,
            carryoverDebtKcal = 0
        )
        SettingsUiState(
            profile = profile,
            estimatedTdeeKcal = tdee,
            floorKcal = floor,
            ceilingKcal = ceiling,
            minProteinG = minProt,
            currencySymbol = currency,
            allRowsForExport = rows,
            isAdaptingRoutine = adapting,
            feedbackMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun adaptSpecificRoutineWithGemini(
        dailyRoutineText: String,
        exerciseRoutineText: String,
        weightKg: Double,
        ageYears: Int,
        sex: BiologicalSex,
        selectedModel: String
    ) {
        if (dailyRoutineText.isBlank() && exerciseRoutineText.isBlank()) {
            feedbackMessage.value = "Escribe tu rutina diaria, de ejercicio o ambas para adaptarla."
            return
        }
        viewModelScope.launch {
            isAdaptingRoutine.value = true
            try {
                val result = repository.adaptRoutineWithGemini(
                    dailyRoutineText = dailyRoutineText.trim(),
                    exerciseRoutineText = exerciseRoutineText.trim(),
                    weightKg = weightKg,
                    ageYears = ageYears,
                    sex = sex,
                    modelName = selectedModel
                )
                val adaptedActivity = runCatching { DailyActivityLevel.valueOf(result.actividad_sugerida) }
                    .getOrDefault(uiState.value.profile.activityLevel)
                val adaptedStrength = runCatching { StrengthTrainingMode.valueOf(result.entrenamiento_sugerido) }
                    .getOrDefault(uiState.value.profile.strengthTraining)

                val current = uiState.value.profile
                val updated = current.copy(
                    currentWeightKg = weightKg,
                    ageYears = ageYears,
                    sex = sex,
                    activityLevel = adaptedActivity,
                    strengthTraining = adaptedStrength,
                    dailyRoutineNotes = dailyRoutineText.trim(),
                    trainingRoutineNotes = exerciseRoutineText.trim(),
                    routineBonusKcal = result.kcal_ajuste_diario,
                    routineAdaptationSummary = result.resumen_adaptacion,
                    selectedGeminiModel = selectedModel
                )
                repository.saveProfile(updated, updateOpenTodaySnapshot = true)
                feedbackMessage.value = "Rutina adaptada: ${adaptedActivity.labelEs} / ${adaptedStrength.labelEs} (${result.kcal_ajuste_diario} kcal)."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                feedbackMessage.value = "No se pudo adaptar con Gemini: ${e.localizedMessage ?: "verifica tu conexión"}."
            } finally {
                isAdaptingRoutine.value = false
            }
        }
    }

    fun saveProfile(
        currentWeightKg: Double,
        targetWeightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: BiologicalSex,
        activityLevel: DailyActivityLevel,
        strengthTraining: StrengthTrainingMode,
        dailyRoutineNotes: String,
        trainingNotes: String,
        isSoundEnabled: Boolean,
        biweeklyBudget: Double,
        examSurvivalMode: Boolean,
        targetProteinPerKg: Float,
        desiredDeficitKcal: Int,
        baseErrorBudgetKcal: Int,
        selectedGeminiModel: String,
        currencySymbol: String
    ) {
        viewModelScope.launch {
            val current = uiState.value.profile
            val updated = current.copy(
                currentWeightKg = currentWeightKg.coerceIn(30.0, 300.0),
                targetWeightKg = targetWeightKg.coerceIn(30.0, 250.0),
                heightCm = heightCm.coerceIn(110.0, 230.0),
                ageYears = ageYears.coerceIn(14, 100),
                sex = sex,
                activityLevel = activityLevel,
                strengthTraining = strengthTraining,
                dailyRoutineNotes = dailyRoutineNotes.trim(),
                trainingRoutineNotes = trainingNotes.trim(),
                isSoundEnabled = isSoundEnabled,
                biweeklyBudget = biweeklyBudget.coerceIn(100.0, 50000.0),
                examSurvivalMode = examSurvivalMode,
                targetProteinPerKg = targetProteinPerKg.coerceIn(0.8f, 3.2f),
                desiredDeficitKcal = desiredDeficitKcal.coerceIn(0, 1400),
                baseErrorBudgetKcal = baseErrorBudgetKcal.coerceIn(0, 500),
                selectedGeminiModel = selectedGeminiModel
            )
            repository.saveProfile(updated, updateOpenTodaySnapshot = true)
            repository.setCurrencySymbol(currencySymbol)
            feedbackMessage.value = "Perfil biométrico y configuración guardados."
        }
    }

    fun exportExcel(uri: Uri) {
        viewModelScope.launch {
            repository.exportExcel(uri, uiState.value.allRowsForExport)
                .onSuccess { count ->
                    feedbackMessage.value = "Exportadas $count jornadas a .xlsx."
                }
                .onFailure { err ->
                    feedbackMessage.value = "Error exportando .xlsx: ${err.localizedMessage}"
                }
        }
    }

    fun importExcel(uri: Uri) {
        viewModelScope.launch {
            repository.importExcel(uri)
                .onSuccess { count ->
                    feedbackMessage.value = "Importadas $count jornadas desde .xlsx."
                }
                .onFailure { err ->
                    feedbackMessage.value = "Error importando .xlsx: ${err.localizedMessage}"
                }
        }
    }

    fun clearFeedback() {
        feedbackMessage.value = null
    }

    class Factory(private val repository: NutriRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
