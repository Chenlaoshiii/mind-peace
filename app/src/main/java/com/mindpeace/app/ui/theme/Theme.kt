package com.mindpeace.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.data.Appearance
import com.mindpeace.app.data.ColorMode
import com.mindpeace.app.data.VisualStyle

val LocalVisualStyle = staticCompositionLocalOf { VisualStyle.MATERIAL_YOU }
val LocalDarkTheme = staticCompositionLocalOf { false }
val LocalPeaceBackdrop = staticCompositionLocalOf<Backdrop?> { null }

private val LightColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = SageContainer,
    onPrimaryContainer = Ink,
    secondary = Sage,
    onSecondary = Color.White,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE3EBE6),
    onSurfaceVariant = Mist,
    outline = Color(0xFFB5C4BE),
    surfaceContainerLow = Color(0xFFEEF4F1),
)

private val DarkColors = darkColorScheme(
    primary = SageDark,
    onPrimary = Color(0xFF0E1A16),
    primaryContainer = Color(0xFF3A5C52),
    onPrimaryContainer = Color(0xFFD5E8E0),
    secondary = SageDark,
    onSecondary = Color(0xFF0E1A16),
    background = Color(0xFF121917),
    onBackground = Color(0xFFE6EEEA),
    surface = Color(0xFF121917),
    onSurface = Color(0xFFE6EEEA),
    surfaceVariant = Color(0xFF2A3330),
    onSurfaceVariant = Color(0xFFC2CDC8),
    outline = Color(0xFFA8B8B2),
    outlineVariant = Color(0xFF8A9A94),
    surfaceContainerLow = Color(0xFF1A2220),
)

private val OrangeLight = lightColorScheme(
    primary = OrangeAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E0D6),
    onPrimaryContainer = OrangeInk,
    secondary = OrangeAccentDeep,
    onSecondary = Color.White,
    background = OrangePaper,
    onBackground = OrangeInk,
    surface = OrangePaperHi,
    onSurface = OrangeInk,
    surfaceVariant = OrangeHairline,
    onSurfaceVariant = Color(0xFF5C5A53),
    outline = OrangeMuted,
    surfaceContainerLow = OrangePaperHi,
    secondaryContainer = Color(0xFFF3E6DC),
    onSecondaryContainer = OrangeInk,
)

private val OrangeDark = darkColorScheme(
    primary = OrangeAccent,
    onPrimary = OrangeCharcoal,
    primaryContainer = Color(0xFF5C2E1E),
    onPrimaryContainer = Color(0xFFF3E0D6),
    secondary = OrangeAccentDeep,
    onSecondary = OrangeCharcoal,
    background = OrangeCharcoal,
    onBackground = Color(0xFFFAF9F5),
    surface = OrangeCharcoalHi,
    onSurface = Color(0xFFFAF9F5),
    surfaceVariant = Color(0xFF2C2A26),
    onSurfaceVariant = OrangeMuted,
    outline = Color(0xFF3D3B36),
    surfaceContainerLow = OrangeCharcoalHi,
    secondaryContainer = Color(0xFF3D2A22),
    onSecondaryContainer = Color(0xFFF3E0D6),
)

private val AppleLight = lightColorScheme(
    primary = AppleGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F5E1),
    onPrimaryContainer = Color(0xFF0B3B1C),
    secondary = AppleGreen,
    onSecondary = Color.White,
    background = AppleGrouped,
    onBackground = Color(0xFF000000),
    surface = AppleTable,
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = AppleLabel,
    outline = AppleHairline,
    surfaceContainerLow = AppleTable,
    secondaryContainer = AppleGrouped,
    onSecondaryContainer = Color(0xFF000000),
)

private val AppleDark = darkColorScheme(
    primary = AppleGreenDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0D3B1E),
    onPrimaryContainer = Color(0xFFCFF6D9),
    secondary = AppleGreenDark,
    onSecondary = Color.Black,
    background = AppleGroupedDark,
    onBackground = Color(0xFFF2F2F7),
    surface = AppleTableDark,
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = AppleLabel,
    outline = AppleHairlineDark,
    surfaceContainerLow = AppleTableDark,
    secondaryContainer = AppleTableDark,
    onSecondaryContainer = Color(0xFFF2F2F7),
)

@Composable
fun MindPeaceThemed(
    showAtmosphere: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MindPeaceApp
    val appearance = app.container.settings.appearance.collectAsStateWithLifecycle().value
    MindPeaceTheme(appearance = appearance, showAtmosphere = showAtmosphere, content = content)
}

@Composable
fun MindPeaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    appearance: Appearance? = null,
    showAtmosphere: Boolean = true,
    content: @Composable () -> Unit,
) {
    val resolvedDark = when (appearance?.colorMode) {
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
        ColorMode.SYSTEM, null -> darkTheme
    }
    val style = appearance?.style ?: VisualStyle.MATERIAL_YOU
    val context = LocalContext.current
    val colorScheme = when (style) {
        VisualStyle.MATERIAL_YOU -> when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (resolvedDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            resolvedDark -> DarkColors
            else -> LightColors
        }
        VisualStyle.ORANGE -> if (resolvedDark) OrangeDark else OrangeLight
        VisualStyle.APPLE -> if (resolvedDark) AppleDark else AppleLight
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !resolvedDark
                isAppearanceLightNavigationBars = !resolvedDark
            }
        }
    }

    val backdrop = rememberLayerBackdrop()

    CompositionLocalProvider(
        LocalVisualStyle provides style,
        LocalDarkTheme provides resolvedDark,
        LocalPeaceBackdrop provides backdrop,
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .background(
                        when {
                            style == VisualStyle.APPLE -> if (resolvedDark) AppleGroupedDark else AppleGrouped
                            else -> colorScheme.background
                        },
                    ),
            ) {
                if (style == VisualStyle.APPLE && showAtmosphere) {
                    AppleAtmosphere(Modifier.fillMaxSize(), dark = resolvedDark)
                }
            }
            MaterialTheme(
                colorScheme = colorScheme,
                typography = typographyFor(style),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (style == VisualStyle.APPLE) Color.Transparent else colorScheme.background,
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun peaceContainerColor(): Color {
    return if (LocalVisualStyle.current == VisualStyle.APPLE) Color.Transparent
    else MaterialTheme.colorScheme.background
}

@Composable
fun peaceSurfaceColor(): Color {
    return if (LocalVisualStyle.current == VisualStyle.APPLE) Color.Transparent
    else MaterialTheme.colorScheme.surface
}
