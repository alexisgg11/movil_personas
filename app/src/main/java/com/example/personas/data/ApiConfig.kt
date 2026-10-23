package com.example.personas.data

import android.os.Build
import com.example.personas.BuildConfig
import com.example.personas.domain.ServidorUrl

object ApiConfig {
    val baseUrl: String by lazy {
        BuildConfig.API_BASE_URL.trim().trimEnd('/').ifBlank {
            if (BuildConfig.DEBUG) {
                val emulador = Build.FINGERPRINT.contains("generic") || Build.FINGERPRINT.contains("emulator") ||
                    Build.MODEL.contains("Emulator") || Build.PRODUCT.contains("sdk")
                if (emulador) "http://10.0.2.2:8081" else "http://127.0.0.1:8081"
            } else ""
        }
    }

    fun error(): String? = if (ServidorUrl.validar(baseUrl, BuildConfig.DEBUG) == null) null
        else "El servidor de esta versión no está disponible. Contacta al responsable de la aplicación."
}
