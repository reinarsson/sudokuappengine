package com.sudokuengine.app.pipeline

import com.sudokuengine.Grid
import com.sudokuengine.SolveResult
import com.sudokuengine.SudokuSolver
import com.sudokuengine.reader.ReadOutcome
import com.sudokuengine.reader.SudokuReader

/**
 * Outcome of running [SolvePipeline.solve] on an image.
 *
 * Mirrors the pipeline steps described in `docs/APP_SPEC.md`: a [SudokuReader] reads a [Grid]
 * from the image, then a [SudokuSolver] solves it. Each failure mode of either step maps to its
 * own [PipelineResult] case so the UI can render the right error state with a retry action.
 */
sealed interface PipelineResult {
    /**
     * The image was read and solved successfully.
     *
     * @property originalGrid the grid as read from the image, before solving. Non-zero cells are
     *   the "givens"; the UI styles these differently from solver-filled cells.
     * @property solvedGrid the fully solved grid.
     */
    data class Success(val originalGrid: Grid, val solvedGrid: Grid) : PipelineResult

    /** No Sudoku board could be located in the image. */
    data object BoardNotFound : PipelineResult

    /** The board was read, but the resulting puzzle has no solution. */
    data object Unsolvable : PipelineResult

    /** The board was read, but the resulting puzzle is malformed or rule-inconsistent. */
    data class Invalid(val reason: String) : PipelineResult
}

/**
 * Orchestrates the image → grid → solved-grid pipeline described in `docs/APP_SPEC.md`.
 *
 * Wraps [reader] and [solver] behind their interfaces so this class is pure and testable with a
 * fake [SudokuReader] and the real [SudokuSolver].
 *
 * @property reader turns image bytes into a [Grid] (or reports that no board was found).
 * @property solver solves a [Grid] (or reports it is unsolvable/invalid).
 */
class SolvePipeline(
    private val reader: SudokuReader,
    private val solver: SudokuSolver,
) {
    /**
     * Runs the full pipeline on [image]: reads the board, then solves it.
     *
     * @param image encoded JPEG/PNG bytes, as produced by the picker/camera flow.
     * @return a [PipelineResult] describing the outcome.
     */
    fun solve(image: ByteArray): PipelineResult {
        val readOutcome = reader.read(image)
        val grid =
            when (readOutcome) {
                is ReadOutcome.Success -> readOutcome.result.grid
                is ReadOutcome.BoardNotFound -> return PipelineResult.BoardNotFound
            }

        return when (val solveResult = solver.solve(grid)) {
            is SolveResult.Solved -> PipelineResult.Success(originalGrid = grid, solvedGrid = solveResult.grid)
            is SolveResult.Unsolvable -> PipelineResult.Unsolvable
            is SolveResult.Invalid -> PipelineResult.Invalid(solveResult.reason)
        }
    }
}
