package com.example.blocklens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.blocklens.ui.theme.BlockLensTheme
import com.example.blocklens.ui.theme.ColorBlindMode
import com.example.blocklens.ui.theme.TextSizeOption
import com.example.blocklens.ui.theme.getColorScheme
import com.example.blocklens.ui.theme.getGradientBrush


const val TAG = "BlockLens TEST"

enum class TextSizeOption {
    Small, Default, Large
}

class MainActivity : ComponentActivity() {

    // List permission request for runtime permissions
    private val requiredPermissions = mutableListOf(
        Manifest.permission.CAMERA
    ).apply{
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    // Permissions launcher on runtime
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.forEach { (permission, isGranted) ->
                if (!isGranted) {
                    Toast.makeText(this, "Permission denied: $permission", Toast.LENGTH_LONG).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check/request permissions
        if (!hasAllPermissions()) {
            requestPermissionsLauncher.launch(requiredPermissions.toTypedArray())
        }

        setContent {
            BlockLensApp()
        }
    }

    // Function *TEST* to check if permissions are granted
    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            Log.d(TAG, "Checking runtime permission: $it")
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

}

@Composable
fun BlockLensApp() {
    var textSizeOption by remember { mutableStateOf(TextSizeOption.Default) }
    var colorBlindMode by remember { mutableStateOf(ColorBlindMode.Default) }
    var currentPage by remember { mutableStateOf("landing") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var openGalleryShortcut by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Image picker
    val pickImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            selectedImageUri = uri
        }

    // Permission check
    val checkGalleryPermission: () -> Unit = {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            pickImageLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Gallery permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    BlockLensTheme(
        colorBlindMode = colorBlindMode,
        textSizeOption = textSizeOption
    ) {
        when (currentPage) {
            "landing" -> LandingPage(
                textSizeOption = textSizeOption,
                colorBlindMode = colorBlindMode,
                onNavigateToSettings = { currentPage = "settings" },
                onNavigateToCamera = { currentPage = "camera" },
                onNavigateToGallery = {
                    openGalleryShortcut = true
                    currentPage = "camera"
                }
            )

            "settings" -> SettingsPage(
                textSizeOption = textSizeOption,
                colorBlindMode = colorBlindMode,
                onTextSizeChange = { textSizeOption = it },
                onColorBlindModeChange = { colorBlindMode = it },
                onBack = { currentPage = "landing" }
            )

            "camera" -> CameraPage(
                onBack = {
                    currentPage = "landing"
                    openGalleryShortcut = false // Reset the shortcut state
                },
                onOpenGallery = {
                    checkGalleryPermission()
                },
                openGalleryShortcut = openGalleryShortcut
            )

            "gallery" -> GalleryPage(
                onBack = {
                    currentPage = "landing"
                    selectedImageUri = null // Reset the URI
            },
            selectedImageUri = selectedImageUri
            )
        }

        // Show the pop-up for either captured or selected images
        val imageUriForPopUp = capturedImageUri ?: selectedImageUri
        imageUriForPopUp?.let { uri ->
            ImagePopUp(uri) {
                capturedImageUri = null
                selectedImageUri = null
            }
        }
    }
}

@Composable
fun LandingPage(
    textSizeOption: TextSizeOption,
    colorBlindMode: ColorBlindMode,
    onNavigateToSettings: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToGallery: () -> Unit
) {

    // sets the colorblind modes
    val colorScheme = getColorScheme(colorBlindMode)

    //setting text sizes
    val fontSize = when (textSizeOption) {
        TextSizeOption.Small -> 32.sp  // Small text size
        TextSizeOption.Default -> 48.sp // Default text size
        TextSizeOption.Large -> 64.sp  // Large text size
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getGradientBrush(colorBlindMode)) // Apply the gradient
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center, // Center everything
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "Block Lens",
                fontSize = textSizeOption.title,
                style = MaterialTheme.typography.headlineLarge,
                //style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold),
                //color = colorScheme?.textColor ?: Color(0xFFFFA500) // Orange color
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(48.dp)) // Space below the title


            // Gallery Icon (Top Center)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onNavigateToGallery,
                    modifier = Modifier.size(96.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = "Gallery",
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.White
                    )
                }
                Text(
                    "Gallery",
                    fontSize = textSizeOption.subtext,
                    //style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(64.dp)) // Adjust spacing between Gallery & bottom icons

            // Bottom row containing Camera and Settings icons
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.7f), // Keep them closer to the center
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Settings Icon (Bottom Left)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(96.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.White //Adjustable Icon color that is overwritten by colorscheme
                        )
                    }
                    Text(
                        "Settings",
                        fontSize = textSizeOption.subtext,
                        //style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                        color = Color.White
                    )
                }

                // Camera Icon (Bottom Right)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onNavigateToCamera,
                        modifier = Modifier.size(96.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Camera",
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.White
                        )
                    }
                    Text(
                        "Camera",
                        fontSize = textSizeOption.subtext,
                        //style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                        color = Color.White
                    )
                }

            }

        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "© 2024 HyperFix8 | Bricked Up",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFF000000)
                )
            )
        }
    }
}