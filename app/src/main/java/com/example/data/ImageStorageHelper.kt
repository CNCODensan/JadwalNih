package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.domain.DateTimeHelper
import com.example.domain.PhotoTextAlignment
import com.example.domain.PhotoTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImageStorageHelper(private val context: Context) {
    data class CollageExportResult(
        val shareUri: Uri,
        val savedToGallery: Boolean,
    )

    private val recapDir: File
        get() = File(context.filesDir, "recaps").apply {
            if (!exists()) mkdirs()
        }

    private val collageCacheDir: File
        get() = File(context.cacheDir, "collages").apply {
            if (!exists()) mkdirs()
        }

    suspend fun saveCompressedFromUri(
        uri: Uri,
        textStyle: PhotoTextStyle = PhotoTextStyle.NONE,
        textAlignment: PhotoTextAlignment = PhotoTextAlignment.CENTER,
        overlayTimeLabel: String = "",
        overlayTitle: String = "",
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val maxDim = 1080
            val tempFile = copyUriToTempFile(uri)
            try {
                val decoded = decodeBitmapFromFile(tempFile, maxDim)
                val scaled = scaleDownIfNeeded(decoded, maxDim)
                val flattened = flattenAlphaIfNeeded(scaled)
                val styled = applyTextOverlay(
                    source = flattened,
                    textStyle = textStyle,
                    textAlignment = textAlignment,
                    timeLabel = overlayTimeLabel,
                    title = overlayTitle,
                )

                val outFile = File(recapDir, "recap_${UUID.randomUUID()}.jpg")
                FileOutputStream(outFile).use { fos ->
                    if (!styled.compress(Bitmap.CompressFormat.JPEG, 86, fos)) {
                        throw IllegalStateException("Gagal menyimpan gambar terkompresi.")
                    }
                }
                if (styled !== flattened) styled.recycle()
                if (flattened !== scaled) flattened.recycle()
                if (scaled !== decoded) decoded.recycle()
                scaled.recycle()
                outFile.absolutePath
            } finally {
                tempFile.delete()
            }
        }
    }

    suspend fun exportCollageToGalleryAndShareUri(
        dateIso: String,
        title: String,
        imagePaths: List<String>,
    ): Result<CollageExportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val validFiles = imagePaths.map { File(it) }.filter { it.exists() }.take(4)
            require(validFiles.isNotEmpty()) { "Belum ada foto di tanggal ini untuk diekspor." }

            val bitmaps = validFiles.mapNotNull { file ->
                decodeBitmapFromFile(file, 960)
            }
            require(bitmaps.isNotEmpty()) { "Foto rekap tidak dapat dimuat untuk kolase." }

            val width = 1080
            val height = 1260
            val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)

            // Clean light Pesisir solid backdrop
            canvas.drawColor(Color.parseColor("#F5F7FA"))

            val pad = 44f
            val headerH = 148f
            val headerRect = RectF(pad, pad, width - pad, pad + headerH)
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1677FF")
            }
            canvas.drawRoundRect(headerRect, 32f, 32f, headerPaint)

            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FACC15")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("JADWALNIH • REKAP HARIAN", pad + 32f, pad + 52f, brandPaint)

            val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 40f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headline = title.trim().ifBlank { DateTimeHelper.formatIndonesianFullDate(dateIso) }
            canvas.drawText(headline.take(32), pad + 32f, pad + 104f, datePaint)

            if (title.isNotBlank()) {
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(220, 224, 242, 254)
                    textSize = 24f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText(DateTimeHelper.formatShortDate(dateIso), width - pad - 32f, pad + 52f, subPaint)
            }

            // Collage photo grid area
            val gridTop = pad + headerH + 26f
            val gridBottom = height - pad
            val gridLeft = pad
            val gridRight = width - pad
            val gridW = gridRight - gridLeft
            val gridH = gridBottom - gridTop
            val gap = 18f

            val rects = when (bitmaps.size) {
                1 -> listOf(RectF(gridLeft, gridTop, gridRight, gridBottom))
                2 -> {
                    val tileW = (gridW - gap) / 2f
                    listOf(
                        RectF(gridLeft, gridTop, gridLeft + tileW, gridBottom),
                        RectF(gridLeft + tileW + gap, gridTop, gridRight, gridBottom),
                    )
                }
                3 -> {
                    val leftW = gridW * 0.56f
                    val rightLeft = gridLeft + leftW + gap
                    val rightH = (gridH - gap) / 2f
                    listOf(
                        RectF(gridLeft, gridTop, gridLeft + leftW, gridBottom),
                        RectF(rightLeft, gridTop, gridRight, gridTop + rightH),
                        RectF(rightLeft, gridTop + rightH + gap, gridRight, gridBottom),
                    )
                }
                else -> {
                    val tileW = (gridW - gap) / 2f
                    val tileH = (gridH - gap) / 2f
                    listOf(
                        RectF(gridLeft, gridTop, gridLeft + tileW, gridTop + tileH),
                        RectF(gridLeft + tileW + gap, gridTop, gridRight, gridTop + tileH),
                        RectF(gridLeft, gridTop + tileH + gap, gridLeft + tileW, gridBottom),
                        RectF(gridLeft + tileW + gap, gridTop + tileH + gap, gridRight, gridBottom),
                    )
                }
            }

            bitmaps.zip(rects).forEach { (bmp, destRect) ->
                drawCenterCroppedRoundedBitmap(canvas, bmp, destRect, cornerRadius = 28f)
                bmp.recycle()
            }

            val shareFile = File(collageCacheDir, "kolase_${dateIso}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(shareFile).use { fos ->
                if (!out.compress(Bitmap.CompressFormat.JPEG, 92, fos)) {
                    throw IllegalStateException("Gagal membuat file kolase.")
                }
            }

            val savedToGallery = saveToGalleryMediaStore(out, dateIso)
            out.recycle()

            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                shareFile,
            )
            CollageExportResult(
                shareUri = shareUri,
                savedToGallery = savedToGallery,
            )
        }
    }

    private fun drawCenterCroppedRoundedBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        dest: RectF,
        cornerRadius: Float,
    ) {
        val clipPath = Path().apply {
            addRoundRect(dest, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        val saveCount = canvas.save()
        canvas.clipPath(clipPath)

        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()
        val dstW = dest.width()
        val dstH = dest.height()
        val scale = maxOf(dstW / srcW, dstH / srcH)
        val cropW = (dstW / scale).toInt().coerceIn(1, bitmap.width)
        val cropH = (dstH / scale).toInt().coerceIn(1, bitmap.height)
        val cropLeft = ((bitmap.width - cropW) / 2).coerceAtLeast(0)
        val cropTop = ((bitmap.height - cropH) / 2).coerceAtLeast(0)

        val srcRect = Rect(cropLeft, cropTop, cropLeft + cropW, cropTop + cropH)
        canvas.drawBitmap(bitmap, srcRect, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restoreToCount(saveCount)
    }

    private fun saveToGalleryMediaStore(bitmap: Bitmap, dateIso: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false
        }
        return runCatching {
            val fileName = "JadwalNih_Kolase_${dateIso}_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/JadwalNih")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching false

            resolver.openOutputStream(itemUri)?.use { os ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, os)
            } ?: return@runCatching false

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            true
        }.getOrDefault(false)
    }

    private fun copyUriToTempFile(uri: Uri): File {
        val resolver = context.contentResolver
        val tempFile = File.createTempFile("recap_pick_", ".tmp", context.cacheDir)
        var copiedBytes = 0L

        val opened = resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                copiedBytes = input.copyTo(output)
            }
            true
        } ?: resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            afd.createInputStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    copiedBytes = input.copyTo(output)
                }
            }
            true
        } ?: false

        if (!opened || copiedBytes <= 0L) {
            tempFile.delete()
            throw IllegalStateException("Tidak dapat membaca berkas gambar yang dipilih.")
        }
        return tempFile
    }

    private fun decodeBitmapFromFile(file: File, maxDim: Int): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val decodedWithImageDecoder = runCatching {
                val source = ImageDecoder.createSource(file)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width
                    val h = info.size.height
                    if (w > maxDim || h > maxDim) {
                        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
                        decoder.setTargetSize(
                            (w * ratio).toInt().coerceAtLeast(1),
                            (h * ratio).toInt().coerceAtLeast(1),
                        )
                    }
                }
            }.getOrNull()
            if (decodedWithImageDecoder != null) {
                return decodedWithImageDecoder
            }
        }

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOpts)

        val decodeOpts = BitmapFactory.Options()
        if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > maxDim * 2 ||
                boundsOpts.outHeight / sampleSize > maxDim * 2
            ) {
                sampleSize *= 2
            }
            decodeOpts.inSampleSize = sampleSize
        }

        return BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
            ?: BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalStateException("Format gambar tidak didukung atau berkas rusak.")
    }

    private fun flattenAlphaIfNeeded(bitmap: Bitmap): Bitmap {
        if (!bitmap.hasAlpha()) return bitmap
        val whiteBacked = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(whiteBacked)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return whiteBacked
    }

    private fun applyTextOverlay(
        source: Bitmap,
        textStyle: PhotoTextStyle,
        textAlignment: PhotoTextAlignment,
        timeLabel: String,
        title: String,
    ): Bitmap {
        val cleanTime = timeLabel.trim()
        val cleanTitle = title.trim()
        val needsText = textStyle != PhotoTextStyle.NONE &&
            (cleanTime.isNotEmpty() || cleanTitle.isNotEmpty())
        if (!needsText) return source

        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(source, 0f, 0f, imagePaint)

        val w = out.width.toFloat()
        val h = out.height.toFloat()
        val minSide = minOf(w, h)
        val margin = minSide * 0.055f

        when (textStyle) {
            PhotoTextStyle.TIME_PILL -> {
                val badgeText = cleanTime.ifEmpty { cleanTitle }
                val textSizePx = (minSide * 0.044f).coerceIn(20f, 44f)
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = textSizePx
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val padH = textSizePx * 0.85f
                val padV = textSizePx * 0.45f
                val textWidth = textPaint.measureText(badgeText)
                val fm = textPaint.fontMetrics
                val textHeight = fm.descent - fm.ascent
                val pillW = textWidth + padH * 2f
                val pillH = textHeight + padV * 2f
                val left = when (textAlignment) {
                    PhotoTextAlignment.LEFT -> margin
                    PhotoTextAlignment.CENTER -> (w - pillW) / 2f
                    PhotoTextAlignment.RIGHT -> (w - pillW - margin).coerceAtLeast(margin)
                }
                val bottom = h - (minSide * 0.06f)
                val top = bottom - pillH

                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(180, 10, 14, 34)
                }
                val rect = RectF(left, top, left + pillW, bottom)
                canvas.drawRoundRect(rect, pillH / 2f, pillH / 2f, bgPaint)

                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(75, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawRoundRect(rect, pillH / 2f, pillH / 2f, borderPaint)

                val baselineY = top + padV - fm.ascent
                canvas.drawText(badgeText, left + padH, baselineY, textPaint)
            }

            PhotoTextStyle.CAPTION_BOTTOM -> {
                val scrimTop = h * 0.68f
                val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f,
                        scrimTop,
                        0f,
                        h,
                        intArrayOf(Color.TRANSPARENT, Color.argb(190, 7, 11, 25)),
                        null,
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, scrimTop, w, h, scrimPaint)

                val paintAlign = when (textAlignment) {
                    PhotoTextAlignment.LEFT -> Paint.Align.LEFT
                    PhotoTextAlignment.CENTER -> Paint.Align.CENTER
                    PhotoTextAlignment.RIGHT -> Paint.Align.RIGHT
                }
                val anchorX = when (textAlignment) {
                    PhotoTextAlignment.LEFT -> margin
                    PhotoTextAlignment.CENTER -> w / 2f
                    PhotoTextAlignment.RIGHT -> w - margin
                }

                val headline = cleanTitle.ifEmpty { cleanTime }
                val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = (minSide * 0.052f).coerceIn(22f, 52f)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = paintAlign
                }
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(220, 219, 234, 254)
                    textSize = (minSide * 0.036f).coerceIn(16f, 36f)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textAlign = paintAlign
                }
                val subY = h - margin
                if (cleanTitle.isNotEmpty() && cleanTime.isNotEmpty()) {
                    canvas.drawText(cleanTime, anchorX, subY, subPaint)
                    canvas.drawText(headline, anchorX, subY - headlinePaint.textSize * 0.92f, headlinePaint)
                } else {
                    canvas.drawText(headline, anchorX, subY, headlinePaint)
                }
            }

            PhotoTextStyle.NONE -> Unit
        }
        return out
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
            collageCacheDir.listFiles()?.forEach { it.delete() }
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
