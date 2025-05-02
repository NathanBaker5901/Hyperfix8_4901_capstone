package com.example.blocklens


import android.R.attr.orientation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.random.Random
import android.speech.tts.TextToSpeech


import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

import android.util.Log
import android.widget.Toast

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.io.InputStream
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.graphics.scaleMatrix

import com.example.blocklens.ui.theme.ColorBlindMode
import com.example.blocklens.ui.theme.TextSizeOption
import com.example.blocklens.ui.theme.BlockLensTheme

// mlkit libraries need to look into live camera
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.objects.DetectedObject
import kotlinx.coroutines.selects.select
import java.io.IOException
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.delay
import androidx.core.content.edit

/**
 * CameraPage
 *
 * Description: Main UI for capturing images using the device's back camera, launching the gallery,
 * detecting LEGO bricks, and displaying detection results via popups. Supports voice feedback,
 * haptic feedback, and a one-time hint overlay for first-time users.
 *
 * @Composable: Indicates this is a Jetpack Compose UI component.
 *
 * @param onBack: () -> Unit – called when the user taps the back button.
 *
 * @param onOpenGallery: () -> Unit – launches the gallery picker or shortcut for selecting images.
 *
 * @param openGalleryShortcut: Boolean – when true, triggers gallery open on load (used for shortcuts).
 *
 * @param selectedImageUri: Uri? – URI of the image selected from the gallery (if any).
 *
 * @param onClearSelection: () -> Unit – resets the selected image state in parent scope when popup is closed.
 *
 * @param tts: TextToSpeech? – instance used to provide audio feedback when enabled.
 *
 * @param voiceFeedbackEnabled: Boolean – determines whether TTS should be used for user feedback.
 *
 * @return Unit: renders the full camera interaction screen, including all detection and control logic.
 */
@Composable
fun CameraPage(
    onBack: () -> Unit,
    onOpenGallery: () -> Unit,
    openGalleryShortcut: Boolean,
    selectedImageUri: Uri?,
    onClearSelection: () -> Unit,
    tts: TextToSpeech?,
    voiceFeedbackEnabled: Boolean
) {
    // Context and camera setup
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val haptic = LocalHapticFeedback.current

    // State variables
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var annotatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectedObjects by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }
    var isObjectDetectionDone by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showImagePopup by remember { mutableStateOf(false) }
    var showObjectInfo by remember { mutableStateOf(false) }

    // Preference to show first-time hint
    val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    var showFirstTimeHint by remember { mutableStateOf(prefs.getBoolean("first_camera_hint", true)) }

    // Launch gallery if shortcut triggered
    LaunchedEffect(openGalleryShortcut) {
        if (openGalleryShortcut) onOpenGallery()
    }

    // Text-to-speech helper
    fun speak(text: String) {
        if (voiceFeedbackEnabled) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview display
        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.weight(1f),
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                }
            )
        }

        // One-time hint overlay
        if (showFirstTimeHint) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Text(
                    "Tap the button below to take a picture of a LEGO brick.",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            LaunchedEffect(Unit) {
                delay(5000)
                prefs.edit().putBoolean("first_camera_hint", false).apply()
                showFirstTimeHint = false
            }
        }

        // Capture + Gallery Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Capture button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center)
                    .background(Color.White, CircleShape)
                    .clip(CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        speak("Capture button clicked")
                        val photoFile = File(context.cacheDir, "captured_${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    capturedImageUri = Uri.fromFile(photoFile)
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                                    Log.e("CameraPage", "Capture error", exc)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Camera, contentDescription = "Capture", tint = Color.Black)
            }

            // Gallery button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = (-65).dp)
                    .background(Color.DarkGray, RoundedCornerShape(8.dp))
                    .clickable {
                        speak("Gallery button clicked")
                        onOpenGallery()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Photo, contentDescription = "Gallery", tint = Color.White)
            }
        }

        // Back Button
        IconButton(
            onClick = {
                speak("Back button clicked")
                onBack()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White)
        }

        // Image detection + popup logic
        val imageUriForDetection = capturedImageUri ?: selectedImageUri
        imageUriForDetection?.let { uri ->
            val bitmap = uriToBitmap(context, uri)

            if (bitmap != null && !isObjectDetectionDone) {
                isObjectDetectionDone = true
                isLoading = true

                detectObjects(context, uri) { annotatedBitmapResult, detectedObjs ->
                    annotatedBitmap = annotatedBitmapResult
                    detectedObjects = detectedObjs
                    isLoading = false

                    if (voiceFeedbackEnabled && detectedObjs.isNotEmpty()) {
                        val labelTexts = detectedObjs.flatMap { it.labels }
                            .joinToString(separator = ", ") { it.text }
                        speak("Detected: $labelTexts")
                    }

                    showImagePopup = true
                }
            }

            // Annotated image display popup
            if (showImagePopup && annotatedBitmap != null) {
                ImagePopUp(
                    annotatedBitmap = annotatedBitmap,
                    onClose = {
                        showImagePopup = false
                        capturedImageUri = null
                        annotatedBitmap = null
                        detectedObjects = emptyList()
                        isObjectDetectionDone = false
                        showObjectInfo = false
                        onClearSelection() // reset gallery image selection
                    },
                    onShowObjectInfo = {
                        showObjectInfo = true
                    }
                )
            }
        }

        // Object info popup
        if (showObjectInfo) {
            ObjectInfoPopup(
                detectedObjects = detectedObjects,
                onClose = { showObjectInfo = false },
                tts = tts,
                voiceFeedbackEnabled = voiceFeedbackEnabled
            )
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Detecting LEGO bricks...", color = Color.White)
                }
            }
        }
    }
}

/**
 * uriToBitmap
 *
 * Description: Converts a content URI to a properly oriented Bitmap image by checking and applying
 * EXIF orientation metadata. This ensures the image appears correctly when displayed in the app.
 *
 * @param context: Context – the calling context used to access the content resolver.
 *
 * @param uri: Uri – the content URI pointing to the image to be decoded.
 *
 * @return Bitmap? – a rotated bitmap if successful, or null if the image couldn't be loaded.
 */
fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        val exif = ExifInterface(context.contentResolver.openInputStream(uri)!!)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        // Apply rotation based on EXIF orientation
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

//will create a special bitmap that is pre rotated 90 degrees
//**can add more rotation options in the future if screen rotation is made available for our app**


//function to detect object and handle results
fun detectObjects(
    context: Context,
    imageUri: Uri,
    onDetectionComplete: (Bitmap, List<DetectedObject>) -> Unit
) {
    context.contentResolver.openInputStream(imageUri)?.use { imageStream ->
        val rotatedBitmap = uriToBitmap(context, imageUri)

        rotatedBitmap?.let {
            val image = InputImage.fromBitmap(it, 0)
            val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()

            val objectDetector = ObjectDetection.getClient(options)

            objectDetector.process(image)
                .addOnSuccessListener { objects ->
                    val annotatedBitmap = drawBoundingBoxesOnBitmap(it, objects)
                    onDetectionComplete(annotatedBitmap, objects)
                }
                .addOnFailureListener { e ->
                    // If detection fails, return the original bitmap and an empty list
                    onDetectionComplete(it, emptyList())
                }
        }
    }
}

//function to display the boxes and labels
fun drawBoundingBoxesOnBitmap(bitmap: Bitmap, detectedObjects: List<DetectedObject>): Bitmap {
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)

    //deque array of colors for the bounding boxes
    val colorDeque: ArrayDeque<Int> = ArrayDeque(listOf(android.graphics.Color.RED, "#16B500".toColorInt(), android.graphics.Color.YELLOW, android.graphics.Color.MAGENTA, android.graphics.Color.CYAN))

    val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4.0f
    }
    val textPaint = Paint().apply {
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }

    for (obj in detectedObjects) {
        val box = obj.boundingBox
        Log.d("BoundingBox", "Drawing bounding box: $box")

        val randomIndex = Random.nextInt(colorDeque.size)

        val colorBox = colorDeque.removeAt(randomIndex)

        paint.color = colorBox
        textPaint.color = colorBox

        canvas.drawRect(box, paint)

        obj.labels.forEach { label ->
            Log.d("BoundingBox", "Drawing label: ${label.text}")

            // Calculate the width and height of the label text
            val labelWidth = textPaint.measureText(label.text)
            val labelHeight = textPaint.textSize

            // Create a small box for the label, making sure it stays within the bounding box
            val labelBox = Rect(
                box.left,
                box.top,
                (box.left + labelWidth + 10).toInt(),  // Adjust the width to fit the label inside
                (box.top + labelHeight + 10).toInt()  // Adjust the height to fit the label
            )

            // Draw the black box behind the label
            val labelBoxPaint = Paint().apply { color = android.graphics.Color.BLACK }
            canvas.drawRect(labelBox, labelBoxPaint)

            // Draw the label text inside the black box
            canvas.drawText(label.text, box.left.toFloat(), box.top + labelHeight, textPaint)
        }
    }

    return mutableBitmap
}

/**
 * ImagePopUp
 *
 * Description: Displays a full-screen popup showing the captured and annotated image.
 * Includes a close button and a "Show Object Info" button at the bottom.
 *
 * @Composable: Indicates this is a Jetpack Compose UI component.
 *
 * @param annotatedBitmap: Bitmap? – optional image passed in to be displayed in the popup.
 *      If null, the composable exits early and does not render anything.
 *
 * @param onClose: () -> Unit – lambda triggered when the user taps the close icon.
 *
 * @param onShowObjectInfo: () -> Unit – lambda triggered when the user taps the "Show Object Info" button.
 *
 * @return Unit: renders the full-screen popup with image and controls.
 */
@Composable
fun ImagePopUp(
    annotatedBitmap: Bitmap?,
    onClose: () -> Unit,
    onShowObjectInfo: () -> Unit
) {
    // Exits early if there's no image to show
    if (annotatedBitmap == null) return

    Box(
        modifier = Modifier
            .fillMaxSize() // Occupy entire screen
            .background(Color.Black), // Black background to highlight image
        contentAlignment = Alignment.Center // Center image content
    ) {
        Image(
            bitmap = annotatedBitmap.asImageBitmap(), // Convert Bitmap to ImageBitmap for Compose rendering
            contentDescription = "Captured Image", // Accessibility label
            modifier = Modifier.fillMaxSize(), // Fill available space
            contentScale = ContentScale.Fit // Maintain aspect ratio within bounds
        )

        IconButton(
            onClick = { onClose() }, // Triggers onClose lambda
            modifier = Modifier
                .align(Alignment.TopEnd) // Places button at top-right corner
                .padding(16.dp) // Gives some breathing room from edge
                .background(Color.Black.copy(alpha = 0.7f), CircleShape) // Semi-transparent black circle background
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) // White close icon
        }

        Button(
            onClick = onShowObjectInfo, // Triggers onShowObjectInfo lambda
            modifier = Modifier
                .align(Alignment.BottomCenter) // Places button at bottom center of screen
                .padding(16.dp) // Space from screen bottom
        ) {
            Text("Show Object Info") // Button label
        }
    }
}


@Composable
fun ObjectInfoPopup(
    detectedObjects: List<DetectedObject>,
    onClose: () -> Unit,
    tts: TextToSpeech?,
    voiceFeedbackEnabled: Boolean
) {
    LaunchedEffect(Unit) {
        if (voiceFeedbackEnabled) {
            val spokenText = if (detectedObjects.isNotEmpty()) {
                buildString {
                    detectedObjects.forEach { obj ->
                        obj.labels.forEach { label ->
                            append("${label.text}, confidence ${"%.1f".format(label.confidence * 100)} percent. ")
                        }
                    }
                }
            } else {
                "No objects detected."
            }
            tts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            Button(onClick = onClose) {
                Text("Close")
            }
        },
        title = { Text("Detected Objects", color = Color.White) },
        text = {
            Column {
                if (detectedObjects.isEmpty()) {
                    Text(
                        "No objects detected.",
                        color = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.DarkGray)
                            .fillMaxWidth()
                    )
                } else {
                    detectedObjects.forEach { obj ->
                        if (obj.labels.isNotEmpty()) {
                            obj.labels.forEach { label ->
                                Text(
                                    text = "Label: ${label.text}\nConfidence: ${"%.2f".format(label.confidence * 100)}%",
                                    color = Color.White,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .background(Color.DarkGray)
                                        .fillMaxWidth()
                                )
                            }
                        } else {
                            Text(
                                text = "Unknown object",
                                color = Color.White,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .background(Color.DarkGray)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.Black
    )
}

fun logExifData(context: Context, uri: Uri) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.use {
            val exif = ExifInterface(it)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            Log.d("EXIFOrientation", "Actual EXIF orientation: $orientation")
        }
    } catch (e: Exception) {
        Log.e("EXIFOrientation", "Failed to read EXIF data", e)
    }
}

/**
 * GalleryPage
 *
 * Description: Displays a simple gallery page with a title, optional selected image,
 * and a button to go back to the previous screen.
 *
 * @Composable: Indicates this is a Jetpack Compose UI component.
 *
 * @param onBack: () -> Unit – lambda function triggered when the "Back" button is pressed.
 *
 * @param selectedImageUri: Uri? – optional URI of the image selected by the user.
 *      If not null, the image will be displayed.
 *
 * @return Unit: renders a vertically centered column layout showing the image (if any) and a back button.
 */
@Composable
fun GalleryPage(onBack: () -> Unit, selectedImageUri: Uri?) {
    Column(
        modifier = Modifier
            .fillMaxSize() // Fill the entire screen
            .background(MaterialTheme.colorScheme.background), // Use theme's background color
        verticalArrangement = Arrangement.Center, // Center contents vertically
        horizontalAlignment = Alignment.CenterHorizontally // Center contents horizontally
    ) {
        Text("Gallery Page", // Screen title
            style = MaterialTheme.typography.headlineLarge, // Use large headline typography from theme
            color = MaterialTheme.colorScheme.onBackground) // Ensure readable text based on background

        Spacer(modifier = Modifier.height(16.dp)) // Vertical spacing

        // If an image URI is provided, display the image
        selectedImageUri?.let{
            Image(
                painter = rememberAsyncImagePainter(it), // Load image from URI asynchronously
                contentDescription = "Selected Image", // Accessibility label
                modifier = Modifier
                    .size(200.dp) // Fixed size for image preview
                    .padding(8.dp) // Spacing around image
            )
        }
        Spacer(modifier = Modifier.height(16.dp)) // More vertical spacing
        Button(onClick = onBack) { // Back button that triggers onBack lambda
            Text("Back") // Button label
        }
    }
}
