package com.example.blocklens.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp


// Enum for managing font sizes
enum class TextSizeOption(val regular: TextUnit, val label: TextUnit) {
    Small(16.sp, 28.sp),
    Default(20.sp, 32.sp),
    Large(24.sp, 36.sp)
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

