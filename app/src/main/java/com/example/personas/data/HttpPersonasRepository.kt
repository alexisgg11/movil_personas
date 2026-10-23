package com.example.personas.data

import com.example.personas.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class HttpPersonasRepository(private val baseUrl: () -> String = { ApiConfig.baseUrl }) : PersonasRepository {
    override fun errorConfiguracion(): String? = ApiConfig.error()
    override suspend fun profesiones(): List<Profesion> = withContext(Dispatchers.IO) {
        val array = JSONArray(request("GET", "/api/profesiones"))
        (0 until array.length()).map { array.getJSONObject(it).let { o -> Profesion(o.getLong("id"), o.getString("nombre")) } }
    }
    override suspend fun crearProfesion(nombre: String): Profesion = withContext(Dispatchers.IO) {
        val o = JSONObject(request("POST", "/api/profesiones", JSONObject().put("nombre", nombre).toString()))
        Profesion(o.getLong("id"), o.getString("nombre"))
    }
    override suspend fun buscar(dni: String): Persona = withContext(Dispatchers.IO) { parsePersona(JSONObject(request("GET", "/api/personas/$dni"))) }
    override suspend fun existe(dni: String): Boolean = withContext(Dispatchers.IO) { JSONObject(request("GET", "/api/personas/$dni/existe")).getBoolean("existe") }
    override suspend fun eliminar(dni: String) = withContext(Dispatchers.IO) { request("DELETE", "/api/personas/$dni"); Unit }
    override suspend fun guardar(datos: Borrador, editar: Boolean): Persona = withContext(Dispatchers.IO) {
        val boundary = "Personas-${UUID.randomUUID()}"
        val connection = connection(if (editar) "PUT" else "POST", if (editar) "/api/personas/${datos.dni}" else "/api/personas")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setChunkedStreamingMode(8192)
            connection.outputStream.buffered().use { out ->
                fun write(text: String) { out.write(text.toByteArray(Charsets.UTF_8)) }
                write("--$boundary\r\nContent-Disposition: form-data; name=\"datos\"\r\nContent-Type: application/json; charset=utf-8\r\n\r\n")
                write(JSONObject().put("dni", datos.dni).put("nombres", datos.nombres.trim()).put("apellidos", datos.apellidos.trim())
                    .put("direccion", datos.direccion.trim()).put("profesionId", datos.profesionId).put("celular", datos.celular).put("correo", datos.correo.trim()).toString())
                if (datos.fotoLocal.isNotBlank()) {
                    write("\r\n--$boundary\r\nContent-Disposition: form-data; name=\"foto\"; filename=\"foto.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n")
                    File(datos.fotoLocal).inputStream().use { it.copyTo(out) }
                }
                write("\r\n--$boundary--\r\n")
            }
            parsePersona(JSONObject(response(connection)))
        } finally { connection.disconnect() }
    }
    override fun fotoUrl(ruta: String): String = baseUrl() + ruta
    private fun connection(method: String, path: String) = (URL(baseUrl() + path).openConnection() as HttpURLConnection).apply {
        requestMethod = method; connectTimeout = 10000; readTimeout = 20000; instanceFollowRedirects = false
        setRequestProperty("Accept", "application/json")
    }
    private fun request(method: String, path: String, body: String? = null): String {
        val connection = connection(method, path)
        return try {
            if (body != null) {
                connection.doOutput = true; connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            response(connection)
        } finally { connection.disconnect() }
    }
    private fun response(connection: HttpURLConnection): String {
        val code = connection.responseCode
        val text = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val error = runCatching { JSONObject(text) }.getOrNull()
            val campos = error?.optJSONObject("campos")
            val fields = campos?.keys()?.asSequence()?.associateWith { campos.optString(it) }.orEmpty()
            throw ApiException(code, error?.optString("mensaje")?.takeIf { it.isNotBlank() } ?: "El servidor devolvió un error ($code).", fields)
        }
        return text
    }
}
