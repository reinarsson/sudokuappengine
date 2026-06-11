package com.sudokuengine.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.sudokuengine.app.ui.HomeScreen

/**
 * Single-activity entry point for the Sudoku app.
 *
 * Shows [HomeScreen], which lets the user pick an image from the gallery or take a photo with
 * the camera. The reader → solver → result pipeline is added in follow-up PRs (see
 * `docs/APP_SPEC.md`).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SudokuAppTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
fun SudokuAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
