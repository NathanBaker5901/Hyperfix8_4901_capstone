package com.example.blocklens

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun CameraPage(
    onBack: () -> Unit,
    onOpenGallery: () -> Unit,
    openGalleryShortcut: Boolean,
    selectedImageUri: Uri?,
    onClearSelection: () -> Unit,
    onShowInfo: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var annotatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isObjectDetectionDone by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var detectedInfo by remember { mutableStateOf<List<String>>(emptyList()) }
    var showObjectInfo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(openGalleryShortcut) {
        if (openGalleryShortcut) onOpenGallery()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
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

        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Back", Modifier.clickable { onBack() })
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(Color.Black, CircleShape)
                    .clickable {
                        val photoFile =
                            File(context.cacheDir, "captured_${System.currentTimeMillis()}.jpg")
                        val outputOptions =
                            ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    capturedImageUri = Uri.fromFile(photoFile)
                                    isObjectDetectionDone = false
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT)
                                        .show()
                                    Log.e("CameraPage", "Capture error", exc)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(24.dp).background(Color.White, CircleShape))
            }
            Text("Gallery", Modifier.clickable { onOpenGallery() })
        }
    }

    val imageUri = capturedImageUri ?: selectedImageUri
    imageUri?.let { uri ->
        val rawBitmap = uriToBitmap(context, uri)
        val bitmap = rawBitmap?.let { correctOrientation(context, it, uri) }

        if (bitmap != null && !isObjectDetectionDone) {
            isObjectDetectionDone = true
            scope.launch {
                isLoading = true
                val (resultBitmap, infoList) = withContext(Dispatchers.IO) {
                    detectObjects(context, bitmap)
                }
                annotatedBitmap = resultBitmap
                detectedInfo = infoList
                isLoading = false
            }
        }

        if (annotatedBitmap != null) {
            Box(Modifier.fillMaxSize()) {
                ImagePopUp(
                    uri = uri,
                    annotatedBitmap = annotatedBitmap,
                    onClose = {
                        onClearSelection()
                        isObjectDetectionDone = false
                    },
                    onShowObjectInfo = { onShowInfo() }
                )

                if (showObjectInfo) {
                    ObjectInfoPopup(detectedInfo) { showObjectInfo = false }
                }
            }
        }
    }
}


fun detectObjects(context: Context, original: Bitmap): Pair<Bitmap, List<String>> {
    val apiKey = "7UhZ8whGm96kLw1QG87H"
    val url = "https://serverless.roboflow.com/infer/workflows/capstone-block-lens/custom-workflow-3"

    val baos = ByteArrayOutputStream().apply {
        original.compress(Bitmap.CompressFormat.JPEG, 90, this)
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
                    val infoList = mutableListOf<String>()
                    for (i in 0 until preds.length()) {
                        val obj = preds.getJSONObject(i)
                        val cls = obj.getString("class")
                        val conf = obj.optDouble("confidence", -1.0)
                        infoList.add("$cls: ${"%.2f".format(conf * 100)}%")
                    }

                    val annotated = drawRoboflowAnnotations(original, preds)
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


fun extractPredictionInfo(bitmap: Bitmap): List<String> {
    // Stub for prediction info (if needed, attach labels during drawing and return from detectObjects)
    return listOf("Prediction info display not yet implemented.")
}

fun drawRoboflowAnnotations(original: Bitmap, preds: JSONArray): Bitmap {
    val result = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.Red.toArgb()
        isAntiAlias = true
    }
    val textPaint = Paint().apply {
        color = Color.White.toArgb()
        textSize = 48f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    val backgroundPaint = Paint().apply {
        color = Color.Black.copy(alpha = 0.7f).toArgb()
    }

    for (i in 0 until preds.length()) {
        val obj = preds.getJSONObject(i)
        val x = obj.getDouble("x").toFloat()
        val y = obj.getDouble("y").toFloat()
        val w = obj.getDouble("width").toFloat()
        val h = obj.getDouble("height").toFloat()
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
        val padding = 10f
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
    uri: Uri,
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
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Red, CircleShape)
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
fun ObjectInfoPopup(objectsInfo: List<String>, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { Button(onClick = onClose) { Text("Close") } },
        title = { Text("Detected Objects", color = Color.White) },
        text = {
            Column {
                objectsInfo.forEach { info ->
                    Text(
                        text = info,
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.DarkGray)
                            .fillMaxWidth()
                    )
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
