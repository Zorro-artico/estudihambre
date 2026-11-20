package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageStorageHelper {

    private const val MAX_DIMENSION_PX = 1024
    private const val PENDING_DIR = "pending_meals"

    private fun pendingDir(context: Context): File {
        return File(context.filesDir, PENDING_DIR).apply { if (!exists()) mkdirs() }
    }

    suspend fun saveCompressedUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@runCatching null

            val scaled = scaleDownBitmap(decoded, MAX_DIMENSION_PX)
            val outFile = File(pendingDir(context), "meal_${UUID.randomUUID()}.webp")
            FileOutputStream(outFile).use { fos ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                scaled.compress(format, 80, fos)
            }
            if (scaled !== decoded) decoded.recycle()
            scaled.recycle()
            outFile.absolutePath
        }.getOrNull()
    }

    suspend fun saveCompressedFile(context: Context, sourceFile: File): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
            val sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOpts) ?: return@runCatching null
            val scaled = scaleDownBitmap(decoded, MAX_DIMENSION_PX)
            val outFile = File(pendingDir(context), "meal_${UUID.randomUUID()}.webp")
            FileOutputStream(outFile).use { fos ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                scaled.compress(format, 80, fos)
            }
            if (scaled !== decoded) decoded.recycle()
            scaled.recycle()
            sourceFile.delete()
            outFile.absolutePath
        }.getOrNull()
    }

    suspend fun loadBitmapForAi(path: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)
            if (!file.exists()) return@runCatching null
            BitmapFactory.decodeFile(file.absolutePath)
        }.getOrNull()
    }

    fun deleteLocalPhotoIfOwned(context: Context, path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path)
            val pendingRoot = pendingDir(context).canonicalPath
            if (file.exists() && file.canonicalPath.startsWith(pendingRoot)) {
                file.delete()
            }
        }
    }

    private fun computeInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        if (height > maxDim || width > maxDim) {
            val halfH = height / 2
            val halfW = width / 2
            while ((halfH / inSampleSize) >= maxDim && (halfW / inSampleSize) >= maxDim) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun scaleDownBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val matrix = Matrix().apply { postScale(ratio, ratio) }
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
    }
}
