package com.example.personas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personas.viewmodel.Pantalla
import com.example.personas.viewmodel.PersonasViewModel

@Composable
internal fun FichaModal(s: Pantalla, vm: PersonasViewModel, camera: () -> Unit, picker: () -> Unit) {
    val consulta = s.modo == "ver"
    val scroll = rememberScrollState()
    val focus = LocalFocusManager.current
    LaunchedEffect(s.tab, s.modo) { scroll.scrollTo(0) }
    Dialog(onDismissRequest = vm::cerrar, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
        dismissOnBackPress = !s.ocupado, dismissOnClickOutside = false
    )) {
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(12.dp), contentAlignment = Alignment.Center) {
            val compacto = maxHeight < 700.dp
            Surface(Modifier.widthIn(max = 480.dp).fillMaxWidth().heightIn(max = maxHeight).testTag("modal-ficha"),
                shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(when (s.modo) { "nuevo" -> "Nueva persona"; "editar" -> "Editar persona"; else -> "Ficha de persona" },
                            Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = vm::cerrar, enabled = !s.ocupado) {
                            AppIcon(Symbol.Close, color = MaterialTheme.colorScheme.onSurfaceVariant, description = "Cerrar ficha")
                        }
                    }
                    if (s.ocupado) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
                    Column(Modifier.weight(1f, fill = false).verticalScroll(scroll).padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("contenido-ficha"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (consulta) s.persona?.let { DetallePersona(it, vm.fotoUrl(it.fotoUrl)) }
                        else EditorPersona(s, vm, compacto, camera, picker)
                    }
                    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).testTag("acciones-ficha"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonSecundario(onClick = { focus.clearFocus(); if (consulta) vm.cerrar() else vm.atras() }, enabled = !s.ocupado) {
                            Text(if (consulta) "Cerrar" else if (s.tab == 0) "Cancelar" else "Atrás")
                        }
                        if (consulta) FilledIconButton(onClick = vm::editar, modifier = Modifier.size(48.dp),
                            enabled = !s.ocupado, shape = RoundedCornerShape(7.dp)) {
                            AppIcon(Symbol.Edit, color = MaterialTheme.colorScheme.onPrimary, description = "Editar persona")
                        } else BotonPrimario(onClick = {
                            focus.clearFocus()
                            if (s.tab == 2) vm.solicitarGuardar() else vm.siguiente()
                        }, enabled = !s.ocupado) {
                            Text(if (s.ocupado) "Procesando…" else if (s.tab == 2) "Guardar" else "Siguiente")
                        }
                    }
                }
            }
        }
    }
}
