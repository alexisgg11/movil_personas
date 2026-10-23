package com.example.personas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personas.viewmodel.Pantalla
import com.example.personas.viewmodel.PersonasViewModel

@Composable
internal fun DireccionCampo(s: Pantalla, abrir: () -> Unit) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Etiqueta("Dirección")
        Surface(onClick = { focus.clearFocus(); keyboard?.hide(); abrir() }, enabled = !s.ocupado,
            shape = RoundedCornerShape(7.dp), border = BorderStroke(1.dp,
                if (s.errores["direccion"] != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("campo-Dirección")
                .semantics { contentDescription = "Registrar dirección" }) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(Symbol.Home, Modifier.size(20.dp))
                Text(s.borrador.direccion.texto.ifBlank { "Registrar dirección" }, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                    color = if (s.borrador.direccion.texto.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                AppIcon(Symbol.Edit, Modifier.size(18.dp))
            }
        }
        s.errores["direccion"]?.let { ErrorTexto(it) }
    }
}

@Composable
internal fun DireccionModal(s: Pantalla, vm: PersonasViewModel) {
    val d = s.direccionBorrador
    val focus = LocalFocusManager.current
    Dialog(onDismissRequest = vm::cerrarDireccion, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(12.dp), contentAlignment = Alignment.Center) {
            Surface(Modifier.widthIn(max = 480.dp).fillMaxWidth().heightIn(max = maxHeight).testTag("modal-direccion"), shape = RoundedCornerShape(14.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Dirección", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { focus.clearFocus(); vm.cerrarDireccion() }) { AppIcon(Symbol.Close, description = "Cerrar dirección") }
                    }
                    if (s.cargandoUbigeo) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        UbigeoCampo("Región", d.region, "Selecciona una región", s.regiones.map { it.id to it.nombre },
                            !s.cargandoUbigeo, s.erroresDireccion["region"], vm::regionDireccion)
                        UbigeoCampo("Provincia", d.provincia, "Selecciona una provincia", s.provincias.map { it.id to it.nombre },
                            !s.cargandoUbigeo && d.regionId.isNotBlank(), s.erroresDireccion["provincia"], vm::provinciaDireccion)
                        UbigeoCampo("Distrito", d.distrito, "Selecciona un distrito", s.distritos.map { it.id to it.nombre },
                            !s.cargandoUbigeo && d.provinciaId.isNotBlank(), s.erroresDireccion["distrito"], vm::distritoDireccion)
                        Campo("Vía y número", d.detalle, "Ej. Av. Sánchez Cerro 26, interior 2", s.erroresDireccion["detalle"], habilitado = d.distritoId.isNotBlank() && !s.cargandoUbigeo, multilinea = true,
                            cambio = vm::detalleDireccion)
                        if (d.detalle.isNotBlank() && d.distrito.isNotBlank()) FilaResumen(Symbol.Home, d.texto)
                        s.errorUbigeo?.let {
                            ErrorTexto(it)
                            BotonSecundario(onClick = vm::cargarUbicacion) { Text("Reintentar") }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(16.dp).testTag("acciones-direccion"), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        BotonSecundario(onClick = { focus.clearFocus(); vm.cerrarDireccion() }) { Text("Cancelar") }
                        BotonPrimario(onClick = { focus.clearFocus(); vm.aceptarDireccion() }, enabled = !s.cargandoUbigeo && s.errorUbigeo == null) { Text("Aceptar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun UbigeoCampo(etiqueta: String, valor: String, placeholder: String, opciones: List<Pair<String, String>>, habilitado: Boolean,
    error: String?, seleccionar: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Etiqueta(etiqueta)
        Box {
            Surface(onClick = { abierto = true }, enabled = habilitado && opciones.isNotEmpty(), shape = RoundedCornerShape(7.dp),
                color = if (habilitado) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("campo-$etiqueta").semantics { contentDescription = etiqueta }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(valor.ifBlank { placeholder }, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                        color = if (valor.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    AppIcon(Symbol.ChevronDown, Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DropdownMenu(expanded = abierto && habilitado, onDismissRequest = { abierto = false }, modifier = Modifier.heightIn(max = 300.dp)) {
                opciones.forEach { (id, nombre) -> DropdownMenuItem(text = { Text(nombre) }, onClick = { abierto = false; seleccionar(id) }) }
            }
        }
        error?.let { ErrorTexto(it) }
    }
}
