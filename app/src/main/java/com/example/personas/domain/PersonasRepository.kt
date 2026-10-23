package com.example.personas.domain

interface PersonasRepository {
    fun errorConfiguracion(): String?
    suspend fun profesiones(): List<Profesion>
    suspend fun crearProfesion(nombre: String): Profesion
    suspend fun buscar(dni: String): Persona
    suspend fun existe(dni: String): Boolean
    suspend fun eliminar(dni: String)
    suspend fun guardar(datos: Borrador, editar: Boolean): Persona
    fun fotoUrl(ruta: String): String
}
