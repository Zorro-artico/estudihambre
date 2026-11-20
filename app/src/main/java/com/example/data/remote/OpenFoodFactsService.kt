package com.example.data.remote

import com.example.domain.model.GeminiNutritionResponse
import com.example.domain.usecase.FoodEmojiMapper
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class BarcodeProductLookupResult(
    val barcode: String,
    val productName: String,
    val nutrition: GeminiNutritionResponse,
    val fromOpenFoodFacts: Boolean
)

class OpenFoodFactsService {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    suspend fun lookupBarcode(barcode: String): Result<BarcodeProductLookupResult> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanCode = barcode.filter { it.isDigit() }
            require(cleanCode.isNotBlank()) { "Código de barras vacío o inválido." }

            val request = Request.Builder()
                .url("https://world.openfoodfacts.org/api/v2/product/$cleanCode.json")
                .header("User-Agent", "EstudihambreLife-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Error consultando producto (HTTP ${response.code}).")
                }
                val body = response.body?.string() ?: throw IllegalStateException("Respuesta vacía del catálogo.")
                val root = JSONObject(body)
                if (root.optInt("status", 0) != 1) {
                    throw IllegalStateException("Producto $cleanCode no encontrado en OpenFoodFacts. Puedes tomar foto de su tabla nutrimental.")
                }

                val product = root.optJSONObject("product") ?: JSONObject()
                val rawName = product.optString("product_name_es")
                    .ifBlank { product.optString("product_name") }
                    .ifBlank { "Producto empaquetado ($cleanCode)" }
                val brand = product.optString("brands").trim()
                val displayName = if (brand.isNotBlank() && !rawName.contains(brand, ignoreCase = true)) {
                    "$rawName ($brand)"
                } else {
                    rawName
                }

                val nutriments = product.optJSONObject("nutriments") ?: JSONObject()
                val kcalServing = nutriments.optDouble("energy-kcal_serving", -1.0)
                val kcal100g = nutriments.optDouble("energy-kcal_100g", -1.0)
                val avgKcal = when {
                    kcalServing > 0.0 -> kcalServing.roundToInt()
                    kcal100g > 0.0 -> kcal100g.roundToInt()
                    else -> 220
                }

                val protServing = nutriments.optDouble("proteins_serving", -1.0)
                val prot100g = nutriments.optDouble("proteins_100g", -1.0)
                val avgProt = when {
                    protServing >= 0.0 -> protServing.toFloat()
                    prot100g >= 0.0 -> prot100g.toFloat()
                    else -> 5f
                }

                val sodium100g = nutriments.optDouble("sodium_100g", 0.0)
                val sugars100g = nutriments.optDouble("sugars_100g", 0.0)
                val tags = buildList {
                    if (sodium100g >= 0.5) add("exceso de sodio")
                    if (sugars100g >= 12.0) add("exceso de azúcar")
                    if (avgProt >= 12f) add("buena proteína")
                    add("código de barras $cleanCode")
                }

                val basis = if (kcalServing > 0.0) "por porción" else "estimado por 100g"
                val titled = FoodEmojiMapper.formatTitleWithEmoji(displayName)
                val nutrition = GeminiNutritionResponse(
                    calorias_kcal = avgKcal,
                    proteina_g = avgProt,
                    etiquetas_alerta = tags,
                    resumen_breve = "Escaneado por código $cleanCode ($basis).",
                    calorias_menor = (avgKcal * 0.85).roundToInt(),
                    proteina_menor = avgProt * 0.85f,
                    calorias_mayor = (avgKcal * 1.25).roundToInt(),
                    proteina_mayor = avgProt * 1.15f,
                    trampas_ocultas = if (sodium100g >= 0.5) listOf("Alto contenido de sodio en etiqueta ($basis)") else emptyList(),
                    sugerencia_ahorro_kcal = if (kcalServing <= 0.0) "Ajusta el slider de porción según el peso neto del empaque." else ""
                )

                BarcodeProductLookupResult(
                    barcode = cleanCode,
                    productName = titled,
                    nutrition = nutrition,
                    fromOpenFoodFacts = true
                )
            }
        }
    }
}
