# :app — Sudoku app (Android)

The Compose Android application: pick or capture a photo of a Sudoku board, read it via
`:reader`, solve it via the root solver, and display the solved board. The full contract is in
[`../docs/APP_SPEC.md`](../docs/APP_SPEC.md).

> **Status:** MVP complete. The home screen lets the user pick an image from the gallery
> (`PickVisualMedia`) or capture one with the camera (`TakePicture` + `FileProvider`, with
> `CAMERA` runtime permission handling). Either path runs the image through `SolvePipeline`
> (`OpenCvSudokuReader` + `LiteRtDigitClassifier` from `:reader`, then the root `SudokuSolver`)
> off the main thread, and the screen renders the outcome: a solved-board grid (`SudokuGrid`,
> with given vs. solver-filled cells styled differently), or an error message with a "Try
> again" action. See `docs/APP_SPEC.md` for the full contract.

## Build & test

This is a self-contained Gradle build, run from the repo root with `-p app`:

```bash
./gradlew -p app ktlintCheck         # lint
./gradlew -p app assembleDebug       # build the app
./gradlew -p app testDebugUnitTest   # pure-JVM unit tests
./gradlew -p app koverVerify         # coverage (>=80%)
```

Requires the Android SDK (`ANDROID_HOME`, or `local.properties` with `sdk.dir`).

## Module layout

- `MainActivity.kt` — single-activity Compose entry point.
- `ui/HomeScreen.kt` — home screen: builds the `SolvePipeline` (real `OpenCvSudokuReader` +
  `LiteRtDigitClassifier`, loading `digits.tflite` from `:reader`'s bundled assets, and the
  default `SudokuSolver`), wires the gallery picker and camera capture to it, and renders each
  `AppScreenState`.
- `ui/AppScreenState.kt` — the screen-state sealed interface (`Idle`, `Loading`, `Result`,
  `Error`) and `errorMessageFor`, the pure `PipelineResult` -> user-facing error message mapping
  ("No board found", "No solution", or the solver's `Invalid` reason).
- `ui/SudokuGrid.kt` — pure Compose `Canvas` composable rendering a 9x9 board: thin lines
  between cells, thick lines around each 3x3 box, given cells in bold black vs. solver-filled
  cells in a different color/weight.
- `ui/CameraCaptureButton.kt` — camera capture entry point; reports captured bytes via a
  callback, with no separate status text (feedback is solely via `AppScreenState`).
- `pipeline/SolvePipeline.kt` — small testable orchestration class wrapping `SudokuReader.read()`
  and `SudokuSolver.solve()`, mapping their outcomes to a `PipelineResult` sealed type
  (`Success` with both the original and solved grids, `BoardNotFound`, `Unsolvable`, `Invalid`).
  Constructor-injected dependencies, unit-tested with a fake reader and the real solver. Wired
  into `HomeScreen`, run off the main thread via `LaunchedEffect` + `Dispatchers.Default`.

See [`../docs/APP_SPEC.md`](../docs/APP_SPEC.md) for the full contract, pipeline, and definition
of done.
