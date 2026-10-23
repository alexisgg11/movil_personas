package com.example.personas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personas.domain.Borrador
import com.example.personas.domain.Persona

@Composable
internal fun ResultadoPersona(p: Persona, foto: String, habilitado: Boolean, ver: () -> Unit, editar: () -> Unit, eliminar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Persona encontrada", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column {
                Row(Modifier.fillMaxWidth().clickable(enabled = habilitado, role = Role.Button, onClickLabel = "Abrir ficha", onClick = ver).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    FotoPersona(foto, Borrador.de(p).iniciales, Modifier.size(64.dp, 72.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${p.nombres} ${p.apellidos}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(p.profesion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("DNI ${p.dni}", style = MaterialTheme.typography.bodyMedium)
                    }
                    AppIcon(Symbol.ChevronRight, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AccionFicha(Symbol.Eye, "Ver ficha", habilitado, ver, Modifier.weight(1f))
                    VerticalDivider(Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    AccionFicha(Symbol.Edit, "Editar persona", habilitado, editar, Modifier.weight(1f))
                    VerticalDivider(Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    AccionFicha(Symbol.Delete, "Eliminar persona", habilitado, eliminar, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AccionFicha(simbolo: Symbol, descripcion: String, habilitado: Boolean, accion: () -> Unit, modifier: Modifier) {
    IconButton(onClick = accion, enabled = habilitado, modifier = modifier.height(48.dp)) {
        AppIcon(simbolo, description = descripcion)
    }
}

@Composable
internal fun FichaEnVivo(d: Borrador, foto: String, contacto: Boolean, compacto: Boolean) {
    Row(Modifier.fillMaxWidth().testTag("ficha-en-vivo"), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        FotoPersona(foto, d.iniciales, Modifier.size(if (compacto) 64.dp else 76.dp, if (compacto) 72.dp else 84.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(d.nombreCompleto.ifBlank { "Nombre y apellidos" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(d.profesion.ifBlank { "Profesión" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val detalle = if (contacto) listOf(d.celular, d.correo, d.direccion).filter { it.isNotBlank() }.joinToString(" | ") else ""
            Text(detalle.ifBlank { "DNI ${d.dni.ifBlank { "--------" }}" }, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("resumen-contacto"))
        }
    }
}

@Composable
internal fun DetallePersona(p: Persona, foto: String) {
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                FotoPersona(foto, Borrador.de(p).iniciales, Modifier.size(88.dp, 96.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${p.nombres} ${p.apellidos}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(p.profesion, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("DNI ${p.dni}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DatoContacto(Symbol.Location, "Dirección", p.direccion)
            DatoContacto(Symbol.Phone, "Celular", p.celular)
            DatoContacto(Symbol.Mail, "Correo electrónico", p.correo)
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun DatoContacto(simbolo: Symbol, titulo: String, valor: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        AppIcon(simbolo, Modifier.padding(top = 3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        }
    }
}
