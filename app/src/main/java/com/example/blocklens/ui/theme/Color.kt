package com.example.blocklens.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ColorScheme
 *
 * Description: A data class that defines a complete set of colors used throughout the app’s UI.
 * Each instance of this class represents a cohesive color theme, often tailored to a specific
 * type of color blindness for better accessibility.
 *
 * Used in:
 * - [getColorScheme]: Returns an instance of this class based on the selected [ColorBlindMode].
 * - [BlockLensTheme]: Applies the appropriate colors from this class to the Material theme.
 *
 * @property mainColor The primary color used for key UI elements.
 * @property selectedBoxColor The color used to indicate selected components or items.
 * @property accentColor A secondary color used for visual accents or unselected elements.
 * @property backgroundColor The background color of the app's main surfaces.
 * @property highlightBoxColor A highlight color used to draw attention (e.g., to active or focused elements).
 * @property textColor The default color used for text across the UI.
 * @property borderColor The color used for borders, outlines, or dividers.
 */
data class ColorScheme(
    val mainColor: Color,
    val selectedBoxColor: Color,
    val accentColor: Color,
    val backgroundColor: Color,
    val highlightBoxColor: Color,
    val textColor: Color,
    val borderColor: Color
)

/**
 * ColorBlindMode
 *
 * Description: Enum class representing the supported types of color blindness modes within the app.
 * These modes are used to dynamically adjust the app's color scheme to improve visual accessibility.
 *
 * Used in:
 * - [getColorScheme]: Determines which color palette to return based on the selected mode.
 * - [BlockLensTheme]: Applies the appropriate color scheme to the Material theme based on this mode.
 * - [getGradientBrush]: Adjusts gradient colors for improved accessibility depending on the mode.
 *
 * Enum Values:
 * @property Default Standard color mode with no colorblind-specific adjustments.
 * @property Protanopia Red-green color blindness (difficulty distinguishing red hues).
 * @property Deuteranopia Another form of red-green color blindness (difficulty distinguishing green hues).
 * @property Tritanopia Blue-yellow color blindness (difficulty distinguishing blue/yellow hues).
 */
enum class ColorBlindMode {
    Default, Protanopia, Deuteranopia, Tritanopia
}

/**
 * getColorScheme
 *
 * Description: Returns a color scheme tailored to a specific type of color blindness. Each scheme is
 * designed to enhance visual accessibility by providing distinct colors for UI elements such as
 * backgrounds, highlights, borders, and text.
 *
 * @param mode The selected color blindness mode (e.g., Default, Protanopia, Deuteranopia, Tritanopia).
 *             Determines which set of colors will be returned.
 * @return A [ColorScheme] object containing color values for main elements, background, highlights, text, and borders,
 *         customized for the specified color blindness mode.
 */
fun getColorScheme(mode: ColorBlindMode): ColorScheme {
    return when (mode) {
        ColorBlindMode.Default -> ColorScheme(
            mainColor = Color(0xFF0052A3),         // Main blue color
            selectedBoxColor = Color(0xFF0051A3), // Selected box color
            accentColor = Color(0xFF002447),      // Accent/non-selected color
            backgroundColor = Color(0xFF00407E),  // Background color
            highlightBoxColor = Color(0xFFFF0000),// Highlight box color
            textColor = Color(0xFFFFFFFF),        // White text color
            borderColor = Color(0xFF000000)       // Black border color
        )
        ColorBlindMode.Protanopia -> ColorScheme(
            mainColor = Color(0xFF24509F),
            selectedBoxColor = Color(0xFF21509F),
            accentColor = Color(0xFF182746),
            backgroundColor = Color(0xFF213F7B),
            highlightBoxColor = Color(0xFF8E7D1E),
            textColor = Color(0xFFFFFFFF),
            borderColor = Color(0xFF000000)
        )
        ColorBlindMode.Deuteranopia -> ColorScheme(
            mainColor = Color(0xFF005693),
            selectedBoxColor = Color(0xFF005592),
            accentColor = Color(0xFF002947),
            backgroundColor = Color(0xFF004475),
            highlightBoxColor = Color(0xFFA17800),
            textColor = Color(0xFFFFFFFF),
            borderColor = Color(0xFF000000)
        )
        ColorBlindMode.Tritanopia -> ColorScheme(
            mainColor = Color(0xFF005D63),
            selectedBoxColor = Color(0xFF005C62),
            accentColor = Color(0xFF002D2F),
            backgroundColor = Color(0xFF004A4E),
            highlightBoxColor = Color(0xFFFD1700),
            textColor = Color(0xFFFFFFFF),
            borderColor = Color(0xFF000000)
        )
    }
}
