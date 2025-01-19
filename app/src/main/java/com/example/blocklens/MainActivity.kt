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
//import androidx.compose.material3.*
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppContent()
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentPage by remember { mutableStateOf("main") }

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

        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Gallery permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Page Navigation
    when (currentPage) {
        "main" -> MainPage(
            onOpenManual = { currentPage = "manual" },
            onOpenCamera = { currentPage = "camera" },
            onOpenSettings = { currentPage = "settings" }
        )
        "camera" -> CameraPage(
            onBack = { currentPage = "main" },
            onOpenGallery = checkGalleryPermission
        )
        "manual" -> ManualPage { currentPage = "main" }
        "settings" -> SettingsPage { currentPage = "main" }
    }

    // Show the pop-up when an image is selected
    selectedImageUri?.let { uri ->
        ImagePopUp(uri) { selectedImageUri = null }
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
            // Camera Preview
            AndroidView(
                modifier = Modifier
                    .weight(1f) // Takes up most of the screen
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

            // Bottom Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Back",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { onBack() }
                )

                // Capture Button
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color.White, CircleShape)
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
                    Text("●", fontSize = 40.sp, color = Color.Red)
                }

                Text(
                    "Gallery",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { onOpenGallery() }
                )
            }
        }
    }

    // Show captured image pop-up
    capturedImageUri?.let { uri ->
        ImagePopUp(uri) { capturedImageUri = null }
    }
}



@Composable
fun ImagePopUp(imageUri: Uri, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "X",
                    color = Color.Red,
                    fontSize = 18.sp,
                    modifier = Modifier.clickable { onClose() }
                )
            }
            // Display Image
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )
            // Analyze Button
            Button(
                onClick = { /* Placeholder for analyze logic */ },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Analyze")
            }
        }
    }
}

@Composable
fun MainPage(onOpenManual: () -> Unit, onOpenCamera: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0033CC)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Block Lens", fontSize = 24.sp, color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))
        CircleButton("Manual") { onOpenManual() }
        CircleButton("Camera") { onOpenCamera() }
        CircleButton("Settings") { onOpenSettings() }
    }
}

@Composable
fun CircleButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(Color.Black, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 16.sp, color = Color.White)
    }
}

@Composable
fun ManualPage(onBack: () -> Unit) = PagePlaceholder("Manual Page", onBack)
@Composable
fun SettingsPage(onBack: () -> Unit) = PagePlaceholder("Settings Page", onBack)

@Composable
fun PagePlaceholder(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 20.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))
        CircleButton("Back") { onBack() }
    }
}
