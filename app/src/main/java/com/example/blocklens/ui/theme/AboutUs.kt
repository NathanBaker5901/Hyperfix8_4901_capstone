package com.example.blocklens.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
fun AboutUsContent() {
    Column {
        Text(
            "Welcome to Block Lens!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Block Lens is an innovative app designed to help users with color blindness " +
                    "better perceive and interact with the world around them. Our mission is to " +
                    "make technology accessible and inclusive for everyone.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Features:",
            style = MaterialTheme.typography.bodyLarge,
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
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Lorem ipsum odor amet, consectetuer adipiscing elit. Ad bibendum vulputate dis montes sagittis nascetur; fames nunc. " +
                    "Mi dapibus nisl aliquam vestibulum tortor lorem nisi lectus. Sed mollis mi; rutrum quis morbi integer maecenas. " +
                    "Nam mi arcu faucibus morbi lacinia netus ex condimentum ipsum? " +
                    "Eleifend mi nisl faucibus vitae purus odio bibendum mi tempor. Cras sagittis libero consequat volutpat class neque maecenas nostra? " +
                    "Ligula porttitor iaculis mattis enim sem venenatis euismod felis nunc. " +
                    "Ultricies platea rhoncus phasellus eleifend laoreet hendrerit molestie integer aptent.\n" +
                    "\n" +
                    "Justo ac habitasse odio amet nec faucibus. Maximus cras aliquam, dui tincidunt scelerisque nullam. " +
                    "Risus habitant lacus vestibulum tellus integer. " +
                    "Bibendum hendrerit consequat donec fames euismod ultricies parturient elit. " +
                    "Montes facilisis ligula urna torquent cras cursus. Aenean netus egestas praesent hendrerit vivamus nisl aenean. " +
                    "Lacus scelerisque vulputate arcu imperdiet sed.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Thank you for using Block Lens!",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}