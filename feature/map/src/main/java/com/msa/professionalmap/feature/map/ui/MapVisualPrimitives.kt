package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.model.PoiCategory

internal enum class MapGlyph {
    Menu,
    Location,
    Route,
    Navigation,
    Map,
    Voice,
    Offline,
    Pin,
    Target,
    Close,
}

@Composable
internal fun MapGlyphBadge(
    glyph: MapGlyph,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = tint.copy(alpha = 0.12f),
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        MapGlyphIcon(glyph = glyph, tint = tint, size = iconSize)
    }
}

@Composable
internal fun MapGlyphIcon(
    glyph: MapGlyph,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val unit = this.size.minDimension
        val strokeWidth = unit * 0.095f
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        when (glyph) {
            MapGlyph.Menu -> {
                val x1 = unit * 0.18f
                val x2 = unit * 0.82f
                listOf(0.28f, 0.50f, 0.72f).forEach { y ->
                    drawLine(tint, Offset(x1, unit * y), Offset(x2, unit * y), strokeWidth, StrokeCap.Round)
                }
            }
            MapGlyph.Location,
            MapGlyph.Target -> {
                drawCircle(tint, radius = unit * 0.31f, style = stroke)
                drawCircle(tint, radius = unit * 0.10f, style = Fill)
                if (glyph == MapGlyph.Location) {
                    drawLine(tint, Offset(unit * 0.50f, unit * 0.05f), Offset(unit * 0.50f, unit * 0.20f), strokeWidth, StrokeCap.Round)
                    drawLine(tint, Offset(unit * 0.50f, unit * 0.80f), Offset(unit * 0.50f, unit * 0.95f), strokeWidth, StrokeCap.Round)
                    drawLine(tint, Offset(unit * 0.05f, unit * 0.50f), Offset(unit * 0.20f, unit * 0.50f), strokeWidth, StrokeCap.Round)
                    drawLine(tint, Offset(unit * 0.80f, unit * 0.50f), Offset(unit * 0.95f, unit * 0.50f), strokeWidth, StrokeCap.Round)
                }
            }
            MapGlyph.Route -> {
                val path = Path().apply {
                    moveTo(unit * 0.18f, unit * 0.78f)
                    cubicTo(unit * 0.26f, unit * 0.30f, unit * 0.62f, unit * 0.72f, unit * 0.80f, unit * 0.24f)
                }
                drawPath(path, tint, style = stroke)
                drawCircle(tint, unit * 0.10f, Offset(unit * 0.18f, unit * 0.78f), style = Fill)
                drawCircle(tint, unit * 0.10f, Offset(unit * 0.80f, unit * 0.24f), style = Fill)
            }
            MapGlyph.Navigation -> {
                val path = Path().apply {
                    moveTo(unit * 0.50f, unit * 0.08f)
                    lineTo(unit * 0.86f, unit * 0.86f)
                    lineTo(unit * 0.50f, unit * 0.68f)
                    lineTo(unit * 0.14f, unit * 0.86f)
                    close()
                }
                drawPath(path, tint, style = Fill)
            }
            MapGlyph.Map -> {
                val path = Path().apply {
                    moveTo(unit * 0.10f, unit * 0.24f)
                    lineTo(unit * 0.34f, unit * 0.12f)
                    lineTo(unit * 0.66f, unit * 0.24f)
                    lineTo(unit * 0.90f, unit * 0.12f)
                    lineTo(unit * 0.90f, unit * 0.76f)
                    lineTo(unit * 0.66f, unit * 0.88f)
                    lineTo(unit * 0.34f, unit * 0.76f)
                    lineTo(unit * 0.10f, unit * 0.88f)
                    close()
                }
                drawPath(path, tint, style = stroke)
                drawLine(tint, Offset(unit * 0.34f, unit * 0.12f), Offset(unit * 0.34f, unit * 0.76f), strokeWidth, StrokeCap.Round)
                drawLine(tint, Offset(unit * 0.66f, unit * 0.24f), Offset(unit * 0.66f, unit * 0.88f), strokeWidth, StrokeCap.Round)
            }
            MapGlyph.Voice -> {
                val speaker = Path().apply {
                    moveTo(unit * 0.16f, unit * 0.42f)
                    lineTo(unit * 0.34f, unit * 0.42f)
                    lineTo(unit * 0.54f, unit * 0.24f)
                    lineTo(unit * 0.54f, unit * 0.76f)
                    lineTo(unit * 0.34f, unit * 0.58f)
                    lineTo(unit * 0.16f, unit * 0.58f)
                    close()
                }
                drawPath(speaker, tint, style = Fill)
                drawArc(tint, -48f, 96f, false, Offset(unit * 0.48f, unit * 0.31f), Size(unit * 0.26f, unit * 0.38f), style = stroke)
                drawArc(tint, -48f, 96f, false, Offset(unit * 0.48f, unit * 0.20f), Size(unit * 0.42f, unit * 0.60f), style = stroke)
            }
            MapGlyph.Offline -> {
                drawArc(tint, 195f, 150f, false, Offset(unit * 0.12f, unit * 0.20f), Size(unit * 0.76f, unit * 0.50f), style = stroke)
                drawLine(tint, Offset(unit * 0.50f, unit * 0.48f), Offset(unit * 0.50f, unit * 0.86f), strokeWidth, StrokeCap.Round)
                drawLine(tint, Offset(unit * 0.35f, unit * 0.72f), Offset(unit * 0.50f, unit * 0.86f), strokeWidth, StrokeCap.Round)
                drawLine(tint, Offset(unit * 0.65f, unit * 0.72f), Offset(unit * 0.50f, unit * 0.86f), strokeWidth, StrokeCap.Round)
            }
            MapGlyph.Pin -> {
                val path = Path().apply {
                    moveTo(unit * 0.50f, unit * 0.94f)
                    cubicTo(unit * 0.38f, unit * 0.72f, unit * 0.20f, unit * 0.55f, unit * 0.20f, unit * 0.35f)
                    cubicTo(unit * 0.20f, unit * 0.15f, unit * 0.34f, unit * 0.06f, unit * 0.50f, unit * 0.06f)
                    cubicTo(unit * 0.66f, unit * 0.06f, unit * 0.80f, unit * 0.15f, unit * 0.80f, unit * 0.35f)
                    cubicTo(unit * 0.80f, unit * 0.55f, unit * 0.62f, unit * 0.72f, unit * 0.50f, unit * 0.94f)
                    close()
                }
                drawPath(path, tint, style = Fill)
                drawCircle(Color.White.copy(alpha = 0.94f), unit * 0.11f, Offset(unit * 0.50f, unit * 0.34f))
            }
            MapGlyph.Close -> {
                drawLine(tint, Offset(unit * 0.22f, unit * 0.22f), Offset(unit * 0.78f, unit * 0.78f), strokeWidth, StrokeCap.Round)
                drawLine(tint, Offset(unit * 0.78f, unit * 0.22f), Offset(unit * 0.22f, unit * 0.78f), strokeWidth, StrokeCap.Round)
            }
        }
    }
}

@Composable
internal fun poiAccentColor(category: PoiCategory): Color = when (category) {
    PoiCategory.General -> MaterialTheme.colorScheme.outline
    PoiCategory.Office -> MaterialTheme.colorScheme.primary
    PoiCategory.Warehouse -> MaterialTheme.colorScheme.tertiary
    PoiCategory.Customer -> MaterialTheme.colorScheme.secondary
    PoiCategory.Alert -> MaterialTheme.colorScheme.error
}
