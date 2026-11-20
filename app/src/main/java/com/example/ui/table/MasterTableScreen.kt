package com.example.ui.table

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.MasterTableRow
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
fun MasterTableScreen(
    uiState: MasterTableUiState,
    onSelectFilter: (TableIntervalFilter) -> Unit,
    onCustomRangeApply: (String, String) -> Unit,
    onExportExcel: (Uri) -> Unit,
    onImportExcel: (Uri) -> Unit,
    onClearFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showCustomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.excelFeedback) {
        uiState.excelFeedback?.let {
            snackbarHostState.showSnackbar(it)
            onClearFeedback()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri != null) onExportExcel(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportExcel(uri)
    }

    val hScrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tabla Maestra",
                            style = MaterialTheme.typography.headlineMedium,
                            color = HoneyBrothGold
                        )
                        Text(
                            text = "Libro de crónicas metabólicas y exportación .xlsx",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedParchment
                        )
                    }
                }
            }

            // SAF Excel Export / Import Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { exportLauncher.launch("estudihambre_tabla_${DateUtils.todayLogicalDate()}.xlsx") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_excel_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar .xlsx")
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
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_excel_button")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar .xlsx")
                    }
                }
            }

            // Quick Interval Filters
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TableIntervalFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.activeFilter == filter,
                            onClick = {
                                if (filter == TableIntervalFilter.CUSTOM) {
                                    showCustomDialog = true
                                } else {
                                    onSelectFilter(filter)
                                }
                            },
                            label = { Text(filter.labelEs) }
                        )
                    }
                }
            }

            // Interval Statistics Summary Card
            item {
                val stats = uiState.intervalStats
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
                        Text(
                            text = "Estadísticas del Intervalo (${stats.dayCount} días)",
                            style = MaterialTheme.typography.titleSmall,
                            color = HoneyBrothGold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatBlock("Ingesta Total / Prom", "${stats.totalIntakeKcal} / ${stats.avgDailyIntakeKcal.roundToInt()} kcal")
                            StatBlock("Gasto Total", "${stats.totalExpenditureKcal} kcal")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatBlock("Déficit Neto / Prom", "${stats.netDeficitKcal} / ${stats.avgDailyDeficitKcal.roundToInt()} kcal")
                            StatBlock(
                                "Grasa Teórica vs Báscula",
                                String.format(
                                    Locale.US,
                                    "%.0fg (%.2fkg) | %s",
                                    stats.theoreticalFatOxidizedGrams,
                                    stats.theoreticalFatOxidizedGrams / 1000.0,
                                    stats.realScaleDeltaKg?.let { String.format(Locale.US, "%+.2f kg", it) } ?: "N/D"
                                )
                            )
                        }
                    }
                }
            }

            // Tabular 8-Column Master Table Header
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, LeatherBorder)
                ) {
                    Column(modifier = Modifier.horizontalScroll(hScrollState)) {
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableCellHeader("Día", 56.dp)
                            TableCellHeader("Fecha", 104.dp)
                            TableCellHeader("Ingesta (kcal)", 108.dp)
                            TableCellHeader("Gasto (kcal)", 104.dp)
                            TableCellHeader("Déficit (kcal)", 108.dp)
                            TableCellHeader("Peso (kg)", 86.dp)
                            TableCellHeader("Proteína (g)", 100.dp)
                            TableCellHeader("Nota Biológica", 320.dp)
                        }
                        HorizontalDivider(color = LeatherBorder)
                    }
                }
            }

            if (uiState.filteredRows.isEmpty()) {
                item {
                    Text(
                        text = "No hay jornadas en este intervalo aún.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedParchment,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(uiState.filteredRows, key = { it.logicalDate }) { row ->
                    MasterTableRowItem(row = row, hScrollState = hScrollState)
                }
            }
        }
    }

    if (showCustomDialog) {
        var start by remember { mutableStateOf(uiState.customStartDate) }
        var end by remember { mutableStateOf(uiState.customEndDate) }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Rango Personalizado (YYYY-MM-DD)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Desde (YYYY-MM-DD)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("Hasta (YYYY-MM-DD)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCustomRangeApply(start.trim(), end.trim())
                    showCustomDialog = false
                }) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MutedParchment)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = CrystalWaterBlue)
    }
}

@Composable
private fun TableCellHeader(text: String, width: Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = HoneyBrothGold,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp)
    )
}

@Composable
private fun MasterTableRowItem(
    row: MasterTableRow,
    hScrollState: androidx.compose.foundation.ScrollState
) {
    Column(modifier = Modifier.horizontalScroll(hScrollState)) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${row.dayNumber}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(56.dp).padding(horizontal = 6.dp))
            Text(row.logicalDate, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(104.dp).padding(horizontal = 6.dp))
            Text("${row.intakeKcal}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(108.dp).padding(horizontal = 6.dp))
            Text("${row.expenditureKcal}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(104.dp).padding(horizontal = 6.dp))
            Text(
                text = "${row.deficitKcal}",
                style = MaterialTheme.typography.labelMedium,
                color = if (row.deficitKcal >= 0) HyruleSage else EmberCrimson,
                modifier = Modifier.width(108.dp).padding(horizontal = 6.dp)
            )
            Text(
                text = row.weightKg?.let { String.format(Locale.US, "%.2f", it) } ?: "-",
                style = MaterialTheme.typography.labelMedium,
                color = CrystalWaterBlue,
                modifier = Modifier.width(86.dp).padding(horizontal = 6.dp)
            )
            Text(
                text = String.format(Locale.US, "%.1f", row.proteinG),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(100.dp).padding(horizontal = 6.dp)
            )
            Text(
                text = row.biologicalNote.ifBlank {
                    if (row.pendingSyncCount > 0) "Pendiente sync (${row.pendingSyncCount})" else "Jornada abierta"
                },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(320.dp).padding(horizontal = 6.dp)
            )
        }
        HorizontalDivider(color = LeatherBorder.copy(alpha = 0.5f))
    }
}
