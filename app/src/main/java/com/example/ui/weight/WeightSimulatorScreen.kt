package com.example.ui.weight

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.HabitAchievement
import com.example.domain.model.WeightAchievement
import com.example.ui.components.ConfettiCelebrationOverlay
import com.example.ui.components.MinecraftAdvancementToast
import com.example.ui.components.ProjectionAndHistoryChart
import com.example.ui.theme.CrystalWaterBlue
import com.example.ui.theme.EmberCrimson
import com.example.ui.theme.HoneyBrothGold
import com.example.ui.theme.HyruleSage
import com.example.ui.theme.LeatherBorder
import com.example.ui.theme.MutedParchment
import com.example.util.DateUtils
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeightSimulatorScreen(
    uiState: WeightSimulatorUiState,
    onSubmitWeight: (Double) -> Unit,
    onConfirmOutlier: () -> Unit,
    onDismissOutlier: () -> Unit,
    onDeleteWeight: (Long) -> Unit,
    onUpdateSimulatedDeficit: (Int) -> Unit,
    onUpdateProjectionWeeks: (Int) -> Unit,
    onUpdateCheatExtraKcal: (Int) -> Unit,
    onUpdateCheatSpreadDays: (Int) -> Unit,
    onApplySimulatedDeficit: () -> Unit,
    onApplyCalibration: () -> Unit,
    onDismissCelebration: () -> Unit,
    onClearFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var weightInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearFeedback()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Báscula & Oráculo",
                        style = MaterialTheme.typography.headlineMedium,
                        color = HoneyBrothGold
                    )
                    Text(
                        text = "Peso real vs. Peso teórico seco (Δkcal / 7,700), retención hídrica y logros.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedParchment
                    )
                }

                // Weigh-in input card
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
                            Text("Registrar Pesaje (${DateUtils.todayLogicalDate()})", style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = weightInput,
                                    onValueChange = { weightInput = it },
                                    label = { Text("Peso en kg (ej. 73.4)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("weight_input")
                                )
                                Button(
                                    onClick = {
                                        val parsed = weightInput.replace(',', '.').toDoubleOrNull()
                                        if (parsed != null) {
                                            onSubmitWeight(parsed)
                                            weightInput = ""
                                        }
                                    },
                                    modifier = Modifier.testTag("save_weight_button")
                                ) {
                                    Icon(Icons.Default.MonitorWeight, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Registrar")
                                }
                            }

                            uiState.weightsDesc.firstOrNull()?.let { latest ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Media Móvil 7d", style = MaterialTheme.typography.bodySmall, color = MutedParchment)
                                            Text(
                                                String.format(Locale.US, "%.2f kg", latest.movingAverage7dKg),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = HyruleSage
                                            )
                                        }
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Peso Teórico Seco", style = MaterialTheme.typography.bodySmall, color = MutedParchment)
                                            Text(
                                                String.format(Locale.US, "%.2f kg", latest.theoreticalDryWeightKg),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = CrystalWaterBlue
                                            )
                                        }
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Agua / Glucógeno", style = MaterialTheme.typography.bodySmall, color = MutedParchment)
                                            val sign = if (latest.waterRetentionGrams >= 0) "+" else ""
                                            Text(
                                                "$sign${latest.waterRetentionGrams} g",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (latest.isWhooshDetected) CrystalWaterBlue else HoneyBrothGold
                                            )
                                        }
                                    }
                                }
                                if (latest.isWhooshDetected) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = CrystalWaterBlue)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "¡Efecto Whoosh detectado! Descarga súbita de retención hídrica.",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CrystalWaterBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Target Weight Progress & Minecraft-Style Achievements Shelf
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, LeatherBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Camino al Peso Ideal (${String.format(Locale.US, "%.1f", uiState.profile.targetWeightKg)} kg)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = HoneyBrothGold
                                )
                                Text(
                                    text = "${uiState.targetProgressPercent}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = HyruleSage
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (uiState.targetProgressPercent / 100f).coerceIn(0f, 1f) },
                                color = HyruleSage,
                                trackColor = LeatherBorder,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp))
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                WeightAchievement.entries.forEach { ach ->
                                    val unlocked = ach.id in uiState.unlockedAchievementIds
                                    Surface(
                                        color = if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (unlocked) HoneyBrothGold else LeatherBorder)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                                contentDescription = ach.subtitleEs,
                                                tint = if (unlocked) HoneyBrothGold else MutedParchment,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = ach.subtitleEs,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (unlocked) HoneyBrothGold else MutedParchment
                                            )
                                        }
                                    }
                                }
                                HabitAchievement.entries.forEach { ach ->
                                    val unlocked = ach.id in uiState.unlockedAchievementIds
                                    Surface(
                                        color = if (unlocked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (unlocked) HyruleSage else LeatherBorder)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                                contentDescription = ach.subtitleEs,
                                                tint = if (unlocked) HyruleSage else MutedParchment,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = ach.subtitleEs,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (unlocked) HyruleSage else MutedParchment
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Auto-Calibration Suggestion (14-21 consecutive days)
                uiState.calibrationSuggestion?.let { calib ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            border = BorderStroke(1.5.dp, HoneyBrothGold)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = HoneyBrothGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Auto-Calibración Termodinámica (${calib.consecutiveDaysAnalyzed} días)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = HoneyBrothGold
                                    )
                                }
                                Text(
                                    text = calib.summaryEs,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "Teórico: %.2f kg vs Real (MA7): %.2f kg",
                                        calib.theoreticalWeightDeltaKg,
                                        calib.realMovingAverageDeltaKg
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedParchment
                                )
                                Button(
                                    onClick = onApplyCalibration,
                                    modifier = Modifier.testTag("apply_calibration_button")
                                ) {
                                    Text("Aplicar ajuste (${calib.suggestedTdeeDeltaKcal} kcal/día)")
                                }
                            }
                        }
                    }
                }

                // Interactive Deficit Simulator + Canvas Projection Chart
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Simulador de Déficit", style = MaterialTheme.typography.titleMedium, color = HoneyBrothGold)
                                Text(
                                    text = "${uiState.simulatedDeficitKcal} kcal/día · ${uiState.projectionWeeks} sem",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrystalWaterBlue
                                )
                            }

                            ProjectionAndHistoryChart(
                                weightsAsc = uiState.weightsDesc.reversed(),
                                projectionPoints = uiState.projectionPoints
                            )

                            Text(
                                text = "Déficit diario deseado: ${uiState.simulatedDeficitKcal} kcal",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Slider(
                                value = uiState.simulatedDeficitKcal.toFloat(),
                                onValueChange = { onUpdateSimulatedDeficit((it / 50f).roundToInt() * 50) },
                                valueRange = 0f..1200f,
                                steps = 23,
                                modifier = Modifier.testTag("deficit_slider")
                            )

                            Text(
                                text = "Horizonte de proyección: ${uiState.projectionWeeks} semanas (Meta final: ${
                                    uiState.projectionPoints.lastOrNull()?.let { String.format(Locale.US, "%.1f kg", it.projectedWeightKg) } ?: "-"
                                })",
                                style = MaterialTheme.typography.labelMedium,
                                color = MutedParchment
                            )
                            Slider(
                                value = uiState.projectionWeeks.toFloat(),
                                onValueChange = { onUpdateProjectionWeeks(it.roundToInt()) },
                                valueRange = 4f..24f
                            )

                            Surface(
                                color = if (uiState.riskAssessment.isDangerous) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (uiState.riskAssessment.isDangerous) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = EmberCrimson)
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = uiState.riskAssessment.riskLevelLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (uiState.riskAssessment.isDangerous) EmberCrimson else HyruleSage
                                        )
                                    }
                                    uiState.riskAssessment.warnings.forEach { warn ->
                                        Text("• $warn", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            Button(
                                onClick = onApplySimulatedDeficit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_deficit_button")
                            ) {
                                Text("Guardar ${uiState.simulatedDeficitKcal} kcal como déficit objetivo")
                            }
                        }
                    }
                }

                // Weekly Cheat Meal / Craving Bank Simulator
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, LeatherBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fastfood, contentDescription = null, tint = HoneyBrothGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Simulador de Comida Libre / Antojo (Banco Semanal)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = HoneyBrothGold
                                )
                            }
                            Text(
                                text = "Excedente del antojo o salida: ${uiState.cheatExtraKcal} kcal",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Slider(
                                value = uiState.cheatExtraKcal.toFloat(),
                                onValueChange = { onUpdateCheatExtraKcal((it / 50f).roundToInt() * 50) },
                                valueRange = 150f..2000f,
                                steps = 36,
                                modifier = Modifier.testTag("cheat_extra_kcal_slider")
                            )
                            Text(
                                text = "Días para compensar suavemente: ${uiState.cheatSpreadDays} días",
                                style = MaterialTheme.typography.labelMedium,
                                color = MutedParchment
                            )
                            Slider(
                                value = uiState.cheatSpreadDays.toFloat(),
                                onValueChange = { onUpdateCheatSpreadDays(it.roundToInt()) },
                                valueRange = 2f..7f,
                                steps = 4,
                                modifier = Modifier.testTag("cheat_spread_days_slider")
                            )
                            uiState.cheatBudgetPlan?.let { plan ->
                                Surface(
                                    color = if (plan.keepsDebtZero) {
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Ajuste: -${plan.dailyCompensationKcal} kcal/día -> Meta temporal: ${plan.compensatedDailyTargetKcal} kcal",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (plan.keepsDebtZero) HyruleSage else EmberCrimson
                                        )
                                        Text(
                                            text = plan.recommendationEs,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Déficit neto semanal proyectado: ${plan.projectedWeeklyDeficitKcal} kcal",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CrystalWaterBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Historical Weigh-ins
                item {
                    Text(
                        text = "Historial de Pesajes (${uiState.weightsDesc.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(uiState.weightsDesc, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, LeatherBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.logicalDate} · ${String.format(Locale.US, "%.2f kg", item.weightKg)}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "MA7: %.2f kg | Seco: %.2f kg | Agua: %+d g",
                                        item.movingAverage7dKg,
                                        item.theoreticalDryWeightKg,
                                        item.waterRetentionGrams
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedParchment
                                )
                            }
                            IconButton(onClick = { onDeleteWeight(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Borrar pesaje", tint = EmberCrimson)
                            }
                        }
                    }
                }
            }
        }

        // Confetti & Minecraft-Style Advancement Banner Overlay
        ConfettiCelebrationOverlay(
            visible = uiState.showConfetti,
            intense = uiState.activeCelebration?.isIdealWeight == true
        )

        MinecraftAdvancementToast(
            banner = uiState.activeBanner,
            onDismiss = onDismissCelebration,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    uiState.pendingOutlier?.let { pending ->
        AlertDialog(
            onDismissRequest = onDismissOutlier,
            title = { Text("Confirmar Cambio Abrupto (±3.0 kg)") },
            text = {
                Text(
                    String.format(
                        Locale.US,
                        "El pesaje de %.2f kg representa una variación de %+.2f kg en menos de 24 horas. ¿Es un dato real o deseas corregirlo?",
                        pending.weightKg,
                        pending.deltaKg
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmOutlier,
                    colors = ButtonDefaults.buttonColors(containerColor = EmberCrimson)
                ) {
                    Text("Sí, es real")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissOutlier) {
                    Text("Corregir")
                }
            }
        )
    }
}
