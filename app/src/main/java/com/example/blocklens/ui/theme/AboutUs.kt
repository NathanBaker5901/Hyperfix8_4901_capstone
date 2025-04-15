package com.example.blocklens.ui

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.blocklens.ui.theme.TextSizeOption
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

/**
 * AboutUsContent
 *
 * Description: Displays the "About Us" section of the Block Lens app using a vertically arranged
 * column of styled text components. This includes an introduction, list of key features, and
 * a detailed mission statement. The content is styled with the app’s current Material theme.
 *
 * @return This function does not return a value; it renders a UI layout composed of multiple
 *         text elements and spacers inside a column.
 */
@Composable
fun AboutUsContent(textSizeOption: TextSizeOption) {
    var teamPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        teamPhotoUri = uri
    }

    // Local-only font sizes
    val headingSize = 24.sp
    val nameSize = 18.sp
    val bioSize = 14.sp

    Column {
        Text(
            text = "Welcome to Block Lens!",
            fontSize = headingSize,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Block Lens is an innovative app designed to help users with color blindness " +
                    "better perceive and interact with the world around them. Our mission is to " +
                    "make technology accessible and inclusive for everyone.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Features",
            fontSize = headingSize,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "- Colorblind mode support (Protanopia, Deuteranopia, Tritanopia)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "- Adjustable font sizes for better readability",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "- Easy-to-use interface",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "- Voice feedback for detected image labels (Text-to-Speech)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "The Hyperfix8 Team",
            fontSize = headingSize,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Uploadable team photo placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (teamPhotoUri != null) {
                AsyncImage(
                    model = teamPhotoUri,
                    contentDescription = "Team Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "Tap to upload team photo",
                    fontSize = textSizeOption.subtext,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Team header
        Text(
            text = "The Team",
            fontSize = headingSize,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Team members
        @Composable
        fun teamMember(name: String, bio: String) {
            Text(
                text = name,
                fontSize = nameSize,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = bio.trimIndent(),
                fontSize = bioSize,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        teamMember("Abel Montoya", """
            Space for his bio.
            Add education, contributions, or fun facts here.
        """)

        teamMember("Andres Montoya", """
            Space for his bio.
            Highlight technical focus or specific project areas.
        """)

        teamMember("Carlos Garcia", """
            Space for his bio.
            Mention design, UI/UX, or research roles if relevant.
        """)

        teamMember("Nathan Baker", """
            Space for his bio.
            Could include leadership role, technical specialties, etc.
        """)

        teamMember("Joel Hunt", """
            Space for his bio.
            Add project vision, full-stack responsibilities, or personal motivation.
        """)

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Thank you for using Block Lens!",
            fontSize = headingSize,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
