package com.sudokuengine.app.ui

import com.sudokuengine.app.pipeline.PipelineResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Unit tests for [errorMessageFor], the pure [PipelineResult] -> error message mapping. */
class AppScreenStateTest {
    @Test
    fun `board not found maps to 'No board found'`() {
        assertEquals("No board found", errorMessageFor(PipelineResult.BoardNotFound))
    }

    @Test
    fun `unsolvable maps to 'No solution'`() {
        assertEquals("No solution", errorMessageFor(PipelineResult.Unsolvable))
    }

    @Test
    fun `invalid maps to its reason`() {
        val result = PipelineResult.Invalid("duplicate 5 in row 0")

        assertEquals("duplicate 5 in row 0", errorMessageFor(result))
    }

    @Test
    fun `success has no error message`() {
        val grid = Array(9) { IntArray(9) }
        val result = PipelineResult.Success(originalGrid = grid, solvedGrid = grid)

        assertFailsWith<IllegalArgumentException> { errorMessageFor(result) }
    }
}
