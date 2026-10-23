package com.example.personas.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID
import kotlin.math.max

object FotoImporter {
    fun nuevaCaptura(context: Context): File = File(context.filesDir, "drafts").apply { mkdirs() }
        .resolve("captura-${UUID.randomUUID()}.jpg").apply { createNewFile() }
    fun importar(context: Context, uri: Uri): String {
        val directory = File(context.filesDir, "drafts").apply { mkdirs() }
        val input = File(directory, "entrada-${UUID.randomUUID()}.tmp")
        val output = File(directory, "foto-${UUID.randomUUID()}.jpg")
        try {
            context.contentResolver.openInputStream(uri)?.use { source ->
                input.outputStream().use { target ->
                    val buffer = ByteArray(8192); var total = 0L
                    while (true) {
                        val count = source.read(buffer); if (count < 0) break
                        total += count; require(total <= 10 * 1024 * 1024) { "La imagen supera los 10 MB. Selecciona otra fotografía." }
                        target.write(buffer, 0, count)
                    }
                }
            } ?: error("No se pudo abrir la fotografía.")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(input.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Selecciona una imagen válida." }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > 2000) sample *= 2
            val bitmap = BitmapFactory.decodeFile(input.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: error("No se pudo leer la imagen.")
            val orientation = runCatching { ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(1)
            val matrix = Matrix().apply {
                when (orientation) {
                    2 -> setScale(-1f, 1f)
                    3 -> setRotate(180f)
                    4 -> { setRotate(180f); postScale(-1f, 1f) }
                    5 -> { setRotate(90f); postScale(-1f, 1f) }
                    6 -> setRotate(90f)
                    7 -> { setRotate(-90f); postScale(-1f, 1f) }
                    8 -> setRotate(-90f)
                }
            }
            val corrected = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            output.outputStream().use { require(corrected.compress(Bitmap.CompressFormat.JPEG, 88, it)) { "No se pudo guardar la imagen." } }
            if (corrected !== bitmap) corrected.recycle()
            bitmap.recycle()
            return output.absolutePath
        } catch (e: Exception) { output.delete(); throw e }
        finally { input.delete() }
    }
    fun borrar(context: Context, path: String) {
        if (path.isBlank()) return
        val file = File(path)
        if (file.canonicalFile.parentFile == File(context.filesDir, "drafts").canonicalFile) file.delete()
    }
}
