package com.example.personas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import com.example.personas.viewmodel.Pantalla
import com.example.personas.viewmodel.PersonasViewModel

@Composable
internal fun EditorPersona(s: Pantalla, vm: PersonasViewModel, compacto: Boolean, camera: () -> Unit, picker: () -> Unit) {
    val d = s.borrador
    val foto = d.fotoLocal.ifBlank { vm.fotoUrl(d.fotoRemota) }
    Column(verticalArrangement = Arrangement.spacedBy(if (compacto) 12.dp else 16.dp)) {
        FichaEnVivo(d, foto, s.tab == 1, compacto)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PestanasFormulario(s.tab, !s.ocupado, vm::tab)
        when (s.tab) {
            0 -> DatosPersona(s, vm)
            1 -> {
                Campo("Dirección", d.direccion, "Av. Sánchez Cerro 27, Piura", s.errores["direccion"], !s.ocupado,
                    multilinea = true) { valor -> vm.cambiar { it.copy(direccion = valor.take(200)) } }
                Campo("Celular", d.celular, "Ej. 987654321", s.errores["celular"], !s.ocupado, KeyboardType.Phone) { valor ->
                    vm.cambiar { it.copy(celular = valor.filter { c -> c in '0'..'9' }.take(9)) }
                }
                Campo("Correo electrónico", d.correo, "Ej. alexis@correo.com", s.errores["correo"], !s.ocupado, KeyboardType.Email) { valor ->
                    vm.cambiar { it.copy(correo = valor.take(180)) }
                }
            }
            2 -> {
                FotoPersona(foto, d.iniciales, Modifier.fillMaxWidth().height(if (compacto) 96.dp else 160.dp), vistaPrevia = true)
                if (d.tieneFoto && !compacto) Text("Fotografía seleccionada", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
                AccionesFoto(d.tieneFoto, !s.ocupado, camera, picker)
                s.errores["foto"]?.let { ErrorTexto(it) }
            }
        }
    }
}

@Composable
private fun DatosPersona(s: Pantalla, vm: PersonasViewModel) {
    val d = s.borrador
    val dni: @Composable (Modifier) -> Unit = { modifier ->
        Campo("DNI", d.dni, "Ej. 74581236", s.errores["dni"], !s.ocupado && s.modo == "nuevo",
            KeyboardType.Number, modifier) { valor -> vm.cambiar { it.copy(dni = valor.filter { c -> c in '0'..'9' }.take(8)) } }
    }
    val nombres: @Composable (Modifier) -> Unit = { modifier ->
        Campo("Nombres", d.nombres, "Ej. Alexis", s.errores["nombres"], !s.ocupado, modifier = modifier, maximo = 100) { valor -> vm.cambiar { it.copy(nombres = valor.take(100)) } }
    }
    val apellidos: @Composable (Modifier) -> Unit = { modifier ->
        Campo("Apellidos", d.apellidos, "Ej. Sandoval", s.errores["apellidos"], !s.ocupado, modifier = modifier, maximo = 100) { valor -> vm.cambiar { it.copy(apellidos = valor.take(100)) } }
    }
    val escala = LocalDensity.current.fontScale
    val pantallaCompacta = LocalConfiguration.current.screenHeightDp < 700
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val dosColumnas = pantallaCompacta && maxWidth >= 240.dp && escala <= 1.3f
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (dosColumnas) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { dni(Modifier.weight(1f)); nombres(Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { apellidos(Modifier.weight(1f)); ProfesionCampo(s, vm, Modifier.weight(1f)) }
            } else {
                dni(Modifier.fillMaxWidth())
                nombres(Modifier.fillMaxWidth())
                apellidos(Modifier.fillMaxWidth())
                ProfesionCampo(s, vm, Modifier.fillMaxWidth())
            }
            if (s.modo == "editar") Text("El DNI no se modifica.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfesionCampo(s: Pantalla, vm: PersonasViewModel, modifier: Modifier) {
    var abierto by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Etiqueta("Profesión")
        Box {
            Surface(onClick = { abierto = true }, enabled = !s.ocupado, shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, if (s.errores["profesionId"] != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("campo-Profesión")) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(s.borrador.profesion.ifBlank { "Selecciona una profesión" }, Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (s.borrador.profesion.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    AppIcon(Symbol.ChevronDown, Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                s.profesiones.forEach { p ->
                    DropdownMenuItem(text = { Text(p.nombre) }, onClick = {
                        vm.cambiar { it.copy(profesionId = p.id, profesion = p.nombre) }; abierto = false
                    })
                }
                if (s.profesiones.isEmpty()) DropdownMenuItem(
                    text = { Text(if (s.cargandoCatalogo) "Cargando…" else "Reintentar carga") },
                    enabled = !s.cargandoCatalogo, onClick = { vm.cargarProfesiones(); abierto = false })
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Añadir profesión…", color = MaterialTheme.colorScheme.primary) },
                    leadingIcon = { AppIcon(Symbol.Plus) }, onClick = { abierto = false; vm.abrirProfesion() })
            }
        }
        s.errores["profesionId"]?.let { ErrorTexto(it) }
    }
}

@Composable
internal fun PestanasFormulario(selected: Int, enabled: Boolean, select: (Int) -> Unit) {
    val labels = listOf("Datos", "Contacto", "Foto")
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge.copy(lineHeight = TextUnit.Unspecified)
    val tabHeight = with(density) { labels.maxOf { measurer.measure(it, style, maxLines = 1).size.height }.toDp() } + 24.dp
    val minimum = with(density) { labels.maxOf { measurer.measure(it, style.copy(fontWeight = FontWeight.SemiBold), maxLines = 1).size.width }.toDp() } + 32.dp + if (density.fontScale > 1.3f) 24.dp else 0.dp
    val focus = LocalFocusManager.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val desplazable = maxWidth < minimum * labels.size
        Row(Modifier.fillMaxWidth().then(if (desplazable) Modifier.horizontalScroll(rememberScrollState()) else Modifier).selectableGroup()) {
            labels.forEachIndexed { indice, label ->
                val ancho = if (desplazable) Modifier.width(minimum.coerceAtLeast(96.dp)) else Modifier.weight(1f)
                Column(ancho.selectable(selected == indice, enabled = enabled, role = Role.Tab,
                    onClick = { focus.clearFocus(); select(indice) })
                    .background(if (selected == indice) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                    Box(Modifier.fillMaxWidth().heightIn(min = tabHeight.coerceAtLeast(48.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center) {
                        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = style, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = true,
                            fontWeight = if (selected == indice) FontWeight.SemiBold else FontWeight.Normal)
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(if (selected == indice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                }
            }
        }
    }
}

@Composable
private fun AccionesFoto(tieneFoto: Boolean, habilitado: Boolean, camera: () -> Unit, picker: () -> Unit) {
    val escala = LocalDensity.current.fontScale
    val tomar: @Composable (Modifier) -> Unit = { modifier ->
        BotonSecundario(onClick = camera, enabled = habilitado, modifier = modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
            AppIcon(Symbol.Camera, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if (tieneFoto) "Volver a tomar" else "Tomar foto", maxLines = 1)
        }
    }
    val adjuntar: @Composable (Modifier) -> Unit = { modifier ->
        BotonPrimario(onClick = picker, enabled = habilitado, modifier = modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
            AppIcon(Symbol.Attach, Modifier.size(18.dp), MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(6.dp))
            Text(if (tieneFoto) "Reemplazar foto" else "Adjuntar foto", maxLines = 1)
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (escala > 1.3f) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tomar(Modifier.fillMaxWidth()); adjuntar(Modifier.fillMaxWidth())
        } else if (maxWidth < 300.dp) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonSecundario(onClick = camera, enabled = habilitado, modifier = Modifier.weight(1f), contentPadding = PaddingValues(10.dp)) {
                Text(if (tieneFoto) "Volver a tomar" else "Tomar foto", maxLines = 2, textAlign = TextAlign.Center)
            }
            BotonPrimario(onClick = picker, enabled = habilitado, modifier = Modifier.weight(1f), contentPadding = PaddingValues(10.dp)) {
                Text(if (tieneFoto) "Reemplazar foto" else "Adjuntar foto", maxLines = 2, textAlign = TextAlign.Center)
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tomar(Modifier.weight(1f)); adjuntar(Modifier.weight(1f))
        }
    }
}
