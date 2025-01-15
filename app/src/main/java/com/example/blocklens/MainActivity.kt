package com.example.blocklens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import java.io.File
import com.example.blocklens.ui.theme.ColorBlindMode
import com.example.blocklens.ui.theme.TextSizeOption
import com.example.blocklens.ui.theme.BlockLensTheme
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.activity.compose.rememberLauncherForActivityResult




class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlockLensApp()
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
                onNavigateToSettings = { currentPage = "settings" },
                onNavigateToCamera = { currentPage = "camera" },
                onNavigateToGallery = { currentPage = "gallery" }
            )

            "settings" -> SettingsPage(
                textSizeOption = textSizeOption,
                colorBlindMode = colorBlindMode,
                onTextSizeChange = { textSizeOption = it },
                onColorBlindModeChange = { colorBlindMode = it },
                onBack = { currentPage = "landing" }
            )

            "camera" -> CameraPage(
                onBack = { currentPage = "landing" },
                onOpenGallery = checkGalleryPermission
            )

            "gallery" -> GalleryPage(
                onBack = {
                    currentPage = "landing"
                    selectedImageUri = null // Reset the URI
            },
            selectedImageUri = selectedImageUri
            )
        }

        // Show the pop-up when an image is selected
        capturedImageUri?.let { uri ->
            ImagePopUp(uri) { capturedImageUri = null }
        }
    }
}

@Composable
fun CameraPage(onBack: () -> Unit, onOpenGallery: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { androidx.camera.core.ImageCapture.Builder().build() }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        val preview = androidx.camera.core.Preview.Builder().build()
                        preview.surfaceProvider = previewView.surfaceProvider

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Back",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { onBack() }
                )
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable {
                            val photoFile = File(
                                context.cacheDir,
                                "captured_image_${System.currentTimeMillis()}.jpg"
                            )
                            val outputOptions = androidx.camera.core.ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: androidx.camera.core.ImageCapture.OutputFileResults) {
                                        capturedImageUri = Uri.fromFile(photoFile)
                                    }

                                    override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                                        Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
                                        Log.e("CameraPage", "Image capture failed", exception)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("●", fontSize = 40.sp, color = MaterialTheme.colorScheme.onPrimary)
                }

                Text(
                    "Gallery",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { onOpenGallery() }
                )
            }
        }
    }

    capturedImageUri?.let { uri ->
        ImagePopUp(uri) { capturedImageUri = null }
    }
}

@Composable
fun ImagePopUp(imageUri: Uri, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "X",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { onClose() }
                )
            }
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )
            Button(
                onClick = { /* Analyze logic placeholder */ },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Analyze")
            }
        }
    }
}

@Composable
fun GalleryPage(onBack: () -> Unit, selectedImageUri: Uri?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Gallery Page", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        selectedImageUri?.let{
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
fun LandingPage(
    onNavigateToSettings: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Block Lens",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToCamera) {
            Text("Camera", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToGallery) {
            Text("Gallery", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToSettings) {
            Text("Settings", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

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
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Page title
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))

        //font size section
        Text("Font Size", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onTextSizeChange(TextSizeOption.Small) }) { Text("Small") }
            Button(onClick = { onTextSizeChange(TextSizeOption.Default) }) { Text("Default") }
            Button(onClick = { onTextSizeChange(TextSizeOption.Large) }) { Text("Large") }
        }
        Spacer(modifier = Modifier.height(24.dp))

        //Dropdown menu for colorblindmode
        Text(
            "Color Blind Mode",
            style = MaterialTheme.typography.bodyLarge,
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
                    text = { Text("Default") },
                    onClick = {
                        onColorBlindModeChange(ColorBlindMode.Default)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Protanopia") },
                    onClick = {
                        onColorBlindModeChange(ColorBlindMode.Protanopia)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Deuteranopia") },
                    onClick = {
                        onColorBlindModeChange(ColorBlindMode.Deuteranopia)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Tritanopia") },
                    onClick = {
                        onColorBlindModeChange(ColorBlindMode.Tritanopia)
                        expanded = false
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        //backbutton
        Button(onClick = onBack) { Text("Back") }
    }
}
