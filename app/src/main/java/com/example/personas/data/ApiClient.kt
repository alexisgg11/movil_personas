package com.example.personas.data

import com.example.personas.domain.ErrorRepositorio
import com.example.personas.domain.TipoErrorRepositorio
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiClient(private val baseUrl: () -> String = { ApiConfig.baseUrl }) {
    fun fotoUrl(ruta: String) = baseUrl() + ruta
    fun connection(method: String, path: String) = (URL(baseUrl() + path).openConnection() as HttpURLConnection).apply {
        requestMethod = method; connectTimeout = 10000; readTimeout = 20000; instanceFollowRedirects = false
        setRequestProperty("Accept", "application/json")
    }
    fun request(method: String, path: String, body: String? = null): String {
        val connection = connection(method, path)
        return try {
            if (body != null) {
                connection.doOutput = true; connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            response(connection)
        } finally { connection.disconnect() }
    }
    fun response(connection: HttpURLConnection): String {
        val code = connection.responseCode
        val text = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val error = runCatching { JSONObject(text) }.getOrNull()
            val campos = error?.optJSONObject("campos")
            val fields = campos?.keys()?.asSequence()?.associateWith { campos.optString(it) }.orEmpty()
            val tipo = when (code) {
                400, 413 -> TipoErrorRepositorio.VALIDACION
                404 -> TipoErrorRepositorio.NO_ENCONTRADO
                409 -> TipoErrorRepositorio.DUPLICADO
                else -> TipoErrorRepositorio.SERVIDOR
            }
            throw ErrorRepositorio(tipo, error?.optString("mensaje")?.takeIf { it.isNotBlank() } ?: "El servidor devolvió un error ($code).", fields)
        }
        return text
    }
}
