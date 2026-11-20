package com.example.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NutriRepository
import com.example.domain.model.AchievementBanner
import com.example.domain.model.DailyLog
import com.example.domain.model.HabitAchievement
import com.example.domain.model.MealEntry
import com.example.domain.model.MealSlot
import com.example.domain.model.OptimalRangeMetrics
import com.example.domain.model.QuincenaBudgetSummary
import com.example.domain.model.RepeatedMealHabit
import com.example.domain.model.SyncQueueItem
import com.example.domain.model.UserProfile
import com.example.util.CelebrationSoundPlayer
import com.example.util.DateUtils
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val activeDate: String = DateUtils.todayLogicalDate(),
    val dailyLog: DailyLog? = null,
    val meals: List<MealEntry> = emptyList(),
    val favoriteMeals: List<MealEntry> = emptyList(),
    val metrics: OptimalRangeMetrics? = null,
    val profile: UserProfile = UserProfile(),
    val syncQueue: List<SyncQueueItem> = emptyList(),
    val currencySymbol: String = "$",
    val quincenaBudget: QuincenaBudgetSummary? = null,
    val hydrationStreak: Int = 0,
    val habitsBySlot: Map<MealSlot, RepeatedMealHabit> = emptyMap(),
    val suggestedHabit: RepeatedMealHabit? = null,
    val activeAchievementBanner: AchievementBanner? = null,
    val feedbackMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val repository: NutriRepository) : ViewModel() {

    private val selectedDate = MutableStateFlow(DateUtils.todayLogicalDate())
    private val feedbackMessage = MutableStateFlow<String?>(null)
    private val activeAchievementBanner = MutableStateFlow<AchievementBanner?>(null)

    init {
        viewModelScope.launch {
            repository.ensureDailyLogExists(selectedDate.value)
        }
    }

    private val currentLogFlow = selectedDate.flatMapLatest { date ->
        repository.observeDailyLog(date)
    }

    private val currentMealsFlow = selectedDate.flatMapLatest { date ->
        repository.observeMealsForDate(date)
    }

    private val habitsFlow = selectedDate.flatMapLatest { date ->
        repository.observeHabitsBySlot(date)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        selectedDate,
        currentLogFlow,
        currentMealsFlow,
        repository.favoriteMeals,
        repository.profile,
        repository.syncQueue,
        repository.currencySymbol,
        repository.quincenaBudgetSummary,
        repository.hydrationStreak,
        habitsFlow,
        activeAchievementBanner,
        feedbackMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val date = values[0] as String
        val log = values[1] as DailyLog?
        val meals = values[2] as List<MealEntry>
        val favorites = values[3] as List<MealEntry>
        val profile = values[4] as UserProfile
        val queue = values[5] as List<SyncQueueItem>
        val currency = values[6] as String
        val budget = values[7] as QuincenaBudgetSummary
        val hydroStreak = values[8] as Int
        @Suppress("UNCHECKED_CAST")
        val habitsMap = values[9] as Map<MealSlot, RepeatedMealHabit>
        val banner = values[10] as AchievementBanner?
        val msg = values[11] as String?

        val metrics = log?.let { repository.metabolicUseCase.computeMetrics(it, meals) }
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val suggested = repository.repeatedMealHabitUseCase.pickSuggestedHabitForHour(habitsMap, hour)

        DashboardUiState(
            activeDate = date,
            dailyLog = log,
            meals = meals,
            favoriteMeals = favorites,
            metrics = metrics,
            profile = profile,
            syncQueue = queue,
            currencySymbol = currency,
            quincenaBudget = budget,
            hydrationStreak = hydroStreak,
            habitsBySlot = habitsMap,
            suggestedHabit = suggested,
            activeAchievementBanner = banner,
            feedbackMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun shiftDate(deltaDays: Int) {
        val next = DateUtils.shiftLogicalDate(selectedDate.value, deltaDays)
        selectedDate.value = next
        viewModelScope.launch {
            repository.ensureDailyLogExists(next)
        }
    }

    fun selectToday() {
        val today = DateUtils.todayLogicalDate()
        selectedDate.value = today
        viewModelScope.launch {
            repository.ensureDailyLogExists(today)
        }
    }

    fun adjustWaterGlasses(context: Context, delta: Int) {
        viewModelScope.launch {
            val unlocked = repository.updateWaterGlasses(selectedDate.value, delta)
            handleUnlockedHabits(context, unlocked)
        }
    }

    fun toggleExamSurvivalMode() {
        viewModelScope.launch {
            val next = !uiState.value.profile.examSurvivalMode
            repository.toggleExamSurvivalMode(next)
            feedbackMessage.value = if (next) {
                "Modo Sobrevivencia de Exámenes activado (+80 kcal margen nocturno y +1 vaso hidratación)."
            } else {
                "Modo Exámenes desactivado."
            }
        }
    }

    fun quickAcceptRepeatedHabit(context: Context, habit: RepeatedMealHabit) {
        viewModelScope.launch {
            val unlocked = repository.quickAcceptRepeatedHabit(habit, selectedDate.value)
            feedbackMessage.value = "Aceptado (${habit.slot.labelEs}): ${habit.meal.title}"
            handleUnlockedHabits(context, unlocked)
        }
    }

    fun closeCurrentDay(context: Context) {
        viewModelScope.launch {
            val (closed, unlocked) = repository.closeDay(selectedDate.value)
            feedbackMessage.value = "Día ${closed.dayNumber} cerrado y Nota Biológica consolidada."
            handleUnlockedHabits(context, unlocked)
        }
    }

    fun reopenCurrentDay() {
        viewModelScope.launch {
            repository.reopenDay(selectedDate.value)
            feedbackMessage.value = "Jornada reabierta para ajustes."
        }
    }

    fun editMeal(meal: MealEntry) {
        viewModelScope.launch {
            repository.updateMeal(meal)
            feedbackMessage.value = "Comida actualizada."
        }
    }

    fun reassignMealToYesterday(meal: MealEntry) {
        viewModelScope.launch {
            repository.reassignMealToPreviousDay(meal)
            feedbackMessage.value = "Ingesta movida al día ${DateUtils.previousLogicalDate(meal.logicalDate)}."
        }
    }

    fun deleteMeal(meal: MealEntry) {
        viewModelScope.launch {
            repository.deleteMeal(meal)
            feedbackMessage.value = "Comida eliminada."
        }
    }

    fun quickAddFavorite(context: Context, favorite: MealEntry) {
        viewModelScope.launch {
            val unlocked = repository.quickAddFavoriteMeal(favorite, selectedDate.value)
            feedbackMessage.value = "Añadido rápido: ${favorite.title}"
            handleUnlockedHabits(context, unlocked)
        }
    }

    fun retryOfflineQueue() {
        viewModelScope.launch {
            repository.retrySyncQueue()
            feedbackMessage.value = "Reintentando sincronización con Gemini..."
        }
    }

    fun dismissAchievementBanner() {
        activeAchievementBanner.value = null
    }

    private suspend fun handleUnlockedHabits(context: Context, unlocked: List<HabitAchievement>) {
        val first = unlocked.firstOrNull() ?: return
        activeAchievementBanner.value = AchievementBanner(
            titleEs = first.titleEs,
            subtitleEs = first.subtitleEs,
            isIdealWeight = false
        )
        if (uiState.value.profile.isSoundEnabled) {
            CelebrationSoundPlayer.playMilestoneChime(context)
        }
        delay(4000L)
        activeAchievementBanner.value = null
    }

    fun clearFeedback() {
        feedbackMessage.value = null
    }

    class Factory(private val repository: NutriRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
