package com.example.blocklens.ui.theme

import androidx.compose.ui.graphics.Color

// Data class for storing color schemes
data class ColorScheme(
    val mainColor: Color,
    val selectedBoxColor: Color,
    val accentColor: Color,
    val backgroundColor: Color,
    val highlightBoxColor: Color,
    val textColor: Color,
    val borderColor: Color
)

// Enum for colorblind modes
enum class ColorBlindMode {
    Default, Protanopia, Deuteranopia, Tritanopia
}

// Define color schemes for each mode
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
            mainColor = Color(0xFF2451A0),
            selectedBoxColor = Color(0xFF2451A0),
            accentColor = Color(0xFF2151A0),
            backgroundColor = Color(0xFF182747),
            highlightBoxColor = Color(0xFF8F7E1E),
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
