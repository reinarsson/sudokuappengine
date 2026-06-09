package com.sudokuengine

/** Bit pattern with all nine digit bits set (digits 1..9 → bits 0..8). */
private const val ALL_DIGITS = 0x1FF

/** Side length of the grid and of a box's span. */
private const val SIZE = 9
private const val BOX = 3

/**
 * Default [SudokuSolver]: backtracking search with constraint propagation via row/column/box
 * bitmasks and a Minimum-Remaining-Values cell-selection heuristic.
 *
 * Pure, deterministic, and stateless across calls — a single instance is safe to reuse. Each
 * call builds its own working state, so the caller's grid is never mutated.
 */
internal class DefaultSudokuSolver : SudokuSolver {
    override fun solve(puzzle: Grid): SolveResult {
        validate(puzzle)?.let { return SolveResult.Invalid(it) }
        val engine = Engine(puzzle)
        return if (engine.solveFirst()) SolveResult.Solved(engine.toGrid()) else SolveResult.Unsolvable
    }

    override fun hasUniqueSolution(puzzle: Grid): Boolean {
        if (validate(puzzle) != null) return false
        return Engine(puzzle).countSolutions(limit = 2) == 1
    }
}

/** Index of the 3×3 box containing cell ([row], [col]). */
private fun boxOf(
    row: Int,
    col: Int,
): Int = (row / BOX) * BOX + col / BOX

/**
 * Validates [puzzle] structurally and against Sudoku rules.
 *
 * @return a human-readable reason if the grid is malformed or the givens conflict, or `null`
 *   when the input is well-formed and rule-consistent.
 */
internal fun validate(puzzle: Grid): String? {
    if (puzzle.size != SIZE) return "grid must have $SIZE rows, but has ${puzzle.size}"
    for (row in 0 until SIZE) {
        if (puzzle[row].size != SIZE) {
            return "row $row must have $SIZE columns, but has ${puzzle[row].size}"
        }
    }
    for (row in 0 until SIZE) {
        for (col in 0 until SIZE) {
            val value = puzzle[row][col]
            if (value !in 0..SIZE) {
                return "cell ($row, $col) has value $value, expected 0..$SIZE"
            }
        }
    }

    val rowMask = IntArray(SIZE)
    val colMask = IntArray(SIZE)
    val boxMask = IntArray(SIZE)
    for (row in 0 until SIZE) {
        for (col in 0 until SIZE) {
            val value = puzzle[row][col]
            if (value == 0) continue
            val bit = 1 shl (value - 1)
            val box = boxOf(row, col)
            if (rowMask[row] and bit != 0) return "duplicate $value in row $row"
            if (colMask[col] and bit != 0) return "duplicate $value in column $col"
            if (boxMask[box] and bit != 0) return "duplicate $value in box $box"
            rowMask[row] = rowMask[row] or bit
            colMask[col] = colMask[col] or bit
            boxMask[box] = boxMask[box] or bit
        }
    }
    return null
}

/**
 * Mutable working state for a single solve. Cells are stored flat (row-major) alongside the
 * row/column/box digit bitmasks, giving O(1) legality checks and updates during the search.
 *
 * The constructor assumes [puzzle] has already passed [validate].
 */
private class Engine(puzzle: Grid) {
    private val cells = IntArray(SIZE * SIZE)
    private val rowMask = IntArray(SIZE)
    private val colMask = IntArray(SIZE)
    private val boxMask = IntArray(SIZE)

    init {
        for (row in 0 until SIZE) {
            for (col in 0 until SIZE) {
                val value = puzzle[row][col]
                cells[row * SIZE + col] = value
                if (value != 0) place(row, col, 1 shl (value - 1))
            }
        }
    }

    /** Fills the first found solution into [cells]; returns `true` if one exists. */
    fun solveFirst(): Boolean {
        val cell = selectCell()
        if (cell < 0) return true
        val row = cell / SIZE
        val col = cell % SIZE
        var candidates = candidatesFor(row, col)
        while (candidates != 0) {
            val bit = candidates and -candidates
            candidates = candidates xor bit
            cells[cell] = bit.countTrailingZeroBits() + 1
            place(row, col, bit)
            if (solveFirst()) return true
            remove(row, col, bit)
            cells[cell] = 0
        }
        return false
    }

    /** Counts solutions up to [limit], stopping early once the limit is reached. */
    fun countSolutions(limit: Int): Int {
        val cell = selectCell()
        if (cell < 0) return 1
        val row = cell / SIZE
        val col = cell % SIZE
        var candidates = candidatesFor(row, col)
        var found = 0
        while (candidates != 0 && found < limit) {
            val bit = candidates and -candidates
            candidates = candidates xor bit
            cells[cell] = bit.countTrailingZeroBits() + 1
            place(row, col, bit)
            found += countSolutions(limit - found)
            remove(row, col, bit)
            cells[cell] = 0
        }
        return found
    }

    /** Snapshot of the current cells as a fresh [Grid]. */
    fun toGrid(): Grid = Array(SIZE) { row -> IntArray(SIZE) { col -> cells[row * SIZE + col] } }

    /**
     * Index of the empty cell with the fewest candidates (Minimum Remaining Values), or `-1`
     * when the grid is full. A cell with zero candidates is returned immediately so the caller
     * prunes the dead branch without further work.
     */
    private fun selectCell(): Int {
        var best = -1
        var bestCount = SIZE + 1
        for (cell in cells.indices) {
            if (cells[cell] != 0) continue
            val count = candidatesFor(cell / SIZE, cell % SIZE).countOneBits()
            if (count < bestCount) {
                best = cell
                bestCount = count
                if (count <= 1) return best
            }
        }
        return best
    }

    private fun candidatesFor(
        row: Int,
        col: Int,
    ): Int = ALL_DIGITS and (rowMask[row] or colMask[col] or boxMask[boxOf(row, col)]).inv()

    private fun place(
        row: Int,
        col: Int,
        bit: Int,
    ) {
        rowMask[row] = rowMask[row] or bit
        colMask[col] = colMask[col] or bit
        boxMask[boxOf(row, col)] = boxMask[boxOf(row, col)] or bit
    }

    private fun remove(
        row: Int,
        col: Int,
        bit: Int,
    ) {
        rowMask[row] = rowMask[row] and bit.inv()
        colMask[col] = colMask[col] and bit.inv()
        boxMask[boxOf(row, col)] = boxMask[boxOf(row, col)] and bit.inv()
    }
}
