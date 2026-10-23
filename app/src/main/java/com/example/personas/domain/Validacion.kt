package com.example.personas.domain

object Validacion {
    fun seccion(campo: String) = when (campo.substringBefore(".")) {
        "dni", "nombres", "apellidos", "profesionId" -> 0
        "direccion", "celular", "correo" -> 1
        else -> 2
    }
    fun paso(d: Borrador, indice: Int) = formulario(d).filterKeys { seccion(it) == indice }
    fun dni(valor: String) = valor.matches(Regex("[0-9]{8}"))
    fun celular(valor: String) = valor.matches(Regex("9[0-9]{8}"))
    fun correo(valor: String) = valor.trim().let {
        it.length <= 180 && it.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
    }
    fun formulario(d: Borrador): Map<String, String> = buildMap {
        if (!dni(d.dni)) put("dni", "Ingresa los 8 dígitos del DNI.")
        if (d.nombres.isBlank() || d.nombres.trim().length > 100) put("nombres", "Ingresa los nombres (máximo 100 caracteres).")
        if (d.apellidos.isBlank() || d.apellidos.trim().length > 100) put("apellidos", "Ingresa los apellidos (máximo 100 caracteres).")
        if (!d.direccion.completa) put("direccion", "Registra la dirección y selecciona su región, provincia y distrito.")
        if (d.profesionId <= 0) put("profesionId", "Selecciona una profesión.")
        if (!celular(d.celular)) put("celular", "Ingresa 9 dígitos; el primero debe ser 9.")
        if (!correo(d.correo)) put("correo", "Ingresa un correo válido.")
        if (d.fotoLocal.isBlank() && d.fotoRemota.isBlank()) put("foto", "Toma o adjunta una fotografía.")
    }
}
