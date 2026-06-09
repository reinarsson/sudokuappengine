package com.sudokuengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SudokuSolverTest {
    private val solver = SudokuSolver.create()

    // A known-valid completed grid (a mathematical fact, not derived from the solver under test).
    private val solvedGrid =
        gridOf(
            "534678912",
            "672195348",
            "198342567",
            "859761423",
            "426853791",
            "713924856",
            "961537284",
            "287419635",
            "345286179",
        )

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
    fun `solves a proper puzzle to a valid completed grid containing the givens`() {
        val result = solver.solve(properPuzzle)
        val solved = assertIs<SolveResult.Solved>(result)
        assertValidCompletedGrid(solved.grid)
        assertContainsGivens(properPuzzle, solved.grid)
    }

    @Test
    fun `a complete valid grid solves to itself`() {
        val result = solver.solve(solvedGrid)
        val solved = assertIs<SolveResult.Solved>(result)
        assertGridEquals(solvedGrid, solved.grid)
    }

    @Test
    fun `empty grid yields a valid completed grid`() {
        val result = solver.solve(emptyGrid())
        val solved = assertIs<SolveResult.Solved>(result)
        assertValidCompletedGrid(solved.grid)
    }

    @Test
    fun `wrong number of rows is invalid`() {
        val tooFewRows: Grid = Array(8) { IntArray(9) }
        assertIs<SolveResult.Invalid>(solver.solve(tooFewRows))
    }

    @Test
    fun `wrong number of columns is invalid`() {
        val grid = emptyGrid()
        grid[0] = IntArray(8)
        assertIs<SolveResult.Invalid>(solver.solve(grid))
    }

    @Test
    fun `out-of-range value is invalid`() {
        val grid = emptyGrid()
        grid[0][0] = 10
        assertIs<SolveResult.Invalid>(solver.solve(grid))

        val negative = emptyGrid()
        negative[4][4] = -1
        assertIs<SolveResult.Invalid>(solver.solve(negative))
    }

    @Test
    fun `duplicate given in a row is invalid`() {
        val grid = emptyGrid()
        grid[0][0] = 5
        grid[0][8] = 5
        assertIs<SolveResult.Invalid>(solver.solve(grid))
    }

    @Test
    fun `duplicate given in a column is invalid`() {
        val grid = emptyGrid()
        grid[0][3] = 7
        grid[8][3] = 7
        assertIs<SolveResult.Invalid>(solver.solve(grid))
    }

    @Test
    fun `duplicate given in a box is invalid`() {
        val grid = emptyGrid()
        grid[0][0] = 3
        grid[1][1] = 3
        assertIs<SolveResult.Invalid>(solver.solve(grid))
    }

    @Test
    fun `rule-consistent but uncompletable grid is unsolvable`() {
        // Row 0 holds 1..8, so (0,8) must be 9; but column 8 already has a 9 at (1,8).
        val grid = emptyGrid()
        for (col in 0 until 8) grid[0][col] = col + 1
        grid[1][8] = 9
        assertEquals(SolveResult.Unsolvable, solver.solve(grid))
    }

    @Test
    fun `solve does not mutate the caller's input`() {
        val input = properPuzzle
        val before = input.map { it.copyOf() }
        solver.solve(input)
        before.forEachIndexed { row, expected ->
            assertTrue(expected.contentEquals(input[row]), "row $row was mutated")
        }
    }

    @Test
    fun `solving the same puzzle twice yields identical output`() {
        val first = solver.solve(properPuzzle)
        val second = solver.solve(properPuzzle)
        val a = assertIs<SolveResult.Solved>(first)
        val b = assertIs<SolveResult.Solved>(second)
        assertGridEquals(a.grid, b.grid)
    }

    @Test
    fun `a proper puzzle has a unique solution`() {
        assertTrue(solver.hasUniqueSolution(properPuzzle))
    }

    @Test
    fun `an ambiguous puzzle does not have a unique solution`() {
        // The empty grid admits many completions.
        assertFalse(solver.hasUniqueSolution(emptyGrid()))
    }

    @Test
    fun `uniqueness is false for invalid input`() {
        val grid = emptyGrid()
        grid[0][0] = 5
        grid[0][1] = 5
        assertFalse(solver.hasUniqueSolution(grid))
    }

    @Test
    fun `unsolvable puzzle does not have a unique solution`() {
        val grid = emptyGrid()
        for (col in 0 until 8) grid[0][col] = col + 1
        grid[1][8] = 9
        assertFalse(solver.hasUniqueSolution(grid))
    }

    @Test
    fun `solves a known hard puzzle quickly and correctly`() {
        // "AI Escargot" — a notoriously hard instance. We assert validity, not a fabricated
        // solution: the result must be a valid completed grid that preserves the givens.
        val hard =
            gridOf(
                "100007090",
                "030020008",
                "009600500",
                "005300900",
                "010080002",
                "600004000",
                "300000010",
                "040000007",
                "007000300",
            )
        val result = solver.solve(hard)
        val solved = assertIs<SolveResult.Solved>(result)
        assertValidCompletedGrid(solved.grid)
        assertContainsGivens(hard, solved.grid)
    }
}
