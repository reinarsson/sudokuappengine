# CLAUDE.md — Sudoku Engine

Standing instructions for all agents working in this repo. Keep this file short and current —
it is loaded into every task, so every line costs context.

## What this repo is
This repo contains three modules:

- **Solver** (repo root, `sudokuengine`) — a standalone, **pure-Kotlin/JVM** library, written
  KMP-safe. Input is a 9×9 integer grid; output is a solve result.
- **`:reader`** (`reader/`) — an Android library that turns a photo of a Sudoku board into a 9×9
  grid (OpenCV + LiteRT). A separate Gradle build, run via `./gradlew -p reader ...`.
- **`:app`** (`app/`) — the Android application: pick or capture an image, read it via
  `:reader`, solve it via the root solver, and display the solved board. A separate Gradle
  build, run via `./gradlew -p app ...`.

Each module has its own spec, both treated as source of truth:
- **`docs/SPEC.md`** — solver and `:reader` contracts.
- **`docs/APP_SPEC.md`** — `:app` contract.

This file holds only the rules that apply across every task.

## Target shape (decided)
Plain Kotlin/JVM library, written KMP-safe so promotion to Kotlin Multiplatform is cheap later.
<!-- If you want KMP from day one instead, change this line and the source-set layout. -->

## Agent team & delegation policy
This repo uses a tiered agent team. Run the **lead session on Opus** — it is the orchestrator.

Roles (workers defined in `.claude/agents/`):
- **Orchestrator** — this lead session (Opus). Plans the work, splits it, dispatches to
  workers, reviews their output, sequences PRs. Does not write code directly when a worker can.
- **coder** (Sonnet) — implements features and tests per `docs/SPEC.md`.
- **standards-checker** (Haiku) — runs lint/coverage and reviews against these standards;
  reports issues only, never edits code.

Policy:
- For any implementation task, delegate the coding to the `coder` subagent rather than writing
  it in the lead session.
- After the coder reports done, dispatch the `standards-checker` **before** opening a PR. All
  `[CRITICAL]` findings must be resolved (by the coder) before the PR opens.
- Split genuinely independent work (separate modules or file groups) across parallel `coder`
  subagents; keep tightly-coupled work in a single one.
- One concern per PR; never push to `main`; workers stay within their assigned scope and do not
  modify the public contract or existing passing code without explicit instruction.

## Standing constraints

### Repo-wide (every module)
- **Dependency approval.** Adding *any* new third-party dependency requires explicit human
  approval first, regardless of module.
- **Tests use `kotlin.test`** (not JVM-only JUnit APIs), everywhere.
- **ktlint must pass**, and **Kover coverage must be >= 80%** per module (Android modules may
  exclude vendor/generated packages — see `reader/build.gradle.kts` for the pattern).
- **Branch + PR workflow.** Work on feature branches, open PRs, never push to `main`. One
  concern per PR.

### Solver only (repo root)
- **Zero third-party dependencies.** Standard library only. No solver/optimization libraries —
  OR-Tools, Choco-solver, ojAlgo, etc. are explicitly banned (JVM/JNI-only, and they break the
  iOS path).
- **No platform imports** in production code: no `java.*`, no `android.*`. This keeps the code
  `commonMain`-compatible.
- **Deterministic, pure, stateless:** identical input → identical output; never mutate a
  caller's input; hold no cross-call state.
- **Never fabricate the test oracle.** The golden-puzzle fixture is produced externally by the
  Python `pulp` solver and committed by the maintainer. Do not generate, invent, or compute
  expected solutions yourself — that makes the tests circular and worthless.

### `:reader` and `:app` (Android modules)
- Normal Android/Kotlin/Compose patterns are expected: `android.*` imports, Jetpack Compose,
  Activity Result APIs, OpenCV/LiteRT (in `:reader`).
- For `:app`, Compose + standard AndroidX (`activity-compose`, `FileProvider`) is pre-approved
  as part of the `:app` scaffold. New dependencies beyond what's already approved (per module)
  still need explicit human approval.

## Workflow
- **Scaffold once, first.** A single setup task creates and commits the Gradle wrapper, build
  files, source-set layout, and CI **before** any parallel work begins. Do not re-scaffold the
  build in other tasks.
- **Branch + PR.** Work on feature branches and open pull requests; never push directly to
  `main`.
- Keep changes small and focused — one concern per PR.

## Quality bar
- Formatting / lint: ktlint must pass.
- Coverage: Kover, ≥ 80%.
- Public API documented with KDoc; favor small functions and clear names (Clean Code / SOLID).
- **README.md must be created (if absent) or updated** whenever the public API, build setup, or
  usage instructions change. It should cover: what the library does, build/test commands, the
  public contract (`SudokuSolver`, `SolveResult`), and a minimal usage example.

## Commands
- Build:    `./gradlew build`
- Test:     `./gradlew test`
- Lint:     `./gradlew ktlintCheck`
- Coverage: `./gradlew koverVerify`

`:reader` and `:app` are separate Gradle builds: run the same task names with `-p reader` or
`-p app` (e.g. `./gradlew -p app ktlintCheck`).
