package com.example.personas

import androidx.lifecycle.SavedStateHandle
import com.example.personas.viewmodel.FormularioSavedState
import com.example.personas.domain.*
import com.example.personas.viewmodel.PersonasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PersonasViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val valido = Borrador("01234567", "Alexis", "Sandoval", Direccion(detalle = "Av. Sánchez Cerro 26", distritoId = "200101", distrito = "Piura", provinciaId = "2001", provincia = "Piura", regionId = "20", region = "Piura"), 1,
        "Docencia", "987654321", "alexis@example.com", fotoLocal = "/borrador.jpg")
    private val personas = PersonasFalsas()
    private val fotos = FotosFalsas()
    private val saved = SavedStateHandle()
    private val ubigeos = UbigeosFalsos()
    private lateinit var vm: PersonasViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        vm = PersonasViewModel(personas, fotos, FormularioSavedState(saved), ubigeos)
        vm.redPermitida(true)
    }
    @After fun limpiar() { Dispatchers.resetMain() }

    @Test fun altaBusquedaEdicionYEliminacionUsanElRepositorioInyectado() = runTest {
        vm.nuevo(); vm.cambiar { valido }; vm.guardar(); advanceUntilIdle()
        assertEquals("01234567", vm.state.value.persona?.dni)
        assertTrue("/borrador.jpg" in fotos.borrados)
        vm.busqueda("01234567"); vm.buscar(); advanceUntilIdle()
        vm.editar(); vm.cambiar { it.copy(celular = "999888777") }; vm.guardar(); advanceUntilIdle()
        assertTrue(personas.ultimaEdicion)
        assertEquals("999888777", vm.state.value.persona?.celular)
        vm.eliminar(); advanceUntilIdle()
        assertNotNull(personas.persona)
        assertFalse(personas.activa)
        vm.buscar(); advanceUntilIdle()
        assertTrue(vm.state.value.sinResultado)
        assertNull(vm.state.value.persona)
    }

    @Test fun dniReservadoImpideAvanzarSinEnviarUnAlta() = runTest {
        personas.persona = persona(valido); personas.activa = false
        vm.nuevo(); vm.cambiar { valido }; vm.siguiente(); advanceUntilIdle()
        assertEquals(0, vm.state.value.tab)
        assertTrue(vm.state.value.errores.getValue("dni").contains("registrado"))
        assertEquals(0, personas.guardados)
    }

    @Test fun celularQueNoEmpiezaConNueveNoLlegaAlRepositorio() = runTest {
        vm.nuevo(); vm.cambiar { valido.copy(celular = "887654321") }; vm.solicitarGuardar(); advanceUntilIdle()
        assertEquals(1, vm.state.value.tab)
        assertTrue("celular" in vm.state.value.errores)
        assertEquals(0, personas.guardados)
    }

    @Test fun capturaPendienteSeRecuperaSinApplicationNiContext() = runTest {
        vm.nuevo(); vm.cambiar { valido.copy(fotoLocal = "") }
        assertEquals("content://captura", vm.prepararCamara())
        val restaurado = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
        val otra = PersonasViewModel(personas, fotos, FormularioSavedState(restaurado), UbigeosFalsos())
        otra.camaraTerminada(true); advanceUntilIdle()
        assertEquals("Alexis", otra.state.value.borrador.nombres)
        assertEquals("/importada.jpg", otra.state.value.borrador.fotoLocal)
        assertEquals("file:///captura.jpg", fotos.ultimoOrigen)
        assertTrue("/captura.jpg" in fotos.borrados)
        assertNull(restaurado.get<String>("captura"))
    }

    @Test fun fichaConsultadaSeRestauraConDireccionYFotografia() = runTest {
        personas.persona = persona(valido)
        vm.busqueda(valido.dni); vm.buscar(); advanceUntilIdle(); vm.ver()
        val restaurado = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
        val otra = PersonasViewModel(personas, fotos, FormularioSavedState(restaurado), UbigeosFalsos())
        assertEquals(personas.persona, otra.state.value.persona)
        assertEquals(valido.dni, otra.state.value.busqueda)
        assertEquals("ver", otra.state.value.modo)
    }

    @Test fun falloDeGuardadoConservaElBorradorYPermiteReintentar() = runTest {
        personas.fallarGuardado = true
        vm.nuevo(); vm.cambiar { valido }; vm.guardar(); advanceUntilIdle()
        assertEquals(valido, vm.state.value.borrador)
        assertFalse("/borrador.jpg" in fotos.borrados)
        assertNotNull(vm.state.value.aviso)
        personas.fallarGuardado = false; vm.cerrarAviso(); vm.guardar(); advanceUntilIdle()
        assertEquals("01234567", vm.state.value.persona?.dni)
        assertEquals(Borrador(), vm.state.value.borrador)
    }

    @Test fun importarOtraFotoSoloRetiraLaAnteriorSiLaImportacionTieneExito() = runTest {
        vm.nuevo(); vm.cambiar { valido }; fotos.fallarImportacion = true
        vm.importarFoto("content://otra"); advanceUntilIdle()
        assertEquals("/borrador.jpg", vm.state.value.borrador.fotoLocal)
        assertFalse("/borrador.jpg" in fotos.borrados)
        fotos.fallarImportacion = false; vm.cerrarAviso(); vm.importarFoto("content://otra"); advanceUntilIdle()
        assertEquals("/importada.jpg", vm.state.value.borrador.fotoLocal)
        assertTrue("/borrador.jpg" in fotos.borrados)
    }

    @Test fun nuevoAbreUnBorradorVacioTrasConsultarUnaPersona() = runTest {
        personas.persona = persona(valido)
        vm.busqueda("01234567"); vm.buscar(); advanceUntilIdle(); vm.nuevo()
        assertEquals(Borrador(), vm.state.value.borrador)
        assertEquals("nuevo", vm.state.value.modo)
    }

    @Test fun cambiarRegionLimpiaLosHijosYCancelarConservaLaDireccionOriginal() = runTest {
        vm.nuevo(); vm.cambiar { valido }; vm.abrirDireccion(); advanceUntilIdle()
        vm.regionDireccion("15"); advanceUntilIdle()
        assertEquals("", vm.state.value.direccionBorrador.provinciaId)
        assertEquals("", vm.state.value.direccionBorrador.distritoId)
        assertTrue(vm.state.value.distritos.isEmpty())
        vm.cerrarDireccion()
        assertEquals(valido.direccion, vm.state.value.borrador.direccion)
    }
    @Test fun direccionSeConfirmaSoloConUbigeoCompletoYSeRecuperaElBorrador() = runTest {
        vm.nuevo(); vm.abrirDireccion(); advanceUntilIdle(); vm.aceptarDireccion()
        assertTrue(vm.state.value.direccionAbierta)
        assertTrue("distrito" in vm.state.value.erroresDireccion)
        vm.regionDireccion("20"); advanceUntilIdle(); vm.provinciaDireccion("2001"); advanceUntilIdle()
        vm.distritoDireccion("200101"); vm.detalleDireccion("Av. Sánchez Cerro 26")
        val restaurado = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
        val otra = PersonasViewModel(personas, fotos, FormularioSavedState(restaurado), UbigeosFalsos())
        assertTrue(otra.state.value.direccionAbierta)
        otra.aceptarDireccion()
        assertEquals("Av. Sánchez Cerro 26, Piura", otra.state.value.borrador.direccion.texto)
        assertFalse(otra.state.value.direccionAbierta)
    }
    @Test fun direccionNuevaConsultaUnSoloNivelPorSeleccion() = runTest {
        vm.nuevo(); vm.abrirDireccion(); advanceUntilIdle()
        assertEquals(listOf("regiones"), ubigeos.consultas)
        assertTrue(vm.state.value.provincias.isEmpty())
        assertTrue(vm.state.value.distritos.isEmpty())
        vm.provinciaDireccion("2001"); vm.distritoDireccion("200101"); advanceUntilIdle()
        assertEquals(listOf("regiones"), ubigeos.consultas)
        vm.regionDireccion("20"); advanceUntilIdle()
        assertEquals(listOf("regiones", "provincias:20"), ubigeos.consultas)
        assertTrue(vm.state.value.distritos.isEmpty())
        vm.provinciaDireccion("2001"); advanceUntilIdle()
        assertEquals(listOf("regiones", "provincias:20", "distritos:2001"), ubigeos.consultas)
        vm.distritoDireccion("200101")
        vm.regionDireccion("15")
        assertTrue(vm.state.value.provincias.isEmpty())
        assertTrue(vm.state.value.distritos.isEmpty())
        assertEquals("", vm.state.value.direccionBorrador.distritoId)
        advanceUntilIdle()
        assertEquals("provincias:15", ubigeos.consultas.last())
        assertEquals(4, ubigeos.consultas.size)
        vm.cerrarDireccion(); vm.descartar(); vm.nuevo(); vm.abrirDireccion(); advanceUntilIdle()
        assertTrue(vm.state.value.provincias.isEmpty())
        assertTrue(vm.state.value.distritos.isEmpty())
        assertEquals(4, ubigeos.consultas.size)
    }

    @Test fun reintentarUnNivelFallidoNoRecargaLosAnteriores() = runTest {
        vm.nuevo(); vm.abrirDireccion(); advanceUntilIdle()
        vm.regionDireccion("20"); advanceUntilIdle()
        ubigeos.fallarDistritos = true
        vm.provinciaDireccion("2001"); advanceUntilIdle()
        assertNotNull(vm.state.value.errorUbigeo)
        assertTrue(vm.state.value.distritos.isEmpty())
        ubigeos.fallarDistritos = false
        vm.cargarUbicacion(); advanceUntilIdle()
        assertEquals(listOf("regiones", "provincias:20", "distritos:2001", "distritos:2001"), ubigeos.consultas)
        assertNull(vm.state.value.errorUbigeo)
        assertEquals("200101", vm.state.value.distritos.single().id)
    }

    private class UbigeosFalsos : UbigeoRepository {
        val consultas = mutableListOf<String>()
        var fallarDistritos = false
        override suspend fun regiones(): List<Region> {
            consultas.add("regiones")
            return listOf(Region("20", "Piura"), Region("15", "Lima"))
        }
        override suspend fun provincias(regionId: String): List<Provincia> {
            consultas.add("provincias:$regionId")
            return listOf(Provincia(regionId + "01", if (regionId == "20") "Piura" else "Lima", regionId))
        }
        override suspend fun distritos(provinciaId: String): List<Distrito> {
            consultas.add("distritos:$provinciaId")
            if (fallarDistritos) throw IOException("Sin conexión")
            return listOf(Distrito(provinciaId + "01", if (provinciaId == "2001") "Piura" else "Lima", provinciaId))
        }
    }

    private fun persona(d: Borrador) = Persona(d.dni, d.nombres, d.apellidos, d.direccion,
        d.profesionId, d.profesion, d.celular, d.correo, "/api/personas/${d.dni}/foto?v=1")

    private inner class PersonasFalsas : PersonasRepository {
        var persona: Persona? = null
        var activa = true
        var guardados = 0
        var ultimaEdicion = false
        var fallarGuardado = false
        override fun errorConfiguracion(): String? = null
        override suspend fun profesiones() = listOf(Profesion(1, "Docencia"))
        override suspend fun crearProfesion(nombre: String) = Profesion(2, nombre)
        override suspend fun buscar(dni: String) = persona?.takeIf { activa && it.dni == dni }
            ?: throw ErrorRepositorio(TipoErrorRepositorio.NO_ENCONTRADO, "No encontrada")
        override suspend fun existe(dni: String) = persona?.dni == dni
        override suspend fun eliminar(dni: String) { buscar(dni); activa = false }
        override suspend fun guardar(datos: Borrador, editar: Boolean): Persona {
            if (fallarGuardado) throw IOException("Sin conexión")
            guardados++; ultimaEdicion = editar
            return persona(datos).also { persona = it; activa = true }
        }
        override fun fotoUrl(ruta: String) = "https://example.com$ruta"
    }
    private class FotosFalsas : FotosRepository {
        val borrados = mutableListOf<String>()
        var ultimoOrigen = ""
        var fallarImportacion = false
        override fun prepararCaptura() = CapturaFoto("/captura.jpg", "content://captura")
        override fun origenArchivo(ruta: String) = "file://$ruta"
        override suspend fun importar(uri: String): String {
            if (fallarImportacion) throw IOException("No se pudo importar")
            ultimoOrigen = uri
            return "/importada.jpg"
        }
        override fun borrar(ruta: String) { if (ruta.isNotBlank()) borrados.add(ruta) }
    }
}
