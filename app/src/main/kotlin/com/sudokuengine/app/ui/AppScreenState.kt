package com.sudokuengine.app.ui

import com.sudokuengine.Grid
import com.sudokuengine.app.pipeline.PipelineResult

/**
 * Screen-state sealed class for the single-screen Sudoku app, per `docs/APP_SPEC.md`.
 *
 * The UI is a small state machine driven by user actions (picking/capturing an image) and the
 * outcome of [com.sudokuengine.app.pipeline.SolvePipeline.solve]:
 *
 * - [Idle] — initial state, shows the image picker/capture entry points.
 * - [Loading] — an image was picked/captured and the pipeline is running.
 * - [Result] — the pipeline succeeded; render the solved board.
 * - [Error] — the pipeline failed; show [Error.message] and a "Try again" action that resets to
 *   [Idle].
 */
sealed interface AppScreenState {
    /** Initial state: shows the gallery picker and camera capture entry points. */
    data object Idle : AppScreenState

    /** An image was picked/captured and [com.sudokuengine.app.pipeline.SolvePipeline] is running. */
    data object Loading : AppScreenState

    /**
     * The pipeline succeeded.
     *
     * @property originalGrid the grid as read from the image; non-zero cells are the "givens".
     * @property solvedGrid the fully solved grid.
     */
    data class Result(val originalGrid: Grid, val solvedGrid: Grid) : AppScreenState

    /**
     * The pipeline failed.
     *
     * @property message a user-facing description of what went wrong, derived from a
     *   [PipelineResult] via [errorMessageFor].
     */
    data class Error(val message: String) : AppScreenState
}

/**
 * Maps a failed [PipelineResult] to a user-facing error message, per `docs/APP_SPEC.md`'s
 * "Pipeline" section.
 *
 * Pure and platform-independent so it can be unit-tested without Android.
 *
 * @param result a [PipelineResult] that is not [PipelineResult.Success].
 * @return a human-readable message describing the failure.
 * @throws IllegalArgumentException if [result] is [PipelineResult.Success], which has no error
 *   message.
 */
fun errorMessageFor(result: PipelineResult): String =
    when (result) {
        is PipelineResult.Success ->
            throw IllegalArgumentException("PipelineResult.Success has no error message")
        is PipelineResult.BoardNotFound -> "No board found"
        is PipelineResult.Unsolvable -> "No solution"
        is PipelineResult.Invalid -> result.reason
    }
