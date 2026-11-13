package rw.hafiyawe.store.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The palette is lifted from the web client's crystal.css so both clients match. */
object Brand {
    val Green = Color(0xFF14532D)
    val GreenDeep = Color(0xFF0B3A20)
    val GreenSoft = Color(0xFF2C7A50)
    val Gold = Color(0xFF96601E)
    val GoldLight = Color(0xFFE9BD7D)
    val Cream = Color(0xFFF8EDDB)

    val PageLight = Color(0xFFF6F5F0)
    val SurfaceLight = Color(0xFFFFFEFA)
    val SoftLight = Color(0xFFEEEFE8)
    val Ink = Color(0xFF20382E)
    val MutedLight = Color(0xFF616E64)
    val LineLight = Color(0xFFDCE2D8)

    val PageDark = Color(0xFF101B18)
    val SurfaceDark = Color(0xFF1A2B24)
    val SoftDark = Color(0xFF24372D)
    val PaperDark = Color(0xFFEDF2E9)
    val MutedDark = Color(0xFFB4C2B5)
    val LineDark = Color(0xFF3C5042)

    val Danger = Color(0xFFB3261E)
    val DangerDark = Color(0xFFFFB4AB)
    val Success = Color(0xFF1E6B45)
    val SuccessDark = Color(0xFF7FD1A5)
}

private val LightScheme = lightColorScheme(
    primary = Brand.Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EBDD),
    onPrimaryContainer = Brand.GreenDeep,
    secondary = Brand.Gold,
    onSecondary = Color.White,
    secondaryContainer = Brand.Cream,
    onSecondaryContainer = Color(0xFF5A3A11),
    tertiary = Brand.GreenSoft,
    onTertiary = Color.White,
    background = Brand.PageLight,
    onBackground = Brand.Ink,
    surface = Brand.SurfaceLight,
    onSurface = Brand.Ink,
    surfaceVariant = Brand.SoftLight,
    onSurfaceVariant = Brand.MutedLight,
    outline = Brand.LineLight,
    outlineVariant = Brand.LineLight,
    error = Brand.Danger,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkScheme = darkColorScheme(
    primary = Brand.GoldLight,
    onPrimary = Color(0xFF3A2A10),
    primaryContainer = Color(0xFF4A3A1C),
    onPrimaryContainer = Brand.Cream,
    secondary = Brand.GoldLight,
    onSecondary = Color(0xFF3A2A10),
    secondaryContainer = Color(0xFF3D3424),
    onSecondaryContainer = Brand.Cream,
    tertiary = Brand.SuccessDark,
    onTertiary = Color(0xFF0B2A1B),
    background = Brand.PageDark,
    onBackground = Brand.PaperDark,
    surface = Brand.SurfaceDark,
    onSurface = Brand.PaperDark,
    surfaceVariant = Brand.SoftDark,
    onSurfaceVariant = Brand.MutedDark,
    outline = Brand.LineDark,
    outlineVariant = Brand.LineDark,
    error = Brand.DangerDark,
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun HafiYaweTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/** Gold accent that stays readable on both themes. */
val accentColor: Color
    @Composable get() = if (LocalDarkTheme.current) Brand.GoldLight else Brand.Gold
