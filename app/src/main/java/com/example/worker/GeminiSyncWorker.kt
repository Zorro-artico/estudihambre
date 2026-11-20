package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.remote.GeminiNutritionService
import com.example.domain.model.SyncStatus
import com.example.domain.usecase.BiologicalNoteSynthesizer
import com.example.util.ImageStorageHelper
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

class GeminiSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "GeminiSyncWorker"
        private const val UNIQUE_WORK_NAME = "estudihambre_offline_sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<GeminiSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val syncDao = db.syncQueueDao()
        val mealDao = db.mealDao()
        val profileDao = db.userProfileDao()
        val dailyLogDao = db.dailyLogDao()
        val weightDao = db.weightDao()

        val gemini = GeminiNutritionService()
        val synthesizer = BiologicalNoteSynthesizer()
        val modelName = profileDao.getProfileOnce()?.selectedGeminiModel ?: GeminiNutritionService.DEFAULT_MODEL

        val pendingItems = syncDao.getQueueOnce()
        if (pendingItems.isEmpty()) return Result.success()

        var anyTransientFailure = false

        for (item in pendingItems) {
            val meal = mealDao.getMealByIdOnce(item.mealEntryId)
            if (meal == null) {
                ImageStorageHelper.deleteLocalPhotoIfOwned(applicationContext, item.localPhotoPath)
                syncDao.deleteById(item.id)
                continue
            }

            try {
                val bitmap = item.localPhotoPath?.let { ImageStorageHelper.loadBitmapForAi(it) }
                val aiResult = gemini.analyzeMeal(
                    modelName = modelName,
                    promptText = item.promptContext,
                    photoBitmap = bitmap
                )
                bitmap?.recycle()

                val servings = item.batchServings.coerceAtLeast(1)
                val perServingKcal = (aiResult.calorias_kcal.toDouble() / servings).roundToInt()
                val perServingProtein = aiResult.proteina_g / servings

                val updatedMeal = meal.copy(
                    title = com.example.domain.usecase.FoodEmojiMapper.formatTitleWithEmoji(meal.title, meal.description),
                    caloriesKcal = perServingKcal,
                    proteinG = perServingProtein,
                    alertTags = aiResult.etiquetas_alerta,
                    summary = aiResult.resumen_breve,
                    syncStatus = SyncStatus.SYNCED.name,
                    localPhotoPath = null
                )
                mealDao.updateMeal(updatedMeal)

                // Re-synthesize biological note if the original logical day was already closed
                val dayLog = dailyLogDao.getLogByDateOnce(item.targetLogicalDate)
                if (dayLog != null && dayLog.isClosed) {
                    val dayMeals = mealDao.getMealsForDateOnce(item.targetLogicalDate).map { it.toDomain() }
                    val latestWeight = weightDao.getAllWeightsAscOnce().lastOrNull { it.logicalDate <= item.targetLogicalDate }?.toDomain()
                    val refreshedNote = synthesizer.synthesizeClosingNote(dayLog.toDomain(), dayMeals, latestWeight)
                    dailyLogDao.upsertLog(dayLog.copy(biologicalNote = refreshedNote))
                }

                ImageStorageHelper.deleteLocalPhotoIfOwned(applicationContext, item.localPhotoPath)
                syncDao.deleteById(item.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error sincronizando item ${item.id}", e)
                val nextRetry = item.retryCount + 1
                syncDao.updateItem(item.copy(retryCount = nextRetry, lastError = e.localizedMessage))
                if (nextRetry >= 5) {
                    mealDao.updateMeal(meal.copy(syncStatus = SyncStatus.FAILED.name))
                }
                anyTransientFailure = true
            }
        }

        return if (anyTransientFailure) Result.retry() else Result.success()
    }
}
