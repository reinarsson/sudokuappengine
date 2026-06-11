package com.sudokuengine.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Single-activity entry point for the Sudoku app.
 *
 * This is a scaffold-only screen: it proves the Compose/AGP/Kotlin wiring builds. The
 * camera/file-picker → reader → solver → result pipeline is added in follow-up PRs (see
 * `docs/APP_SPEC.md`).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SudokuAppTheme {
                HelloSudokuScreen()
            }
        }
    }
}

@Composable
private fun SudokuAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
private fun HelloSudokuScreen() {
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
                Text(text = "Hello Sudoku")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HelloSudokuScreenPreview() {
    SudokuAppTheme {
        HelloSudokuScreen()
    }
}
