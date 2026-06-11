package com.sudokuengine.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudokuengine.Grid
import com.sudokuengine.app.SudokuAppTheme

/** Stroke width (in px) for the thin lines between adjacent cells. */
private const val THIN_LINE_WIDTH = 2f

/** Stroke width (in px) for the thick lines around each 3x3 box. */
private const val THICK_LINE_WIDTH = 6f

/** Text size for digits drawn in each cell. */
private val DIGIT_FONT_SIZE = 20.sp

/** Color used for cells that were given in the original (unsolved) board. */
private val GIVEN_COLOR = Color.Black

/** Color used for digits filled in by the solver. */
private val SOLVED_COLOR = Color(0xFF1565C0)

/**
 * Renders a 9x9 Sudoku board, per `docs/APP_SPEC.md`'s "Displaying the solved board" section.
 *
 * Draws thin lines between adjacent cells and thick lines around each 3x3 box. Each cell shows
 * the digit from [solvedGrid]; cells that were given in [originalGrid] (non-zero) are styled
 * differently (bold black) from cells the solver filled in (a different color, normal weight).
 *
 * Pure presentation: no business logic. [originalGrid] and [solvedGrid] are both 9x9
 * [Array]<[IntArray]> with values 0-9, where 0 means "empty".
 *
 * @param solvedGrid the fully solved 9x9 grid to render.
 * @param originalGrid the 9x9 grid as read from the image, before solving; non-zero cells are
 *   "givens" and are styled differently from solver-filled cells.
 * @param modifier applied to the outer [Canvas].
 */
@Composable
fun SudokuGrid(
    solvedGrid: Grid,
    originalGrid: Grid,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp),
    ) {
        val cellSize = size.minDimension / 9f

        drawDigits(solvedGrid, originalGrid, cellSize, textMeasurer)
        drawGridLines(cellSize)
    }
}

/** Draws each cell's digit, styling givens (from [originalGrid]) differently from solved cells. */
private fun DrawScope.drawDigits(
    solvedGrid: Grid,
    originalGrid: Grid,
    cellSize: Float,
    textMeasurer: TextMeasurer,
) {
    for (row in 0 until 9) {
        for (col in 0 until 9) {
            val value = solvedGrid[row][col]
            if (value == 0) continue

            val isGiven = originalGrid[row][col] != 0
            val color = if (isGiven) GIVEN_COLOR else SOLVED_COLOR
            val fontWeight = if (isGiven) FontWeight.Bold else FontWeight.Normal

            drawDigit(value, row, col, cellSize, color, fontWeight, textMeasurer)
        }
    }
}

/** Draws a single digit centered in cell ([row], [col]). */
private fun DrawScope.drawDigit(
    value: Int,
    row: Int,
    col: Int,
    cellSize: Float,
    color: Color,
    fontWeight: FontWeight,
    textMeasurer: TextMeasurer,
) {
    val textStyle =
        TextStyle(
            color = color,
            fontSize = DIGIT_FONT_SIZE,
            fontWeight = fontWeight,
        )
    val layoutResult = textMeasurer.measure(text = value.toString(), style = textStyle)

    val x = col * cellSize + (cellSize - layoutResult.size.width) / 2f
    val y = row * cellSize + (cellSize - layoutResult.size.height) / 2f

    translate(left = x, top = y) {
        drawText(layoutResult)
    }
}

/** Draws the 10x10 grid of lines: thin between cells, thick around each 3x3 box. */
private fun DrawScope.drawGridLines(cellSize: Float) {
    val gridSize = cellSize * 9f

    for (i in 0..9) {
        val strokeWidth = if (i % 3 == 0) THICK_LINE_WIDTH else THIN_LINE_WIDTH
        val offset = i * cellSize

        // Horizontal line.
        drawLine(
            color = Color.Black,
            start = Offset(0f, offset),
            end = Offset(gridSize, offset),
            strokeWidth = strokeWidth,
        )

        // Vertical line.
        drawLine(
            color = Color.Black,
            start = Offset(offset, 0f),
            end = Offset(offset, gridSize),
            strokeWidth = strokeWidth,
        )
    }
}

/** Sample solved board for the preview, with a handful of "givens" to show both cell styles. */
private fun previewSolvedGrid(): Grid =
    arrayOf(
        intArrayOf(5, 3, 4, 6, 7, 8, 9, 1, 2),
        intArrayOf(6, 7, 2, 1, 9, 5, 3, 4, 8),
        intArrayOf(1, 9, 8, 3, 4, 2, 5, 6, 7),
        intArrayOf(8, 5, 9, 7, 6, 1, 4, 2, 3),
        intArrayOf(4, 2, 6, 8, 5, 3, 7, 9, 1),
        intArrayOf(7, 1, 3, 9, 2, 4, 8, 5, 6),
        intArrayOf(9, 6, 1, 5, 3, 7, 2, 8, 4),
        intArrayOf(2, 8, 7, 4, 1, 9, 6, 3, 5),
        intArrayOf(3, 4, 5, 2, 8, 6, 1, 7, 9),
    )

/** Sample original (unsolved) board for the preview: only the first three rows have givens. */
private fun previewOriginalGrid(): Grid =
    arrayOf(
        intArrayOf(5, 3, 0, 0, 7, 0, 0, 0, 0),
        intArrayOf(6, 0, 0, 1, 9, 5, 0, 0, 0),
        intArrayOf(0, 9, 8, 0, 0, 0, 0, 6, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
    )

@Preview(showBackground = true)
@Composable
private fun SudokuGridPreview() {
    SudokuAppTheme {
        SudokuGrid(solvedGrid = previewSolvedGrid(), originalGrid = previewOriginalGrid())
    }
}
