package com.example.personas.viewmodel

import com.example.personas.domain.*

data class Pantalla(
    val busqueda: String = "", val persona: Persona? = null, val modo: String = "",
    val borrador: Borrador = Borrador(), val profesiones: List<Profesion> = emptyList(),
    val tab: Int = 0, val ocupado: Boolean = false, val cargandoCatalogo: Boolean = false,
    val error: String? = null, val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null, val confirmacion: String = "",
    val nuevaProfesion: Boolean = false, val nombreProfesion: String = "", val errorProfesion: String? = null,
    val redPermitida: Boolean = false,
    val aviso: Aviso? = null, val sinResultado: Boolean = false,
    val direccionAbierta: Boolean = false, val direccionBorrador: Direccion = Direccion(),
    val regiones: List<Region> = emptyList(), val provincias: List<Provincia> = emptyList(), val distritos: List<Distrito> = emptyList(),
    val cargandoUbigeo: Boolean = false, val errorUbigeo: String? = null, val erroresDireccion: Map<String, String> = emptyMap()
)
