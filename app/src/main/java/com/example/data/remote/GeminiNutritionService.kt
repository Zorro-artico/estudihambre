package com.example.data.remote

import android.graphics.Bitmap
import android.util.Log
import com.example.domain.model.BiologicalSex
import com.example.domain.model.GeminiNutritionResponse
import com.example.domain.model.MealComparisonResult
import com.example.domain.model.PantryRecipeSuggestion
import com.example.domain.model.RoutineAdaptationResponse
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class GeminiNutritionService {

    companion object {
        private const val TAG = "GeminiNutritionService"
        const val DEFAULT_MODEL = "gemini-3.8-flash"
        val SUPPORTED_MODELS = listOf(
            "gemini-3.8-flash",
            "gemini-3.5-flash-lite"
        )
    }

    private val nutritionSchema = Schema.obj(
        properties = mapOf(
            "calorias_menor" to Schema.integer(
                description = "Caso conservador / menor al común (porción ligera, menos aceite/aderezo)."
            ),
            "proteina_menor" to Schema.double(
                description = "Proteína estimada (g) para el caso menor al común."
            ),
            "calorias_kcal" to Schema.integer(
                description = "Caso promedio estándar en kcal para la porción descrita/fotografiada."
            ),
            "proteina_g" to Schema.double(
                description = "Gramos de proteína estimados en el caso promedio."
            ),
            "calorias_mayor" to Schema.integer(
                description = "Caso abundante / mayor al común (porción generosa, extra aceite/queso/pan)."
            ),
            "proteina_mayor" to Schema.double(
                description = "Proteína estimada (g) para el caso mayor al común."
            ),
            "etiquetas_alerta" to Schema.array(
                items = Schema.string(),
                description = "Lista corta de etiquetas nutricionales en español, ej. 'exceso de sodio', 'exceso de azúcar', 'alta densidad proteica', 'fritura', 'ultra-procesado'."
            ),
            "trampas_ocultas" to Schema.array(
                items = Schema.string(),
                description = "Lista breve (0-3 ítems) de calorías ocultas típicas de comida callejera/fonda (aceite de cocción, crema, mayonesa, mantequilla en el pan, azúcar líquida)."
            ),
            "sugerencia_ahorro_kcal" to Schema.string(
                description = "Consejo realista y económico para ahorrar calorías o mejorar saciedad/proteína (ej. 'pedir sin crema ahorra ~140 kcal')."
            ),
            "resumen_breve" to Schema.string(
                description = "Resumen breve en español (1-2 frases) de ingredientes, porción y preparación."
            )
        )
    )

    private val routineSchema = Schema.obj(
        properties = mapOf(
            "actividad_sugerida" to Schema.enumeration(
                values = listOf("SEDENTARY", "LIGHT", "MODERATE", "HIGH"),
                description = "Nivel de actividad cotidiana más acorde con la rutina diaria descrita."
            ),
            "entrenamiento_sugerido" to Schema.enumeration(
                values = listOf("NONE", "LIGHT_HYPERTROPHY", "INTENSE_STRENGTH"),
                description = "Clasificación del entrenamiento de fuerza para proteger masa muscular."
            ),
            "kcal_ajuste_diario" to Schema.integer(
                description = "Ajuste adicional promedio en kcal/día (-200 a +500) por pasos, escaleras, bici o sesiones extra no cubiertas por el nivel base."
            ),
            "resumen_adaptacion" to Schema.string(
                description = "Explicación breve en español de cómo se adaptó el gasto diario y el riesgo catabólico según su rutina específica."
            )
        )
    )

    private val comparisonSchema = Schema.obj(
        properties = mapOf(
            "a_calorias" to Schema.integer(description = "Calorías promedio Opción A"),
            "a_proteina" to Schema.double(description = "Proteína g Opción A"),
            "a_resumen" to Schema.string(description = "Resumen breve Opción A"),
            "b_calorias" to Schema.integer(description = "Calorías promedio Opción B"),
            "b_proteina" to Schema.double(description = "Proteína g Opción B"),
            "b_resumen" to Schema.string(description = "Resumen breve Opción B"),
            "mejor_opcion" to Schema.enumeration(values = listOf("A", "B"), description = "Letra recomendada según el déficit y proteína del día"),
            "explicacion_comparativa" to Schema.string(description = "Por qué conviene más A o B para el usuario hoy.")
        )
    )

    private val pantrySchema = Schema.obj(
        properties = mapOf(
            "recetas" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "nombre" to Schema.string(description = "Nombre apetitoso del platillo estudiantil"),
                        "ingredientes_usados" to Schema.string(description = "Cantidades aproximadas usando lo disponible"),
                        "pasos_rapidos" to Schema.string(description = "Pasos simples (2-3 líneas, sartén/microondas/olla)"),
                        "calorias_kcal" to Schema.integer(description = "Calorías totales aproximadas"),
                        "proteina_g" to Schema.double(description = "Gramos de proteína aproximados"),
                        "costo_nivel" to Schema.string(description = "Ej. Muy económico / Medio")
                    )
                ),
                description = "2 a 3 recetas prácticas adaptadas a la meta calórica y proteica restante."
            )
        )
    )

    private val modelCache = ConcurrentHashMap<String, GenerativeModel>()

    private fun getOrCreateModel(cacheKey: String, modelName: String, schema: Schema): GenerativeModel {
        val safeModel = if (modelName in SUPPORTED_MODELS) modelName else DEFAULT_MODEL
        return modelCache.getOrPut("${cacheKey}_$safeModel") {
            Firebase.ai.generativeModel(
                modelName = safeModel,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = schema
                    temperature = 0.2f
                }
            )
        }
    }

    suspend fun analyzeMeal(
        modelName: String,
        promptText: String,
        photoBitmap: Bitmap?
    ): GeminiNutritionResponse {
        return try {
            val model = getOrCreateModel("nutri", modelName, nutritionSchema)
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        if (photoBitmap != null) image(photoBitmap)
                        text(promptText)
                    }
                )
            }
            val jsonString = response.text
                ?: throw IllegalStateException("El modelo devolvió una respuesta vacía.")
            val json = JSONObject(jsonString)

            val avgCalories = json.optInt("calorias_kcal", 0).coerceAtLeast(0)
            val avgProtein = json.optDouble("proteina_g", 0.0).toFloat().coerceAtLeast(0f)
            val lowerCalories = json.optInt("calorias_menor", (avgCalories * 0.82).roundToInt())
                .coerceIn(0, avgCalories.coerceAtLeast(1))
            val lowerProtein = json.optDouble("proteina_menor", (avgProtein * 0.88).toDouble())
                .toFloat().coerceAtLeast(0f)
            val higherCalories = json.optInt("calorias_mayor", (avgCalories * 1.20).roundToInt())
                .coerceAtLeast(avgCalories)
            val higherProtein = json.optDouble("proteina_mayor", (avgProtein * 1.12).toDouble())
                .toFloat().coerceAtLeast(avgProtein)

            val tagsArray = json.optJSONArray("etiquetas_alerta")
            val tags = if (tagsArray != null) {
                List(tagsArray.length()) { idx -> tagsArray.optString(idx) }.filter { it.isNotBlank() }
            } else emptyList()

            val trapsArray = json.optJSONArray("trampas_ocultas")
            val traps = if (trapsArray != null) {
                List(trapsArray.length()) { idx -> trapsArray.optString(idx) }.filter { it.isNotBlank() }
            } else emptyList()

            val swapTip = json.optString("sugerencia_ahorro_kcal", "")
            val summary = json.optString("resumen_breve", "Análisis completado.")

            GeminiNutritionResponse(
                calorias_kcal = avgCalories,
                proteina_g = avgProtein,
                etiquetas_alerta = tags,
                resumen_breve = summary,
                calorias_menor = lowerCalories,
                proteina_menor = lowerProtein,
                calorias_mayor = higherCalories,
                proteina_mayor = higherProtein,
                trampas_ocultas = traps,
                sugerencia_ahorro_kcal = swapTip
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error en análisis nutricional con Gemini", e)
            throw e
        }
    }

    suspend fun compareMealsAAndB(
        modelName: String,
        optionADesc: String,
        optionABitmap: Bitmap?,
        optionBDesc: String,
        optionBBitmap: Bitmap?,
        remainingBeforeCeilingKcal: Int,
        missingProteinG: Float
    ): MealComparisonResult {
        return try {
            val model = getOrCreateModel("compare", modelName, comparisonSchema)
            val prompt = buildString {
                appendLine("Compara dos opciones de comida para un estudiante que busca cuidar su composición corporal.")
                appendLine("Calorías restantes antes del techo hoy: $remainingBeforeCeilingKcal kcal. Proteína aún faltante hoy: ${missingProteinG.roundToInt()} g.")
                appendLine("Opción A: $optionADesc")
                appendLine("Opción B: $optionBDesc")
                appendLine("Estima calorías y proteína de ambas y recomienda cuál conviene más hoy (A o B).")
            }
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        optionABitmap?.let { image(it) }
                        optionBBitmap?.let { image(it) }
                        text(prompt)
                    }
                )
            }
            val json = JSONObject(response.text ?: throw IllegalStateException("Respuesta vacía en comparador."))
            val aKcal = json.optInt("a_calorias", 0).coerceAtLeast(0)
            val aProt = json.optDouble("a_proteina", 0.0).toFloat().coerceAtLeast(0f)
            val bKcal = json.optInt("b_calorias", 0).coerceAtLeast(0)
            val bProt = json.optDouble("b_proteina", 0.0).toFloat().coerceAtLeast(0f)

            MealComparisonResult(
                optionATitle = optionADesc.ifBlank { "Opción A" }.take(40),
                optionA = GeminiNutritionResponse(
                    calorias_kcal = aKcal,
                    proteina_g = aProt,
                    etiquetas_alerta = listOf("comparador A"),
                    resumen_breve = json.optString("a_resumen", "")
                ),
                optionBTitle = optionBDesc.ifBlank { "Opción B" }.take(40),
                optionB = GeminiNutritionResponse(
                    calorias_kcal = bKcal,
                    proteina_g = bProt,
                    etiquetas_alerta = listOf("comparador B"),
                    resumen_breve = json.optString("b_resumen", "")
                ),
                recommendedChoice = json.optString("mejor_opcion", "A").uppercase().let { if (it == "B") "B" else "A" },
                comparisonSummaryEs = json.optString("explicacion_comparativa", "Comparación completada.")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error en comparador A vs B", e)
            throw e
        }
    }

    suspend fun generatePantryRecipes(
        modelName: String,
        ingredientsText: String,
        fridgeBitmap: Bitmap?,
        remainingBeforeCeilingKcal: Int,
        missingProteinG: Float
    ): List<PantryRecipeSuggestion> {
        return try {
            val model = getOrCreateModel("pantry", modelName, pantrySchema)
            val prompt = buildString {
                appendLine("Eres el Alquimista de Despensa para un estudiante con presupuesto ajustado.")
                appendLine("Ingredientes disponibles: ${ingredientsText.ifBlank { "ver foto adjunta" }}")
                appendLine("Objetivo calórico restante hoy: ~${remainingBeforeCeilingKcal.coerceIn(250, 1200)} kcal; proteína faltante: ${missingProteinG.roundToInt().coerceAtLeast(15)} g.")
                appendLine("Propón 2 o 3 platillos rápidos, sabrosos y baratos usando principalmente esos ingredientes más básicos de alacena (sal, aceite mínimo, agua).")
            }
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        fridgeBitmap?.let { image(it) }
                        text(prompt)
                    }
                )
            }
            val json = JSONObject(response.text ?: throw IllegalStateException("Respuesta vacía en despensa."))
            val arr = json.optJSONArray("recetas") ?: return emptyList()
            List(arr.length()) { idx ->
                val item = arr.getJSONObject(idx)
                PantryRecipeSuggestion(
                    nombre = item.optString("nombre", "Receta rápida"),
                    ingredientes_usados = item.optString("ingredientes_usados", ""),
                    pasos_rapidos = item.optString("pasos_rapidos", ""),
                    calorias_kcal = item.optInt("calorias_kcal", 350).coerceAtLeast(50),
                    proteina_g = item.optDouble("proteina_g", 20.0).toFloat().coerceAtLeast(0f),
                    costo_nivel = item.optString("costo_nivel", "Económico")
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error generando recetas de despensa", e)
            throw e
        }
    }

    suspend fun adaptRoutine(
        modelName: String,
        dailyRoutineText: String,
        exerciseRoutineText: String,
        weightKg: Double,
        ageYears: Int,
        sex: BiologicalSex
    ): RoutineAdaptationResponse {
        return try {
            val model = getOrCreateModel("routine", modelName, routineSchema)
            val prompt = buildString {
                appendLine("Eres un fisiólogo del ejercicio y nutricionista deportivo.")
                appendLine("Analiza la rutina específica escrita por el usuario (puede describir solo su rutina diaria, solo su rutina de ejercicio, o ambas) y adáptala al perfil energético.")
                appendLine("Datos: peso=$weightKg kg, edad=$ageYears años, sexo=${sex.labelEs}.")
                appendLine("Rutina diaria cotidiana escrita: ${dailyRoutineText.ifBlank { "No especificada" }}")
                appendLine("Rutina de ejercicio / fuerza escrita: ${exerciseRoutineText.ifBlank { "No especificada" }}")
                appendLine("Devuelve JSON estricto eligiendo actividad_sugerida, entrenamiento_sugerido, un kcal_ajuste_diario conservador (-200 a +450) y un resumen_adaptacion práctico.")
            }
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonString = response.text
                ?: throw IllegalStateException("El modelo devolvió una respuesta vacía.")
            val json = JSONObject(jsonString)
            RoutineAdaptationResponse(
                actividad_sugerida = json.optString("actividad_sugerida", "LIGHT"),
                entrenamiento_sugerido = json.optString("entrenamiento_sugerido", "NONE"),
                kcal_ajuste_diario = json.optInt("kcal_ajuste_diario", 0).coerceIn(-200, 450),
                resumen_adaptacion = json.optString("resumen_adaptacion", "Rutina adaptada.")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error adaptando rutina con Gemini", e)
            throw e
        }
    }

    fun buildMealPrompt(
        description: String,
        containerName: String?,
        fillPercent: Int?,
        occupiedVolumeMl: Double?,
        batchServings: Int,
        dailyRoutineNotes: String,
        trainingNotes: String
    ): String {
        return buildString {
            appendLine("Eres un nutricionista clínico experto en gastronomía cotidiana, comida callejera/fonda, comida estudiantil y composición corporal.")
            appendLine("Analiza la comida descrita y/o mostrada en la imagen y devuelve SIEMPRE tres escenarios de porción/preparación:")
            appendLine("1) calorias_menor / proteina_menor: caso un tanto menor al común (porción ligera, menos aceite o menos pan/tortilla).")
            appendLine("2) calorias_kcal / proteina_g: caso promedio estándar.")
            appendLine("3) calorias_mayor / proteina_mayor: caso un tanto mayor al común (porción generosa, aceite de fonda, aderezo o queso extra).")
            appendLine("Incluye en trampas_ocultas las posibles calorías ocultas típicas (aceite, crema, mayonesa, pan, azúcar) y en sugerencia_ahorro_kcal un cambio realista.")
            if (description.isNotBlank()) {
                appendLine("Descripción del usuario: $description")
            }
            if (!containerName.isNullOrBlank() && occupiedVolumeMl != null && occupiedVolumeMl > 0.0) {
                val pctText = fillPercent?.let { "$it% de llenado" } ?: ""
                appendLine("Escala física exacta del recipiente: '$containerName' ($pctText), volumen ocupado = ${occupiedVolumeMl.toInt()} ml. Úsalo como referencia geométrica estricta.")
            }
            if (batchServings > 1) {
                appendLine("NOTA (Modo Olla de Aventurero): La descripción o foto corresponde a una preparación completa dividida en $batchServings porciones iguales. Reporta las calorías y proteína TOTALES del lote completo mostrado; la app dividirá entre $batchServings.")
            }
            if (dailyRoutineNotes.isNotBlank()) {
                appendLine("Rutina diaria del usuario: $dailyRoutineNotes.")
            }
            if (trainingNotes.isNotBlank()) {
                appendLine("Rutina de ejercicio del usuario: $trainingNotes.")
            }
            appendLine("Responde estrictamente en JSON válido según el esquema configurado, en español.")
        }
    }
}
