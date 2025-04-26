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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val haptic = LocalHapticFeedback.current

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var annotatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectedObjects by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }
    var isObjectDetectionDone by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showImagePopup by remember { mutableStateOf(false) }
    var showObjectInfo by remember { mutableStateOf(false) }

    val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    var showFirstTimeHint by remember { mutableStateOf(prefs.getBoolean("first_camera_hint", true)) }

    LaunchedEffect(openGalleryShortcut) {
        if (openGalleryShortcut) onOpenGallery()
    }

    fun speak(text: String) {
        if (voiceFeedbackEnabled) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        onClearSelection() // <-- 🛠️ Important: clear uploaded gallery image!
                    },
                    onShowObjectInfo = {
                        showObjectInfo = true
                    }
                )
            }
        }

        if (showObjectInfo) {
            ObjectInfoPopup(
                detectedObjects = detectedObjects,
                onClose = { showObjectInfo = false },
                tts = tts,
                voiceFeedbackEnabled = voiceFeedbackEnabled
            )
        }

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

//convert Uri to Bitmap check API version to use different libraries
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

@Composable
fun ImagePopUp(
    annotatedBitmap: Bitmap?,
    onClose: () -> Unit,
    onShowObjectInfo: () -> Unit
) {
    if (annotatedBitmap == null) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = annotatedBitmap.asImageBitmap(),
            contentDescription = "Captured Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        IconButton(
            onClick = { onClose() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        Button(
            onClick = onShowObjectInfo,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Show Object Info")
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

@Composable
fun GalleryPage(onBack: () -> Unit, selectedImageUri: Uri?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Gallery Page",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground)
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
