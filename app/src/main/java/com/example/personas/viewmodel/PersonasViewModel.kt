package com.example.personas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.personas.data.*
import com.example.personas.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import org.json.JSONObject
import java.io.IOException

class PersonasViewModel(
    private val repository: PersonasRepository,
    private val fotos: FotosRepository,
    private val saved: SavedStateHandle,
    private val ubigeo: UbigeoRepository
) : ViewModel() {
    private var catalogVersion = 0
    private var ubicacionVersion = 0
    private var ubicacionJob: Job? = null
    private val mutable = MutableStateFlow(Pantalla(
        busqueda = saved["busqueda"] ?: "",
        persona = runCatching { saved.get<String>("persona")?.let { parsePersona(JSONObject(it)) } }.getOrNull(),
        modo = saved["modo"] ?: "",
        borrador = runCatching { saved.get<String>("borrador")?.let(::parseBorrador) }.getOrNull() ?: Borrador(),
        tab = saved["tab"] ?: 0,
        direccionAbierta = saved["direccionAbierta"] ?: false,
        direccionBorrador = runCatching { saved.get<String>("direccionBorrador")?.let { parseDireccion(JSONObject(it)) } }.getOrNull() ?: Direccion()
    ))
    val state = mutable.asStateFlow()
    private fun update(block: (Pantalla) -> Pantalla) {
        mutable.value = block(mutable.value)
        val s = mutable.value
        saved["busqueda"] = s.busqueda; saved["modo"] = s.modo; saved["borrador"] = s.borrador.json(); saved["tab"] = s.tab
        saved["persona"] = s.persona?.json()
        saved["direccionAbierta"] = s.direccionAbierta; saved["direccionBorrador"] = s.direccionBorrador.jsonObject().toString()
    }
    private fun error(e: Exception): String = when (e) {
        is ApiException -> e.message ?: "Revisa los datos."
        is IOException -> "No se pudo conectar. Comprueba tu conexión y vuelve a intentarlo."
        is SecurityException -> "Permite el acceso a la red local para conectar con el servidor."
        else -> e.message ?: "No se pudo completar la operación. Vuelve a intentarlo."
    }
    private fun operation(action: suspend () -> Unit) {
        if (mutable.value.ocupado) return
        update { it.copy(ocupado = true, error = null, confirmacion = "") }
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val campos = if (e is ApiException && e.status == 409 && mutable.value.modo == "nuevo") mapOf("dni" to "Este DNI ya está registrado.") else if (e is ApiException) e.campos else emptyMap()
                update { it.copy(error = error(e), aviso = Aviso("No se pudo completar", error(e)),
                    errores = campos.ifEmpty { it.errores },
                    tab = if (it.modo.isNotEmpty() && campos.isNotEmpty()) Validacion.seccion(campos.keys.first()) else it.tab) }
            }
            finally { update { it.copy(ocupado = false) } }
        }
    }
    fun redPermitida(permitida: Boolean) {
        val changed = mutable.value.redPermitida != permitida
        update { it.copy(redPermitida = permitida) }
        if (permitida && changed) { cargarProfesiones(); if (mutable.value.direccionAbierta) cargarUbicacion() }
    }
    private fun puedeConectar(mostrarAviso: Boolean = true): Boolean {
        if (!mutable.value.redPermitida) { update { it.copy(error = "Permite el acceso a la red local para continuar.", aviso = if (mostrarAviso) Aviso("Conexión", "Permite el acceso a la red local para continuar.") else it.aviso) }; return false }
        val validation = repository.errorConfiguracion()
        if (validation != null) { update { it.copy(error = validation, aviso = if (mostrarAviso) Aviso("Conexión", validation) else it.aviso) }; return false }
        return true
    }
    fun cargarProfesiones() {
        if (mutable.value.cargandoCatalogo || !puedeConectar(mostrarAviso = false)) return
        val version = ++catalogVersion
        update { it.copy(cargandoCatalogo = true) }
        viewModelScope.launch {
            try { val catalogo = repository.profesiones(); if (version == catalogVersion) update { it.copy(profesiones = catalogo, error = null) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (version == catalogVersion) update { it.copy(error = error(e)) } }
            finally { if (version == catalogVersion) update { it.copy(cargandoCatalogo = false) } }
        }
    }
    fun busqueda(value: String) { update { it.copy(busqueda = value.filter { c -> c in '0'..'9' }.take(8), persona = null, error = null, sinResultado = false) } }
    fun buscar() {
        val dni = mutable.value.busqueda
        if (!Validacion.dni(dni)) { update { it.copy(error = "Ingresa los 8 dígitos del DNI.", aviso = Aviso("Revisa el DNI", "Ingresa los 8 dígitos del DNI."), persona = null) }; return }
        if (!puedeConectar()) return
        operation {
            try { val persona = repository.buscar(dni); update { it.copy(persona = persona, sinResultado = false) } }
            catch (e: ApiException) {
                if (e.status != 404) throw e
                update { it.copy(persona = null, sinResultado = true) }
            }
        }
    }
    fun nuevo() {
        if (mutable.value.ocupado) return
        limpiarFoto()
        update { it.copy(modo = "nuevo", borrador = Borrador(), tab = 0, errores = emptyMap(), error = null, confirmacion = "", aviso = null) }
        if (mutable.value.profesiones.isEmpty()) cargarProfesiones()
    }
    fun ver() { update { it.copy(modo = "ver", error = null) } }
    fun editar() {
        val persona = mutable.value.persona ?: return
        update { it.copy(modo = "editar", borrador = Borrador.de(persona), tab = 0, errores = emptyMap(), error = null) }
        if (mutable.value.profesiones.isEmpty()) cargarProfesiones()
    }
    fun cambiar(block: (Borrador) -> Borrador) { update { it.copy(borrador = block(it.borrador), errores = emptyMap(), error = null) } }
    fun tab(value: Int) { if (!mutable.value.ocupado) update { it.copy(tab = value.coerceIn(0, 2), error = null) } }
    fun atras() { if (mutable.value.tab == 0) cerrar() else tab(mutable.value.tab - 1) }
    fun siguiente() {
        val s = mutable.value
        if (s.ocupado) return
        val errores = Validacion.paso(s.borrador, s.tab)
        if (errores.isNotEmpty()) { mostrarValidacion(errores); return }
        if (s.tab == 0 && s.modo == "nuevo") {
            if (!puedeConectar()) { informar(mutable.value.error ?: "No se pudo conectar."); return }
            operation {
                if (repository.existe(s.borrador.dni)) {
                    mostrarValidacion(mapOf("dni" to "Este DNI ya está registrado, incluso si fue eliminado."))
                } else tabInterna(1)
            }
        } else tab(s.tab + 1)
    }
    private fun tabInterna(value: Int) { update { it.copy(tab = value.coerceIn(0, 2), error = null) } }
    private fun mostrarValidacion(errores: Map<String, String>) {
        update { it.copy(errores = errores, tab = errores.keys.minOf(Validacion::seccion), error = null,
            aviso = Aviso("Revisa los datos", errores.values.distinct().joinToString("\n"))) }
    }
    fun cerrarAviso() { update { it.copy(aviso = null) } }
    fun cerrar() {
        if (mutable.value.ocupado) return
        val s = mutable.value
        val initial = if (s.modo == "editar") s.persona?.let(Borrador::de) ?: Borrador() else Borrador()
        if (s.modo != "ver" && s.borrador != initial) confirmar("descartar") else descartar()
    }
    fun confirmar(value: String) { update { it.copy(confirmacion = value) } }
    fun cancelarConfirmacion() { update { it.copy(confirmacion = "") } }
    fun solicitarGuardar() {
        val errors = Validacion.formulario(mutable.value.borrador)
        if (errors.isNotEmpty()) {
            mostrarValidacion(errors)
        } else if (puedeConectar()) confirmar("guardar")
    }
    fun guardar() {
        if (!puedeConectar()) return
        val s = mutable.value
        if (Validacion.formulario(s.borrador).isNotEmpty()) { solicitarGuardar(); return }
        operation {
            if (s.modo == "nuevo" && repository.existe(s.borrador.dni)) throw ApiException(409, "Este DNI ya está registrado, incluso si fue eliminado.", mapOf("dni" to "El DNI ya está registrado."))
            val persona = repository.guardar(s.borrador, s.modo == "editar")
            limpiarFoto()
            update { it.copy(persona = persona, busqueda = persona.dni, modo = "", borrador = Borrador(), errores = emptyMap(), sinResultado = false, mensaje = "Persona guardada correctamente.") }
        }
    }
    fun eliminar() {
        if (!puedeConectar()) return
        val persona = mutable.value.persona ?: return
        operation {
            repository.eliminar(persona.dni)
            update { it.copy(persona = null, modo = "", mensaje = "Persona eliminada. El DNI permanece reservado.") }
        }
    }
    fun descartar() {
        limpiarFoto()
        update { it.copy(modo = "", borrador = Borrador(), confirmacion = "", errores = emptyMap(), error = null, aviso = null) }
    }
    private fun limpiarFoto() { fotos.borrar(mutable.value.borrador.fotoLocal) }
    fun abrirProfesion() { update { it.copy(nuevaProfesion = true, nombreProfesion = "", errorProfesion = null) } }
    fun cerrarProfesion() { if (!mutable.value.ocupado) update { it.copy(nuevaProfesion = false) } }
    fun nombreProfesion(value: String) { update { it.copy(nombreProfesion = value.take(80), errorProfesion = null) } }
    fun crearProfesion() {
        val nombre = mutable.value.nombreProfesion.trim()
        if (nombre.isBlank()) { update { it.copy(errorProfesion = "Ingresa el nombre de la profesión.") }; return }
        if (!puedeConectar() || mutable.value.ocupado) return
        update { it.copy(ocupado = true, errorProfesion = null) }
        viewModelScope.launch {
            try {
                val profesion = repository.crearProfesion(nombre)
                update { it.copy(nuevaProfesion = false, profesiones = (it.profesiones + profesion).sortedBy { p -> p.nombre }, borrador = it.borrador.copy(profesionId = profesion.id, profesion = profesion.nombre), errores = it.errores - "profesionId") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { update { it.copy(errorProfesion = error(e)) } }
            finally { update { it.copy(ocupado = false) } }
        }
    }
    fun abrirDireccion() {
        if (mutable.value.ocupado) return
        update { it.copy(direccionAbierta = true, direccionBorrador = it.borrador.direccion, erroresDireccion = emptyMap(), errorUbigeo = null) }
        cargarUbicacion()
    }
    fun cerrarDireccion() {
        ubicacionVersion++; ubicacionJob?.cancel()
        update { it.copy(direccionAbierta = false, cargandoUbigeo = false, errorUbigeo = null, erroresDireccion = emptyMap()) }
    }
    fun detalleDireccion(value: String) { update { it.copy(direccionBorrador = it.direccionBorrador.copy(detalle = value.take(200)), erroresDireccion = it.erroresDireccion - "detalle") } }
    fun regionDireccion(id: String) {
        val region = mutable.value.regiones.find { it.id == id } ?: return
        if (mutable.value.direccionBorrador.regionId == id) return
        update { it.copy(direccionBorrador = Direccion(id = it.direccionBorrador.id, detalle = it.direccionBorrador.detalle, regionId = region.id, region = region.nombre), provincias = emptyList(), distritos = emptyList(), erroresDireccion = emptyMap()) }
        cargarUbicacion()
    }
    fun provinciaDireccion(id: String) {
        val provincia = mutable.value.provincias.find { it.id == id && it.regionId == mutable.value.direccionBorrador.regionId } ?: return
        if (mutable.value.direccionBorrador.provinciaId == id) return
        update { it.copy(direccionBorrador = it.direccionBorrador.copy(provinciaId = provincia.id, provincia = provincia.nombre, distritoId = "", distrito = ""), distritos = emptyList(), erroresDireccion = emptyMap()) }
        cargarUbicacion()
    }
    fun distritoDireccion(id: String) {
        val distrito = mutable.value.distritos.find { it.id == id && it.provinciaId == mutable.value.direccionBorrador.provinciaId } ?: return
        update { it.copy(direccionBorrador = it.direccionBorrador.copy(distritoId = distrito.id, distrito = distrito.nombre), erroresDireccion = it.erroresDireccion - "distrito") }
    }
    fun aceptarDireccion() {
        val d = mutable.value.direccionBorrador
        val errores = buildMap {
            if (d.regionId.isBlank()) put("region", "Selecciona una región.")
            if (d.provinciaId.isBlank()) put("provincia", "Selecciona una provincia.")
            if (d.distritoId.isBlank()) put("distrito", "Selecciona un distrito.")
            if (d.detalle.isBlank()) put("detalle", "Ingresa la vía y el número, manzana o lote.")
        }
        if (errores.isNotEmpty() || !d.completa) { update { it.copy(erroresDireccion = errores) }; return }
        update { it.copy(borrador = it.borrador.copy(direccion = d.copy(detalle = d.detalle.trim())), direccionAbierta = false, errores = it.errores - "direccion") }
    }
    fun cargarUbicacion() {
        if (!mutable.value.direccionAbierta) return
        ubicacionJob?.cancel()
        val version = ++ubicacionVersion
        update { it.copy(cargandoUbigeo = true, errorUbigeo = null) }
        ubicacionJob = viewModelScope.launch {
            try {
                if (!puedeConectar(false)) throw IOException("Sin conexión")
                val regiones = ubigeo.regiones()
                val seleccion = mutable.value.direccionBorrador
                val provincias = if (seleccion.regionId.isNotBlank()) ubigeo.provincias(seleccion.regionId) else emptyList()
                val distritos = if (seleccion.provinciaId.isNotBlank()) ubigeo.distritos(seleccion.provinciaId) else emptyList()
                if (version == ubicacionVersion) update { it.copy(regiones = regiones, provincias = provincias, distritos = distritos) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (version == ubicacionVersion) update { it.copy(errorUbigeo = error(e)) } }
            finally { if (version == ubicacionVersion) update { it.copy(cargandoUbigeo = false) } }
        }
    }
    fun prepararCamara(): String? = try {
        val captura = fotos.prepararCaptura()
        saved["captura"] = captura.ruta
        captura.uri
    } catch (e: Exception) { informar(error(e)); null }
    fun camaraTerminada(success: Boolean) {
        val path = saved.get<String>("captura") ?: return
        saved["captura"] = null
        if (success) importarFoto(fotos.origenArchivo(path), path) else fotos.borrar(path)
    }
    fun importarFoto(uri: String, captura: String = "") {
        operation {
            try {
                val path = fotos.importar(uri)
                limpiarFoto()
                update { it.copy(borrador = it.borrador.copy(fotoLocal = path), errores = it.errores - "foto") }
            } finally { fotos.borrar(captura) }
        }
    }
    fun informar(texto: String) { update { it.copy(error = texto, aviso = Aviso("Aviso", texto)) } }
    fun mensajeLeido() { update { it.copy(mensaje = null) } }
    fun fotoUrl(path: String): String = if (path.isBlank()) "" else repository.fotoUrl(path)
}
