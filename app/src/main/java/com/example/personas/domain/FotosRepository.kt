package com.example.personas.domain

interface FotosRepository {
    fun prepararCaptura(): CapturaFoto
    fun origenArchivo(ruta: String): String
    suspend fun importar(uri: String): String
    fun borrar(ruta: String)
}
