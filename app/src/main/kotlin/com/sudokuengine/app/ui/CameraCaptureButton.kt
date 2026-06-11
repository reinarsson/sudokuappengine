package com.sudokuengine.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Button that captures a photo with the device camera and forwards the result via
 * [onImageCaptured].
 *
 * Handles the `CAMERA` runtime permission and the [ActivityResultContracts.TakePicture] flow:
 * a temporary file in [Context.getCacheDir] is exposed via `FileProvider` as the capture
 * target, and on success its bytes are read back and passed to [onImageCaptured].
 *
 * Capture progress/results are surfaced solely through [AppScreenState] (driven by
 * [onImageCaptured] transitioning the caller to [AppScreenState.Loading]); this button does not
 * render its own status text, to avoid a second, overlapping feedback mechanism. Permission
 * denials and user-cancelled captures are silently ignored: the screen simply stays in
 * [AppScreenState.Idle].
 *
 * @param onImageCaptured invoked with the captured image's bytes on a successful capture.
 */
@Composable
fun CameraCaptureButton(onImageCaptured: (ByteArray) -> Unit = {}) {
    val context = LocalContext.current

    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val file = pendingCaptureFile
            if (success && file != null) {
                onImageCaptured(file.readBytes())
            }
        }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                val (uri, file) = createCaptureTarget(context)
                pendingCaptureFile = file
                takePictureLauncher.launch(uri)
            }
        }

    Button(onClick = {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val (uri, file) = createCaptureTarget(context)
            pendingCaptureFile = file
            takePictureLauncher.launch(uri)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }) {
        Text(text = "Take photo")
    }
}

/**
 * Creates a fresh capture target file in [Context.getCacheDir] and returns its `content://`
 * [Uri] (via `FileProvider`) alongside the underlying [File].
 */
private fun createCaptureTarget(context: Context): Pair<Uri, File> {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(imagesDir, "capture_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return uri to file
}
