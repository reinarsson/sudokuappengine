package com.sudokuengine.reader

/**
 * A 9×9 Sudoku grid in row-major order. `0` denotes an empty cell; `1..9` a filled digit.
 *
 * This is the same shape the solver consumes, so a [ReadResult] can be handed straight to it.
 */
typealias Grid = Array<IntArray>

/**
 * Per-cell read confidence aligned 1:1 with a [Grid]. Each value is in `0f..1f`; empty cells are
 * reported as `0f`. The app uses this to decide whether to trust a read or ask for a retake — this
 * module only reports it.
 */
typealias ConfidenceGrid = Array<FloatArray>

/**
 * The outcome of a successful read: the recognised [grid] and the matching per-cell [confidence].
 */
data class ReadResult(val grid: Grid, val confidence: ConfidenceGrid)

/**
 * The result of [SudokuReader.read]: either a [Success] carrying a [ReadResult], or
 * [BoardNotFound] when no Sudoku board could be located in the image.
 */
sealed interface ReadOutcome {
    /** A board was located and read. */
    data class Success(val result: ReadResult) : ReadOutcome

    /** No Sudoku board could be located in the image. */
    data object BoardNotFound : ReadOutcome
}

/**
 * A single-cell digit prediction.
 *
 * @property digit the recognised digit, in `1..9` (empty cells never reach the classifier).
 * @property confidence the model's confidence for [digit], in `0f..1f`.
 */
data class Prediction(val digit: Int, val confidence: Float)
