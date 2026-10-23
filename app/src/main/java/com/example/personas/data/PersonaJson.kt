package com.example.personas.data

import com.example.personas.domain.*
import org.json.JSONObject

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
