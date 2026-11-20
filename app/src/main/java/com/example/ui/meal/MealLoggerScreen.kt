package com.example.ui.meal

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.domain.model.ContainerItem
import com.example.domain.model.EstimationScenario
import com.example.domain.model.MealSlot
import com.example.domain.model.PantryRecipeSuggestion
import com.example.domain.model.RepeatedMealHabit
import com.example.ui.theme.CrystalWaterBlue
import com.example.ui.theme.EmberCrimson
import com.example.ui.theme.HoneyBrothGold
import com.example.ui.theme.HyruleSage
import com.example.ui.theme.LeatherBorder
import com.example.ui.theme.MutedParchment
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

private enum class CameraMode { NONE, PHOTO, BARCODE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MealLoggerScreen(
    uiState: MealLoggerUiState,
    onModeChange: (MealWorkbenchMode) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSlotChange: (MealSlot) -> Unit,
    onContainerChange: (ContainerItem?) -> Unit,
    onFillPercentChange: (Int) -> Unit,
    onBatchServingsChange: (Int) -> Unit,
    onCostChange: (String) -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    onQuickAcceptHabit: (RepeatedMealHabit) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onPhotoCapturedFile: (File) -> Unit,
    onRemovePhoto: () -> Unit,
    onSimulate: () -> Unit,
    onSelectScenario: (EstimationScenario) -> Unit,
    onAdjustSimulatedPortion: (Int) -> Unit,
    onDiscardSimulation: () -> Unit,
    onConfirmSimulatedMeal: () -> Unit,
    onQueueOfflineMeal: () -> Unit,
    onOptionADescChange: (String) -> Unit,
    onOptionAPhotoPicked: (Uri) -> Unit,
    onOptionBDescChange: (String) -> Unit,
    onOptionBPhotoPicked: (Uri) -> Unit,
    onCompareOptions: () -> Unit,
    onAcceptComparisonOption: (String) -> Unit,
    onPantryIngredientsChange: (String) -> Unit,
    onPantryPhotoPicked: (Uri) -> Unit,
    onGeneratePantryRecipes: () -> Unit,
    onAcceptPantryRecipe: (PantryRecipeSuggestion) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var activeCameraMode by remember { mutableStateOf(CameraMode.NONE) }
    var pendingCameraMode by remember { mutableStateOf(CameraMode.PHOTO) }

    LaunchedEffect(uiState.savedSuccessMessage) {
        if (uiState.savedSuccessMessage != null) {
            onNavigateBack()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPhotoPicked(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) activeCameraMode = pendingCameraMode
    }

    when (activeCameraMode) {
        CameraMode.PHOTO -> {
            BackHandler { activeCameraMode = CameraMode.NONE }
            CameraXCaptureOverlay(
                onCaptured = { file ->
                    activeCameraMode = CameraMode.NONE
                    onPhotoCapturedFile(file)
                },
                onCancel = { activeCameraMode = CameraMode.NONE }
            )
            return
        }
        CameraMode.BARCODE -> {
            BackHandler { activeCameraMode = CameraMode.NONE }
            BarcodeScannerOverlay(
                onBarcodeScanned = { code ->
                    activeCameraMode = CameraMode.NONE
                    onBarcodeScanned(code)
                },
                onCancel = { activeCameraMode = CameraMode.NONE }
            )
            return
        }
        CameraMode.NONE -> Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesa de Alquimia · Comida") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("meal_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Workbench Mode Tabs
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MealWorkbenchMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.workbenchMode == mode,
                            onClick = { onModeChange(mode) },
                            label = { Text(mode.labelEs) }
                        )
                    }
                }
            }

            // Slot Selector + Preselected Habitual Meal for current slot
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
                        Text("Horario / Tiempo de Comida", style = MaterialTheme.typography.titleMedium, color = HoneyBrothGold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MealSlot.entries.forEach { slot ->
                                FilterChip(
                                    selected = uiState.draft.slot == slot,
                                    onClick = { onSlotChange(slot) },
                                    label = { Text(slot.labelEs) }
                                )
                            }
                        }

                        // Preselected repeated meal for the chosen slot
                        val slotHabit = uiState.habitsBySlot[uiState.draft.slot]
                        if (slotHabit != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, HyruleSage.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Repeat, contentDescription = null, tint = HyruleSage, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Preselección habitual (${slotHabit.slot.labelEs})",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = HyruleSage
                                            )
                                        }
                                        Text(
                                            text = "${slotHabit.meal.title} · ${slotHabit.meal.caloriesKcal} kcal · ${slotHabit.meal.proteinG.roundToInt()}g P",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onQuickAcceptHabit(slotHabit) },
                                        colors = ButtonDefaults.buttonColors(containerColor = HyruleSage),
                                        modifier = Modifier.testTag("accept_slot_habit_button")
                                    ) {
                                        Text("Aceptar")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            when (uiState.workbenchMode) {
                MealWorkbenchMode.COMPARE_AB -> {
                    item {
                        CompareABSection(
                            uiState = uiState,
                            onOptionADescChange = onOptionADescChange,
                            onOptionAPhotoPicked = onOptionAPhotoPicked,
                            onOptionBDescChange = onOptionBDescChange,
                            onOptionBPhotoPicked = onOptionBPhotoPicked,
                            onCompare = onCompareOptions,
                            onAcceptOption = onAcceptComparisonOption
                        )
                    }
                }
                MealWorkbenchMode.PANTRY_ALCHEMIST -> {
                    item {
                        PantryAlchemistSection(
                            uiState = uiState,
                            onIngredientsChange = onPantryIngredientsChange,
                            onPantryPhotoPicked = onPantryPhotoPicked,
                            onGenerateRecipes = onGeneratePantryRecipes,
                            onAcceptRecipe = onAcceptPantryRecipe
                        )
                    }
                }
                MealWorkbenchMode.SINGLE -> {
                    // Photo Capture, Photo Picker & ML Kit Barcode Scanner
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
                                Text("1. Captura Visual o Código de Barras", style = MaterialTheme.typography.titleMedium, color = HoneyBrothGold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            pendingCameraMode = CameraMode.PHOTO
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                activeCameraMode = CameraMode.PHOTO
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_camera_button")
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Foto")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            pendingCameraMode = CameraMode.BARCODE
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                activeCameraMode = CameraMode.BARCODE
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("scan_barcode_button")
                                    ) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Código")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_gallery_button")
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Galería")
                                    }
                                }

                                if (uiState.isLookingUpBarcode) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Text(uiState.barcodeStatusMessage ?: "Consultando producto...", style = MaterialTheme.typography.bodySmall)
                                    }
                                } else if (uiState.barcodeStatusMessage != null) {
                                    Text(
                                        text = uiState.barcodeStatusMessage,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CrystalWaterBlue
                                    )
                                }

                                if (uiState.draft.internalPhotoPath != null) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        AsyncImage(
                                            model = File(uiState.draft.internalPhotoPath),
                                            contentDescription = "Foto de comida capturada",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(170.dp)
                                                .clip(MaterialTheme.shapes.medium)
                                        )
                                        IconButton(
                                            onClick = onRemovePhoto,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(999.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Quitar foto",
                                                    tint = EmberCrimson,
                                                    modifier = Modifier.padding(6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Title & Description
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
                                Text("2. Descripción (emoticón automático al guardar)", style = MaterialTheme.typography.titleMedium, color = HoneyBrothGold)
                                OutlinedTextField(
                                    value = uiState.draft.title,
                                    onValueChange = onTitleChange,
                                    label = { Text("Título (ej. Maruchan, Torta de jamón, Tacos)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("meal_title_input")
                                )
                                OutlinedTextField(
                                    value = uiState.draft.description,
                                    onValueChange = onDescriptionChange,
                                    label = { Text("Ingredientes, capas, cocción o gramajes") },
                                    minLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("meal_description_input")
                                )
                            }
                        }
                    }

                    // Container scale & Batch cooking ("Olla de Aventurero") + Cost
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
                                Text("3. Recipiente Geométrico y Olla de Aventurero", style = MaterialTheme.typography.titleMedium, color = HoneyBrothGold)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = uiState.draft.selectedContainer == null,
                                        onClick = { onContainerChange(null) },
                                        label = { Text("Sin recipiente") }
                                    )
                                    uiState.containers.forEach { container ->
                                        FilterChip(
                                            selected = uiState.draft.selectedContainer?.id == container.id,
                                            onClick = { onContainerChange(container) },
                                            label = { Text("${container.name} (${container.volumeMl.roundToInt()} ml)") }
                                        )
                                    }
                                }

                                uiState.draft.selectedContainer?.let { selected ->
                                    Text(
                                        text = "Nivel de llenado: ${uiState.draft.fillPercent}% (${(selected.volumeMl * uiState.draft.fillPercent / 100.0).roundToInt()} ml ocupados)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CrystalWaterBlue
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(25, 50, 75, 100).forEach { pct ->
                                            FilterChip(
                                                selected = uiState.draft.fillPercent == pct,
                                                onClick = { onFillPercentChange(pct) },
                                                label = { Text("$pct%") },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Dividir olla / lote en porciones", style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            text = if (uiState.draft.batchServings == 1) "Porción completa (1/1)"
                                            else "Cocinado en lote: se registrará 1/${uiState.draft.batchServings} de la olla",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedParchment
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(onClick = { onBatchServingsChange(uiState.draft.batchServings - 1) }) { Text("-") }
                                        Text(
                                            text = "${uiState.draft.batchServings}",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        )
                                        OutlinedButton(onClick = { onBatchServingsChange(uiState.draft.batchServings + 1) }) { Text("+") }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.draft.estimatedCostText,
                                        onValueChange = onCostChange,
                                        label = { Text("Costo (${uiState.currencySymbol}) opc.") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Checkbox(
                                            checked = uiState.draft.saveAsFavorite,
                                            onCheckedChange = onFavoriteChange
                                        )
                                        Text("Guardar favorita", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    // Simulate / Offline Queue buttons
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onSimulate,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("simulate_meal_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simular (3 casos)")
                            }
                            OutlinedButton(
                                onClick = onQueueOfflineMeal,
                                modifier = Modifier.testTag("queue_offline_button")
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cola")
                            }
                        }
                    }

                    // Simulation Results Card (with 3-case selector & Hidden Street Food Traps)
                    item {
                        when (val sim = uiState.simulationState) {
                            SimulationState.Idle -> Unit
                            SimulationState.Analyzing -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        Text("Calculando caso menor, promedio, mayor y trampas ocultas...", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            is SimulationState.Error -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    border = BorderStroke(1.dp, EmberCrimson)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(sim.message, color = EmberCrimson, style = MaterialTheme.typography.bodyMedium)
                                        if (sim.canQueueOffline) {
                                            Button(
                                                onClick = onQueueOfflineMeal,
                                                colors = ButtonDefaults.buttonColors(containerColor = CrystalWaterBlue)
                                            ) {
                                                Icon(Icons.Default.CloudQueue, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Guardar en Cola Offline")
                                            }
                                        }
                                    }
                                }
                            }
                            is SimulationState.Simulated -> {
                                val dayMetrics = uiState.currentDayMetrics
                                val projectedTotalKcal = (dayMetrics?.totalIntakeKcal ?: 0) + sim.scaledCalories
                                val projectedProtein = (dayMetrics?.totalProteinG ?: 0f) + sim.scaledProtein
                                val floor = dayMetrics?.floorKcal ?: 1400
                                val ceiling = dayMetrics?.ceilingKcal ?: 1800

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.5.dp, HoneyBrothGold),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Simulación en 3 Escenarios", style = MaterialTheme.typography.titleMedium, color = HoneyBrothGold)
                                            Icon(Icons.Default.Scale, contentDescription = null, tint = HoneyBrothGold)
                                        }

                                        // 3-tier scenario selector (Menor al común / Promedio / Mayor al común)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            EstimationScenario.entries.forEach { scenario ->
                                                val isSel = sim.scenario == scenario
                                                val sKcal = sim.caloriesForScenario(scenario)
                                                val sProt = sim.proteinForScenario(scenario).roundToInt()
                                                FilterChip(
                                                    selected = isSel,
                                                    onClick = { onSelectScenario(scenario) },
                                                    label = {
                                                        Column {
                                                            Text(scenario.labelEs, style = MaterialTheme.typography.labelSmall)
                                                            Text("$sKcal kcal · ${sProt}g", style = MaterialTheme.typography.labelMedium)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("scenario_${scenario.name.lowercase()}")
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${sim.scaledCalories} kcal",
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", sim.scaledProtein)} g proteína",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = HyruleSage
                                            )
                                        }

                                        Text(
                                            text = sim.response.resumen_breve,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedParchment
                                        )

                                        // Hidden Traps & Savings Tip (Idea #6)
                                        if (sim.response.trampas_ocultas.isNotEmpty() || sim.response.sugerencia_ahorro_kcal.isNotBlank()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                                                shape = MaterialTheme.shapes.small,
                                                border = BorderStroke(1.dp, EmberCrimson.copy(alpha = 0.5f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Warning, contentDescription = null, tint = EmberCrimson, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Trampas Ocultas Detectadas", style = MaterialTheme.typography.labelLarge, color = EmberCrimson)
                                                    }
                                                    sim.response.trampas_ocultas.forEach { trap ->
                                                        Text("• $trap", style = MaterialTheme.typography.bodySmall)
                                                    }
                                                    if (sim.response.sugerencia_ahorro_kcal.isNotBlank()) {
                                                        Text(
                                                            text = "Tip de ahorro: ${sim.response.sugerencia_ahorro_kcal}",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = HyruleSage
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (sim.response.etiquetas_alerta.isNotEmpty()) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                sim.response.etiquetas_alerta.forEach { tag ->
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.errorContainer,
                                                        shape = RoundedCornerShape(999.dp)
                                                    ) {
                                                        Text(
                                                            text = tag,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = EmberCrimson,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Ajustar porción a comer: ${sim.portionPercent}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CrystalWaterBlue
                                        )
                                        Slider(
                                            value = sim.portionPercent.toFloat(),
                                            onValueChange = { onAdjustSimulatedPortion((it / 25f).roundToInt() * 25) },
                                            valueRange = 25f..100f,
                                            steps = 2
                                        )

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val balanceStatus = when {
                                                projectedTotalKcal > ceiling -> "Perfora techo (+${projectedTotalKcal - ceiling} kcal)"
                                                projectedTotalKcal < floor -> "Aún bajo piso (-${floor - projectedTotalKcal} kcal)"
                                                else -> "Dentro del Rango Óptimo ($floor–$ceiling kcal)"
                                            }
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "Balance del día si la consumes: $projectedTotalKcal kcal · ${projectedProtein.roundToInt()}g P",
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                                Text(
                                                    text = balanceStatus,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (projectedTotalKcal > ceiling) EmberCrimson else HyruleSage
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = onDiscardSimulation,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Descartar")
                                            }
                                            Button(
                                                onClick = onConfirmSimulatedMeal,
                                                colors = ButtonDefaults.buttonColors(containerColor = HyruleSage),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("confirm_simulated_meal_button")
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Confirmar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraXCaptureOverlay(
    onCaptured: (File) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var cameraError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    runCatching {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    }.onFailure { err ->
                        cameraError = err.localizedMessage ?: "No se pudo iniciar la cámara."
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        if (cameraError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Text(
                    text = cameraError.orEmpty(),
                    color = EmberCrimson,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onCancel) {
                Text("Cancelar")
            }
            Button(
                onClick = {
                    val tempFile = File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                onCaptured(tempFile)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                cameraError = exception.localizedMessage
                            }
                        }
                    )
                },
                modifier = Modifier.testTag("capture_shutter_button")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Tomar foto")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Capturar")
            }
        }
    }
}
