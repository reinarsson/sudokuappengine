# Sudoku App — Implementation Spec (image → solved board, on screen)

> **How to use this file:** task spec for the `:app` module, the Compose Android application
> that ties `:reader` and the root solver together: pick or capture a photo of a Sudoku board,
> read it, solve it, and display the solved board. Repo/module-wide conventions live in
> `CLAUDE.md`; this file is the contract.

## Context & scope
`:app` is the **presentation layer**. It consumes two existing public contracts and does not
reimplement any vision or solving logic itself:

- `com.sudokuengine.reader.SudokuReader.read(image: ByteArray): ReadOutcome` — see
  `reader/src/main/kotlin/com/sudokuengine/reader/SudokuReader.kt` and `ReaderTypes.kt`.
- `com.sudokuengine.SudokuSolver.solve(grid: Grid): SolveResult` and `SudokuSolver.create()` —
  see `src/main/kotlin/com/sudokuengine/SudokuSolver.kt` and `SolveResult.kt`.

**In scope:**
- image acquisition (file/photo picker and camera capture);
- invoking `:reader` then the solver in sequence;
- rendering the solved board on screen.

**Out of scope for MVP:**
- cell-correction UI (letting the user fix misread digits);
- settings;
- history/persistence of past scans;
- multiple solving strategies or solver configuration;
- animations;
- exporting or sharing images of the solved board.

## Settled decisions
- **UI:** Jetpack Compose (Material3), single `Activity`, with a sealed-class screen state (no
  Navigation library — the state machine is small enough to live in one screen).
- **Image source:**
  - File/photo picker — `ActivityResultContracts.PickVisualMedia`.
  - Camera capture — `ActivityResultContracts.TakePicture`, writing to a `FileProvider`-backed
    `Uri` (the manifest `<provider>` and `file_paths.xml` are already scaffolded).
- **Versions:** minSdk 24 / compileSdk 34 / Java 17 / Kotlin 2.1.21 / AGP 8.7.3 / ktlint 12.1.1 /
  Kover 0.7.6 — matching `:reader`.

## Pipeline
1. The user picks or captures an image, yielding a `Uri`.
2. `Uri` → `ByteArray` via `ContentResolver.openInputStream(uri)`.
3. `reader.read(bytes)`:
   - `ReadOutcome.BoardNotFound` → error state ("no board found"), with a retry action.
   - `ReadOutcome.Success(ReadResult(grid, confidence))` → continue. `confidence` is ignored for
     MVP.
4. `solver.solve(grid)`:
   - `SolveResult.Unsolvable` → error state ("no solution"), with a retry action.
   - `SolveResult.Invalid(reason)` → error state showing `reason`, with a retry action.
   - `SolveResult.Solved(solvedGrid)` → result screen, rendering `solvedGrid`.

## Displaying the solved board (MVP definition)
A Compose-drawn 9×9 grid (`Canvas` or nested layout) — the `SudokuGrid` composable:
- thin borders between adjacent cells;
- thick borders around each 3×3 box;
- each cell shows the digit from `solvedGrid`;
- cells that were **given** in the original input `grid` (non-zero) are styled differently from
  cells the solver filled in — e.g. bold/black text for givens vs a different color for
  solver-filled digits.

On-screen rendering only for MVP — no bitmap export or sharing.

## Dependencies — pre-approved for `:app`
- `androidx.compose.*` (BOM-managed), Material3, `ui`, `ui-tooling-preview`.
- `androidx.activity:activity-compose`.
- AndroidX core `FileProvider` (`androidx.core:core-ktx` / `androidx.core`).

No OpenCV, LiteRT, or CameraX in `:app` for MVP — those stay behind `:reader`. Any dependency
beyond this list needs explicit human approval first (see `CLAUDE.md`).

## Architecture / source layout
- `MainActivity.kt` — single-activity entry point, hosts the Compose content and owns the
  screen-state sealed class.
- `ui/` — Compose screens (picker/idle, loading, error, result) and the `SudokuGrid` composable.
- `pipeline/` — a small, pure, testable orchestration class wrapping `reader.read()` and
  `solver.solve()`, mapping their outcomes to the screen-state sealed class. Depends only on the
  `SudokuReader` and `SudokuSolver` interfaces (constructor-injected), so it is unit-testable
  with a fake reader and the real solver.

## Tests — required
- `kotlin.test` unit tests for the pipeline orchestration class, using a **fake**
  `SudokuReader` and the real `SudokuSolver` — added once the orchestration class exists (a
  follow-up PR; not part of this scaffold).
- Compose UI tests are a later follow-up, not required for MVP.

## Definition of done (MVP — whole roadmap, not just this scaffold)
- The user can pick an image from the file/photo picker and see either a solved-board rendering
  or an error state with a retry action.
- The user can take a photo with the camera and go through the same flow.
- The pipeline orchestration logic is unit-tested with a fake reader.
- `ktlintCheck` and `koverVerify` pass for `:app`.
