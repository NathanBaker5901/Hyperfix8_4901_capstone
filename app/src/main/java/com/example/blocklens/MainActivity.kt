package com.example.blocklens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    private fun checkCameraPermission(onPermissionGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            onPermissionGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainPage { checkCameraPermission { navigateToCameraPage() } }
        }
    }

    private fun navigateToCameraPage() {
        // Logic to navigate to the camera page
    }
}

@Composable
fun MainPage(navigateToCamera: () -> Unit) {
    var currentPage by remember { mutableStateOf("main") }

    when (currentPage) {
        "main" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0033CC)), // Blue background
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Block Lens",
                        fontSize = 24.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    CircleButton("Go to settings page") { /* Navigate to settings */ }
                    CircleButton("Go to camera page") {
                        navigateToCamera()
                        currentPage = "camera"
                    }
                    CircleButton("Go to manual page") { /* Navigate to manual */ }
                }
            }
        }
        "camera" -> CameraPage(onBackToMain = { currentPage = "main" })
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
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CameraPage(onBackToMain: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0033CC)), // Blue background
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera Preview
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16 / 9f),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            // Unbind use cases before rebinding
                            cameraProvider.unbindAll()

                            // Preview Use Case
                            val preview = androidx.camera.core.Preview.Builder().build()
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            // Bind lifecycle and use cases
                            preview.setSurfaceProvider(previewView.surfaceProvider)
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (e: Exception) {
                            Log.e("CameraPage", "Camera initialization failed: ${e.message}")
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Capture Button (placeholder)
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .background(Color.Gray)
                    .size(120.dp, 50.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Capture", fontSize = 16.sp, color = Color.White)
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ClickableText(
                    text = AnnotatedString("Go to front page"),
                    onClick = { onBackToMain() },
                    modifier = Modifier
                        .background(Color.Gray)
                        .padding(16.dp),
                    style = TextStyle(color = Color.White, fontSize = 14.sp)
                )
                ClickableText(
                    text = AnnotatedString("Gallery"),
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .background(Color.Gray)
                        .padding(16.dp),
                    style = TextStyle(color = Color.White, fontSize = 14.sp)
                )
            }
        }
    }
}
