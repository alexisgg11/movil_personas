package com.example.personas.data

import com.example.personas.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class HttpPersonasRepository(private val api: ApiClient = ApiClient()) : PersonasRepository {
    override fun errorConfiguracion(): String? = ApiConfig.error()
    override suspend fun profesiones(): List<Profesion> = withContext(Dispatchers.IO) {
        val array = JSONArray(api.request("GET", "/api/profesiones"))
        (0 until array.length()).map { array.getJSONObject(it).let { o -> Profesion(o.getLong("id"), o.getString("nombre")) } }
    }
    override suspend fun crearProfesion(nombre: String): Profesion = withContext(Dispatchers.IO) {
        val o = JSONObject(api.request("POST", "/api/profesiones", JSONObject().put("nombre", nombre).toString()))
        Profesion(o.getLong("id"), o.getString("nombre"))
    }
    override suspend fun buscar(dni: String): Persona = withContext(Dispatchers.IO) { parsePersona(JSONObject(api.request("GET", "/api/personas/$dni"))) }
    override suspend fun existe(dni: String): Boolean = withContext(Dispatchers.IO) { JSONObject(api.request("GET", "/api/personas/$dni/existe")).getBoolean("existe") }
    override suspend fun eliminar(dni: String) = withContext(Dispatchers.IO) { api.request("DELETE", "/api/personas/$dni"); Unit }
    override suspend fun guardar(datos: Borrador, editar: Boolean): Persona = withContext(Dispatchers.IO) {
        val boundary = "Personas-${UUID.randomUUID()}"
        val connection = api.connection(if (editar) "PUT" else "POST", if (editar) "/api/personas/${datos.dni}" else "/api/personas")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setChunkedStreamingMode(8192)
            connection.outputStream.buffered().use { out ->
                fun write(text: String) { out.write(text.toByteArray(Charsets.UTF_8)) }
                write("--$boundary\r\nContent-Disposition: form-data; name=\"datos\"\r\nContent-Type: application/json; charset=utf-8\r\n\r\n")
                write(JSONObject().put("dni", datos.dni).put("nombres", datos.nombres.trim()).put("apellidos", datos.apellidos.trim())
                    .put("direccion", JSONObject().put("detalle", datos.direccion.detalle.trim()).put("distritoId", datos.direccion.distritoId)).put("profesionId", datos.profesionId).put("celular", datos.celular).put("correo", datos.correo.trim()).toString())
                if (datos.fotoLocal.isNotBlank()) {
                    write("\r\n--$boundary\r\nContent-Disposition: form-data; name=\"foto\"; filename=\"foto.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n")
                    File(datos.fotoLocal).inputStream().use { it.copyTo(out) }
                }
                write("\r\n--$boundary--\r\n")
            }
            parsePersona(JSONObject(api.response(connection)))
        } finally { connection.disconnect() }
    }
    override fun fotoUrl(ruta: String): String = api.fotoUrl(ruta)
}
