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
                        FilaResumen(Symbol.Briefcase, p.profesion)
                        FilaResumen(Symbol.IdCard, "DNI ${p.dni}")
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
    Column(Modifier.fillMaxWidth().testTag("ficha-en-vivo"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            FotoPersona(foto, d.iniciales, Modifier.size(if (compacto) 64.dp else 72.dp, if (compacto) 72.dp else 80.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(d.nombreCompleto.ifBlank { "Nombre y apellidos" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                FilaResumen(Symbol.Briefcase, d.profesion.ifBlank { "Profesión" })
                FilaResumen(Symbol.IdCard, "DNI ${d.dni.ifBlank { "--------" }}")
            }
        }
        if (contacto) Column(Modifier.testTag("resumen-contacto"), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (d.celular.isNotBlank()) FilaResumen(Symbol.Phone, d.celular)
            if (d.correo.isNotBlank()) FilaResumen(Symbol.Mail, d.correo)
            if (d.direccion.texto.isNotBlank()) FilaResumen(Symbol.Home, d.direccion.texto)
        }
    }
}

@Composable
internal fun FilaResumen(simbolo: Symbol, texto: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        AppIcon(simbolo, Modifier.size(18.dp).padding(top = 1.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun DetallePersona(p: Persona, foto: String) {
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                FotoPersona(foto, Borrador.de(p).iniciales, Modifier.size(80.dp, 88.dp))
                Text("${p.nombres} ${p.apellidos}", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            DatoContacto(Symbol.IdCard, "DNI", p.dni)
            DatoContacto(Symbol.Briefcase, "Profesión", p.profesion)
            DatoContacto(Symbol.Phone, "Celular", p.celular)
            DatoContacto(Symbol.Mail, "Correo electrónico", p.correo)
            DatoContacto(Symbol.Home, "Dirección", p.direccion.texto)
        }
    }
}

@Composable
private fun DatoContacto(simbolo: Symbol, titulo: String, valor: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        AppIcon(simbolo, Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        }
    }
}
