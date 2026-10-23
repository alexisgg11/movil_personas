package com.example.personas.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.personas.domain.Borrador
import com.example.personas.domain.Direccion
import com.example.personas.domain.Persona
import org.json.JSONObject

class FormularioSavedState(private val handle: SavedStateHandle) {
    var capturaPendiente: String?
        get() = handle["captura"]
        set(value) { handle["captura"] = value }

    fun restaurar() = Pantalla(
        busqueda = handle["busqueda"] ?: "",
        modo = handle["modo"] ?: "",
        tab = (handle.get<Int>("tab") ?: 0).coerceIn(0, 2),
        persona = leer("persona") { o -> Persona(o.getString("dni"), o.getString("nombres"),
            o.getString("apellidos"), o.direccion(), o.getLong("profesionId"), o.getString("profesion"),
            o.getString("celular"), o.getString("correo"), o.getString("fotoUrl")) },
        borrador = leer("borrador") { o -> Borrador(o.optString("dni"), o.optString("nombres"),
            o.optString("apellidos"), o.direccion(), o.optLong("profesionId"), o.optString("profesion"),
            o.optString("celular"), o.optString("correo"), o.optString("fotoLocal"), o.optString("fotoRemota")) } ?: Borrador(),
        direccionAbierta = handle["direccionAbierta"] ?: false,
        direccionBorrador = leer("direccionBorrador", ::leerDireccion) ?: Direccion()
    )

    fun guardar(s: Pantalla) {
        handle["busqueda"] = s.busqueda
        handle["modo"] = s.modo
        handle["tab"] = s.tab
        handle["persona"] = s.persona?.let { p ->
            campos(p.dni, p.nombres, p.apellidos, p.direccion, p.profesionId, p.profesion, p.celular, p.correo)
                .put("fotoUrl", p.fotoUrl).toString()
        }
        val b = s.borrador
        handle["borrador"] = campos(b.dni, b.nombres, b.apellidos, b.direccion, b.profesionId, b.profesion, b.celular, b.correo)
            .put("fotoLocal", b.fotoLocal).put("fotoRemota", b.fotoRemota).toString()
        handle["direccionAbierta"] = s.direccionAbierta
        handle["direccionBorrador"] = s.direccionBorrador.json().toString()
    }

    private fun <T> leer(key: String, parse: (JSONObject) -> T): T? =
        runCatching { handle.get<String>(key)?.let { parse(JSONObject(it)) } }.getOrNull()

    private fun leerDireccion(o: JSONObject) = Direccion(o.optLong("id"), o.optString("detalle"),
        o.optString("distritoId"), o.optString("distrito"), o.optString("provinciaId"), o.optString("provincia"),
        o.optString("regionId"), o.optString("region"))

    private fun JSONObject.direccion() = optJSONObject("direccion")?.let(::leerDireccion)
        ?: Direccion(detalle = optString("direccion"))

    private fun Direccion.json() = JSONObject().put("id", id).put("detalle", detalle)
        .put("distritoId", distritoId).put("distrito", distrito).put("provinciaId", provinciaId)
        .put("provincia", provincia).put("regionId", regionId).put("region", region)

    private fun campos(dni: String, nombres: String, apellidos: String, direccion: Direccion,
        profesionId: Long, profesion: String, celular: String, correo: String) =
        JSONObject().put("dni", dni).put("nombres", nombres).put("apellidos", apellidos)
            .put("direccion", direccion.json()).put("profesionId", profesionId).put("profesion", profesion)
            .put("celular", celular).put("correo", correo)
}
