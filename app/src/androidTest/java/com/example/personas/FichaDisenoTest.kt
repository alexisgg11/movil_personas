package com.example.personas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.personas.domain.Persona
import com.example.personas.domain.Direccion
import com.example.personas.ui.ResultadoPersona
import com.example.personas.ui.theme.PersonasTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FichaDisenoTest {
    @get:Rule val compose = createComposeRule()
    @Test fun resultUsesAccessibleIconsAndOpensTheSummary() {
        var vistas = 0
        var ediciones = 0
        var eliminaciones = 0
        val p = Persona("01234567", "Alexis", "Sandoval", Direccion(detalle = "Av. Sánchez Cerro 26", distritoId = "200101", distrito = "Piura", provinciaId = "2001", provincia = "Piura", regionId = "20", region = "Piura"), 1, "Arquitectura", "987654321", "alexis@correo.com", "")
        compose.setContent {
            PersonasTheme {
                Box(Modifier.width(320.dp)) {
                    ResultadoPersona(p, "", true, { vistas++ }, { ediciones++ }, { eliminaciones++ })
                }
            }
        }
        compose.onNodeWithText("Ver").assertDoesNotExist()
        compose.onNodeWithText("Editar").assertDoesNotExist()
        compose.onNodeWithText("Eliminar").assertDoesNotExist()
        compose.onNodeWithContentDescription("Ver ficha").performClick()
        compose.onNodeWithContentDescription("Editar persona").performClick()
        compose.onNodeWithContentDescription("Eliminar persona").performClick()
        compose.onNodeWithText("Alexis Sandoval").performClick()
        compose.runOnIdle { assertEquals(2, vistas); assertEquals(1, ediciones); assertEquals(1, eliminaciones) }
    }
}
