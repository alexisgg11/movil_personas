package com.example.personas.domain

data class Direccion(
    val id: Long = 0, val detalle: String = "",
    val distritoId: String = "", val distrito: String = "",
    val provinciaId: String = "", val provincia: String = "",
    val regionId: String = "", val region: String = ""
) {
    val texto get() = listOf(detalle.trim(), distrito).filter { it.isNotBlank() }.joinToString(", ")
    val completa get() = detalle.isNotBlank() && detalle.trim().length <= 200 &&
        regionId.matches(Regex("[0-9]{2}")) && provinciaId.matches(Regex("[0-9]{4}")) &&
        distritoId.matches(Regex("[0-9]{6}")) && provinciaId.startsWith(regionId) &&
        distritoId.startsWith(provinciaId) && distrito.isNotBlank()
}
