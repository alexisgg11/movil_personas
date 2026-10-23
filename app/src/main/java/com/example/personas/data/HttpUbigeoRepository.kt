package com.example.personas.data

import com.example.personas.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class HttpUbigeoRepository(private val api: ApiClient = ApiClient()) : UbigeoRepository {
    override suspend fun regiones(): List<Region> = withContext(Dispatchers.IO) {
        val datos = JSONArray(api.request("GET", "/api/ubigeo/regiones"))
        (0 until datos.length()).map { datos.getJSONObject(it).let { o -> Region(o.getString("id"), o.getString("nombre")) } }
    }
    override suspend fun provincias(regionId: String): List<Provincia> = withContext(Dispatchers.IO) {
        val datos = JSONArray(api.request("GET", "/api/ubigeo/regiones/$regionId/provincias"))
        (0 until datos.length()).map { datos.getJSONObject(it).let { o -> Provincia(o.getString("id"), o.getString("nombre"), regionId) } }
    }
    override suspend fun distritos(provinciaId: String): List<Distrito> = withContext(Dispatchers.IO) {
        val datos = JSONArray(api.request("GET", "/api/ubigeo/provincias/$provinciaId/distritos"))
        (0 until datos.length()).map { datos.getJSONObject(it).let { o -> Distrito(o.getString("id"), o.getString("nombre"), provinciaId) } }
    }
}
