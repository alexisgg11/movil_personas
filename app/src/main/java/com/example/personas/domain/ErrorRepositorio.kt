package com.example.personas.domain

class ErrorRepositorio(
    val tipo: TipoErrorRepositorio,
    message: String,
    val campos: Map<String, String> = emptyMap()
) : Exception(message)
