package com.sudokuengine.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sudokuengine.SudokuSolver
import com.sudokuengine.app.SudokuAppTheme
import com.sudokuengine.app.pipeline.PipelineResult
import com.sudokuengine.app.pipeline.SolvePipeline
import com.sudokuengine.reader.SudokuReader
import com.sudokuengine.reader.litert.LiteRtDigitClassifier
import com.sudokuengine.reader.opencv.OpenCvSudokuReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * Builds a [SolvePipeline] backed by the real [OpenCvSudokuReader] (using the bundled
 * `digits.tflite` model from `:reader`'s assets) and the default [SudokuSolver].
 *
 * Construction reads the model bytes from [android.content.res.AssetManager], which is
 * relatively cheap but should still happen only once per app session — callers should
 * `remember` the result.
 */
private fun buildSolvePipeline(context: android.content.Context): SolvePipeline {
    val modelBytes = context.assets.open("digits.tflite").use { it.readBytes() }
    val classifier = LiteRtDigitClassifier.fromModelBytes(modelBytes)
    val reader: SudokuReader = OpenCvSudokuReader(classifier)
    return SolvePipeline(reader, SudokuSolver.create())
}

/**
 * Home screen of the Sudoku app: lets the user pick an image from the device gallery or
 * capture one with the camera, runs it through [SolvePipeline], and renders the result.
 *
 * The screen is driven by [AppScreenState]:
 * - [AppScreenState.Idle] — shows the picker/capture entry points.
 * - [AppScreenState.Loading] — shown while [SolvePipeline.solve] runs off the main thread.
 * - [AppScreenState.Result] — shows [SudokuGrid] for the solved board.
 * - [AppScreenState.Error] — shows the error message and a "Try again" button that resets to
 *   [AppScreenState.Idle].
 */
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val pipeline = remember { buildSolvePipeline(context) }

    var screenState by remember { mutableStateOf<AppScreenState>(AppScreenState.Idle) }
    var pendingImage by remember { mutableStateOf<ByteArray?>(null) }

    val pickImageLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            val bytes =
                uri?.let {
                    context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
                }
            if (bytes != null) {
                pendingImage = bytes
                screenState = AppScreenState.Loading
            }
        }

    LaunchedEffect(pendingImage) {
        val image = pendingImage ?: return@LaunchedEffect
        val result = withContext(Dispatchers.Default) { pipeline.solve(image) }
        screenState =
            when (result) {
                is PipelineResult.Success -> AppScreenState.Result(result.originalGrid, result.solvedGrid)
                else -> AppScreenState.Error(errorMessageFor(result))
            }
        pendingImage = null
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
                when (val state = screenState) {
                    is AppScreenState.Idle ->
                        IdleContent(
                            onPickImage = {
                                pickImageLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            onImageCaptured = { bytes ->
                                pendingImage = bytes
                                screenState = AppScreenState.Loading
                            },
                        )
                    is AppScreenState.Loading -> LoadingContent()
                    is AppScreenState.Result ->
                        SudokuGrid(
                            solvedGrid = state.solvedGrid,
                            originalGrid = state.originalGrid,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    is AppScreenState.Error ->
                        ErrorContent(
                            message = state.message,
                            onTryAgain = { screenState = AppScreenState.Idle },
                        )
                }
            }
        }
    }
}

/** Idle-state content: the gallery picker and camera capture entry points. */
@Composable
private fun IdleContent(
    onPickImage: () -> Unit,
    onImageCaptured: (ByteArray) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(onClick = onPickImage) {
            Text(text = "Pick image from gallery")
        }
        CameraCaptureButton(onImageCaptured = onImageCaptured)
    }
}

/** Loading-state content: a progress indicator shown while the pipeline runs. */
@Composable
private fun LoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(text = "Solving...")
    }
}

/** Error-state content: the error [message] and a "Try again" button resetting to [AppScreenState.Idle]. */
@Composable
private fun ErrorContent(
    message: String,
    onTryAgain: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = message)
        Button(onClick = onTryAgain) {
            Text(text = "Try again")
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
