package com.msa.professionalmap.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0057C2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A42),
    secondary = Color(0xFF246A50),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8ECD9),
    onSecondaryContainer = Color(0xFF062115),
    tertiary = Color(0xFF8D5A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB1),
    onTertiaryContainer = Color(0xFF2D1600),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF171B22),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF171B22),
    surfaceVariant = Color(0xFFE1E6F1),
    onSurfaceVariant = Color(0xFF454A55),
    outline = Color(0xFF757B87),
    outlineVariant = Color(0xFFC5CAD6),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFADC6FF),
    onPrimary = Color(0xFF002E6D),
    primaryContainer = Color(0xFF00459A),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFACD0BD),
    onSecondary = Color(0xFF103727),
    secondaryContainer = Color(0xFF0E4F38),
    onSecondaryContainer = Color(0xFFC8ECD9),
    tertiary = Color(0xFFFFB955),
    onTertiary = Color(0xFF4B2800),
    tertiaryContainer = Color(0xFF6C4100),
    onTertiaryContainer = Color(0xFFFFDDB1),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF10141B),
    onBackground = Color(0xFFE2E6EF),
    surface = Color(0xFF10141B),
    onSurface = Color(0xFFE2E6EF),
    surfaceVariant = Color(0xFF454A55),
    onSurfaceVariant = Color(0xFFC5CAD6),
    outline = Color(0xFF8F95A0),
    outlineVariant = Color(0xFF454A55),
)


private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ProfessionalMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = AppShapes,
        content = content,
    )
}
