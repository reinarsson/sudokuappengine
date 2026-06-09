package com.sudokuengine

import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Builds a [Grid] from nine 9-character rows where '0' or '.' denotes an empty cell. */
fun gridOf(vararg rows: String): Grid {
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

/** A fresh all-zero (empty) grid. */
fun emptyGrid(): Grid = Array(9) { IntArray(9) }

/** Asserts the two grids hold identical values. */
fun assertGridEquals(
    expected: Grid,
    actual: Grid,
) {
    for (r in 0 until 9) {
        assertTrue(expected[r].contentEquals(actual[r]), "row $r differs: expected ${expected[r].toList()}, got ${actual[r].toList()}")
    }
}

/** Asserts that every non-zero given in [puzzle] is preserved at the same position in [grid]. */
fun assertContainsGivens(
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
fun assertValidCompletedGrid(grid: Grid) {
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
