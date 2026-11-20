package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImageStorageHelper(private val context: Context) {
    private val recapDir: File
        get() = File(context.filesDir, "recaps").apply {
            if (!exists()) mkdirs()
        }

    suspend fun saveCompressedFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            } ?: throw IllegalStateException("Tidak dapat membaca berkas gambar yang dipilih.")

            val maxDim = 1080
            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > maxDim * 2 ||
                boundsOpts.outHeight / sampleSize > maxDim * 2
            ) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: throw IllegalStateException("Gagal memproses gambar dari galeri.")

            val scaled = scaleDownIfNeeded(decoded, maxDim)
            val outFile = File(recapDir, "recap_${UUID.randomUUID()}.jpg")
            FileOutputStream(outFile).use { fos ->
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, 82, fos)) {
                    throw IllegalStateException("Gagal menyimpan gambar terkompresi.")
                }
            }
            if (scaled !== decoded) {
                decoded.recycle()
            }
            scaled.recycle()
            outFile.absolutePath
        }
    }

    suspend fun saveFromBase64(base64: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val outFile = File(recapDir, "recap_${UUID.randomUUID()}.jpg")
            FileOutputStream(outFile).use { it.write(bytes) }
            outFile.absolutePath
        }.getOrNull()
    }

    suspend fun encodeFileToBase64(path: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)
            if (!file.exists()) return@runCatching null
            val bytes = file.readBytes()
            // Guard against huge individual payloads
            if (bytes.size > 1_200_000) {
                val bmp = BitmapFactory.decodeFile(path) ?: return@runCatching null
                val scaled = scaleDownIfNeeded(bmp, 720)
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                if (scaled !== bmp) bmp.recycle()
                scaled.recycle()
                Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            } else {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        }.getOrNull()
    }

    fun deleteFileIfExists(path: String) {
        runCatching {
            val f = File(path)
            if (f.exists()) f.delete()
        }
    }

    fun clearAllRecapImages() {
        runCatching {
            recapDir.listFiles()?.forEach { it.delete() }
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap
        val ratio = minOf(maxDimension.toFloat() / w, maxDimension.toFloat() / h)
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
