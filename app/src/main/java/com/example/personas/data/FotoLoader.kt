package com.example.personas.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object FotoLoader {
    private val imagenes = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    suspend fun cargar(origen: String): Bitmap? = withContext(Dispatchers.IO) {
        if (origen.isBlank()) return@withContext null
        imagenes.get(origen)?.let { return@withContext it }
        runCatching {
            val bytes = if (origen.startsWith("http")) {
                val conexion = URL(origen).openConnection() as HttpURLConnection
                try {
                    conexion.connectTimeout = 8000; conexion.readTimeout = 10000; conexion.instanceFollowRedirects = false
                    conexion.inputStream.use { it.readBytes() }
                } finally { conexion.disconnect() }
            } else File(origen).readBytes()
            val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)
            opciones.inSampleSize = 1
            while (maxOf(opciones.outWidth, opciones.outHeight) / opciones.inSampleSize > 800) opciones.inSampleSize *= 2
            opciones.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)?.also { imagenes.put(origen, it) }
        }.getOrNull()
    }
}
