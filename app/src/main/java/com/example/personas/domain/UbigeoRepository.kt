package com.example.personas.domain

interface UbigeoRepository {
    suspend fun regiones(): List<Region>
    suspend fun provincias(regionId: String): List<Provincia>
    suspend fun distritos(provinciaId: String): List<Distrito>
}
