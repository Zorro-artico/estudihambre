package com.example.ui.weight

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NutriRepository
import com.example.domain.model.AchievementBanner
import com.example.domain.model.CalibrationSuggestion
import com.example.domain.model.DeficitProjectionPoint
import com.example.domain.model.DeficitRiskAssessment
import com.example.domain.model.UserProfile
import com.example.domain.model.WeightCelebrationEvent
import com.example.domain.model.WeightEntry
import com.example.domain.model.WeeklyCheatBudgetPlan
import com.example.util.CelebrationSoundPlayer
import com.example.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PendingOutlierConfirmation(
    val logicalDate: String,
    val weightKg: Double,
    val deltaKg: Double
)

data class WeightSimulatorUiState(
    val weightsDesc: List<WeightEntry> = emptyList(),
    val profile: UserProfile = UserProfile(),
    val simulatedDeficitKcal: Int = 450,
    val projectionWeeks: Int = 12,
    val projectionPoints: List<DeficitProjectionPoint> = emptyList(),
    val riskAssessment: DeficitRiskAssessment = DeficitRiskAssessment(false, "Déficit Sostenible", emptyList()),
    val cheatExtraKcal: Int = 650,
    val cheatSpreadDays: Int = 5,
    val cheatBudgetPlan: WeeklyCheatBudgetPlan? = null,
    val calibrationSuggestion: CalibrationSuggestion? = null,
    val pendingOutlier: PendingOutlierConfirmation? = null,
    val unlockedAchievementIds: Set<String> = emptySet(),
    val targetProgressPercent: Int = 0,
    val activeCelebration: WeightCelebrationEvent? = null,
    val activeBanner: AchievementBanner? = null,
    val showConfetti: Boolean = false,
    val feedbackMessage: String? = null
)

class WeightSimulatorViewModel(private val repository: NutriRepository) : ViewModel() {

    private val simulatedDeficitKcal = MutableStateFlow(450)
    private val projectionWeeks = MutableStateFlow(12)
    private val cheatExtraKcal = MutableStateFlow(650)
    private val cheatSpreadDays = MutableStateFlow(5)
    private val calibrationSuggestion = MutableStateFlow<CalibrationSuggestion?>(null)
    private val pendingOutlier = MutableStateFlow<PendingOutlierConfirmation?>(null)
    private val activeCelebration = MutableStateFlow<WeightCelebrationEvent?>(null)
    private val activeBanner = MutableStateFlow<AchievementBanner?>(null)
    private val showConfetti = MutableStateFlow(false)
    private val feedbackMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.profile.collect { p ->
                simulatedDeficitKcal.value = p.desiredDeficitKcal
            }
        }
        refreshCalibration()
    }

    private val celebrationTuple = combine(
        repository.unlockedAchievements,
        activeCelebration,
        activeBanner,
        showConfetti,
        feedbackMessage
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        CelebrationTuple(
            unlockedIds = arr[0] as Set<String>,
            celebration = arr[1] as WeightCelebrationEvent?,
            banner = arr[2] as AchievementBanner?,
            confetti = arr[3] as Boolean,
            msg = arr[4] as String?
        )
    }

    private data class CelebrationTuple(
        val unlockedIds: Set<String>,
        val celebration: WeightCelebrationEvent?,
        val banner: AchievementBanner?,
        val confetti: Boolean,
        val msg: String?
    )

    val uiState: StateFlow<WeightSimulatorUiState> = combine(
        repository.weightsDesc,
        repository.profile,
        simulatedDeficitKcal,
        projectionWeeks,
        cheatExtraKcal,
        cheatSpreadDays,
        calibrationSuggestion,
        pendingOutlier,
        celebrationTuple
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val weights = values[0] as List<WeightEntry>
        val profile = values[1] as UserProfile
        val simDeficit = values[2] as Int
        val weeks = values[3] as Int
        val cheatKcal = values[4] as Int
        val cheatDays = values[5] as Int
        val calib = values[6] as CalibrationSuggestion?
        val outlier = values[7] as PendingOutlierConfirmation?
        val cel = values[8] as CelebrationTuple

        val latestWeight = weights.firstOrNull()?.weightKg ?: profile.currentWeightKg
        val tdee = repository.metabolicUseCase.calculateEstimatedTdeeKcal(profile)
        val (floor, ceiling, _) = repository.metabolicUseCase.calculateOptimalFloorAndCeiling(
            profile = profile,
            tdeeKcal = tdee,
            carryoverDebtKcal = 0
        )
        val projection = repository.deficitProjectionUseCase.projectWeightCurve(
            startWeightKg = latestWeight,
            desiredDailyDeficitKcal = simDeficit,
            weeks = weeks
        )
        val minProtein = (latestWeight * profile.targetProteinPerKg).toFloat()
        val risk = repository.deficitProjectionUseCase.assessRisk(
            profile = profile,
            estimatedTdeeKcal = tdee,
            desiredDeficitKcal = simDeficit,
            currentProteinG = minProtein
        )
        val cheatPlan = repository.cheatMealPlannerUseCase.simulateWeeklyCheatCompensation(
            profile = profile,
            estimatedTdeeKcal = tdee,
            floorKcal = floor,
            ceilingKcal = ceiling,
            extraCravingKcal = cheatKcal,
            spreadDays = cheatDays
        )
        val progressPct = repository.weightAchievementUseCase.computeCurrentProgressPercent(
            weightsDesc = weights,
            fallbackCurrentKg = profile.currentWeightKg,
            targetWeightKg = profile.targetWeightKg
        )

        WeightSimulatorUiState(
            weightsDesc = weights,
            profile = profile,
            simulatedDeficitKcal = simDeficit,
            projectionWeeks = weeks,
            projectionPoints = projection,
            riskAssessment = risk,
            cheatExtraKcal = cheatKcal,
            cheatSpreadDays = cheatDays,
            cheatBudgetPlan = cheatPlan,
            calibrationSuggestion = calib,
            pendingOutlier = outlier,
            unlockedAchievementIds = cel.unlockedIds,
            targetProgressPercent = progressPct,
            activeCelebration = cel.celebration,
            activeBanner = cel.banner,
            showConfetti = cel.confetti,
            feedbackMessage = cel.msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeightSimulatorUiState()
    )

    fun refreshCalibration() {
        viewModelScope.launch {
            calibrationSuggestion.value = repository.computeCalibrationSuggestion()
        }
    }

    fun submitWeight(context: Context, weightKg: Double, logicalDate: String = DateUtils.todayLogicalDate()) {
        if (weightKg <= 25.0 || weightKg >= 350.0) {
            feedbackMessage.value = "Ingresa un peso válido entre 25 y 350 kg."
            return
        }
        val existing = uiState.value.weightsDesc
        val (needsConfirm, delta) = repository.dryWeightUseCase.requiresOutlierConfirmation(
            newWeightKg = weightKg,
            newTimestampMillis = System.currentTimeMillis(),
            existingWeights = existing
        )
        if (needsConfirm && delta != null) {
            pendingOutlier.value = PendingOutlierConfirmation(logicalDate, weightKg, delta)
        } else {
            saveWeightInternal(context, logicalDate, weightKg, outlierConfirmed = false)
        }
    }

    fun confirmOutlierWeight(context: Context) {
        val pending = pendingOutlier.value ?: return
        pendingOutlier.value = null
        saveWeightInternal(context, pending.logicalDate, pending.weightKg, outlierConfirmed = true)
    }

    fun dismissOutlierDialog() {
        pendingOutlier.value = null
    }

    private fun saveWeightInternal(
        context: Context,
        logicalDate: String,
        weightKg: Double,
        outlierConfirmed: Boolean
    ) {
        viewModelScope.launch {
            val soundEnabled = uiState.value.profile.isSoundEnabled
            val celebration = repository.recordWeight(logicalDate, weightKg, outlierConfirmed)
            refreshCalibration()

            val unlocked = celebration.unlockedAchievement
            if (unlocked != null) {
                activeCelebration.value = celebration
                activeBanner.value = AchievementBanner(
                    titleEs = unlocked.titleEs,
                    subtitleEs = unlocked.subtitleEs,
                    isIdealWeight = celebration.isIdealWeight
                )
                showConfetti.value = celebration.isIdealWeight
            }

            if (soundEnabled) {
                when {
                    celebration.isIdealWeight -> CelebrationSoundPlayer.playIdealWeightVictoryFanfare(context)
                    unlocked != null -> CelebrationSoundPlayer.playMilestoneChime(context)
                    celebration.isNewLowerWeight -> CelebrationSoundPlayer.playLowerWeightChime(context)
                }
            }

            if (unlocked != null) {
                launch {
                    delay(if (celebration.isIdealWeight) 5500L else 4200L)
                    activeCelebration.value = null
                    activeBanner.value = null
                    showConfetti.value = false
                }
            }

            feedbackMessage.value = String.format(
                java.util.Locale.US,
                "Pesaje de %.2f kg registrado.",
                weightKg
            )
        }
    }

    fun dismissCelebration() {
        activeCelebration.value = null
        activeBanner.value = null
        showConfetti.value = false
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch {
            repository.deleteWeight(id)
            refreshCalibration()
        }
    }

    fun updateSimulatedDeficit(kcal: Int) {
        simulatedDeficitKcal.value = kcal
    }

    fun updateProjectionWeeks(weeks: Int) {
        projectionWeeks.value = weeks.coerceIn(4, 24)
    }

    fun updateCheatExtraKcal(kcal: Int) {
        cheatExtraKcal.value = kcal.coerceIn(150, 2000)
    }

    fun updateCheatSpreadDays(days: Int) {
        cheatSpreadDays.value = days.coerceIn(2, 7)
    }

    fun applySimulatedDeficitToProfile() {
        viewModelScope.launch {
            val current = uiState.value.profile
            repository.saveProfile(current.copy(desiredDeficitKcal = simulatedDeficitKcal.value))
            feedbackMessage.value = "Déficit objetivo actualizado a ${simulatedDeficitKcal.value} kcal/día."
        }
    }

    fun applyCalibrationSuggestion() {
        val suggestion = calibrationSuggestion.value ?: return
        viewModelScope.launch {
            repository.applyCalibrationAdjustment(suggestion.suggestedTdeeDeltaKcal)
            calibrationSuggestion.value = null
            feedbackMessage.value = "Gasto calibrado en ${suggestion.suggestedTdeeDeltaKcal} kcal/día."
        }
    }

    fun clearFeedback() {
        feedbackMessage.value = null
    }

    class Factory(private val repository: NutriRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WeightSimulatorViewModel(repository) as T
        }
    }
}
