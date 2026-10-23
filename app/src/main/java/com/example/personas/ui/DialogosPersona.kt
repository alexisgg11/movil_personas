package com.example.personas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.personas.viewmodel.Pantalla
import com.example.personas.viewmodel.PersonasViewModel

@Composable
internal fun DialogosPersona(s: Pantalla, vm: PersonasViewModel) {
    if (s.nuevaProfesion) AlertDialog(
        onDismissRequest = vm::cerrarProfesion,
        title = { Text("Nueva profesión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Campo("Nombre de la profesión", s.nombreProfesion, "Ej. Diseño gráfico",
                    s.errorProfesion, !s.ocupado, maximo = 80, cambio = vm::nombreProfesion)
            }
        },
        confirmButton = { BotonPrimario(onClick = vm::crearProfesion, enabled = !s.ocupado) { Text(if (s.ocupado) "Guardando…" else "Añadir") } },
        dismissButton = { BotonSecundario(onClick = vm::cerrarProfesion, enabled = !s.ocupado) { Text("Cancelar") } }
    )
    if (s.confirmacion.isNotEmpty()) {
        val tipo = s.confirmacion
        AlertDialog(
            onDismissRequest = vm::cancelarConfirmacion,
            title = { Text(when (tipo) { "guardar" -> if (s.modo == "editar") "¿Guardar cambios?" else "¿Registrar persona?"; "eliminar" -> "¿Eliminar persona?"; else -> "¿Descartar cambios?" }) },
            text = {
                Text(when (tipo) {
                    "guardar" -> "${s.borrador.nombreCompleto}\nDNI ${s.borrador.dni}"
                    "eliminar" -> "${s.persona?.nombres.orEmpty()} ${s.persona?.apellidos.orEmpty()} dejará de aparecer en las búsquedas. Sus datos se conservarán como inactivos."
                    else -> "Se perderán los cambios de este formulario."
                })
            },
            confirmButton = {
                BotonPrimario(onClick = { when (tipo) { "guardar" -> vm.guardar(); "eliminar" -> vm.eliminar(); else -> vm.descartar() } }) {
                    Text(when (tipo) { "guardar" -> "Guardar"; "eliminar" -> "Eliminar"; else -> "Descartar" })
                }
            },
            dismissButton = { BotonSecundario(onClick = vm::cancelarConfirmacion) { Text(if (tipo == "descartar") "Seguir editando" else "Cancelar") } }
        )
    }
    s.aviso?.let { aviso ->
        AlertDialog(onDismissRequest = vm::cerrarAviso, title = { Text(aviso.titulo) },
            text = { Text(aviso.mensaje, Modifier.testTag("mensaje-validacion")) },
            confirmButton = { BotonPrimario(onClick = vm::cerrarAviso) { Text("Entendido") } })
    }
}
