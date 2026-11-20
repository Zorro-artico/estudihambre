package com.example.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.remote.GeminiNutritionService
import com.example.domain.model.BiologicalSex
import com.example.domain.model.DailyActivityLevel
import com.example.domain.model.StrengthTrainingMode
import com.example.ui.theme.CrystalWaterBlue
import com.example.ui.theme.HoneyBrothGold
import com.example.ui.theme.HyruleSage
import com.example.ui.theme.LeatherBorder
import com.example.ui.theme.MutedParchment
import com.example.util.DateUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAdaptRoutineWithGemini: (
        dailyRoutineText: String,
        exerciseRoutineText: String,
        weightKg: Double,
        ageYears: Int,
        sex: BiologicalSex,
        selectedModel: String
    ) -> Unit,
    onSaveProfile: (
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
    ) -> Unit,
    onExportExcel: (Uri) -> Unit,
    onImportExcel: (Uri) -> Unit,
    onClearFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val p = uiState.profile

    var weightText by remember(p) { mutableStateOf(p.currentWeightKg.toString()) }
    var targetWeightText by remember(p) { mutableStateOf(p.targetWeightKg.toString()) }
    var heightText by remember(p) { mutableStateOf(p.heightCm.toString()) }
    var ageText by remember(p) { mutableStateOf(p.ageYears.toString()) }
    var sex by remember(p) { mutableStateOf(p.sex) }
    var activity by remember(p) { mutableStateOf(p.activityLevel) }
    var strength by remember(p) { mutableStateOf(p.strengthTraining) }
    var dailyRoutineNotes by remember(p) { mutableStateOf(p.dailyRoutineNotes) }
    var trainingNotes by remember(p) { mutableStateOf(p.trainingRoutineNotes) }
    var isSoundEnabled by remember(p) { mutableStateOf(p.isSoundEnabled) }
    var biweeklyBudgetText by remember(p) { mutableStateOf(p.biweeklyBudget.toInt().toString()) }
    var examSurvivalMode by remember(p) { mutableStateOf(p.examSurvivalMode) }
    var proteinPerKgText by remember(p) { mutableStateOf(p.targetProteinPerKg.toString()) }
    var deficitText by remember(p) { mutableStateOf(p.desiredDeficitKcal.toString()) }
    var errorBudgetText by remember(p) { mutableStateOf(p.baseErrorBudgetKcal.toString()) }
    var selectedModel by remember(p) { mutableStateOf(p.selectedGeminiModel) }
    var currencySymbol by remember(uiState.currencySymbol) { mutableStateOf(uiState.currencySymbol) }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearFeedback()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri -> if (uri != null) onExportExcel(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onImportExcel(uri) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Perfil & Ajustes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = HoneyBrothGold
                )
                Text(
                    text = "TDEE calculado: ${uiState.estimatedTdeeKcal} kcal/día · Rango Óptimo: ${uiState.floorKcal}–${uiState.ceilingKcal} kcal · Proteína mín: ${uiState.minProteinG.roundToInt()} g",
                    style = MaterialTheme.typography.labelMedium,
                    color = HyruleSage
                )
            }

            // Biometric Data
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, LeatherBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Datos Biométricos y Meta Manual", style = MaterialTheme.typography.titleMedium, color = HoneyBrothGold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it },
                                label = { Text("Peso actual (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = targetWeightText,
                                onValueChange = { targetWeightText = it },
                                label = { Text("Peso objetivo (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = heightText,
                                onValueChange = { heightText = it },
                                label = { Text("Estatura (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = ageText,
                                onValueChange = { ageText = it },
                                label = { Text("Edad") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BiologicalSex.entries.forEach { option ->
                                FilterChip(
                                    selected = sex == option,
                                    onClick = { sex = option },
                                    label = { Text(option.labelEs) }
                                )
                            }
                        }
                    }
                }
            }

            // Specific Daily Routine, Exercise Routine, or Both + AI Adaptation
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, LeatherBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Apartado de Rutina (Diaria y/o Ejercicio)", style = MaterialTheme.typography.titleMedium, color = HoneyBrothGold)
                        Text(
                            text = "Escribe específicamente tu rutina diaria, tu rutina de ejercicio o ambas. Puedes adaptarla automáticamente al perfil energético con IA.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedParchment
                        )

                        OutlinedTextField(
                            value = dailyRoutineNotes,
                            onValueChange = { dailyRoutineNotes = it },
                            label = { Text("Rutina diaria específica (pasos, trabajo, traslados, clases)") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("daily_routine_input")
                        )

                        OutlinedTextField(
                            value = trainingNotes,
                            onValueChange = { trainingNotes = it },
                            label = { Text("Rutina de ejercicio específica (días, pesas, cardio o sin entreno)") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("exercise_routine_input")
                        )

                        Button(
                            onClick = {
                                onAdaptRoutineWithGemini(
                                    dailyRoutineNotes,
                                    trainingNotes,
                                    weightText.replace(',', '.').toDoubleOrNull() ?: p.currentWeightKg,
                                    ageText.toIntOrNull() ?: p.ageYears,
                                    sex,
                                    selectedModel
                                )
                            },
                            enabled = !uiState.isAdaptingRoutine,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("adapt_routine_ai_button")
                        ) {
                            if (uiState.isAdaptingRoutine) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Adaptando rutina...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Adaptar rutina y gasto con Gemini")
                            }
                        }

                        if (p.routineAdaptationSummary.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, HyruleSage.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Adaptación aplicada (${if (p.routineBonusKcal >= 0) "+" else ""}${p.routineBonusKcal} kcal/día):",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = HyruleSage
                                    )
                                    Text(
                                        text = p.routineAdaptationSummary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Text("Nivel de actividad base:", style = MaterialTheme.typography.labelMedium, color = MutedParchment)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DailyActivityLevel.entries.forEach { level ->
                                FilterChip(
                                    selected = activity == level,
                                    onClick = { activity = level },
                                    label = { Text(level.labelEs) }
                                )
                            }
                        }

                        Text("Entrenamiento de fuerza:", style = MaterialTheme.typography.labelMedium, color = MutedParchment)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StrengthTrainingMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = strength == mode,
                                    onClick = { strength = mode },
                                    label = { Text(mode.labelEs) }
                                )
                            }
                        }
                    }
                }
            }

            // Metabolic Budgets, Celebration Audio & Gemini Model
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, LeatherBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Presupuestos Metabólicos, Audio e IA", style = MaterialTheme.typography.titleMedium, color = HoneyBrothGold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = errorBudgetText,
                                onValueChange = { errorBudgetText = it },
                                label = { Text("Margen error base (kcal)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = deficitText,
                                onValueChange = { deficitText = it },
                                label = { Text("Déficit objetivo (kcal)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = proteinPerKgText,
                                onValueChange = { proteinPerKgText = it },
                                label = { Text("Proteína mín (g/kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = biweeklyBudgetText,
                                onValueChange = { biweeklyBudgetText = it },
                                label = { Text("Presupuesto quincena") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("biweekly_budget_input")
                            )
                            OutlinedTextField(
                                value = currencySymbol,
                                onValueChange = { currencySymbol = it.take(3) },
                                label = { Text("Moneda") },
                                singleLine = true,
                                modifier = Modifier.width(84.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo Sobrevivencia de Exámenes / Desvelo", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "+80 kcal de margen nocturno, protege la deuda catabólica y +1 vaso de hidratación.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedParchment
                                )
                            }
                            Switch(
                                checked = examSurvivalMode,
                                onCheckedChange = { examSurvivalMode = it },
                                modifier = Modifier.testTag("exam_survival_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Música de celebración en pesajes", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "Fanfarrias retro al bajar de peso y al lograr tu peso ideal.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedParchment
                                )
                            }
                            Switch(
                                checked = isSoundEnabled,
                                onCheckedChange = { isSoundEnabled = it }
                            )
                        }

                        Text(
                            text = "Modelo Gemini (Firebase AI Logic · Sin API Key)",
                            style = MaterialTheme.typography.labelLarge,
                            color = CrystalWaterBlue
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GeminiNutritionService.SUPPORTED_MODELS.forEach { modelName ->
                                FilterChip(
                                    selected = selectedModel == modelName,
                                    onClick = { selectedModel = modelName },
                                    label = { Text(modelName) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onSaveProfile(
                                    weightText.replace(',', '.').toDoubleOrNull() ?: p.currentWeightKg,
                                    targetWeightText.replace(',', '.').toDoubleOrNull() ?: p.targetWeightKg,
                                    heightText.replace(',', '.').toDoubleOrNull() ?: p.heightCm,
                                    ageText.toIntOrNull() ?: p.ageYears,
                                    sex,
                                    activity,
                                    strength,
                                    dailyRoutineNotes,
                                    trainingNotes,
                                    isSoundEnabled,
                                    biweeklyBudgetText.replace(',', '.').toDoubleOrNull() ?: p.biweeklyBudget,
                                    examSurvivalMode,
                                    proteinPerKgText.replace(',', '.').toFloatOrNull() ?: p.targetProteinPerKg,
                                    deficitText.toIntOrNull() ?: p.desiredDeficitKcal,
                                    errorBudgetText.toIntOrNull() ?: p.baseErrorBudgetKcal,
                                    selectedModel,
                                    currencySymbol
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_settings_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar Perfil y Rutina")
                        }
                    }
                }
            }

            // Excel Backup / Import
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, LeatherBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Respaldo y Continuidad en Excel (.xlsx)", style = MaterialTheme.typography.titleMedium, color = HoneyBrothGold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { exportLauncher.launch("estudihambre_backup_${DateUtils.todayLogicalDate()}.xlsx") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exportar")
                            }
                            OutlinedButton(
                                onClick = {
                                    importLauncher.launch(
                                        arrayOf(
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            "application/octet-stream"
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Importar")
                            }
                        }
                    }
                }
            }
        }
    }
}
