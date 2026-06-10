package com.sudokuengine.reader

/**
 * Turns a photo or screenshot of a Sudoku board into a 9×9 [Grid].
 *
 * Operating assumption (carried over from the Python pipeline): the photo is roughly upright and the
 * board is the largest object in frame. There is no 90°/180° orientation handling and no heavy-skew
 * correction beyond the perspective warp.
 *
 * Implementations are the Android adapters; the orchestration depends only on a [DigitClassifier].
 */
interface SudokuReader {
    /**
     * Reads the board in [image].
     *
     * @param image encoded JPEG/PNG bytes, decodable by OpenCV (not an `android.graphics.Bitmap`,
     *   so the orchestration is testable without an emulator).
     * @return [ReadOutcome.Success] with the grid and per-cell confidence, or
     *   [ReadOutcome.BoardNotFound] if no board could be located.
     */
    fun read(image: ByteArray): ReadOutcome
}
