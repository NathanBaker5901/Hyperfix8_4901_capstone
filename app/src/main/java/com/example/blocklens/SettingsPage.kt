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
import com.example.blocklens.ui.theme.getGradientBrush
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Shape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    textSizeOption: TextSizeOption,
    colorBlindMode: ColorBlindMode,
    onTextSizeChange: (TextSizeOption) -> Unit,
    onColorBlindModeChange: (ColorBlindMode) -> Unit,
    onBack: () -> Unit,
    voiceFeedbackEnabled: Boolean,
    onToggleVoiceFeedback: (Boolean) -> Unit,
    tts: TextToSpeech?
) {
    var expanded by remember { mutableStateOf(false) }

    fun speak(text: String) {
        if (voiceFeedbackEnabled) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(getGradientBrush(colorBlindMode))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Settings",
                fontSize = textSizeOption.title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Font Size Section
            Text(
                "Font Size",
                fontSize = textSizeOption.label,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpeakButton(
                    speakLabel = "Small",
                    displayLabel = "Small",
                    tts = tts,
                    voiceFeedbackEnabled = voiceFeedbackEnabled,
                    onClickAction = { onTextSizeChange(TextSizeOption.Small) }
                )
                SpeakButton(
                    speakLabel = "Default",
                    displayLabel = "Default",
                    tts = tts,
                    voiceFeedbackEnabled = voiceFeedbackEnabled,
                    onClickAction = { onTextSizeChange(TextSizeOption.Default) }
                )
                SpeakButton(
                    speakLabel = "Large",
                    displayLabel = "Large",
                    tts = tts,
                    voiceFeedbackEnabled = voiceFeedbackEnabled,
                    onClickAction = { onTextSizeChange(TextSizeOption.Large) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Color Blind Mode Section
            Text(
                "Color Blind Mode",
                fontSize = textSizeOption.label,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

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
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    },
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                        disabledIndicatorColor = Color.Transparent
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
                            speak("Default Color Mode")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Protanopia", fontSize = textSizeOption.regular) },
                        onClick = {
                            onColorBlindModeChange(ColorBlindMode.Protanopia)
                            expanded = false
                            speak("Protanopia Color Mode")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Deuteranopia", fontSize = textSizeOption.regular) },
                        onClick = {
                            onColorBlindModeChange(ColorBlindMode.Deuteranopia)
                            expanded = false
                            speak("Deuteranopia Color Mode")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tritanopia", fontSize = textSizeOption.regular) },
                        onClick = {
                            onColorBlindModeChange(ColorBlindMode.Tritanopia)
                            expanded = false
                            speak("Tritanopia Color Mode")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Voice Feedback Toggle
            Text(
                text = "Voice Feedback",
                fontSize = textSizeOption.label,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                onClick = {
                    if (!voiceFeedbackEnabled) {
                        // Voice Feedback was OFF, and we're turning it ON
                        tts?.speak("Voice Feedback Enabled", TextToSpeech.QUEUE_FLUSH, null, null)
                    } else {
                        // Voice Feedback was ON, and we're turning it OFF
                        tts?.speak("Voice Feedback Disabled", TextToSpeech.QUEUE_FLUSH, null, null)
                    }

                    onToggleVoiceFeedback(!voiceFeedbackEnabled)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (voiceFeedbackEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    contentColor = if (voiceFeedbackEnabled)
                        Color.White
                    else
                        Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .defaultMinSize(minWidth = 100.dp)
            ) {
                Text(
                    text = if (voiceFeedbackEnabled) "ON" else "OFF",
                    fontSize = textSizeOption.subtext,
                    fontWeight = FontWeight.Bold
                )
            }



            Spacer(modifier = Modifier.height(24.dp))

            // About Us Section
            Text(
                text = "About Us",
                fontSize = textSizeOption.label,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            AboutUsContent(textSizeOption)
        }

        // Back Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            SpeakButton(
                speakLabel = "Back to Home Page",
                displayLabel = "Back",
                tts = tts,
                voiceFeedbackEnabled = voiceFeedbackEnabled,
                onClickAction = { onBack() },
                modifier = Modifier.align(Alignment.BottomCenter),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}


@Composable
fun SpeakButton(
    speakLabel: String,
    displayLabel: String,
    tts: TextToSpeech?,
    voiceFeedbackEnabled: Boolean,
    onClickAction: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(
        onClick = {
            if (voiceFeedbackEnabled) {
                tts?.speak(speakLabel, TextToSpeech.QUEUE_FLUSH, null, null)
            }
            onClickAction()
        },
        modifier = modifier,
        colors = colors
    ) {
        Text(displayLabel)
    }
}