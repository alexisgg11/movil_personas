package com.example.personas.data

import com.example.personas.domain.*
import org.json.JSONObject

fun parseDireccion(o: JSONObject) = Direccion(o.optLong("id"), o.optString("detalle"),
    o.optString("distritoId"), o.optString("distrito"), o.optString("provinciaId"), o.optString("provincia"),
    o.optString("regionId"), o.optString("region"))
fun Direccion.jsonObject(): JSONObject = JSONObject().put("id", id).put("detalle", detalle)
    .put("distritoId", distritoId).put("distrito", distrito).put("provinciaId", provinciaId)
    .put("provincia", provincia).put("regionId", regionId).put("region", region)
private fun JSONObject.direccion() = optJSONObject("direccion")?.let(::parseDireccion)
    ?: Direccion(detalle = optString("direccion"))
fun parsePersona(o: JSONObject) = Persona(o.getString("dni"), o.getString("nombres"), o.getString("apellidos"),
    o.direccion(), o.getLong("profesionId"), o.getString("profesion"), o.getString("celular"), o.getString("correo"), o.getString("fotoUrl"))
fun Persona.json(): String = JSONObject().put("dni", dni).put("nombres", nombres).put("apellidos", apellidos)
    .put("direccion", direccion.jsonObject()).put("profesionId", profesionId).put("profesion", profesion).put("celular", celular).put("correo", correo).put("fotoUrl", fotoUrl).toString()
fun Borrador.json(): String = JSONObject().put("dni", dni).put("nombres", nombres).put("apellidos", apellidos)
    .put("direccion", direccion.jsonObject()).put("profesionId", profesionId).put("profesion", profesion).put("celular", celular).put("correo", correo)
    .put("fotoLocal", fotoLocal).put("fotoRemota", fotoRemota).toString()
fun parseBorrador(text: String): Borrador = JSONObject(text).let { o ->
    Borrador(o.optString("dni"), o.optString("nombres"), o.optString("apellidos"), o.direccion(),
        o.optLong("profesionId"), o.optString("profesion"), o.optString("celular"), o.optString("correo"), o.optString("fotoLocal"), o.optString("fotoRemota"))
}
