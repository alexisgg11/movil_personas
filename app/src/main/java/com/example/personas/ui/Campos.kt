package com.example.personas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun Etiqueta(texto: String) {
    Text(texto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
}

@Composable
internal fun Campo(
    etiqueta: String, valor: String, placeholder: String, error: String? = null,
    habilitado: Boolean = true, teclado: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier, multilinea: Boolean = false, maximo: Int = 200, cambio: (String) -> Unit
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Etiqueta(etiqueta)
        Entrada(etiqueta, valor, placeholder, cambio, habilitado, error != null, teclado,
            if (teclado == KeyboardType.Email) ImeAction.Done else ImeAction.Next, multilinea, maximo = maximo)
        error?.let { ErrorTexto(it) }
    }
}

@Composable
internal fun CampoBusqueda(valor: String, cambio: (String) -> Unit, habilitado: Boolean, buscar: () -> Unit, modifier: Modifier) {
    Entrada("DNI de búsqueda", valor, "Ej. 74581236", cambio, habilitado, false,
        KeyboardType.Number, ImeAction.Search, false, modifier, buscar, maximo = 8)
}

@Composable
private fun Entrada(
    etiqueta: String, valor: String, placeholder: String, cambio: (String) -> Unit,
    habilitado: Boolean, error: Boolean, teclado: KeyboardType, accion: ImeAction,
    multilinea: Boolean, modifier: Modifier = Modifier, buscar: () -> Unit = {}, maximo: Int = 200
) {
    val interaction = remember { MutableInteractionSource() }
    val enfocado by interaction.collectIsFocusedAsState()
    var edicion by remember(etiqueta) { mutableStateOf(TextFieldValue(valor)) }
    LaunchedEffect(valor, enfocado) {
        if (!enfocado && valor != edicion.text) edicion = TextFieldValue(valor, TextRange(valor.length))
    }
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(7.dp)
    val focus = LocalFocusManager.current
    BasicTextField(
        value = edicion, onValueChange = { siguiente ->
            val limite = when (teclado) { KeyboardType.Number -> 8; KeyboardType.Phone -> 9; KeyboardType.Email -> 180; else -> maximo }
            val texto = (if (teclado == KeyboardType.Number || teclado == KeyboardType.Phone) siguiente.text.filter { it in '0'..'9' } else siguiente.text).take(limite)
            edicion = if (texto == siguiente.text) siguiente else TextFieldValue(texto, TextRange(siguiente.selection.end.coerceAtMost(texto.length)))
            cambio(texto)
        }, enabled = habilitado,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).clip(forma)
            .background(if (habilitado) colores.surface else colores.surfaceVariant)
            .border(if (enfocado || error) 2.dp else 1.dp,
                when { error -> colores.error; enfocado -> colores.primary; else -> colores.outlineVariant }, forma)
            .testTag("campo-$etiqueta").semantics { contentDescription = etiqueta },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (habilitado) colores.onSurface else colores.onSurfaceVariant),
        singleLine = !multilinea, maxLines = if (multilinea) 3 else 1,
        interactionSource = interaction, cursorBrush = SolidColor(colores.primary),
        keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = accion),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) },
            onDone = { focus.clearFocus() }, onSearch = { buscar() }),
        decorationBox = { interior ->
            Box(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), contentAlignment = Alignment.CenterStart) {
                if (edicion.text.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colores.onSurfaceVariant)
                interior()
            }
        }
    )
}

@Composable
internal fun ErrorTexto(texto: String) {
    Text(texto, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
