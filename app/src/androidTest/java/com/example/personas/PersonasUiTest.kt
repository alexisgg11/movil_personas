package com.example.personas

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonasUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun sectionsAndFooterMatchRegistrationFlow() {
        compose.onNodeWithContentDescription("Ajustes del servidor").assertDoesNotExist()
        compose.onNodeWithText("URL del servidor").assertDoesNotExist()
        compose.onNodeWithText("Nuevo").performClick()
        compose.onNodeWithText("Datos").assertIsSelected()
        compose.onNodeWithTag("campo-Nombres").assertExists()
        compose.onNodeWithTag("campo-Dirección").assertDoesNotExist()
        compose.onNodeWithText("Guardar").assertDoesNotExist()
        compose.onNodeWithText("Siguiente").assertIsDisplayed()
        compose.onNodeWithText("Contacto").performClick().assertIsSelected()
        compose.onNodeWithTag("campo-Dirección").assertExists()
        compose.onNodeWithTag("campo-Celular").assertExists()
        compose.onNodeWithTag("campo-Correo electrónico").assertExists()
        compose.onNodeWithText("Atrás").assertIsDisplayed()
        compose.onNodeWithText("Foto").performClick().assertIsSelected()
        compose.onNodeWithText("Adjuntar foto").assertIsDisplayed()
        compose.onNodeWithText("Tomar foto").assertIsDisplayed()
        compose.onNodeWithText("Guardar").performClick()
        compose.onNodeWithText("Revisa los datos").assertIsDisplayed()
        compose.onNodeWithText("Entendido").performClick()
        compose.onNodeWithText("Datos").assertIsSelected()
        compose.onNodeWithContentDescription("Cerrar ficha").performClick()
    }

    @Test fun liveSummaryAndContactValidationUseEnteredValues() {
        compose.onNodeWithText("Nuevo").performClick()
        compose.onNodeWithTag("campo-Nombres").performTextInput("Alexis")
        compose.onNodeWithTag("campo-Apellidos").performTextInput("Sandoval")
        compose.onNodeWithText("Alexis Sandoval").assertExists()
        compose.onNodeWithText("Contacto").performClick()
        compose.onNodeWithTag("campo-Celular").performTextInput("887654321")
        compose.onNodeWithTag("campo-Correo electrónico").performTextInput("alexis@correo.com")
        compose.onNodeWithTag("resumen-contacto").onChildren().filterToOne(hasText("887654321")).assertExists()
        compose.onNodeWithTag("resumen-contacto").onChildren().filterToOne(hasText("alexis@correo.com")).assertExists()
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Revisa los datos").assertIsDisplayed()
        compose.onNodeWithTag("mensaje-validacion").assertTextContains("Ingresa 9 dígitos; el primero debe ser 9.", substring = true)
        compose.onNodeWithText("Entendido").performClick()
        compose.onNodeWithText("Contacto").assertIsSelected()
        compose.onNodeWithContentDescription("Cerrar ficha").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Descartar").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Descartar").performClick()
    }
    @Test fun addressUsesCascadingSelectorsAndCancelPreservesTheAcceptedValue() {
        fun esperar(etiqueta: String) {
            compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("campo-$etiqueta") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        }
        fun seleccionar(etiqueta: String, nombre: String) {
            esperar(etiqueta)
            compose.onNodeWithTag("campo-$etiqueta").performScrollTo().performClick()
            compose.onAllNodesWithText(nombre).onLast().performScrollTo().performClick()
        }
        compose.onNodeWithText("Nuevo").performClick()
        compose.onNodeWithText("Contacto").performClick()
        compose.onNodeWithTag("campo-Dirección").performClick()
        esperar("Región")
        compose.onNodeWithTag("campo-Provincia").assertIsNotEnabled()
        compose.onNodeWithTag("campo-Distrito").assertIsNotEnabled()
        compose.onNodeWithTag("campo-Vía y número").assertIsNotEnabled()
        compose.onNodeWithText("Aceptar").assertIsDisplayed().performClick()
        compose.onNodeWithText("Selecciona una región.").assertExists()
        seleccionar("Región", "Piura")
        compose.onNodeWithTag("campo-Distrito").assertIsNotEnabled()
        compose.onNodeWithTag("campo-Vía y número").assertIsNotEnabled()
        seleccionar("Provincia", "Piura")
        compose.onNodeWithTag("campo-Vía y número").assertIsNotEnabled()
        seleccionar("Distrito", "Piura")
        compose.onNodeWithTag("campo-Vía y número").assertIsEnabled()
        compose.onNodeWithTag("campo-Vía y número").performScrollTo().performTextInput("Av. Sánchez Cerro 26")
        compose.onNodeWithText("Aceptar").assertIsDisplayed().performClick()
        compose.onNodeWithTag("campo-Dirección").assertTextContains("Av. Sánchez Cerro 26, Piura")
        compose.onNodeWithTag("campo-Dirección").performClick()
        seleccionar("Región", "Lima")
        esperar("Provincia")
        compose.onNodeWithTag("campo-Distrito").assertIsNotEnabled()
        compose.onNodeWithTag("campo-Vía y número").assertIsNotEnabled()
        compose.onNodeWithText("Selecciona una provincia").assertExists()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithTag("campo-Dirección").assertTextContains("Av. Sánchez Cerro 26, Piura")
        compose.onNodeWithContentDescription("Cerrar ficha").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Descartar").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Descartar").performClick()
    }

    @Test fun openingKeyboardKeepsFocusAndTheCompleteDraft() {
        compose.onNodeWithText("Nuevo").performClick()
        compose.onNodeWithTag("campo-DNI").performScrollTo().performClick().assertIsFocused().performTextInput("00601023")
        compose.onNodeWithTag("campo-DNI").performImeAction()
        compose.onNodeWithTag("campo-Nombres").assertIsFocused().performTextInput("Alexis")
        compose.onNodeWithTag("campo-Nombres").performImeAction()
        compose.onNodeWithTag("campo-Apellidos").assertIsFocused().performTextInput("Sandoval")
        compose.onNodeWithTag("campo-DNI").assertTextContains("00601023")
        compose.onNodeWithTag("campo-Nombres").assertTextContains("Alexis")
        compose.onNodeWithTag("campo-Apellidos").assertTextContains("Sandoval")
        compose.onNodeWithText("Alexis Sandoval").assertExists()
        compose.onNodeWithContentDescription("Cerrar ficha").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Descartar").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Descartar").performClick()
    }
}
