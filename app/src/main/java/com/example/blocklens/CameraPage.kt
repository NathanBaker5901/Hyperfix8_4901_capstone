package com.example.blocklens

import android.content.Context
import android.graphics.*
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.core.graphics.scale
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
    val scope = rememberCoroutineScope()

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var annotatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isObjectDetectionDone by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var detectedInfo by remember { mutableStateOf<List<String>>(emptyList()) }
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
                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
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
                prefs.edit { putBoolean("first_camera_hint", false) }
                showFirstTimeHint = false
            }
        }

// Bottom row with capture and gallery buttons on the same line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Capture Button - centered
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
                                    isObjectDetectionDone = false
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

            // Gallery Button - to the right
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterEnd) //This puts it to the right of the capture button
                    .offset(x = (-65).dp)       //Optional: nudge it inward from the edge
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



        // Top-left Back Button
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

        // Handle detection / display
        val imageUri = capturedImageUri ?: selectedImageUri
        imageUri?.let { uri ->
            val rawBitmap = uriToBitmap(context, uri)
            val bitmap = rawBitmap?.let { correctOrientation(context, it, uri) }

            if (bitmap != null && !isObjectDetectionDone) {
                isObjectDetectionDone = true
                scope.launch {
                    isLoading = true
                    val (resultBitmap, infoList) = withContext(Dispatchers.IO) {
                        detectObjects(bitmap)
                    }
                    annotatedBitmap = resultBitmap
                    detectedInfo = infoList
                    isLoading = false

                    if (voiceFeedbackEnabled) {
                        val spokenText = if (infoList.isNotEmpty()) {
                            infoList.joinToString(". ")
                        } else {
                            "No objects detected."
                        }
                        speak(spokenText)
                    }
                }
            }

            if (annotatedBitmap != null) {
                ImagePopUp(
                    annotatedBitmap = annotatedBitmap,
                    onClose = {
                        onClearSelection()
                        isObjectDetectionDone = false
                        annotatedBitmap = null
                        capturedImageUri?.let {
                            val file = File(it.path!!)
                            if (file.exists()) file.delete()
                        }
                        capturedImageUri = null
                    },
                    onShowObjectInfo = {
                        if (detectedInfo.isNotEmpty()) {
                            speak("Show Object Info button clicked")
                            showObjectInfo = true
                        } else {
                            speak("No objects to show.")
                            Toast.makeText(context, "No objects detected.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                if (showObjectInfo) {
                    ObjectInfoPopup(
                        objectsInfo = detectedInfo,
                        onClose = { showObjectInfo = false },
                        tts = tts,
                        voiceFeedbackEnabled = voiceFeedbackEnabled
                    )
                }
            }
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


fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
    return bitmap.scale(width, height)
}

fun detectObjects(original: Bitmap): Pair<Bitmap, List<String>> {
    val apiKey = "7UhZ8whGm96kLw1QG87H"
    val url = "https://serverless.roboflow.com/infer/workflows/capstone-block-lens/custom-workflow-3"

    val resizedBitmap = resizeBitmap(original, 640, 640)
    val baos = ByteArrayOutputStream().apply {
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, this)
    }
    val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

    val payload = JSONObject().apply {
        put("api_key", apiKey)
        put("confidence", 0.5)
        put("inputs", JSONObject().apply {
            put("image", JSONObject().apply {
                put("type", "base64")
                put("value", base64)
            })
        })
    }

    val body = payload.toString().toRequestBody("application/json".toMediaType())
    val request = Request.Builder().url(url).post(body).build()

    return try {
        OkHttpClient().newCall(request).execute().use { response ->
            val text = response.body?.string()
            if (response.isSuccessful && text != null) {
                val json = JSONObject(text)
                val preds = json.getJSONArray("outputs")
                    .getJSONObject(0)
                    .getJSONObject("predictions")
                    .optJSONArray("predictions")

                return if (preds != null && preds.length() > 0) {
                    var topLabel = ""
                    var topConfidence = -1.0

                    for (i in 0 until preds.length()) {
                        val obj = preds.getJSONObject(i)
                        val cls = obj.getString("class")
                        val conf = obj.optDouble("confidence", -1.0)

                        if (conf > topConfidence) {
                            topConfidence = conf
                            topLabel = cls
                        }
                    }

                    val infoList = if (topLabel.isNotEmpty()) {
                        listOf("$topLabel: ${"%.2f".format(topConfidence * 100)}%")
                    } else {
                        listOf("No objects detected.")
                    }

                    val annotated = drawRoboflowAnnotations(original, preds, resizedBitmap.width, resizedBitmap.height)
                    annotated to infoList
                } else {
                    original to listOf("No objects detected.")
                }
            } else {
                Log.e("Roboflow", "Error: ${response.code}")
                original to listOf("Detection failed.")
            }
        }
    } catch (e: Exception) {
        Log.e("Roboflow", "Exception", e)
        original to listOf("Error: ${e.localizedMessage}")
    }
}


fun drawRoboflowAnnotations(original: Bitmap, preds: JSONArray, resizedWidth: Int, resizedHeight: Int): Bitmap {
    val result = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    val scaleX = original.width / resizedWidth.toFloat()
    val scaleY = original.height / resizedHeight.toFloat()
    val scaleFactor = original.width / 640f

    val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f * scaleFactor
        color = Color.Red.toArgb()
        isAntiAlias = true
    }

    val textPaint = Paint().apply {
        color = Color.White.toArgb()
        textSize = 48f * scaleFactor
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val backgroundPaint = Paint().apply {
        color = Color.Black.copy(alpha = 0.7f).toArgb()
    }

    var topObj: JSONObject? = null
    var topConfidence = -1.0
    for (i in 0 until preds.length()) {
        val obj = preds.getJSONObject(i)
        val conf = obj.optDouble("confidence", -1.0)
        if (conf > topConfidence) {
            topConfidence = conf
            topObj = obj
        }
    }


    topObj?.let { obj ->
        val x = obj.getDouble("x").toFloat() * scaleX
        val y = obj.getDouble("y").toFloat() * scaleY
        val w = obj.getDouble("width").toFloat() * scaleX
        val h = obj.getDouble("height").toFloat() * scaleY
        val cls = obj.getString("class")
        val conf = obj.optDouble("confidence", -1.0)
        val label = "$cls %.2f".format(conf)

        val left = x - w / 2
        val top = y - h / 2
        val right = x + w / 2
        val bottom = y + h / 2
        canvas.drawRect(left, top, right, bottom, paint)

        val textWidth = textPaint.measureText(label)
        val textHeight = textPaint.textSize
        val padding = 10f * scaleFactor
        val labelTop = if (top - textHeight - padding < 0) top + textHeight + padding else top
        canvas.drawRect(left, labelTop - textHeight - padding, left + textWidth + 2 * padding, labelTop, backgroundPaint)
        canvas.drawText(label, left + padding, labelTop - padding, textPaint)
    }

    return result
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap? =
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }

fun correctOrientation(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
    context.contentResolver.openInputStream(uri)?.use {
        val exif = ExifInterface(it)
        val matrix = Matrix().apply {
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            }
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    return bitmap
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
            contentDescription = "Annotated Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // "X" close button
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


data class LegoPart(
    val id: String,
    val rebrickableName: String,
    val officialName: String,
    val category: String
)

fun loadLegoPartsFromCSV(context: Context): Map<String, LegoPart> {
    val assetManager = context.assets
    val inputStream = assetManager.open("legos.csv") // put your CSV file in `assets`
    val reader = inputStream.bufferedReader()

    val legoMap = mutableMapOf<String, LegoPart>()
    reader.useLines { lines ->
        lines.drop(1).forEach { line ->
            val tokens = line.split(",").map { it.trim() }
            if (tokens.size >= 7) {
                val id = tokens[0]
                val rebrickableName = tokens[5]
                val officialName = tokens[6]
                val category = tokens.getOrNull(7) ?: "Unknown"
                legoMap[id] = LegoPart(id, rebrickableName, officialName, category)
            }
        }
    }

    return legoMap
}

@Composable
fun ObjectInfoPopup(
    objectsInfo: List<String>,
    onClose: () -> Unit,
    tts: TextToSpeech?,
    voiceFeedbackEnabled: Boolean
) {
    val context = LocalContext.current
    val legoMap = remember { loadLegoPartsFromCSV(context) }

    LaunchedEffect(Unit) {
        if (voiceFeedbackEnabled) {
            val spokenText = buildString {
                append("Detected objects are: ")
                objectsInfo.forEach { entry ->
                    val (id, confidence) = entry.split(":").map { it.trim() }
                    val part = legoMap[id]
                    append(
                        part?.let {
                            "$id, confidence $confidence percent. ${it.rebrickableName}, ${it.officialName}, in category ${it.category}. "
                        } ?: "$id with confidence $confidence percent. "
                    )
                }
            }
            tts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { Button(onClick = onClose) { Text("Close") } },
        title = { Text("Detected Object", color = Color.White) },
        text = {
            Column {
                objectsInfo.forEach { entry ->
                    val parts = entry.split(":").map { it.trim() }
                    if (parts.size == 2) {
                        val (id, confidence) = parts
                        val part = legoMap[id]
                        val detail = part?.let {
                            "ID: $id\nConfidence: $confidence\n${it.rebrickableName} - ${it.officialName} (${it.category})"
                        } ?: "$id: $confidence"

                        Text(
                            text = detail,
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier
                                .padding(8.dp)
                                .background(Color.DarkGray)
                                .fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = entry, // fallback for unexpected format
                            fontSize = 16.sp,
                            color = Color.LightGray,
                            modifier = Modifier
                                .padding(8.dp)
                                .background(Color.DarkGray)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        },
        containerColor = Color.Black
    )
}


@Composable
fun GalleryPage(onBack: () -> Unit, selectedImageUri: Uri?) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Gallery Page",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))
        selectedImageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
