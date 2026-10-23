package com.example.personas.data

class ApiException(val status: Int, message: String, val campos: Map<String, String> = emptyMap()) : Exception(message)
