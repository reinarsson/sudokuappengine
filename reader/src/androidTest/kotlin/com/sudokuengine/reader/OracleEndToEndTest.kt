package com.sudokuengine.reader

import androidx.test.platform.app.InstrumentationRegistry
import com.sudokuengine.reader.litert.LiteRtDigitClassifier
import com.sudokuengine.reader.opencv.OpenCvSudokuReader
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Minimum per-board accuracy over filled cells (cells where the ground truth digit is non-zero).
 *
 * The finetuned `digits.tflite` model is not perfect on printed digits: there is a known
 * domain gap between the handwritten-digit training distribution and printed Sudoku puzzle
 * fonts, which causes a small fraction of misreads (e.g. a printed `6` read as `8`). The Python
 * reference pipeline, using the identical model weights, makes the exact same misread on one of
 * the sample boards (38/39 filled cells correct = 97.4%), so exact 100% match across all sample
 * boards is not achievable and was never required by the reference (its `MIN_GRID_ACCURACY` is
 * 0.65). This finetuned model is much stronger than that floor, so we require 0.95: high enough
 * that the gate still fails hard on a broken model/pipeline contract (which yields near-0%
 * accuracy) and on real regressions, while tolerating the model's known small error rate.
 */
private const val MIN_FILLED_ACCURACY = 0.95

/**
 * End-to-end oracle: for each sample image under `androidTest/assets/samples`,
 * [SudokuReader.read] must return [ReadOutcome.Success], and the resulting `grid` must match the
 * `grid` array in the sibling JSON file with at least [MIN_FILLED_ACCURACY] accuracy over the
 * cells the ground truth marks as filled (non-zero). Exact match is not required because of a
 * known small printed-digit domain gap in the model (see [MIN_FILLED_ACCURACY] KDoc).
 *
 * Runs against the real, committed `digits.tflite` model and the real OpenCV pipeline.
 */
class OracleEndToEndTest {
    @Test
    fun readsAllSampleBoardsWithinAccuracyThreshold() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val testContext = instrumentation.context
        val appContext = instrumentation.targetContext

        val modelBytes = TestAssets.readAppAsset(appContext, "digits.tflite")
        val classifier = LiteRtDigitClassifier.fromModelBytes(modelBytes)
        val reader = OpenCvSudokuReader(classifier)

        val basenames = TestAssets.sampleBasenames(testContext)
        check(basenames.isNotEmpty()) { "No sample boards found under samples/" }

        data class Mismatch(val basename: String, val row: Int, val col: Int, val expected: Int, val actual: Int)

        val mismatches = mutableListOf<Mismatch>()
        val belowThreshold = mutableListOf<String>()

        for (basename in basenames) {
            val imageBytes = TestAssets.readTestAsset(testContext, "samples/$basename.png")
            val expected = TestAssets.expectedGrid(testContext, basename)

            val outcome = reader.read(imageBytes)
            val success =
                assertIs<ReadOutcome.Success>(outcome, "Expected Success for $basename, got $outcome")
            val actual = success.result.grid

            var filledCount = 0
            var correctCount = 0
            for (r in 0 until 9) {
                for (c in 0 until 9) {
                    val expectedValue = expected[r][c]
                    if (expectedValue == 0) continue
                    filledCount++
                    val actualValue = actual[r][c]
                    if (actualValue == expectedValue) {
                        correctCount++
                    } else {
                        mismatches.add(Mismatch(basename, r, c, expectedValue, actualValue))
                    }
                }
            }

            val accuracy = if (filledCount == 0) 1.0 else correctCount.toDouble() / filledCount
            if (accuracy < MIN_FILLED_ACCURACY) {
                belowThreshold.add(
                    "$basename: accuracy=$accuracy ($correctCount/$filledCount filled cells correct)",
                )
            }
        }

        assertTrue(
            belowThreshold.isEmpty(),
            buildString {
                appendLine("Boards below MIN_FILLED_ACCURACY ($MIN_FILLED_ACCURACY):")
                belowThreshold.forEach { appendLine("  $it") }
                appendLine("Mismatches:")
                mismatches.forEach {
                    appendLine(
                        "  ${it.basename} (${it.row},${it.col}): expected=${it.expected} actual=${it.actual}",
                    )
                }
            },
        )
    }
}
