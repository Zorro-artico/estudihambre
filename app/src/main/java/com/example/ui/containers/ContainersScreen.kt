package com.example.ui.containers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.ContainerItem
import com.example.domain.model.ContainerShape
import com.example.ui.components.ContainerGeometryDiagram
import com.example.ui.theme.CrystalWaterBlue
import com.example.ui.theme.EmberCrimson
import com.example.ui.theme.HoneyBrothGold
import com.example.ui.theme.LeatherBorder
import com.example.ui.theme.MutedParchment
import kotlin.math.roundToInt

@Composable
fun ContainersScreen(
    uiState: ContainersUiState,
    onOpenNew: () -> Unit,
    onOpenEdit: (ContainerItem) -> Unit,
    onCloseEditor: () -> Unit,
    onNameChange: (String) -> Unit,
    onShapeChange: (ContainerShape) -> Unit,
    onRadiusChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onLengthChange: (String) -> Unit,
    onWidthChange: (String) -> Unit,
    onPreviewFillChange: (Int) -> Unit,
    onSaveEditor: () -> Unit,
    onToggleFavorite: (ContainerItem) -> Unit,
    onDeleteContainer: (Long) -> Unit,
    onAddStudentPresets: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenNew,
                icon = { Icon(Icons.Default.Add, contentDescription = "Nuevo recipiente") },
                text = { Text("Nuevo Recipiente") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_container_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Arsenal Geométrico",
                    style = MaterialTheme.typography.headlineMedium,
                    color = HoneyBrothGold
                )
                Text(
                    text = "Mide tus tuppers, vasos o bowls en cm para darle a Gemini escala exacta en mililitros.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedParchment
                )
            }

            if (uiState.containers.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, LeatherBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Aún no tienes recipientes guardados",
                                style = MaterialTheme.typography.titleMedium,
                                color = HoneyBrothGold
                            )
                            Text(
                                text = "Puedes crear los tuyos con una regla o cargar presets típicos de estudiante (vaso de sopa instantánea, tupper mediano y bowl).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedParchment
                            )
                            OutlinedButton(
                                onClick = onAddStudentPresets,
                                modifier = Modifier.testTag("load_container_presets_button")
                            ) {
                                Text("Cargar presets estudiantiles")
                            }
                        }
                    }
                }
            } else {
                items(uiState.containers, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, LeatherBorder),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContainerGeometryDiagram(shape = item.shape, fillPercent = 75)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${item.shape.labelEs} · ${item.shape.formulaLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HoneyBrothGold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "100% = ${item.volumeMl.roundToInt()} ml  |  50% = ${(item.volumeMl * 0.5).roundToInt()} ml",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrystalWaterBlue
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                IconButton(onClick = { onToggleFavorite(item) }) {
                                    Icon(
                                        imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Favorito",
                                        tint = HoneyBrothGold
                                    )
                                }
                                Row {
                                    IconButton(onClick = { onOpenEdit(item) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                                    }
                                    IconButton(onClick = { onDeleteContainer(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = EmberCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isEditorVisible) {
        val ed = uiState.editor
        AlertDialog(
            onDismissRequest = onCloseEditor,
            title = { Text(if (ed.editingId == 0L) "Nuevo Recipiente" else "Editar Recipiente") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ed.name,
                        onValueChange = onNameChange,
                        label = { Text("Nombre (ej. Vaso Maruchan / Tupper)") },
                        singleLine = true,
                        modifier = Modifier.testTag("container_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ContainerShape.entries.forEach { shape ->
                            FilterChip(
                                selected = ed.shape == shape,
                                onClick = { onShapeChange(shape) },
                                label = { Text(shape.labelEs.take(6)) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ContainerGeometryDiagram(shape = ed.shape, fillPercent = ed.previewFillPercent)
                        Column {
                            Text(ed.shape.formulaLabel, style = MaterialTheme.typography.labelMedium, color = HoneyBrothGold)
                            Text(
                                "Total: ${uiState.liveTotalVolumeMl.roundToInt()} ml",
                                style = MaterialTheme.typography.titleMedium,
                                color = CrystalWaterBlue
                            )
                            Text(
                                "Al ${ed.previewFillPercent}%: ${uiState.liveOccupiedVolumeMl.roundToInt()} ml",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedParchment
                            )
                        }
                    }

                    when (ed.shape) {
                        ContainerShape.CYLINDRICAL -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = ed.radiusText,
                                    onValueChange = onRadiusChange,
                                    label = { Text("Radio (cm)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = ed.heightText,
                                    onValueChange = onHeightChange,
                                    label = { Text("Altura (cm)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        ContainerShape.RECTANGULAR -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = ed.lengthText,
                                    onValueChange = onLengthChange,
                                    label = { Text("Largo") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = ed.widthText,
                                    onValueChange = onWidthChange,
                                    label = { Text("Ancho") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = ed.heightText,
                                    onValueChange = onHeightChange,
                                    label = { Text("Alto") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        ContainerShape.HEMISPHERICAL -> {
                            OutlinedTextField(
                                value = ed.radiusText,
                                onValueChange = onRadiusChange,
                                label = { Text("Radio interno (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(25, 50, 75, 100).forEach { pct ->
                            FilterChip(
                                selected = ed.previewFillPercent == pct,
                                onClick = { onPreviewFillChange(pct) },
                                label = { Text("$pct%") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onSaveEditor,
                    modifier = Modifier.testTag("save_container_button")
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onCloseEditor) {
                    Text("Cancelar")
                }
            }
        )
    }
}
