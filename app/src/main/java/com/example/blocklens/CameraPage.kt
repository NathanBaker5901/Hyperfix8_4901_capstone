package com.example.blocklens


import android.R.attr.orientation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap


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

@Composable
fun CameraPage(onBack: () -> Unit, onOpenGallery: () -> Unit, openGalleryShortcut: Boolean, selectedImageUri: Uri?) {
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
                Text(
                    "Back",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { onBack() }
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

                Text(
                    "Gallery",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { onOpenGallery() }
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            // Use ImageDecoder for API 28+
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            // Use BitmapFactory for older versions
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        }

    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
    Log.d("EXIFOrientation", "Image orientation in uriToBitmap: $orientation")

}

//will create a special bitmap that is pre rotated 90 degrees
//**can add more rotation options in the future if screen rotation is made available for our app**
fun correctBitmapOrientation(bitmap: Bitmap, uri: Uri, context: Context): Bitmap {
    // Read the EXIF data to check the orientation
    val exif = ExifInterface(context.contentResolver.openInputStream(uri)!!)
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    // Initialize a matrix to apply transformations
    val matrix = Matrix()

    // Apply the necessary rotation or flip based on EXIF orientation
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        // No action for normal orientation
    }

    // Create a new bitmap with the applied transformation
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

//function to detect object and handle results
fun detectObjects(context: Context, imageUri: Uri, onDetectionComplete: (Bitmap) -> Unit) {
    context.contentResolver.openInputStream(imageUri)?.use { imageStream ->
        val bitmap = BitmapFactory.decodeStream(imageStream)
        Log.d("EXIFOrientation", "Image orientation start detect objects: $orientation")

        bitmap?.let {
            val image = InputImage.fromBitmap(bitmap, 0)
            val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()

            val objectDetector = ObjectDetection.getClient(options)

            objectDetector.process(image)
                .addOnSuccessListener { objects ->
                    Log.d("ObjectDetection", "Detected objects: ${objects.size}")
                    if (objects.isEmpty()) {
                        Log.d("ObjectDetection", "No objects detected")
                    }
                    val annotatedBitmap = drawBoundingBoxesOnBitmap(it, objects)
                    onDetectionComplete(annotatedBitmap)
                }
                .addOnFailureListener { e ->
                    Log.e("MLKit", "Object detection failed", e)
                    onDetectionComplete(it) // Return original image if detection fails
                }

        }
    }
    Log.d("EXIFOrientation", "Image orientation end detect objects: $orientation")
}

//function to display the boxes and labels
fun drawBoundingBoxesOnBitmap(bitmap: Bitmap, detectedObjects: List<DetectedObject>): Bitmap {
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)
    val paint = Paint().apply {
        color = android.graphics.Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
    }
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }
    for (obj in detectedObjects) {
        val box = obj.boundingBox
        Log.d("BoundingBox", "Drawing bounding box: $box")
        canvas.drawRect(box, paint)

        obj.labels.forEach { label ->
            Log.d("BoundingBox", "Drawing label: ${label.text}")
            canvas.drawText(label.text, box.left.toFloat(), box.top.toFloat() - 10, textPaint)
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

            // Close button
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
