package com.example.personas.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

enum class Symbol { Search, Person, Eye, Edit, Delete, Phone, Mail, Location, Camera, Attach, Plus, Close, Check, ChevronRight, ChevronDown, Briefcase, Home, IdCard }

@Composable
fun AppIcon(symbol: Symbol, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, description: String? = null) {
    Canvas(modifier.size(22.dp).semantics { if (description != null) contentDescription = description }) {
        scale(size.width / 24f, size.height / 24f, Offset.Zero) {
            val stroke = Stroke(1.8f, cap = StrokeCap.Round)
            fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(color, Offset(x, y), Offset(x2, y2), 1.8f, StrokeCap.Round)
            fun path(vararg points: Float) {
                drawPath(Path().apply { moveTo(points[0], points[1]); for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1]) }, color, style = stroke)
            }
            when (symbol) {
                Symbol.ChevronRight -> path(9f, 5f, 16f, 12f, 9f, 19f)
                Symbol.ChevronDown -> path(5f, 9f, 12f, 16f, 19f, 9f)
                Symbol.Search -> { drawCircle(color, 7f, Offset(10f, 10f), style = stroke); line(15f, 15f, 21f, 21f) }
                Symbol.Person -> {
                    drawCircle(color, 4f, Offset(12f, 7f), style = stroke)
                    drawPath(Path().apply { moveTo(3f, 22f); cubicTo(3f, 11f, 21f, 11f, 21f, 22f) }, color, style = stroke)
                }
                Symbol.Plus -> { line(12f, 4f, 12f, 20f); line(4f, 12f, 20f, 12f) }
                Symbol.Close -> { line(5f, 5f, 19f, 19f); line(19f, 5f, 5f, 19f) }
                Symbol.Check -> path(4f, 12f, 9f, 17f, 20f, 6f)
                Symbol.Eye -> {
                    drawPath(Path().apply { moveTo(2f, 12f); cubicTo(7f, 2f, 17f, 2f, 22f, 12f); cubicTo(17f, 22f, 7f, 22f, 2f, 12f); close() }, color, style = stroke)
                    drawCircle(color, 3f, Offset(12f, 12f), style = stroke)
                }
                Symbol.Edit -> { path(4f, 20f, 5f, 14f, 17f, 2f, 22f, 7f, 10f, 19f, 4f, 20f); line(14f, 5f, 19f, 10f) }
                Symbol.Delete -> { path(5f, 6f, 6f, 21f, 18f, 21f, 19f, 6f); line(3f, 6f, 21f, 6f); path(8f, 6f, 8f, 2f, 16f, 2f, 16f, 6f); line(10f, 10f, 10f, 17f); line(14f, 10f, 14f, 17f) }
                Symbol.Mail -> { path(2f, 5f, 22f, 5f, 22f, 19f, 2f, 19f, 2f, 5f); path(2f, 6f, 12f, 13f, 22f, 6f) }
                Symbol.Phone -> path(4f, 3f, 9f, 3f, 10f, 8f, 7f, 10f, 14f, 17f, 16f, 14f, 21f, 15f, 21f, 20f, 17f, 21f, 10f, 17f, 4f, 10f, 3f, 6f, 4f, 3f)
                Symbol.Briefcase -> {
                    path(3f, 7f, 21f, 7f, 21f, 21f, 3f, 21f, 3f, 7f)
                    path(8f, 7f, 8f, 3f, 16f, 3f, 16f, 7f)
                    line(3f, 12f, 21f, 12f); line(10f, 12f, 14f, 12f); line(12f, 11f, 12f, 15f)
                }
                Symbol.Home -> {
                    path(2f, 11f, 12f, 3f, 22f, 11f)
                    path(5f, 9f, 5f, 21f, 19f, 21f, 19f, 9f)
                    path(9f, 21f, 9f, 14f, 15f, 14f, 15f, 21f)
                }
                Symbol.IdCard -> {
                    path(2f, 5f, 22f, 5f, 22f, 20f, 2f, 20f, 2f, 5f)
                    drawCircle(color, 2f, Offset(7f, 11f), style = stroke)
                    path(4f, 17f, 4f, 15f, 10f, 15f, 10f, 17f)
                    line(14f, 10f, 19f, 10f); line(14f, 15f, 19f, 15f)
                }
                Symbol.Location -> {
                    drawPath(Path().apply { moveTo(12f, 22f); cubicTo(-10f, 2f, 34f, 2f, 12f, 22f); close() }, color, style = stroke)
                    drawCircle(color, 2.5f, Offset(12f, 9f), style = stroke)
                }
                Symbol.Camera -> { path(2f, 7f, 7f, 7f, 9f, 3f, 15f, 3f, 17f, 7f, 22f, 7f, 22f, 21f, 2f, 21f, 2f, 7f); drawCircle(color, 4f, Offset(12f, 14f), style = stroke) }
                Symbol.Attach -> drawPath(Path().apply { moveTo(8f, 16f); lineTo(16f, 8f); cubicTo(20f, 4f, 25f, 9f, 20f, 14f); lineTo(12f, 22f); cubicTo(4f, 28f, -2f, 17f, 5f, 10f); lineTo(13f, 2f) }, color, style = stroke)

            }
        }
    }
}
