package com.sudokuengine.reader

/**
 * Pure assembly logic: turns 81 per-cell [Prediction]s (or `null` for empty cells, in row-major
 * order) into the [Grid] / [ConfidenceGrid] pair returned by [SudokuReader.read].
 *
 * Kept free of OpenCV/LiteRT so it can be unit-tested with a fake [DigitClassifier] and canned
 * predictions.
 */
internal object GridAssembler {
    /** Number of cells per row/column of a Sudoku board. */
    const val BOARD_SIZE: Int = 9

    /**
     * Assembles a [ReadResult] from [predictions], a row-major list of 81 entries where index
     * `r * 9 + c` holds the prediction for row `r`, column `c`, or `null` if that cell is empty.
     *
     * @throws IllegalArgumentException if [predictions] does not contain exactly 81 entries.
     */
    fun assemble(predictions: List<Prediction?>): ReadResult {
        require(predictions.size == BOARD_SIZE * BOARD_SIZE) {
            "Expected ${BOARD_SIZE * BOARD_SIZE} predictions, got ${predictions.size}"
        }

        val grid: Grid = Array(BOARD_SIZE) { IntArray(BOARD_SIZE) }
        val confidence: ConfidenceGrid = Array(BOARD_SIZE) { FloatArray(BOARD_SIZE) }

        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                val prediction = predictions[r * BOARD_SIZE + c]
                if (prediction != null) {
                    grid[r][c] = prediction.digit
                    confidence[r][c] = prediction.confidence
                }
            }
        }

        return ReadResult(grid = grid, confidence = confidence)
    }
}
