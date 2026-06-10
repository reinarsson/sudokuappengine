package com.sudokuengine.reader.opencv

import com.sudokuengine.reader.DigitClassifier
import com.sudokuengine.reader.GridAssembler
import com.sudokuengine.reader.ReadOutcome
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import com.sudokuengine.reader.SudokuReader as SudokuReaderPort

/**
 * OpenCV/LiteRT-backed [SudokuReaderPort] implementation.
 *
 * Pipeline (mirrors `sudoku_reader.py`): decode -> [BoardLocator.findBoard] ->
 * [BoardLocator.splitCells] -> [CellPreprocessor.preprocess] -> [DigitClassifier.classify] ->
 * [GridAssembler.assemble]. Empty cells (per [CellPreprocessor]) never reach [classifier].
 *
 * Ensures the OpenCV native library is loaded via [OpenCvInit] before doing any [Mat] work.
 *
 * @property classifier the digit model adapter; only invoked for non-empty cells.
 */
class OpenCvSudokuReader(
    private val classifier: DigitClassifier,
) : SudokuReaderPort {
    override fun read(image: ByteArray): ReadOutcome {
        OpenCvInit.ensureLoaded()

        val encoded = MatOfByte(*image)
        val gray = Imgcodecs.imdecode(encoded, Imgcodecs.IMREAD_GRAYSCALE)
        encoded.release()

        if (gray.empty()) {
            return ReadOutcome.BoardNotFound
        }

        val board = BoardLocator.findBoard(gray, CellTuning.CELL_PX)
        gray.release()
        if (board == null) {
            return ReadOutcome.BoardNotFound
        }

        val cells = BoardLocator.splitCells(board, CellTuning.CELL_PX)
        board.release()

        val predictions =
            cells.map { cell ->
                val prepped = CellPreprocessor.preprocess(cell)
                cell.release()
                prepped?.let { classifier.classify(it) }
            }

        val result = GridAssembler.assemble(predictions)
        return ReadOutcome.Success(result)
    }
}
