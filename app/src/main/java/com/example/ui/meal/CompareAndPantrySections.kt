package com.example.ui.meal

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.PantryRecipeSuggestion
import com.example.ui.theme.CrystalWaterBlue
import com.example.ui.theme.EmberCrimson
import com.example.ui.theme.HoneyBrothGold
import com.example.ui.theme.HyruleSage
import com.example.ui.theme.LeatherBorder
import com.example.ui.theme.MutedParchment
import kotlin.math.roundToInt

@Composable
fun CompareABSection(
    uiState: MealLoggerUiState,
    onOptionADescChange: (String) -> Unit,
    onOptionAPhotoPicked: (Uri) -> Unit,
    onOptionBDescChange: (String) -> Unit,
    onOptionBPhotoPicked: (Uri) -> Unit,
    onCompare: () -> Unit,
    onAcceptOption: (String) -> Unit
) {
    val pickA = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onOptionAPhotoPicked(uri)
    }
    val pickB = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onOptionBPhotoPicked(uri)
    }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, tint = HoneyBrothGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Comparador A vs. B antes de elegir", style = MaterialTheme.typography.titleMedium, color = HoneyBrothGold)
            }
            Text(
                text = "Evalúa dos opciones lado a lado respecto a tu presupuesto calórico y proteína faltante hoy.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedParchment
            )

            OutlinedTextField(
                value = uiState.optionADescription,
                onValueChange = onOptionADescChange,
                label = { Text("Opción A (ej. Torta de huevo con frijoles)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { pickA.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (uiState.optionAPhotoPath != null) "Foto A adjunta ✓" else "Foto A (opcional)")
            }

            OutlinedTextField(
                value = uiState.optionBDescription,
                onValueChange = onOptionBDescChange,
                label = { Text("Opción B (ej. Quesadillas de pollo sin crema)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { pickB.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (uiState.optionBPhotoPath != null) "Foto B adjunta ✓" else "Foto B (opcional)")
            }

            Button(
                onClick = onCompare,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("compare_ab_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Comparar A vs. B con Gemini")
            }

            when (val st = uiState.comparisonState) {
                ComparisonUiState.Idle -> Unit
                ComparisonUiState.Comparing -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        Text("Evaluando ambas opciones...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is ComparisonUiState.Error -> {
                    Text(st.message, color = EmberCrimson, style = MaterialTheme.typography.bodySmall)
                }
                is ComparisonUiState.Ready -> {
                    val res = st.result
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ComparisonChoiceCard(
                            label = "Opción A",
                            title = res.optionATitle,
                            kcal = res.optionA.calorias_kcal,
                            proteinG = res.optionA.proteina_g,
                            isRecommended = res.recommendedChoice == "A",
                            onSelect = { onAcceptOption("A") },
                            modifier = Modifier.weight(1f)
                        )
                        ComparisonChoiceCard(
                            label = "Opción B",
                            title = res.optionBTitle,
                            kcal = res.optionB.calorias_kcal,
                            proteinG = res.optionB.proteina_g,
                            isRecommended = res.recommendedChoice == "B",
                            onSelect = { onAcceptOption("B") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Veredicto: ${res.comparisonSummaryEs}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonChoiceCard(
    label: String,
    title: String,
    kcal: Int,
    proteinG: Float,
    isRecommended: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isRecommended) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.5.dp, if (isRecommended) HyruleSage else LeatherBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isRecommended) "$label ★ Mejor" else label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isRecommended) HyruleSage else HoneyBrothGold
            )
            Text(title, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            Text("$kcal kcal · ${proteinG.roundToInt()}g P", style = MaterialTheme.typography.labelMedium, color = CrystalWaterBlue)
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Elegir")
            }
        }
    }
}

@Composable
fun PantryAlchemistSection(
    uiState: MealLoggerUiState,
    onIngredientsChange: (String) -> Unit,
    onPantryPhotoPicked: (Uri) -> Unit,
    onGenerateRecipes: () -> Unit,
    onAcceptRecipe: (PantryRecipeSuggestion) -> Unit
) {
    val pickFridge = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPantryPhotoPicked(uri)
    }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Kitchen, contentDescription = null, tint = HoneyBrothGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Alquimista de Despensa", style = MaterialTheme.typography.titleMedium, color = HoneyBrothGold)
            }
            Text(
                text = "Dinos qué tienes en el refri o sube una foto y Gemini creará recetas rápidas ajustadas a tus kcal y proteína restantes.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedParchment
            )

            OutlinedTextField(
                value = uiState.pantryIngredientsText,
                onValueChange = onIngredientsChange,
                label = { Text("Ingredientes disponibles (ej. 3 huevos, arroz, frijoles, queso)") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pantry_ingredients_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { pickFridge.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.pantryPhotoPath != null) "Foto refri ✓" else "Foto refri")
                }
                Button(
                    onClick = onGenerateRecipes,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("generate_pantry_recipes_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cocinar con IA")
                }
            }

            when (val st = uiState.pantryState) {
                PantryUiState.Idle -> Unit
                PantryUiState.Generating -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        Text("Mezclando ingredientes en el caldero...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is PantryUiState.Error -> {
                    Text(st.message, color = EmberCrimson, style = MaterialTheme.typography.bodySmall)
                }
                is PantryUiState.Ready -> {
                    st.recipes.forEach { recipe ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, LeatherBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(recipe.nombre, style = MaterialTheme.typography.titleSmall, color = HoneyBrothGold)
                                    Text("${recipe.calorias_kcal} kcal · ${recipe.proteina_g.roundToInt()}g P", style = MaterialTheme.typography.labelMedium, color = HyruleSage)
                                }
                                Text("Ingredientes: ${recipe.ingredientes_usados}", style = MaterialTheme.typography.bodySmall)
                                Text("Preparación: ${recipe.pasos_rapidos}", style = MaterialTheme.typography.bodySmall, color = MutedParchment)
                                Button(
                                    onClick = { onAcceptRecipe(recipe) },
                                    colors = ButtonDefaults.buttonColors(containerColor = HyruleSage),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Registrar esta receta")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
