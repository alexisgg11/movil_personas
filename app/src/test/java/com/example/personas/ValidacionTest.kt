package com.example.personas

import com.example.personas.domain.*
import org.junit.Assert.*
import org.junit.Test

class ValidacionTest {
    private val valido = Borrador("01234567", "Alexis", "Sandoval", Direccion(detalle = "Av. Sánchez Cerro 26", distritoId = "200101", distrito = "Piura", provinciaId = "2001", provincia = "Piura", regionId = "20", region = "Piura"), 1,
        "Docencia", "987654321", "alexis@correo.com", fotoLocal = "/foto.jpg")
    @Test fun dniConservaCeroInicialYExigeOchoDigitosAscii() {
        assertTrue(Validacion.dni("01234567"))
        listOf("1234567", "123456789", "12a45678", "１２３４５６７８", " 12345678").forEach { assertFalse(it, Validacion.dni(it)) }
    }
    @Test fun celularPeruanoValido() {
        assertTrue(Validacion.celular("987654321"))
        listOf("887654321", "98765432", "+51987654321", "9abcdefgh").forEach { assertFalse(Validacion.celular(it)) }
    }
    @Test fun correoRequiereDominioYNoEspacios() {
        assertTrue(Validacion.correo("alexis@correo.com"))
        listOf("alexis", "a@correo", "a b@correo.com", "a@@correo.com").forEach { assertFalse(Validacion.correo(it)) }
    }
    @Test fun formularioExigeDatosCatalogoYFoto() {
        assertTrue(Validacion.formulario(valido).isEmpty())
        assertEquals(setOf("dni", "nombres", "apellidos", "direccion", "profesionId", "celular", "correo", "foto"), Validacion.formulario(Borrador()).keys)
        assertTrue(Validacion.formulario(valido.copy(fotoLocal = "", fotoRemota = "/api/foto")).isEmpty())
        assertTrue("foto" in Validacion.formulario(valido.copy(fotoLocal = "")))
        assertTrue("nombres" in Validacion.formulario(valido.copy(nombres = " ")))
        assertTrue("direccion" in Validacion.formulario(valido.copy(direccion = valido.direccion.copy(detalle = "x".repeat(201)))))
    }
    @Test fun debugAceptaRedesPrivadasYReleaseExigeHttps() {
        listOf("http://127.0.0.1:8081", "http://10.0.2.2:8081", "http://192.168.1.2:8081", "http://172.16.0.2:8081").forEach {
            assertNull(ServidorUrl.validar(it, true)); assertNotNull(ServidorUrl.validar(it, false))
        }
        assertNull(ServidorUrl.validar("https://personas.example.com", false))
        assertNotNull(ServidorUrl.validar("http://example.com", true))
        assertNotNull(ServidorUrl.validar("http://172.32.0.2", true))
    }
    @Test fun urlRechazaCredencialesRutasYPuertosInvalidos() {
        listOf("", "servidor", "ftp://localhost", "https://user:password@host.com", "http://localhost:0", "http://localhost:99999", "http://localhost/api", "https://host.com?q=1", "https://host.com/#foto").forEach {
            assertNotNull(it, ServidorUrl.validar(it, true))
        }
    }
    @Test fun camposSeValidanEnLaPestanaCorrespondiente() {
        val soloDatos = valido.copy(direccion = Direccion(), celular = "", correo = "", fotoLocal = "")
        assertTrue(Validacion.paso(soloDatos, 0).isEmpty())
        assertEquals(setOf("direccion", "celular", "correo"), Validacion.paso(soloDatos, 1).keys)
        assertEquals(setOf("foto"), Validacion.paso(soloDatos, 2).keys)
        assertEquals(1, Validacion.seccion("direccion"))
        assertEquals(0, Validacion.seccion("profesionId"))
    }
    @Test fun direccionNoAdmiteUnaJerarquiaIncompatible() {
        assertEquals("Av. Sánchez Cerro 26, Piura", valido.direccion.texto)
        assertFalse(valido.direccion.copy(provinciaId = "1501").completa)
        assertFalse(valido.direccion.copy(distritoId = "150101").completa)
        assertFalse(valido.direccion.copy(regionId = "").completa)
    }
    @Test fun fichaTieneInicialesYNombreSinEspaciosSobrantes() {
        assertEquals("AS", valido.iniciales)
        assertEquals("Alexis Sandoval", valido.copy(nombres = " Alexis ", apellidos = " Sandoval ").nombreCompleto)
        assertTrue(valido.tieneFoto)
        assertFalse(valido.copy(fotoLocal = "").tieneFoto)
    }
}
