package com.sudokuengine.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Button that captures a photo with the device camera and reports the result as status text.
 *
 * Handles the `CAMERA` runtime permission and the [ActivityResultContracts.TakePicture] flow:
 * a temporary file in [Context.getCacheDir] is exposed via `FileProvider` as the capture
 * target, and on success its bytes are read back and summarised via [captureStatusFor].
 *
 * No reader/solver pipeline is wired up here (see `docs/APP_SPEC.md`); this only proves that
 * image bytes can be captured.
 */
@Composable
fun CameraCaptureButton() {
    val context = LocalContext.current

    var captureStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val file = pendingCaptureFile
            captureStatus =
                if (success && file != null) {
                    captureStatusFor(file.readBytes())
                } else {
                    CAPTURE_CANCELLED
                }
        }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                val (uri, file) = createCaptureTarget(context)
                pendingCaptureFile = file
                takePictureLauncher.launch(uri)
            } else {
                captureStatus = CAMERA_PERMISSION_DENIED
            }
        }

    Column {
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

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = captureStatus ?: NO_IMAGE_CAPTURED_YET)
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
