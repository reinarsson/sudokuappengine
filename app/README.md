# :app — Sudoku app (Android)

The Compose Android application: pick or capture a photo of a Sudoku board, read it via
`:reader`, solve it via the root solver, and display the solved board. The full contract is in
[`../docs/APP_SPEC.md`](../docs/APP_SPEC.md).

> **Status:** in progress. The home screen has a "pick image from gallery" entry point
> (`PickVisualMedia`) and a "take photo" entry point (`TakePicture` + `FileProvider`, with
> `CAMERA` runtime permission handling); both read the resulting image into bytes and show a
> placeholder status. It is **not yet wired up** to `:reader` or the root solver — that's a
> follow-up PR. See `docs/APP_SPEC.md` for the planned pipeline and architecture.

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
- `ui/HomeScreen.kt` — home screen with the gallery image picker and `imageStatusText` helper.
- `ui/CameraCaptureButton.kt`, `ui/CaptureStatus.kt` — camera capture entry point and its
  pure status helpers.
- `ui/` (planned) — additional Compose screens and the `SudokuGrid` composable for rendering a
  solved board.
- `pipeline/` (planned) — small testable orchestration class wrapping `SudokuReader.read()` and
  `SudokuSolver.solve()`.

See [`../docs/APP_SPEC.md`](../docs/APP_SPEC.md) for the full contract, pipeline, and definition
of done.
