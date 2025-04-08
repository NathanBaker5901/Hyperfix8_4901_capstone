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


/**
 * TextSizeOption
 *
 * Description: Enum class representing different text size presets for improving readability and
 * supporting accessibility preferences. Each option provides specific font sizes for various text
 * roles such as regular body text, labels, titles, and subtext.
 *
 * Used in:
 * - [BlockLensTheme]: Applies selected text size values to the app’s Material theme typography.
 *
 * @property regular The base font size for standard body text.
 * @property label The font size typically used for labels or headings.
 * @property title The largest font size used for prominent titles or banners.
 * @property subtext The smallest font size used for supporting or supplementary information.
 *
 * Enum Values:
 * @property Small A compact text preset with minimal spacing.
 * @property Default The standard text size preset for general use.
 * @property Large A larger text preset for improved readability and accessibility.
 */
enum class TextSizeOption(val regular: TextUnit, val label: TextUnit, val title: TextUnit, val subtext: TextUnit) {
    Small(16.sp, 28.sp, 48.sp, 12.sp),
    Default(20.sp, 32.sp, 52.sp, 16.sp),
    Large(24.sp, 36.sp, 56.sp, 20.sp)
}

/**
 * getGradientBrush
 *
 * Description: creates a gradient that can be called for
 * covering elements on the app
 *
 * @Composable: represents this is a Jetpack Compose UI component
 *
 * @param colorBlindMode:ColorBlindMode: custom class that specifies which color
 *      blindness the user has selected. Used to customize color palette to be
 *      more visually accessible.
 *
 * @return Brush: vertical gradient defined by which ColorBlindMode is called
 */
@Composable
fun getGradientBrush(colorBlindMode: ColorBlindMode): Brush {

    //defaultGradientColors creates a default blue toned gradient by default
    val defaultGradientColors = listOf(
        Color(0xFF002447), // Dark blue
        Color(0xFF0052A3)  // Lighter blue
    )

    //when colorBlindMode = Protanopia, Deuteranopia, or Tritanopia
    //defines colorBlindGradient to matching visually altered colors as Brush
    val colorBlindGradient = when (colorBlindMode) {
        ColorBlindMode.Protanopia -> listOf(Color(0xFF182746), Color( 0xFF2151A0))  // Red-green colorblind
        ColorBlindMode.Deuteranopia -> listOf(Color(0xFF002947), Color(0xFF005693))  // Another red-green variant
        ColorBlindMode.Tritanopia -> listOf(Color(0xFF002D2F), Color(0xFF005D63))  // Blue-yellow colorblind
        else -> listOf(Color(0xFF002447), Color(0xFF0052A3))  // Default gradient (blue to light blue)
    }
    return Brush.verticalGradient(colors = colorBlindGradient)
}

/**
 * BlockLensTheme
 *
 * Description: sets a custom Material 3 theme that adepts to user accessibility
 * preferences
 *
 * @param colorBlindMode The selected color blindness mode (e.g., Default, Protanopia, Deuteranopia, Tritanopia).
 *                       Used to generate a visually accessible color scheme.
 * @param textSizeOption The selected text size option (e.g., Default, Large, ExtraLarge).
 *                       Used to adjust the font sizes of headline and body text.
 * @param content A composable lambda that defines the UI content to be wrapped in the custom theme.
 * @return This function does not return a value; it applies a theme contextually to the given composable content.
 */
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

