package com.msa.professionalmap.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF1F5EFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE7FF),
    onPrimaryContainer = Color(0xFF071B47),
    inversePrimary = Color(0xFFB6C8FF),
    secondary = Color(0xFF007D68),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB8F1DF),
    onSecondaryContainer = Color(0xFF002019),
    tertiary = Color(0xFF9A5B00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB5),
    onTertiaryContainer = Color(0xFF301900),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF4F7FC),
    onBackground = Color(0xFF131A24),
    surface = Color(0xFFFBFCFF),
    onSurface = Color(0xFF131A24),
    surfaceVariant = Color(0xFFE1E7F2),
    onSurfaceVariant = Color(0xFF414956),
    surfaceTint = Color(0xFF1F5EFF),
    inverseSurface = Color(0xFF28313E),
    inverseOnSurface = Color(0xFFF1F3FA),
    outline = Color(0xFF717A88),
    outlineVariant = Color(0xFFC1C9D6),
    scrim = Color.Black,
    surfaceDim = Color(0xFFD5DAE4),
    surfaceBright = Color(0xFFFBFCFF),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F6FB),
    surfaceContainer = Color(0xFFEEF1F7),
    surfaceContainerHigh = Color(0xFFE8ECF3),
    surfaceContainerHighest = Color(0xFFE1E6EE),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFB6C8FF),
    onPrimary = Color(0xFF002D72),
    primaryContainer = Color(0xFF0645C4),
    onPrimaryContainer = Color(0xFFDDE7FF),
    inversePrimary = Color(0xFF1F5EFF),
    secondary = Color(0xFF82D5BD),
    onSecondary = Color(0xFF00382D),
    secondaryContainer = Color(0xFF005142),
    onSecondaryContainer = Color(0xFFB8F1DF),
    tertiary = Color(0xFFFFB95D),
    onTertiary = Color(0xFF512F00),
    tertiaryContainer = Color(0xFF744500),
    onTertiaryContainer = Color(0xFFFFDDB5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF07101E),
    onBackground = Color(0xFFE2E8F2),
    surface = Color(0xFF0B1422),
    onSurface = Color(0xFFE2E8F2),
    surfaceVariant = Color(0xFF414956),
    onSurfaceVariant = Color(0xFFC1C9D6),
    surfaceTint = Color(0xFFB6C8FF),
    inverseSurface = Color(0xFFE2E8F2),
    inverseOnSurface = Color(0xFF28313E),
    outline = Color(0xFF8B95A4),
    outlineVariant = Color(0xFF414956),
    scrim = Color.Black,
    surfaceDim = Color(0xFF07101E),
    surfaceBright = Color(0xFF303A49),
    surfaceContainerLowest = Color(0xFF030914),
    surfaceContainerLow = Color(0xFF101A29),
    surfaceContainer = Color(0xFF152030),
    surfaceContainerHigh = Color(0xFF1E2A3A),
    surfaceContainerHighest = Color(0xFF293545),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 29.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun ProfessionalMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
