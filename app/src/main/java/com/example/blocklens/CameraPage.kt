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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.ui.layout.onGloballyPositioned
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

@Composable
fun CameraPage(
    onBack: () -> Unit,
    onOpenGallery: () -> Unit,
    openGalleryShortcut: Boolean,
    selectedImageUri: Uri?,
    tts: TextToSpeech?,
    voiceFeedbackEnabled: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var annotatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isObjectDetectionDone by remember { mutableStateOf(false) }

    // Trigger the gallery function automatically only if the shortcut is active
    LaunchedEffect(openGalleryShortcut) {
        if (openGalleryShortcut) {
            onOpenGallery()
        }
    }

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
                        val preview = Preview.Builder().build()
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
                SpeakButton(
                    speakLabel = "Back to Home Page",
                    displayLabel = "Back",
                    tts = tts,
                    voiceFeedbackEnabled = voiceFeedbackEnabled,
                    onClickAction = { onBack() }
                )
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color.Black, CircleShape)
                        .clickable {
                            val photoFile = File(
                                context.cacheDir,
                                "captured_image_${System.currentTimeMillis()}.jpg"
                            )
                            val outputOptions =
                                ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        capturedImageUri = Uri.fromFile(photoFile)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Toast.makeText(
                                            context,
                                            "Failed to capture image",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        Log.e("CameraPage", "Image capture failed", exception)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(color = Color.White, shape = CircleShape)
                    )
                }

                SpeakButton(
                    speakLabel = "Gallery",
                    displayLabel = "Gallery",
                    tts = tts,
                    voiceFeedbackEnabled = voiceFeedbackEnabled,
                    onClickAction = {
                        if (voiceFeedbackEnabled) {
                            tts?.speak("Please select Photos or Albums", TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                        onOpenGallery()
                    }
                )
            }
        }
    }

    val imageUriForPopUp = capturedImageUri ?: selectedImageUri
    imageUriForPopUp?.let { uri ->
        logExifData(context, uri)
        val bitmap = uriToBitmap(context, uri)
        if(bitmap != null && !isObjectDetectionDone) {
            detectObjects(context, uri) { annotatedBitmapResult ->
                annotatedBitmap = annotatedBitmapResult
                isObjectDetectionDone = true
            }
        }



        // Show imagePopup with annotated bitmap
        ImagePopUp(uri = uri, annotatedBitmap = annotatedBitmap, onClose = {
            capturedImageUri = null
            annotatedBitmap = null
            isObjectDetectionDone = false
        })
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
fun detectObjects(context: Context, imageUri: Uri, onDetectionComplete: (Bitmap) -> Unit) {
    context.contentResolver.openInputStream(imageUri)?.use { imageStream ->
        val bitmap = BitmapFactory.decodeStream(imageStream)

        // Apply EXIF orientation
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
                    onDetectionComplete(annotatedBitmap)
                }
                .addOnFailureListener { e ->
                    onDetectionComplete(it) // Return original image if detection fails
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
fun ImagePopUp(uri: Uri, annotatedBitmap: Bitmap?, onClose: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(uri) { uriToBitmap(context, uri) }
    val imageToDisplay = annotatedBitmap ?: bitmap

    imageToDisplay?.let {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Captured Image with Bounding Boxes",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Red, shape = CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "X",
                    color = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
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
