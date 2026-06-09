package com.sudokuengine

import kotlin.test.Test
import kotlin.test.assertFailsWith

class SudokuSolverTest {
    @Test
    fun `default solve implementation throws NotImplementedError`() {
        val solver = object : SudokuSolver {}
        val puzzle: Grid = Array(9) { IntArray(9) }
        assertFailsWith<NotImplementedError> { solver.solve(puzzle) }
    }
}
