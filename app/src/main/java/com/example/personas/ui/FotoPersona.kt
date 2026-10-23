package com.example.personas.ui

import android.graphics.Bitmap
import com.example.personas.data.FotoLoader
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
internal fun FotoPersona(origen: String, iniciales: String, modifier: Modifier, vistaPrevia: Boolean = false) {
    val bitmap by produceState<Bitmap?>(null, origen) {
        value = FotoLoader.cargar(origen)
    }
    val forma = RoundedCornerShape(8.dp)
    Box(modifier.clip(forma).background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.outlineVariant, forma),
        contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it.asImageBitmap(), "Fotografía de la persona", Modifier.fillMaxSize(), contentScale = if (vistaPrevia) ContentScale.Fit else ContentScale.Crop) }
            ?: if (!vistaPrevia && iniciales.isNotBlank()) {
                Text(iniciales, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIcon(Symbol.Person, Modifier.size(if (vistaPrevia) 40.dp else 28.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    if (vistaPrevia) Text("Sin fotografía", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
    }
}
