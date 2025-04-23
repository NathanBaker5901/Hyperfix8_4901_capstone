package com.example.blocklens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

@Composable
fun CameraPage(
    onBack: () -> Unit,
    onOpenGallery: () -> Unit,
    openGalleryShortcut: Boolean,
    selectedImageUri: Uri?
) {
    val context           = LocalContext.current
    val lifecycleOwner    = LocalLifecycleOwner.current
    val cameraProviderFmt = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture      = remember { ImageCapture.Builder().build() }
    var capturedImageUri  by remember { mutableStateOf<Uri?>(null) }
    var annotatedBitmap   by remember { mutableStateOf<Bitmap?>(null) }
    var isObjectDetectionDone by remember { mutableStateOf(false) }
    var isLoading         by remember { mutableStateOf(false) }
    val scope             = rememberCoroutineScope()

    LaunchedEffect(openGalleryShortcut) {
        if (openGalleryShortcut) onOpenGallery()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.weight(1f),
            factory  = { ctx ->
                PreviewView(ctx).also { previewView ->
                    cameraProviderFmt.addListener({
                        val cameraProvider = cameraProviderFmt.get()
                        val preview = androidx.camera.core.Preview.Builder().build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
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
            modifier            = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Back",
                modifier = Modifier.clickable { onBack() },
                color    = MaterialTheme.colorScheme.onBackground
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
                        val opts = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        imageCapture.takePicture(
                            opts,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(res: ImageCapture.OutputFileResults) {
                                    capturedImageUri = Uri.fromFile(photoFile)
                                }
                                override fun onError(exc: ImageCaptureException) {
                                    Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                                    Log.e("CameraPage", "capture error", exc)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(24.dp).background(Color.White, CircleShape))
            }
            Text("Gallery",
                modifier = Modifier.clickable { onOpenGallery() },
                color    = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    // Detection + Popup
    val imageUriForPopUp = capturedImageUri ?: selectedImageUri
    imageUriForPopUp?.let { uri ->
        logExifData(context, uri)
        val raw    = uriToBitmap(context, uri)
        val bitmap = raw?.let { correctOrientation(context, it, uri) }

        if (bitmap != null && !isObjectDetectionDone) {
            scope.launch {
                isLoading = true
                annotatedBitmap = withContext(Dispatchers.IO) {
                    detectObjects(context, bitmap)
                }
                isObjectDetectionDone = true
                isLoading = false
            }
        }

        Box(Modifier.fillMaxSize()) {
            if (isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center))
            ImagePopUp(
                uri             = uri,
                annotatedBitmap = annotatedBitmap,
                onClose         = {
                    capturedImageUri     = null
                    annotatedBitmap      = null
                    isObjectDetectionDone = false
                }
            )
        }
    }
}

// Synchronous: send to Roboflow & draw boxes
fun detectObjects(context: Context, original: Bitmap): Bitmap {
    val baos    = ByteArrayOutputStream().apply { original.compress(Bitmap.CompressFormat.JPEG, 90, this) }
    val base64  = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    val payload = JSONObject().apply {
        put("api_key", "7UhZ8whGm96kLw1QG87H")
        put("inputs", JSONObject().apply {
            put("image", JSONObject().apply {
                put("type", "base64")
                put("value", "data:image/jpeg;base64,$base64")
            })
        })
    }

    val mediaType = "application/json".toMediaType()
    val body      = payload.toString().toRequestBody(mediaType)
    val req       = Request.Builder()
        .url("https://serverless.roboflow.com/infer/workflows/capstone-block-lens/small-object-detection-sahi-2")
        .post(body)
        .build()

    Log.d("Roboflow", "⏳ Sending payload: ${payload.toString(2)}")


    val client   = OkHttpClient()
    val response = client.newCall(req).execute()
    val text     = response.body?.string()

    Log.d("Roboflow", "🔹 Response code: ${response.code}")
    Log.d("Roboflow", "🔹 Response body: $text")

    return if (response.isSuccessful && text != null) {
        drawRoboflowAnnotations(original, text)
    } else {
        Log.e("Roboflow", "API error ${response.code}")
        original
    }
}

fun correctOrientation(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
    context.contentResolver.openInputStream(uri)?.use {
        val exif   = ExifInterface(it)
        val matrix = Matrix().apply {
            when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90  -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            }
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    return bitmap
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap? =
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }

fun logExifData(context: Context, uri: Uri) {
    context.contentResolver.openInputStream(uri)?.use {
        val exif = ExifInterface(it)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        Log.d("ExifData", "Orientation: $orientation")
        Log.d("ExifData", "DateTime: ${exif.getAttribute(ExifInterface.TAG_DATETIME)}")
        Log.d("ExifData", "Make: ${exif.getAttribute(ExifInterface.TAG_MAKE)}")
        Log.d("ExifData", "Model: ${exif.getAttribute(ExifInterface.TAG_MODEL)}")
    }
}


private fun drawRoboflowAnnotations(original: Bitmap, response: String): Bitmap {
    val result = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.Red.toArgb()
    }
    val textPaint = Paint().apply {
        color = Color.White.toArgb()
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
    }

    // Parse the JSON
    val preds = JSONObject(response).optJSONArray("predictions")
    if (preds == null) {
        Log.d("Roboflow", "⚠️ No 'predictions' array in response:\n$response")
        return result
    }
    Log.d("Roboflow", "✅ Found ${preds.length()} predictions")

    // Loop and draw
    for (i in 0 until preds.length()) {
        val obj = preds.getJSONObject(i)
        val x   = obj.getDouble("x").toFloat()
        val y   = obj.getDouble("y").toFloat()
        val w   = obj.getDouble("width").toFloat()
        val h   = obj.getDouble("height").toFloat()
        val cls = obj.getString("class")
        val conf = obj.optDouble("confidence", -1.0)

        val left   = x - w/2
        val top    = y - h/2
        val right  = x + w/2
        val bottom = y + h/2

        Log.d("Roboflow", "📦 pred[$i]: class=$cls conf=$conf " +
                "left=$left top=$top right=$right bottom=$bottom")

        // Draw box
        canvas.drawRect(left, top, right, bottom, paint)
        // Draw label background
        val label = "$cls ${"%.2f".format(conf)}"
        val tw = textPaint.measureText(label)
        val th = textPaint.textSize
        val labelTop = if (top - th < 0) top + th else top
        canvas.drawRect(
            left, labelTop - th,
            left + tw + 8, labelTop,
            Paint().apply { color = Color.Black.copy(alpha = 0.6f).toArgb() }
        )
        canvas.drawText(label, left + 4, labelTop - 4, textPaint)
    }

    return result
}
@Composable
fun ImagePopUp(
    uri: Uri,
    annotatedBitmap: Bitmap?,
    onClose: () -> Unit
) {
    if (annotatedBitmap == null) return

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap            = annotatedBitmap.asImageBitmap(),
            contentDescription = "Annotated Image",
            modifier           = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentScale       = ContentScale.Fit
        )
        IconButton(
            onClick   = onClose,
            modifier  = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
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
        Text("Gallery Page", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        selectedImageUri?.let {
            Image(
                painter            = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier           = Modifier.size(200.dp).padding(8.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
