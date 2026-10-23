package com.example.personas.domain

data class Borrador(
    val dni: String = "", val nombres: String = "", val apellidos: String = "",
    val direccion: Direccion = Direccion(), val profesionId: Long = 0, val profesion: String = "",
    val celular: String = "", val correo: String = "", val fotoLocal: String = "",
    val fotoRemota: String = ""
) {
    val nombreCompleto get() = listOf(nombres.trim(), apellidos.trim()).filter { it.isNotBlank() }.joinToString(" ")
    val tieneFoto get() = fotoLocal.isNotBlank() || fotoRemota.isNotBlank()
    val iniciales get() = listOf(nombres, apellidos).mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }.joinToString("")
    companion object {
        fun de(p: Persona) = Borrador(p.dni, p.nombres, p.apellidos, p.direccion,
            p.profesionId, p.profesion, p.celular, p.correo, fotoRemota = p.fotoUrl)
    }
}
