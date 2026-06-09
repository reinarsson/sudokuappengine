package com.sudokuengine

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.fail

/**
 * Oracle / golden tests.
 *
 * The fixture `src/test/resources/golden_puzzles.json` is produced **externally** by the
 * independent Python `pulp` solver and committed by the maintainer. It is never generated,
 * fabricated, or computed here — doing so would make the test circular and worthless.
 *
 * Expected fixture format — a JSON array of objects, each with a `puzzle` and a `solution`,
 * where each is either an 81-character string (row-major; `0` or `.` for blanks) or a 9×9
 * array of integers:
 * ```json
 * [
 *   { "puzzle": "53007...", "solution": "534678..." }
 * ]
 * ```
 *
 * Until the fixture is committed the test skips with a clear reminder rather than substituting
 * invented data; it activates automatically once the file is added.
 */
class GoldenPuzzlesTest {
    private val solver = SudokuSolver.create()

    @Test
    fun `each golden puzzle solves to its expected solution`() {
        val resource = javaClass.getResource("/golden_puzzles.json")
        if (resource == null) {
            println(
                "SKIP: golden_puzzles.json not present. It must be supplied by the maintainer " +
                    "from the external PuLP solver (do not fabricate it). This test activates " +
                    "automatically once the fixture is committed.",
            )
            return
        }
        val text = resource.readText()

        val cases = Json.parse(text) as List<*>
        if (cases.isEmpty()) fail("golden_puzzles.json contains no cases")

        cases.forEachIndexed { index, raw ->
            val case = raw as Map<*, *>
            val puzzle = readGrid(case["puzzle"], "case $index puzzle")
            val expected = readGrid(case["solution"], "case $index solution")

            val result = solver.solve(puzzle)
            val solved = assertIs<SolveResult.Solved>(result, "case $index expected Solved")
            assertGridEquals(expected, solved.grid)
        }
    }

    /** Reads a grid from either an 81-char string or a 9×9 nested array. */
    private fun readGrid(
        value: Any?,
        label: String,
    ): Grid =
        when (value) {
            is String -> {
                require(value.length == 81) { "$label string must be 81 chars, got ${value.length}" }
                Array(9) { r ->
                    IntArray(9) { c ->
                        val ch = value[r * 9 + c]
                        if (ch == '.' || ch == '0') 0 else ch.digitToInt()
                    }
                }
            }
            is List<*> -> {
                require(value.size == 9) { "$label array must have 9 rows" }
                Array(9) { r ->
                    val row = value[r] as List<*>
                    require(row.size == 9) { "$label row $r must have 9 columns" }
                    IntArray(9) { c -> (row[c] as Number).toInt() }
                }
            }
            else -> error("$label must be a string or a 9x9 array")
        }
}
