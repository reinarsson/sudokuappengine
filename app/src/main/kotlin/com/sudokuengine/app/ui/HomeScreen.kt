package com.sudokuengine.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sudokuengine.app.SudokuAppTheme

/**
 * Returns the status text shown to the user after a gallery picker round-trip.
 *
 * - `null` means the user has not picked anything yet (or cancelled the picker), and is
 *   reported as "No image selected".
 * - A non-null [imageBytes] reports its size, e.g. "Selected image: 1234 bytes".
 *
 * This function is pure and platform-independent so it can be unit-tested without Android.
 */
fun imageStatusText(imageBytes: ByteArray?): String =
    if (imageBytes == null) {
        "No image selected"
    } else {
        "Selected image: ${imageBytes.size} bytes"
    }

/**
 * Home screen of the Sudoku app: lets the user pick an image from the device gallery or
 * capture one with the camera, and shows a placeholder status confirming the image bytes
 * were read.
 *
 * The reader/solver pipeline is wired up in a follow-up PR (see `docs/APP_SPEC.md`); for now
 * this screen only proves the file-picker and camera-capture entry points work end to end.
 */
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var hasPicked by remember { mutableStateOf(false) }

    val pickImageLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            hasPicked = true
            imageBytes =
                uri?.let {
                    context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
                }
        }

    val statusText =
        if (!hasPicked) {
            "Nothing picked yet"
        } else {
            imageStatusText(imageBytes)
        }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold { innerPadding ->
            val boxModifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            Box(
                modifier = boxModifier,
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Button(onClick = {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }) {
                        Text(text = "Pick image from gallery")
                    }
                    Text(text = statusText)
                    CameraCaptureButton()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SudokuAppTheme {
        HomeScreen()
    }
}
