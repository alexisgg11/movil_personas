package com.example.personas.data

import com.example.personas.domain.*
import org.json.JSONObject

fun parseDireccion(o: JSONObject) = Direccion(o.optLong("id"), o.optString("detalle"),
    o.optString("distritoId"), o.optString("distrito"), o.optString("provinciaId"), o.optString("provincia"),
    o.optString("regionId"), o.optString("region"))
private fun JSONObject.direccion() = optJSONObject("direccion")?.let(::parseDireccion)
    ?: Direccion(detalle = optString("direccion"))
fun parsePersona(o: JSONObject) = Persona(o.getString("dni"), o.getString("nombres"), o.getString("apellidos"),
    o.direccion(), o.getLong("profesionId"), o.getString("profesion"), o.getString("celular"), o.getString("correo"), o.getString("fotoUrl"))
