package com.example.ui.meal

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NutriRepository
import com.example.domain.model.ContainerItem
import com.example.domain.model.DailyLog
import com.example.domain.model.EstimationScenario
import com.example.domain.model.GeminiNutritionResponse
import com.example.domain.model.MealComparisonResult
import com.example.domain.model.MealSlot
import com.example.domain.model.OptimalRangeMetrics
import com.example.domain.model.PantryRecipeSuggestion
import com.example.domain.model.RepeatedMealHabit
import com.example.util.DateUtils
import com.example.util.ImageStorageHelper
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MealWorkbenchMode(val labelEs: String) {
    SINGLE("Registrar / Simular"),
    COMPARE_AB("Comparar A vs B"),
    PANTRY_ALCHEMIST("Despensa IA")
}

sealed interface SimulationState {
    data object Idle : SimulationState
    data object Analyzing : SimulationState
    data class Simulated(
        val response: GeminiNutritionResponse,
        val portionPercent: Int = 100,
        val scenario: EstimationScenario = EstimationScenario.AVERAGE
    ) : SimulationState {
        private val baseCalories: Int
            get() = when (scenario) {
                EstimationScenario.LOWER -> response.calorias_menor
                EstimationScenario.AVERAGE -> response.calorias_kcal
                EstimationScenario.HIGHER -> response.calorias_mayor
            }
        private val baseProtein: Float
            get() = when (scenario) {
                EstimationScenario.LOWER -> response.proteina_menor
                EstimationScenario.AVERAGE -> response.proteina_g
                EstimationScenario.HIGHER -> response.proteina_mayor
            }

        val scaledCalories: Int
            get() = (baseCalories * (portionPercent / 100.0)).roundToInt()
        val scaledProtein: Float
            get() = baseProtein * (portionPercent / 100f)

        fun caloriesForScenario(s: EstimationScenario): Int {
            val raw = when (s) {
                EstimationScenario.LOWER -> response.calorias_menor
                EstimationScenario.AVERAGE -> response.calorias_kcal
                EstimationScenario.HIGHER -> response.calorias_mayor
            }
            return (raw * (portionPercent / 100.0)).roundToInt()
        }

        fun proteinForScenario(s: EstimationScenario): Float {
            val raw = when (s) {
                EstimationScenario.LOWER -> response.proteina_menor
                EstimationScenario.AVERAGE -> response.proteina_g
                EstimationScenario.HIGHER -> response.proteina_mayor
            }
            return raw * (portionPercent / 100f)
        }
    }
    data class Error(val message: String, val canQueueOffline: Boolean) : SimulationState
}

sealed interface ComparisonUiState {
    data object Idle : ComparisonUiState
    data object Comparing : ComparisonUiState
    data class Ready(val result: MealComparisonResult) : ComparisonUiState
    data class Error(val message: String) : ComparisonUiState
}

sealed interface PantryUiState {
    data object Idle : PantryUiState
    data object Generating : PantryUiState
    data class Ready(val recipes: List<PantryRecipeSuggestion>) : PantryUiState
    data class Error(val message: String) : PantryUiState
}

data class MealDraft(
    val logicalDate: String = DateUtils.todayLogicalDate(),
    val slot: MealSlot = MealSlot.LUNCH,
    val title: String = "",
    val description: String = "",
    val selectedContainer: ContainerItem? = null,
    val fillPercent: Int = 100,
    val batchServings: Int = 1,
    val estimatedCostText: String = "",
    val saveAsFavorite: Boolean = false,
    val internalPhotoPath: String? = null
)

data class MealLoggerUiState(
    val workbenchMode: MealWorkbenchMode = MealWorkbenchMode.SINGLE,
    val draft: MealDraft = MealDraft(),
    val containers: List<ContainerItem> = emptyList(),
    val currentDayMetrics: OptimalRangeMetrics? = null,
    val simulationState: SimulationState = SimulationState.Idle,
    val habitsBySlot: Map<MealSlot, RepeatedMealHabit> = emptyMap(),
    val isLookingUpBarcode: Boolean = false,
    val barcodeStatusMessage: String? = null,
    // A vs B state
    val optionADescription: String = "",
    val optionAPhotoPath: String? = null,
    val optionBDescription: String = "",
    val optionBPhotoPath: String? = null,
    val comparisonState: ComparisonUiState = ComparisonUiState.Idle,
    // Pantry Alchemist state
    val pantryIngredientsText: String = "",
    val pantryPhotoPath: String? = null,
    val pantryState: PantryUiState = PantryUiState.Idle,
    val currencySymbol: String = "$",
    val savedSuccessMessage: String? = null
)

class MealLoggerViewModel(
    private val repository: NutriRepository,
    initialDate: String = DateUtils.todayLogicalDate()
) : ViewModel() {

    private val workbenchMode = MutableStateFlow(MealWorkbenchMode.SINGLE)
    private val draft = MutableStateFlow(MealDraft(logicalDate = initialDate))
    private val simulationState = MutableStateFlow<SimulationState>(SimulationState.Idle)
    private val isLookingUpBarcode = MutableStateFlow(false)
    private val barcodeStatusMessage = MutableStateFlow<String?>(null)

    private val optionADesc = MutableStateFlow("")
    private val optionAPhoto = MutableStateFlow<String?>(null)
    private val optionBDesc = MutableStateFlow("")
    private val optionBPhoto = MutableStateFlow<String?>(null)
    private val comparisonState = MutableStateFlow<ComparisonUiState>(ComparisonUiState.Idle)

    private val pantryIngredients = MutableStateFlow("")
    private val pantryPhoto = MutableStateFlow<String?>(null)
    private val pantryState = MutableStateFlow<PantryUiState>(PantryUiState.Idle)

    private val savedSuccessMessage = MutableStateFlow<String?>(null)
    private val currentLog = MutableStateFlow<DailyLog?>(null)
    private val currentDayMealsKcal = MutableStateFlow(0)
    private val currentDayProteinG = MutableStateFlow(0f)

    init {
        viewModelScope.launch {
            val log = repository.ensureDailyLogExists(initialDate)
            currentLog.value = log
            repository.observeMealsForDate(initialDate).collect { meals ->
                currentDayMealsKcal.value = meals.sumOf { it.caloriesKcal }
                currentDayProteinG.value = meals.sumOf { it.proteinG.toDouble() }.toFloat()
            }
        }
    }

    private val habitsFlow = repository.observeHabitsBySlot(initialDate)

    private data class ExtraUiParams(
        val mode: MealWorkbenchMode,
        val barcodeLoading: Boolean,
        val barcodeMsg: String?,
        val aDesc: String,
        val aPhoto: String?,
        val bDesc: String,
        val bPhoto: String?,
        val compState: ComparisonUiState,
        val pantryText: String,
        val pantryPic: String?,
        val pantryUi: PantryUiState
    )

    private val extraFlow = combine(
        workbenchMode,
        isLookingUpBarcode,
        barcodeStatusMessage,
        optionADesc,
        optionAPhoto,
        optionBDesc,
        optionBPhoto,
        comparisonState,
        pantryIngredients,
        pantryPhoto,
        pantryState
    ) { arr ->
        ExtraUiParams(
            mode = arr[0] as MealWorkbenchMode,
            barcodeLoading = arr[1] as Boolean,
            barcodeMsg = arr[2] as String?,
            aDesc = arr[3] as String,
            aPhoto = arr[4] as String?,
            bDesc = arr[5] as String,
            bPhoto = arr[6] as String?,
            compState = arr[7] as ComparisonUiState,
            pantryText = arr[8] as String,
            pantryPic = arr[9] as String?,
            pantryUi = arr[10] as PantryUiState
        )
    }

    val uiState: StateFlow<MealLoggerUiState> = combine(
        draft,
        repository.containers,
        simulationState,
        repository.currencySymbol,
        savedSuccessMessage,
        currentLog,
        currentDayMealsKcal,
        currentDayProteinG,
        habitsFlow,
        extraFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val d = values[0] as MealDraft
        @Suppress("UNCHECKED_CAST")
        val containers = values[1] as List<ContainerItem>
        val sim = values[2] as SimulationState
        val currency = values[3] as String
        val msg = values[4] as String?
        val log = values[5] as DailyLog?
        val mealsKcal = values[6] as Int
        val mealsProt = values[7] as Float
        @Suppress("UNCHECKED_CAST")
        val habitsMap = values[8] as Map<MealSlot, RepeatedMealHabit>
        val extra = values[9] as ExtraUiParams

        val metrics = log?.let {
            val totalIntake = it.errorBudgetKcal + mealsKcal
            OptimalRangeMetrics(
                estimatedTdeeKcal = it.estimatedExpenditureKcal,
                floorKcal = it.floorKcal,
                ceilingKcal = it.ceilingKcal,
                minProteinG = it.minProteinG,
                totalIntakeKcal = totalIntake,
                totalProteinG = mealsProt,
                missingToFloorKcal = (it.floorKcal - totalIntake).coerceAtLeast(0),
                remainingBeforeCeilingKcal = (it.ceilingKcal - totalIntake).coerceAtLeast(0),
                projectedDeficitKcal = it.estimatedExpenditureKcal - totalIntake,
                catabolicDebtCarryoverKcal = it.catabolicDebtCarryoverKcal,
                statusBadge = com.example.domain.model.MetabolicBuffStatus.FORJA_ACTIVA
            )
        }

        MealLoggerUiState(
            workbenchMode = extra.mode,
            draft = d,
            containers = containers,
            currentDayMetrics = metrics,
            simulationState = sim,
            habitsBySlot = habitsMap,
            isLookingUpBarcode = extra.barcodeLoading,
            barcodeStatusMessage = extra.barcodeMsg,
            optionADescription = extra.aDesc,
            optionAPhotoPath = extra.aPhoto,
            optionBDescription = extra.bDesc,
            optionBPhotoPath = extra.bPhoto,
            comparisonState = extra.compState,
            pantryIngredientsText = extra.pantryText,
            pantryPhotoPath = extra.pantryPic,
            pantryState = extra.pantryUi,
            currencySymbol = currency,
            savedSuccessMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MealLoggerUiState()
    )

    fun setWorkbenchMode(mode: MealWorkbenchMode) {
        workbenchMode.value = mode
    }

    fun updateTitle(value: String) = draft.update { it.copy(title = value) }
    fun updateDescription(value: String) = draft.update { it.copy(description = value) }
    fun updateSlot(slot: MealSlot) = draft.update { it.copy(slot = slot) }
    fun updateContainer(container: ContainerItem?) = draft.update { it.copy(selectedContainer = container) }
    fun updateFillPercent(pct: Int) = draft.update { it.copy(fillPercent = pct) }
    fun updateBatchServings(n: Int) = draft.update { it.copy(batchServings = n.coerceIn(1, 20)) }
    fun updateEstimatedCost(text: String) = draft.update { it.copy(estimatedCostText = text) }
    fun updateSaveAsFavorite(fav: Boolean) = draft.update { it.copy(saveAsFavorite = fav) }

    fun quickAcceptHabitForSlot(habit: RepeatedMealHabit) {
        val targetDate = draft.value.logicalDate
        viewModelScope.launch {
            repository.quickAcceptRepeatedHabit(habit, targetDate)
            savedSuccessMessage.value = "Aceptado (${habit.slot.labelEs}): ${habit.meal.title}"
        }
    }

    fun onBarcodeDetected(barcode: String) {
        viewModelScope.launch {
            isLookingUpBarcode.value = true
            barcodeStatusMessage.value = "Buscando código $barcode..."
            repository.lookupBarcodeProduct(barcode)
                .onSuccess { res ->
                    draft.update {
                        it.copy(
                            title = res.productName,
                            description = if (it.description.isBlank()) {
                                "Producto empaquetado · Código $barcode"
                            } else it.description
                        )
                    }
                    simulationState.value = SimulationState.Simulated(
                        response = res.nutrition,
                        portionPercent = 100,
                        scenario = EstimationScenario.AVERAGE
                    )
                    barcodeStatusMessage.value = "Producto detectado: ${res.productName}"
                }
                .onFailure { err ->
                    barcodeStatusMessage.value = err.localizedMessage ?: "Código $barcode no encontrado."
                }
            isLookingUpBarcode.value = false
        }
    }

    fun attachPickedPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            val previous = draft.value.internalPhotoPath
            val savedPath = ImageStorageHelper.saveCompressedUri(context, uri)
            if (savedPath != null) {
                ImageStorageHelper.deleteLocalPhotoIfOwned(context, previous)
                draft.update { it.copy(internalPhotoPath = savedPath) }
            }
        }
    }

    fun attachCapturedFile(context: Context, tempFile: File) {
        viewModelScope.launch {
            val previous = draft.value.internalPhotoPath
            val savedPath = ImageStorageHelper.saveCompressedFile(context, tempFile)
            if (savedPath != null) {
                ImageStorageHelper.deleteLocalPhotoIfOwned(context, previous)
                draft.update { it.copy(internalPhotoPath = savedPath) }
            }
        }
    }

    fun removePhoto(context: Context) {
        ImageStorageHelper.deleteLocalPhotoIfOwned(context, draft.value.internalPhotoPath)
        draft.update { it.copy(internalPhotoPath = null) }
    }

    fun simulateImpact() {
        val current = draft.value
        if (current.description.isBlank() && current.title.isBlank() && current.internalPhotoPath == null && current.selectedContainer == null) {
            simulationState.value = SimulationState.Error(
                "Describe la comida, toma/elige una foto o selecciona un recipiente.",
                canQueueOffline = false
            )
            return
        }

        if (!repository.isNetworkConnected()) {
            simulationState.value = SimulationState.Error(
                "Sin conexión a internet. Puedes guardar esta ingesta en la cola offline para que Gemini la procese automáticamente al reconectar.",
                canQueueOffline = true
            )
            return
        }

        viewModelScope.launch {
            simulationState.value = SimulationState.Analyzing
            try {
                val queryDesc = listOf(current.title, current.description)
                    .filter { it.isNotBlank() }
                    .joinToString(" — ")
                val rawResponse = repository.simulateMealWithGemini(
                    description = queryDesc,
                    photoPath = current.internalPhotoPath,
                    container = current.selectedContainer,
                    fillPercent = if (current.selectedContainer != null) current.fillPercent else null,
                    batchServings = current.batchServings
                )
                val divisor = current.batchServings.coerceAtLeast(1)
                val normalized = if (divisor > 1) {
                    rawResponse.copy(
                        calorias_kcal = (rawResponse.calorias_kcal.toDouble() / divisor).roundToInt(),
                        proteina_g = rawResponse.proteina_g / divisor,
                        calorias_menor = (rawResponse.calorias_menor.toDouble() / divisor).roundToInt(),
                        proteina_menor = rawResponse.proteina_menor / divisor,
                        calorias_mayor = (rawResponse.calorias_mayor.toDouble() / divisor).roundToInt(),
                        proteina_mayor = rawResponse.proteina_mayor / divisor,
                        resumen_breve = "${rawResponse.resumen_breve} (Dividido en $divisor porciones: 1/$divisor porción)"
                    )
                } else {
                    rawResponse
                }
                simulationState.value = SimulationState.Simulated(
                    response = normalized,
                    portionPercent = 100,
                    scenario = EstimationScenario.AVERAGE
                )
            } catch (e: TimeoutCancellationException) {
                simulationState.value = SimulationState.Error(
                    "Tiempo de espera agotado (60s). Puedes reintentar o enviarlo a la cola offline.",
                    canQueueOffline = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                simulationState.value = SimulationState.Error(
                    e.localizedMessage ?: "No se pudo evaluar la comida con Gemini.",
                    canQueueOffline = true
                )
            }
        }
    }

    fun selectEstimationScenario(scenario: EstimationScenario) {
        val current = simulationState.value
        if (current is SimulationState.Simulated) {
            simulationState.value = current.copy(scenario = scenario)
        }
    }

    fun adjustSimulatedPortion(percent: Int) {
        val current = simulationState.value
        if (current is SimulationState.Simulated) {
            simulationState.value = current.copy(portionPercent = percent.coerceIn(25, 200))
        }
    }

    fun discardSimulation() {
        simulationState.value = SimulationState.Idle
    }

    fun confirmSimulatedMeal() {
        val sim = simulationState.value as? SimulationState.Simulated ?: return
        val d = draft.value
        val cost = d.estimatedCostText.replace(',', '.').toDoubleOrNull()
        viewModelScope.launch {
            repository.confirmAnalyzedMeal(
                logicalDate = d.logicalDate,
                slot = d.slot,
                title = d.title,
                description = d.description,
                caloriesKcal = sim.scaledCalories,
                proteinG = sim.scaledProtein,
                alertTags = sim.response.etiquetas_alerta,
                summary = "${sim.response.resumen_breve} [Caso: ${sim.scenario.labelEs}]",
                container = d.selectedContainer,
                fillPercent = if (d.selectedContainer != null) d.fillPercent else null,
                batchServings = d.batchServings,
                estimatedCost = cost,
                isFavorite = d.saveAsFavorite,
                tempPhotoPathToDelete = d.internalPhotoPath
            )
            savedSuccessMessage.value = "Comida registrada: ${sim.scaledCalories} kcal (${sim.scaledProtein.roundToInt()}g P)."
        }
    }

    fun queueOfflineMeal() {
        val d = draft.value
        val cost = d.estimatedCostText.replace(',', '.').toDoubleOrNull()
        viewModelScope.launch {
            repository.enqueueOfflineMeal(
                logicalDate = d.logicalDate,
                slot = d.slot,
                title = d.title,
                description = d.description,
                container = d.selectedContainer,
                fillPercent = if (d.selectedContainer != null) d.fillPercent else null,
                batchServings = d.batchServings,
                estimatedCost = cost,
                isFavorite = d.saveAsFavorite,
                internalPhotoPath = d.internalPhotoPath
            )
            savedSuccessMessage.value = "Guardado en cola offline (PENDING_SYNC). Se procesará automáticamente con red."
        }
    }

    // --- A vs B Comparator ---
    fun updateOptionADescription(text: String) { optionADesc.value = text }
    fun updateOptionBDescription(text: String) { optionBDesc.value = text }

    fun attachOptionAPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            val path = ImageStorageHelper.saveCompressedUri(context, uri)
            if (path != null) {
                ImageStorageHelper.deleteLocalPhotoIfOwned(context, optionAPhoto.value)
                optionAPhoto.value = path
            }
        }
    }

    fun attachOptionBPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            val path = ImageStorageHelper.saveCompressedUri(context, uri)
            if (path != null) {
                ImageStorageHelper.deleteLocalPhotoIfOwned(context, optionBPhoto.value)
                optionBPhoto.value = path
            }
        }
    }

    fun compareOptionsAAndB() {
        val aText = optionADesc.value.trim()
        val bText = optionBDesc.value.trim()
        if (aText.isBlank() && optionAPhoto.value == null || bText.isBlank() && optionBPhoto.value == null) {
            comparisonState.value = ComparisonUiState.Error("Ingresa descripción o foto para ambas opciones (A y B).")
            return
        }
        viewModelScope.launch {
            comparisonState.value = ComparisonUiState.Comparing
            try {
                val metrics = uiState.value.currentDayMetrics
                val remaining = metrics?.remainingBeforeCeilingKcal ?: 600
                val missingProt = ((metrics?.minProteinG ?: 120f) - (metrics?.totalProteinG ?: 0f)).coerceAtLeast(15f)
                val result = repository.compareMealsAAndBWithGemini(
                    optionADesc = aText,
                    optionAPhotoPath = optionAPhoto.value,
                    optionBDesc = bText,
                    optionBPhotoPath = optionBPhoto.value,
                    remainingBeforeCeilingKcal = remaining,
                    missingProteinG = missingProt
                )
                comparisonState.value = ComparisonUiState.Ready(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                comparisonState.value = ComparisonUiState.Error(e.localizedMessage ?: "No se pudo comparar con Gemini.")
            }
        }
    }

    fun acceptComparisonOption(choice: String) {
        val ready = comparisonState.value as? ComparisonUiState.Ready ?: return
        val res = ready.result
        val selectedResp = if (choice == "B") res.optionB else res.optionA
        val selectedTitle = if (choice == "B") res.optionBTitle else res.optionATitle
        val d = draft.value
        viewModelScope.launch {
            repository.confirmAnalyzedMeal(
                logicalDate = d.logicalDate,
                slot = d.slot,
                title = selectedTitle,
                description = selectedResp.resumen_breve,
                caloriesKcal = selectedResp.calorias_kcal,
                proteinG = selectedResp.proteina_g,
                alertTags = selectedResp.etiquetas_alerta,
                summary = selectedResp.resumen_breve,
                container = null,
                fillPercent = null,
                batchServings = 1,
                estimatedCost = d.estimatedCostText.replace(',', '.').toDoubleOrNull(),
                isFavorite = d.saveAsFavorite,
                tempPhotoPathToDelete = null
            )
            savedSuccessMessage.value = "Opción $choice registrada: ${selectedResp.calorias_kcal} kcal."
        }
    }

    // --- Pantry Alchemist ("¿Qué cocino con lo que tengo?") ---
    fun updatePantryIngredients(text: String) { pantryIngredients.value = text }

    fun attachPantryPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            val path = ImageStorageHelper.saveCompressedUri(context, uri)
            if (path != null) {
                ImageStorageHelper.deleteLocalPhotoIfOwned(context, pantryPhoto.value)
                pantryPhoto.value = path
            }
        }
    }

    fun generatePantryRecipes() {
        if (pantryIngredients.value.isBlank() && pantryPhoto.value == null) {
            pantryState.value = PantryUiState.Error("Escribe tus ingredientes disponibles o sube una foto de tu refri/alacena.")
            return
        }
        viewModelScope.launch {
            pantryState.value = PantryUiState.Generating
            try {
                val metrics = uiState.value.currentDayMetrics
                val remaining = metrics?.remainingBeforeCeilingKcal ?: 550
                val missingProt = ((metrics?.minProteinG ?: 120f) - (metrics?.totalProteinG ?: 0f)).coerceAtLeast(15f)
                val recipes = repository.generatePantryRecipesWithGemini(
                    ingredientsText = pantryIngredients.value.trim(),
                    fridgePhotoPath = pantryPhoto.value,
                    remainingBeforeCeilingKcal = remaining,
                    missingProteinG = missingProt
                )
                pantryState.value = PantryUiState.Ready(recipes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pantryState.value = PantryUiState.Error(e.localizedMessage ?: "No se pudieron crear recetas de despensa.")
            }
        }
    }

    fun acceptPantryRecipe(recipe: PantryRecipeSuggestion) {
        val d = draft.value
        viewModelScope.launch {
            repository.confirmAnalyzedMeal(
                logicalDate = d.logicalDate,
                slot = d.slot,
                title = recipe.nombre,
                description = recipe.ingredientes_usados,
                caloriesKcal = recipe.calorias_kcal,
                proteinG = recipe.proteina_g,
                alertTags = listOf("alquimista despensa", recipe.costo_nivel),
                summary = "${recipe.ingredientes_usados} · ${recipe.pasos_rapidos}",
                container = null,
                fillPercent = null,
                batchServings = 1,
                estimatedCost = d.estimatedCostText.replace(',', '.').toDoubleOrNull(),
                isFavorite = d.saveAsFavorite,
                tempPhotoPathToDelete = pantryPhoto.value
            )
            savedSuccessMessage.value = "Receta de despensa registrada: ${recipe.nombre}"
        }
    }

    class Factory(
        private val repository: NutriRepository,
        private val initialDate: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MealLoggerViewModel(repository, initialDate) as T
        }
    }
}
