package org.openauto.companion.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val CockpitBackground = Color(0xFF0C100E)
val CockpitSurface = Color(0xFF151B18)
val CockpitSurfaceRaised = Color(0xFF1B231F)
val CockpitOutline = Color(0xFF2A342E)
val CockpitBlue = Color(0xFF534FFD)
val CockpitOnBlue = Color(0xFFFFFFFF)
val CockpitText = Color(0xFFF4F7F3)
val CockpitMuted = Color(0xFF9BA69F)
val CockpitWarning = Color(0xFFF1B86C)

private val CockpitColorScheme = darkColorScheme(
    primary = CockpitBlue,
    onPrimary = CockpitOnBlue,
    primaryContainer = Color(0xFF2F2B72),
    onPrimaryContainer = Color(0xFFE4E2FF),
    secondary = CockpitBlue,
    onSecondary = CockpitOnBlue,
    tertiary = CockpitWarning,
    onTertiary = Color(0xFF2A1700),
    background = CockpitBackground,
    onBackground = CockpitText,
    surface = CockpitSurface,
    onSurface = CockpitText,
    surfaceVariant = CockpitSurfaceRaised,
    onSurfaceVariant = CockpitMuted,
    outline = CockpitOutline,
    outlineVariant = Color(0xFF222B26),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val CockpitShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

private val CockpitTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
        ),
        headlineLarge = headlineLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp,
        ),
        titleLarge = titleLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
        ),
        bodyLarge = bodyLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        bodyMedium = bodyMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        labelSmall = labelSmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            lineHeight = 16.sp,
        ),
    )
}

@Composable
fun OpenAutoCompanionTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.window?.let { window ->
                window.statusBarColor = CockpitBackground.toArgb()
                window.navigationBarColor = CockpitBackground.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = CockpitColorScheme,
        typography = CockpitTypography,
        shapes = CockpitShapes,
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
