package com.example.blocklens.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color


// Enum for managing font sizes
enum class TextSizeOption(val regular: TextUnit, val label: TextUnit, val title: TextUnit, val subtext: TextUnit) {
    Small(16.sp, 28.sp, 48.sp, 12.sp),
    Default(20.sp, 32.sp, 52.sp, 16.sp),
    Large(24.sp, 36.sp, 56.sp, 20.sp)
}

@Composable
fun getGradientBrush(colorBlindMode: ColorBlindMode): Brush {
    val defaultGradientColors = listOf(
        Color(0xFF002447), // Dark blue
        Color(0xFF0052A3)  // Lighter blue
    )

    val colorBlindGradient = when (colorBlindMode) {
        ColorBlindMode.Protanopia -> listOf(Color(0xFF182746), Color( 0xFF2151A0))  // Red-green colorblind
        ColorBlindMode.Deuteranopia -> listOf(Color(0xFF002947), Color(0xFF005693))  // Another red-green variant
        ColorBlindMode.Tritanopia -> listOf(Color(0xFF002D2F), Color(0xFF005D63))  // Blue-yellow colorblind
        else -> listOf(Color(0xFF002447), Color(0xFF0052A3))  // Default gradient (blue to light blue)
    }

    return Brush.verticalGradient(colors = colorBlindGradient)
}

@Composable
fun BlockLensTheme(
    colorBlindMode: ColorBlindMode = ColorBlindMode.Default,
    textSizeOption: TextSizeOption = TextSizeOption.Default,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(colorBlindMode)

    val customColorScheme = lightColorScheme(
        primary = colorScheme.mainColor,
        onPrimary = colorScheme.textColor,
        secondary = colorScheme.selectedBoxColor,
        onSecondary = colorScheme.textColor,
        background = colorScheme.backgroundColor,
        onBackground = colorScheme.textColor,
        surface = colorScheme.accentColor,
        onSurface = colorScheme.textColor
    )

    MaterialTheme(
        colorScheme = customColorScheme,
        typography = Typography.copy(
            headlineLarge = Typography.headlineLarge.copy(fontSize = textSizeOption.label),
            bodyLarge = Typography.bodyLarge.copy(fontSize = textSizeOption.regular)
        ),
        content = content
    )
}

