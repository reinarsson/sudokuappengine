package com.sudokuengine.reader

import androidx.test.platform.app.InstrumentationRegistry
import com.sudokuengine.reader.opencv.OpenCvSudokuReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Cells the Python pipeline marks empty (`grid` value `0`) must:
 *  - resolve to `0` in the returned [Grid] with `0f` confidence, and
 *  - never reach [DigitClassifier.classify].
 *
 * Verified with a counting fake classifier so the assertion doesn't depend on the real model.
 */
class EmptyCellHandlingTest {
    private class CountingClassifier : DigitClassifier {
        val classifiedCells = mutableListOf<FloatArray>()

        override fun classify(cell: FloatArray): Prediction {
            classifiedCells += cell
            // Deterministic, arbitrary non-zero prediction so assembly logic has something to
            // place for filled cells.
            return Prediction(digit = 1, confidence = 1.0f)
        }
    }

    @Test
    fun emptyCellsAreZeroAndNeverClassified() {
        val testContext = InstrumentationRegistry.getInstrumentation().context

        val basenames = TestAssets.sampleBasenames(testContext)
        check(basenames.isNotEmpty()) { "No sample boards found under samples/" }
        val basename = basenames.first()

        val expected = TestAssets.expectedGrid(testContext, basename)
        val emptyCellCount = expected.sumOf { row -> row.count { it == 0 } }
        val filledCellCount = 81 - emptyCellCount

        val classifier = CountingClassifier()
        val reader = OpenCvSudokuReader(classifier)

        val imageBytes = TestAssets.readTestAsset(testContext, "samples/$basename.png")
        val outcome = reader.read(imageBytes)
        val success = assertIs<ReadOutcome.Success>(outcome)

        // The classifier is invoked exactly once per non-empty cell (per the oracle JSON).
        assertEquals(filledCellCount, classifier.classifiedCells.size)

        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (expected[r][c] == 0) {
                    assertEquals(0, success.result.grid[r][c], "grid[$r][$c] should be empty")
                    assertEquals(0f, success.result.confidence[r][c], "confidence[$r][$c] should be 0f")
                }
            }
        }
    }
}
