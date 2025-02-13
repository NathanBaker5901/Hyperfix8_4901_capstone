package com.example.blocklens.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.blocklens.R
import com.example.blocklens.ui.theme.ColorBlindMode
import com.example.blocklens.ui.theme.getColorScheme
import com.example.blocklens.ui.theme.TextSizeOption

// CLass for custom font
val BebasNeueFont = FontFamily(
    Font(R.font.bebasneue_font, FontWeight.Normal)
)



// Set of Material typography styles to start with
val Typography = Typography(
    // Large text style for headings or labels
    headlineLarge = TextStyle(
        fontFamily = BebasNeueFont, // Replace with custom font if needed
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp
    ),
    // Medium text style for smaller headings
    headlineMedium = TextStyle(
        fontFamily = BebasNeueFont, // Replace with custom font if needed
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // Regular text style for body text
    bodyLarge = TextStyle(
        fontFamily = BebasNeueFont, // Replace with custom font if needed
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp
    ),
    // Small text style for secondary text or captions
    bodySmall = TextStyle(
        fontFamily = BebasNeueFont, // Replace with custom font if needed
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
