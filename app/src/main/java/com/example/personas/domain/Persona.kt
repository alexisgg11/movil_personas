package com.example.personas.domain

data class Persona(
    val dni: String, val nombres: String, val apellidos: String, val direccion: Direccion,
    val profesionId: Long, val profesion: String, val celular: String, val correo: String,
    val fotoUrl: String
)
