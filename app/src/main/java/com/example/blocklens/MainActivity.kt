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
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.Executors
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Paint as ComposePaint
import androidx.compose.ui.graphics.ImageBitmap
import android.graphics.BitmapFactory
import org.opencv.core.MatOfByte
import org.opencv.imgproc.Imgproc
import org.opencv.core.Size



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
            AppContent()
        }
        // Initialize OpenCV
        if (OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialized successfully")
        } else {
            Log.d("OpenCV", "OpenCV initialization failed")
        }
    }

    // Function to check if permissions are granted
    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
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
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }  // Changed to Bitmap

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

                        val imageAnalysis = ImageAnalysis.Builder()
                            .build()
                            .also {
                                it.setAnalyzer(
                                    Executors.newSingleThreadExecutor(),
                                    CustomImageAnalyzer { bitmap ->
                                        // Convert Bitmap to ImageBitmap on the main thread
                                        previewBitmap = bitmap
                                    }
                                )
                            }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture,
                            imageAnalysis
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
            // Overlay with boxes on the camera preview


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

// Custom Image Analyzer for processing camera frames
class CustomImageAnalyzer(
    private val onAnalysisResult: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        // Convert ImageProxy to Bitmap (for image processing with OpenCV)
        val bitmap = image.toBitmap()

        // Example OpenCV processing: Edge detection
        val processedBitmap = processWithOpenCV(bitmap)

        // Return the processed bitmap
        onAnalysisResult(processedBitmap)

        // Close the ImageProxy
        image.close()
    }

    // Extension function to convert ImageProxy to Bitmap
    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = this.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    // OpenCV edge detection (simplified)
    private fun processWithOpenCV(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Example OpenCV processing: Edge detection
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)
        Imgproc.GaussianBlur(mat, mat, org.opencv.core.Size(5.0, 5.0), 0.0)
        Imgproc.Canny(mat, mat, 100.0, 200.0)

        val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, resultBitmap)
        return resultBitmap
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
