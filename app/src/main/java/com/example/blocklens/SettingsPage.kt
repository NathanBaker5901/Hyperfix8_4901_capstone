package com.example.blocklens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.ArrowDropDown
import com.example.blocklens.ui.AboutUsContent
import com.example.blocklens.ui.theme.ColorBlindMode
import com.example.blocklens.ui.theme.TextSizeOption
import com.example.blocklens.ui.theme.BlockLensTheme
import com.example.blocklens.ui.theme.getGradientBrush


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    textSizeOption: TextSizeOption,
    colorBlindMode: ColorBlindMode,
    onTextSizeChange: (TextSizeOption) -> Unit,
    onColorBlindModeChange: (ColorBlindMode) -> Unit,
    onBack: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(getGradientBrush(colorBlindMode)) // Apply the gradient
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()) // Enable vertical scrolling
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page title
            Text(
                "Settings",
                fontSize = textSizeOption.title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))

            //font size section
            Text(
                "Font Size",
                fontSize = textSizeOption.label,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onTextSizeChange(TextSizeOption.Small) }) {
                    Text(
                        "Small",
                        fontSize = textSizeOption.regular,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(onClick = { onTextSizeChange(TextSizeOption.Default) }) {
                    Text(
                        "Default",
                        fontSize = textSizeOption.regular,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(onClick = { onTextSizeChange(TextSizeOption.Large) }) {
                    Text(
                        "Large",
                        fontSize = textSizeOption.regular,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            //Dropdown menu for colorblindmode
            Text(
                "Color Blind Mode",
                fontSize = textSizeOption.label,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = when (colorBlindMode) {
                        ColorBlindMode.Default -> "Default"
                        ColorBlindMode.Protanopia -> "Protanopia"
                        ColorBlindMode.Deuteranopia -> "Deuteranopia"
                        ColorBlindMode.Tritanopia -> "Tritanopia"
                    },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    //label = { Text("Color Blind Mode") },
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    },
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surface, // Background color
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, // Text color when focused
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface, // Text color when not focused
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary, // Underline color when focused
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface, // Underline color when not focused
                        disabledIndicatorColor = Color.Transparent // Remove underline when disabled
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Default", fontSize = textSizeOption.regular) },
                        onClick = {
                            onColorBlindModeChange(ColorBlindMode.Default)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Protanopia", fontSize = textSizeOption.regular) },
                        onClick = {
                            onColorBlindModeChange(ColorBlindMode.Protanopia)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Deuteranopia", fontSize = textSizeOption.regular) },
                        onClick = {
                            onColorBlindModeChange(ColorBlindMode.Deuteranopia)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tritanopia", fontSize = textSizeOption.regular) },
                        onClick = {
                            onColorBlindModeChange(ColorBlindMode.Tritanopia)
                            expanded = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "About Us",
                fontSize = textSizeOption.label,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            AboutUsContent() //Calling AboutUs.kt function text

        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp) // Controls how much space back button takes
        ) {
            //backbutton
            Button(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Back", fontSize = textSizeOption.regular, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

