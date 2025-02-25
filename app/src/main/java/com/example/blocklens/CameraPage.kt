package com.example.blocklens


import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned

import com.example.blocklens.ui.theme.ColorBlindMode
import com.example.blocklens.ui.theme.TextSizeOption
import com.example.blocklens.ui.theme.BlockLensTheme

// mlkit libraries need to look into live camera
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.objects.DetectedObject

@Composable
fun CameraPage(onBack: () -> Unit, onOpenGallery: () -> Unit, openGalleryShortcut: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

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
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        capturedImageUri = Uri.fromFile(photoFile)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
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

    capturedImageUri?.let { uri ->
        ImagePopUp(uri) { capturedImageUri = null }
    }
}

@Composable
fun ImagePopUp(imageUri: Uri, onClose: () -> Unit) {
    val context = LocalContext.current //get android context(needed to get files)
    var detectedObjects by remember { mutableStateOf<List<DetectedObject>>(emptyList())} //stores list objects detected
    var showBoundingBox by remember { mutableStateOf(false) } // Controls if the bounding box shows or doesn't
    var processedImageUri by remember { mutableStateOf<Uri?>(null) } // Stores the URI of the full size image
    var imageWidth by remember { mutableIntStateOf(1) } //keep track of width for correct scaling
    var imageHeight by remember { mutableIntStateOf(1) } //keep track of height for correct scaling


    //need to rotate the bitmap to the correct orientation
    fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle) // Rotate by the specified angle
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // will need to change if we want to use live image so a new instance is not created every frame
    // its okay for now since we are using a static image at the moment.
    val detectObjects: () -> Unit = {
        val imageStream: InputStream? = context.contentResolver.openInputStream(imageUri) // open image file
        val bitmap = BitmapFactory.decodeStream(imageStream) // convert image to bitmap

        //**FUTURE IMPLEMENTATION** if we want to implement horizontal screen make sure to make a conditional
        // statement in order to stop the rotation if the screen orientation is horizontal
        val rotatedBitmap = rotateBitmap(bitmap, 90f) // rotate image 90 degrees due to the bitmap rotating on vertical


        //save a temp for the full size image so the bounding boxes accurately size around the object
        val tempFile = File(context.cacheDir, "temp_analyzed_image.jpg")
        tempFile.outputStream().use { out ->
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        processedImageUri = Uri.fromFile(tempFile) //update uri to processed image

        imageWidth = rotatedBitmap.width //update image width
        imageHeight = rotatedBitmap.height //update image height

        val image = InputImage.fromBitmap(rotatedBitmap, 0)
        // configure mlkit object detector
        val options = ObjectDetectorOptions.Builder() // create ObjectDetectorOptions object
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE) // set the mode for a single image
            .enableMultipleObjects() // CAN TURN ON AND OFF TO TEST IF MULTIPLE OBJECTS IS NOT WORKING
            .enableClassification() // OBJECT RECOGNITION NAMES CAN BE USED FOR TESTING FOR NOW BUT WILL NEED TO REMOVE/CHANGE IN THE FUTURE
            .build() // build the options
        
        val objectDetector = ObjectDetection.getClient(options) // create objectDetector instance using the previous options

        //Clear previous detected objects to avoid bad results
        detectedObjects = emptyList()
        showBoundingBox = false

        // if successful updates detectedObjects and showBoundingBox
        objectDetector.process(image)
            .addOnSuccessListener { objects ->
                detectedObjects = objects
                showBoundingBox = objects.isNotEmpty()
            }
            // else if it fails log error for MLKit
            .addOnFailureListener { e ->
                Log.e("MLKit", "Object detection failed", e)
            }
        
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "X",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { onClose() }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            ) {
               // Show the processed image if availiable otherwise keep original
                val displayUri = processedImageUri ?: imageUri
                val displayedImage = rememberAsyncImagePainter(displayUri)
                Image(
                    painter = displayedImage,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { layoutCoordinates ->
                            imageWidth = layoutCoordinates.size.width
                            imageHeight = layoutCoordinates.size.height
                        },
                    contentScale = ContentScale.Fit
                )
                if (showBoundingBox) {
                    detectedObjects.forEach { obj ->
                        obj.boundingBox.let { box ->
                            //calculate the factor to scale the bounding box to the image size
                            val scaleX = imageWidth.toFloat() / box.width().toFloat()
                            val scaleY = imageHeight.toFloat() / box.height().toFloat()

                            Box(
                                modifier = Modifier
                                    .absoluteOffset(x = (box.left / scaleX).dp, y = (box.top / scaleY).dp)
                                    .size((box.width() / scaleX).dp, (box.height() / scaleY).dp)
                                    .border(2.dp, color = Color.Red)
                            )
                            // labels for testing but will most likely need to remove/change in the future when dealing with primarily legos
                            obj.labels.forEach { label ->
                                Text(
                                    text = label.text,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .absoluteOffset(x = (box.left / scaleX).dp, y = (box.top / scaleY).dp - 20.dp)
                                )
                            }
                        }
                    }
                }
            }
            Button(
                onClick = detectObjects,
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Analyze")
            }
        }
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
