package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.MealEntry
import com.example.domain.model.RepeatedMealHabit
import com.example.domain.model.SyncStatus
import com.example.ui.components.MetabolicBuffBadge
import com.example.ui.components.MinecraftAdvancementToast
import com.example.ui.components.OptimalRangeGauge
import com.example.ui.theme.CastIronDark
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
fun DashboardScreen(
    uiState: DashboardUiState,
    onShiftDate: (Int) -> Unit,
    onSelectToday: () -> Unit,
    onOpenMealLogger: (String) -> Unit,
    onAdjustWater: (Int) -> Unit,
    onToggleExamMode: () -> Unit,
    onQuickAcceptHabit: (RepeatedMealHabit) -> Unit,
    onCloseDay: () -> Unit,
    onReopenDay: () -> Unit,
    onEditMeal: (MealEntry) -> Unit,
    onReassignToYesterday: (MealEntry) -> Unit,
    onDeleteMeal: (MealEntry) -> Unit,
    onQuickAddFavorite: (MealEntry) -> Unit,
    onRetryQueue: () -> Unit,
    onDismissAchievementBanner: () -> Unit,
    onClearFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var mealToEdit by remember { mutableStateOf<MealEntry?>(null) }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearFeedback()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { onOpenMealLogger(uiState.activeDate) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Registrar comida") },
                    text = { Text("Registrar comida") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_meal_fab")
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Banner
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, LeatherBorder),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(144.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_culinary_header_1795149620752),
                                contentDescription = "Ilustración culinaria y matemática de Estudihambre Life",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Surface(
                                    color = CastIronDark.copy(alpha = 0.82f),
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, HoneyBrothGold.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "El Caldero de Hoy",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = HoneyBrothGold
                                            )
                                            Text(
                                                text = "Día ${uiState.dailyLog?.dayNumber ?: 1} · ${DateUtils.formatShortDate(uiState.activeDate)} (${uiState.activeDate})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onShiftDate(-1) },
                                                modifier = Modifier.testTag("prev_day_button")
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Día anterior")
                                            }
                                            IconButton(
                                                onClick = { onShiftDate(1) },
                                                modifier = Modifier.testTag("next_day_button")
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Día siguiente")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Preselected Repeated Habitual Meal Card ("Solo acepta que comiste lo mismo")
                uiState.suggestedHabit?.let { habit ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
                            border = BorderStroke(1.5.dp, HyruleSage),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Repeat, contentDescription = null, tint = HyruleSage)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "¿Tu ${habit.slot.labelEs} habitual de siempre?",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = HyruleSage
                                        )
                                        Text(
                                            text = "${habit.meal.title} (${habit.meal.caloriesKcal} kcal · ${habit.meal.proteinG.roundToInt()}g P)",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onQuickAcceptHabit(habit) },
                                        colors = ButtonDefaults.buttonColors(containerColor = HyruleSage),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("accept_daily_habit_button")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Aceptar")
                                    }
                                    OutlinedButton(
                                        onClick = { onOpenMealLogger(uiState.activeDate) }
                                    ) {
                                        Text("Cambiar")
                                    }
                                }
                            }
                        }
                    }
                }

                // Priority Catabolic Debt Alert Banner
                val metrics = uiState.metrics
                if (metrics != null && metrics.catabolicDebtCarryoverKcal > 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            border = BorderStroke(1.5.dp, EmberCrimson),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Deuda catabólica activa", tint = EmberCrimson, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Deuda Catabólica Activa (+${metrics.catabolicDebtCarryoverKcal} kcal)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = EmberCrimson
                                    )
                                    Text(
                                        text = "Cumple hoy el Rango Óptimo (${metrics.floorKcal}–${metrics.ceilingKcal} kcal) para frenar pérdida de masa magra.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Offline Queue Status Card
                if (uiState.syncQueue.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            border = BorderStroke(1.dp, CrystalWaterBlue.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = "Cola offline", tint = CrystalWaterBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${uiState.syncQueue.size} comida(s) en cola offline (PENDING_SYNC)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = CrystalWaterBlue
                                    )
                                    Text(
                                        text = "Se analizarán con Gemini apenas haya conexión.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = onRetryQueue, modifier = Modifier.testTag("retry_sync_button")) {
                                    Icon(Icons.Default.Sync, contentDescription = "Reintentar sincronización", tint = CrystalWaterBlue)
                                }
                            }
                        }
                    }
                }

                // Optimal Range & Protein Summary Card
                if (metrics != null && uiState.dailyLog != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, LeatherBorder),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${metrics.totalIntakeKcal} kcal",
                                            style = MaterialTheme.typography.displaySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Incluye margen base +${uiState.dailyLog.errorBudgetKcal} kcal",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedParchment
                                        )
                                    }
                                    MetabolicBuffBadge(status = metrics.statusBadge)
                                }

                                OptimalRangeGauge(
                                    intakeKcal = metrics.totalIntakeKcal,
                                    floorKcal = metrics.floorKcal,
                                    ceilingKcal = metrics.ceilingKcal,
                                    tdeeKcal = metrics.estimatedTdeeKcal
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricMiniTile(
                                        label = "Para piso seguro",
                                        value = if (metrics.missingToFloorKcal > 0) "+${metrics.missingToFloorKcal} kcal" else "Cubierto",
                                        accent = if (metrics.missingToFloorKcal > 0) HoneyBrothGold else HyruleSage,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricMiniTile(
                                        label = "Antes del techo",
                                        value = "${metrics.remainingBeforeCeilingKcal} kcal",
                                        accent = if (metrics.remainingBeforeCeilingKcal == 0) EmberCrimson else CrystalWaterBlue,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricMiniTile(
                                        label = "Déficit actual",
                                        value = "${metrics.projectedDeficitKcal} kcal",
                                        accent = ParchmentOrGold(metrics.projectedDeficitKcal),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                val proteinPct = (metrics.totalProteinG / metrics.minProteinG.coerceAtLeast(1f)).coerceIn(0f, 1f)
                                val proteinGPerKg = if (uiState.profile.currentWeightKg > 0) {
                                    metrics.totalProteinG / uiState.profile.currentWeightKg
                                } else 0.0
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Proteína: ${metrics.totalProteinG.roundToInt()}g / ${metrics.minProteinG.roundToInt()}g",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = HyruleSage
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.2f g/kg", proteinGPerKg),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MutedParchment
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { proteinPct },
                                        color = HyruleSage,
                                        trackColor = LeatherBorder,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                    )
                                }

                                // Exam Survival Mode quick toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = uiState.profile.examSurvivalMode,
                                        onClick = onToggleExamMode,
                                        leadingIcon = {
                                            Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        label = {
                                            Text(
                                                if (uiState.profile.examSurvivalMode) "Modo Exámenes / Desvelo ACTIVO (+80 kcal)"
                                                else "Activar Modo Exámenes / Desvelo"
                                            )
                                        }
                                    )
                                }

                                if (uiState.dailyLog.isClosed) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                        shape = MaterialTheme.shapes.medium,
                                        border = BorderStroke(1.dp, HyruleSage.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HyruleSage, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Jornada Cerrada · Nota Biológica", style = MaterialTheme.typography.labelLarge, color = HyruleSage)
                                                }
                                                TextButton(onClick = onReopenDay, modifier = Modifier.testTag("reopen_day_button")) {
                                                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Reabrir")
                                                }
                                            }
                                            Text(
                                                text = uiState.dailyLog.biologicalNote,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = onCloseDay,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("close_day_button")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Cerrar Día y Generar Nota Biológica")
                                    }
                                }
                            }
                        }
                    }
                }

                // Hydration & Sodium Streak + Biweekly Student Budget Cards
                uiState.dailyLog?.let { log ->
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocalDrink, contentDescription = null, tint = CrystalWaterBlue)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Hidratación (${log.waterGlasses}/${log.waterTargetGlasses} vasos)",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = CrystalWaterBlue
                                        )
                                    }
                                    Text(
                                        text = "Racha: ${uiState.hydrationStreak}d",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = HyruleSage
                                    )
                                }
                                if (log.waterTargetGlasses > 8) {
                                    Text(
                                        text = "Meta ajustada por sodio/desvelo (+${log.waterTargetGlasses - 8} vasos extra para evitar retención).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedParchment
                                    )
                                }
                                val hydroPct = (log.waterGlasses.toFloat() / log.waterTargetGlasses.coerceAtLeast(1)).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { hydroPct },
                                    color = CrystalWaterBlue,
                                    trackColor = LeatherBorder,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(onClick = { onAdjustWater(-1) }, modifier = Modifier.weight(1f)) {
                                        Text("-1 vaso")
                                    }
                                    Button(
                                        onClick = { onAdjustWater(1) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CrystalWaterBlue),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("add_water_button")
                                    ) {
                                        Text("+1 vaso (250 ml)")
                                    }
                                }
                            }
                        }
                    }
                }

                // Quincena Budget Card (Option #1)
                uiState.quincenaBudget?.let { budget ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, LeatherBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = HoneyBrothGold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Presupuesto de Quincena", style = MaterialTheme.typography.titleSmall, color = HoneyBrothGold)
                                    }
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%s%.0f / %s%.0f",
                                            uiState.currencySymbol,
                                            budget.spentLast14Days,
                                            uiState.currencySymbol,
                                            budget.budgetAmount
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (budget.isOverBudget) EmberCrimson else HyruleSage
                                    )
                                }
                                if (budget.topValueMeals.isNotEmpty()) {
                                    val best = budget.topValueMeals.first()
                                    val ratio = best.proteinG / (best.estimatedCost ?: 1.0)
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "Top relación proteína/costo: %s (%.1f g prot / %s1)",
                                            best.title,
                                            ratio,
                                            uiState.currencySymbol
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CrystalWaterBlue
                                    )
                                } else {
                                    Text(
                                        text = "Registra el costo de tus comidas para ver tu ranking de proteína por peso gastado.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedParchment
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Favorite Meals row
                if (uiState.favoriteMeals.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Recetas Favoritas (1 toque)",
                                style = MaterialTheme.typography.titleMedium,
                                color = HoneyBrothGold
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.favoriteMeals, key = { "fav_${it.id}" }) { fav ->
                                    AssistChip(
                                        onClick = { onQuickAddFavorite(fav) },
                                        label = { Text("${fav.title} · ${fav.caloriesKcal} kcal") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = HoneyBrothGold, modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Meals list header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ingestas del Día (${uiState.meals.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (uiState.activeDate != DateUtils.todayLogicalDate()) {
                            TextButton(onClick = onSelectToday) {
                                Text("Ir a Hoy")
                            }
                        }
                    }
                }

                if (uiState.meals.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, LeatherBorder),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_empty_cauldron_1795149620829),
                                    contentDescription = "Caldero vacío esperando tu primera comida",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                )
                                Text(
                                    text = "Tu caldero está listo",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = HoneyBrothGold
                                )
                                Text(
                                    text = "Toma una foto, escanea un código de barras, o simula en 3 escenarios antes de comer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedParchment
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.meals, key = { it.id }) { meal ->
                        MealItemCard(
                            meal = meal,
                            currencySymbol = uiState.currencySymbol,
                            onEdit = { mealToEdit = meal },
                            onReassignYesterday = { onReassignToYesterday(meal) },
                            onDelete = { onDeleteMeal(meal) }
                        )
                    }
                }
            }
        }

        MinecraftAdvancementToast(
            banner = uiState.activeAchievementBanner,
            onDismiss = onDismissAchievementBanner,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    mealToEdit?.let { editing ->
        EditMealDialog(
            meal = editing,
            onDismiss = { mealToEdit = null },
            onSave = { updated ->
                onEditMeal(updated)
                mealToEdit = null
            }
        )
    }
}

@Composable
private fun ParchmentOrGold(deficit: Int): Color {
    return if (deficit >= 0) HyruleSage else EmberCrimson
}

@Composable
private fun MetricMiniTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, LeatherBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MutedParchment)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.labelLarge, color = accent)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealItemCard(
    meal: MealEntry,
    currencySymbol: String,
    onEdit: () -> Unit,
    onReassignYesterday: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, LeatherBorder),
        shape = MaterialTheme.shapes.medium
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = HoneyBrothGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = meal.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = buildString {
                                append(meal.slot.labelEs)
                                if (meal.batchServings > 1) append(" · 1/${meal.batchServings} olla")
                                if (meal.containerName != null && meal.occupiedVolumeMl != null) {
                                    append(" · ${meal.containerName} (${meal.occupiedVolumeMl.roundToInt()} ml)")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedParchment
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (meal.syncStatus == SyncStatus.PENDING_SYNC) "Pendiente" else "${meal.caloriesKcal} kcal",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (meal.syncStatus == SyncStatus.PENDING_SYNC) CrystalWaterBlue else HoneyBrothGold
                    )
                    Text(
                        text = "${meal.proteinG.roundToInt()}g prot",
                        style = MaterialTheme.typography.labelMedium,
                        color = HyruleSage
                    )
                }
            }

            if (meal.summary.isNotBlank()) {
                Text(
                    text = meal.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (meal.estimatedCost != null && meal.estimatedCost > 0.0 && meal.proteinG > 0f) {
                val protPerCurrency = meal.proteinG / meal.estimatedCost
                Text(
                    text = String.format(
                        Locale.US,
                        "Eficiencia Estudihambre: %.2f g proteína / %s1 (Costo: %s%.2f)",
                        protPerCurrency,
                        currencySymbol,
                        currencySymbol,
                        meal.estimatedCost
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = CrystalWaterBlue
                )
            }

            if (meal.alertTags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    meal.alertTags.forEach { tag ->
                        val isPositive = tag.contains("protei", ignoreCase = true)
                        Surface(
                            color = if (isPositive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPositive) HyruleSage else EmberCrimson,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReassignYesterday) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ayer")
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar")
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = EmberCrimson, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Borrar", color = EmberCrimson)
                }
            }
        }
    }
}

@Composable
private fun EditMealDialog(
    meal: MealEntry,
    onDismiss: () -> Unit,
    onSave: (MealEntry) -> Unit
) {
    var title by remember { mutableStateOf(meal.title) }
    var caloriesText by remember { mutableStateOf(meal.caloriesKcal.toString()) }
    var proteinText by remember { mutableStateOf(meal.proteinG.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Ingesta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it },
                    label = { Text("Calorías (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = proteinText,
                    onValueChange = { proteinText = it },
                    label = { Text("Proteína (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    meal.copy(
                        title = title.trim().ifBlank { meal.title },
                        caloriesKcal = caloriesText.toIntOrNull() ?: meal.caloriesKcal,
                        proteinG = proteinText.replace(',', '.').toFloatOrNull() ?: meal.proteinG
                    )
                )
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
