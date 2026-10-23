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

class ApiException(val status: Int, message: String, val campos: Map<String, String> = emptyMap()) : Exception(message)
class PersonasRepository(private val baseUrl: () -> String = { ApiConfig.baseUrl }) {
    suspend fun profesiones(): List<Profesion> = withContext(Dispatchers.IO) {
        val array = JSONArray(request("GET", "/api/profesiones"))
        (0 until array.length()).map { array.getJSONObject(it).let { o -> Profesion(o.getLong("id"), o.getString("nombre")) } }
    }
    suspend fun crearProfesion(nombre: String): Profesion = withContext(Dispatchers.IO) {
        val o = JSONObject(request("POST", "/api/profesiones", JSONObject().put("nombre", nombre).toString()))
        Profesion(o.getLong("id"), o.getString("nombre"))
    }
    suspend fun buscar(dni: String): Persona = withContext(Dispatchers.IO) { parsePersona(JSONObject(request("GET", "/api/personas/$dni"))) }
    suspend fun existe(dni: String): Boolean = withContext(Dispatchers.IO) { JSONObject(request("GET", "/api/personas/$dni/existe")).getBoolean("existe") }
    suspend fun eliminar(dni: String) = withContext(Dispatchers.IO) { request("DELETE", "/api/personas/$dni"); Unit }
    suspend fun guardar(d: Borrador, editar: Boolean): Persona = withContext(Dispatchers.IO) {
        val boundary = "Personas-${UUID.randomUUID()}"
        val connection = connection(if (editar) "PUT" else "POST", if (editar) "/api/personas/${d.dni}" else "/api/personas")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setChunkedStreamingMode(8192)
            connection.outputStream.buffered().use { out ->
                fun write(text: String) { out.write(text.toByteArray(Charsets.UTF_8)) }
                write("--$boundary\r\nContent-Disposition: form-data; name=\"datos\"\r\nContent-Type: application/json; charset=utf-8\r\n\r\n")
                write(JSONObject().put("dni", d.dni).put("nombres", d.nombres.trim()).put("apellidos", d.apellidos.trim())
                    .put("direccion", d.direccion.trim()).put("profesionId", d.profesionId).put("celular", d.celular).put("correo", d.correo.trim()).toString())
                if (d.fotoLocal.isNotBlank()) {
                    write("\r\n--$boundary\r\nContent-Disposition: form-data; name=\"foto\"; filename=\"foto.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n")
                    File(d.fotoLocal).inputStream().use { it.copyTo(out) }
                }
                write("\r\n--$boundary--\r\n")
            }
            parsePersona(JSONObject(response(connection)))
        } finally { connection.disconnect() }
    }
    fun fotoUrl(ruta: String): String = baseUrl() + ruta
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
fun parsePersona(o: JSONObject) = Persona(o.getString("dni"), o.getString("nombres"), o.getString("apellidos"),
    o.getString("direccion"), o.getLong("profesionId"), o.getString("profesion"), o.getString("celular"), o.getString("correo"), o.getString("fotoUrl"))
fun Persona.json(): String = JSONObject().put("dni", dni).put("nombres", nombres).put("apellidos", apellidos)
    .put("direccion", direccion).put("profesionId", profesionId).put("profesion", profesion).put("celular", celular).put("correo", correo).put("fotoUrl", fotoUrl).toString()
fun Borrador.json(): String = JSONObject().put("dni", dni).put("nombres", nombres).put("apellidos", apellidos)
    .put("direccion", direccion).put("profesionId", profesionId).put("profesion", profesion).put("celular", celular).put("correo", correo)
    .put("fotoLocal", fotoLocal).put("fotoRemota", fotoRemota).toString()
fun parseBorrador(text: String): Borrador = JSONObject(text).let { o ->
    Borrador(o.optString("dni"), o.optString("nombres"), o.optString("apellidos"), o.optString("direccion"),
        o.optLong("profesionId"), o.optString("profesion"), o.optString("celular"), o.optString("correo"), o.optString("fotoLocal"), o.optString("fotoRemota"))
}
