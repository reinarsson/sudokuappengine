package com.sudokuengine.app.pipeline

import com.sudokuengine.SudokuSolver
import com.sudokuengine.reader.ConfidenceGrid
import com.sudokuengine.reader.Grid
import com.sudokuengine.reader.ReadOutcome
import com.sudokuengine.reader.ReadResult
import com.sudokuengine.reader.SudokuReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** A [SudokuReader] stub returning a pre-configured [ReadOutcome], for orchestration tests. */
private class FakeSudokuReader(private val outcome: ReadOutcome) : SudokuReader {
    override fun read(image: ByteArray): ReadOutcome = outcome
}

/** Builds a [Grid] from nine 9-character rows where '0' or '.' denotes an empty cell. */
private fun gridOf(vararg rows: String): Grid {
    require(rows.size == 9) { "expected 9 rows, got ${rows.size}" }
    return Array(9) { r ->
        val row = rows[r]
        require(row.length == 9) { "row $r must have 9 characters, got '$row'" }
        IntArray(9) { c ->
            val ch = row[c]
            if (ch == '.' || ch == '0') 0 else ch.digitToInt()
        }
    }
}

/** A [ConfidenceGrid] of all-1f, matching a fully-confident read. */
private fun fullConfidenceGrid(): ConfidenceGrid = Array(9) { FloatArray(9) { 1f } }

/** A fresh all-zero (empty) grid. */
private fun emptyGrid(): Grid = Array(9) { IntArray(9) }

class SolvePipelineTest {
    private val solver = SudokuSolver.create()

    // The classic Wikipedia example puzzle; a proper puzzle with a unique solution.
    private val properPuzzle =
        gridOf(
            "530070000",
            "600195000",
            "098000060",
            "800060003",
            "400803001",
            "700020006",
            "060000280",
            "000419005",
            "000080079",
        )

    @Test
    fun `success path returns the original and solved grids`() {
        val reader = FakeSudokuReader(ReadOutcome.Success(ReadResult(properPuzzle, fullConfidenceGrid())))
        val pipeline = SolvePipeline(reader, solver)

        val result = pipeline.solve(ByteArray(0))

        val success = assertIs<PipelineResult.Success>(result)
        assertGridEquals(properPuzzle, success.originalGrid)
        assertValidCompletedGrid(success.solvedGrid)
        assertContainsGivens(properPuzzle, success.solvedGrid)
    }

    @Test
    fun `board not found maps to BoardNotFound`() {
        val reader = FakeSudokuReader(ReadOutcome.BoardNotFound)
        val pipeline = SolvePipeline(reader, solver)

        val result = pipeline.solve(ByteArray(0))

        assertEquals(PipelineResult.BoardNotFound, result)
    }

    @Test
    fun `rule-consistent but uncompletable grid maps to Unsolvable`() {
        // Row 0 holds 1..8, so (0,8) must be 9; but column 8 already has a 9 at (1,8).
        val grid = emptyGrid()
        for (col in 0 until 8) grid[0][col] = col + 1
        grid[1][8] = 9

        val reader = FakeSudokuReader(ReadOutcome.Success(ReadResult(grid, fullConfidenceGrid())))
        val pipeline = SolvePipeline(reader, solver)

        val result = pipeline.solve(ByteArray(0))

        assertEquals(PipelineResult.Unsolvable, result)
    }

    @Test
    fun `rule-violating grid maps to Invalid with a reason`() {
        // Duplicate 5s in row 0 already break Sudoku rules.
        val grid = emptyGrid()
        grid[0][0] = 5
        grid[0][8] = 5

        val reader = FakeSudokuReader(ReadOutcome.Success(ReadResult(grid, fullConfidenceGrid())))
        val pipeline = SolvePipeline(reader, solver)

        val result = pipeline.solve(ByteArray(0))

        val invalid = assertIs<PipelineResult.Invalid>(result)
        assertTrue(invalid.reason.isNotBlank())
    }
}

/** Asserts the two grids hold identical values. */
private fun assertGridEquals(
    expected: Grid,
    actual: Grid,
) {
    for (r in 0 until 9) {
        assertTrue(expected[r].contentEquals(actual[r]), "row $r differs: expected ${expected[r].toList()}, got ${actual[r].toList()}")
    }
}

/** Asserts that every non-zero given in [puzzle] is preserved at the same position in [grid]. */
private fun assertContainsGivens(
    puzzle: Grid,
    grid: Grid,
) {
    for (r in 0 until 9) {
        for (c in 0 until 9) {
            if (puzzle[r][c] != 0) {
                assertEquals(puzzle[r][c], grid[r][c], "given at ($r, $c) changed")
            }
        }
    }
}

/** Asserts [grid] is fully filled with 1..9 and breaks no Sudoku row/column/box rule. */
private fun assertValidCompletedGrid(grid: Grid) {
    for (r in 0 until 9) {
        for (c in 0 until 9) {
            assertTrue(grid[r][c] in 1..9, "cell ($r, $c) = ${grid[r][c]} is not in 1..9")
        }
    }
    for (i in 0 until 9) {
        assertTrue(distinctDigits((0 until 9).map { grid[i][it] }), "row $i has a duplicate")
        assertTrue(distinctDigits((0 until 9).map { grid[it][i] }), "column $i has a duplicate")
    }
    for (boxRow in 0 until 3) {
        for (boxCol in 0 until 3) {
            val cells =
                (0 until 3).flatMap { dr ->
                    (0 until 3).map { dc -> grid[boxRow * 3 + dr][boxCol * 3 + dc] }
                }
            assertTrue(distinctDigits(cells), "box ($boxRow, $boxCol) has a duplicate")
        }
    }
}

private fun distinctDigits(values: List<Int>): Boolean = values.toSet().size == values.size
