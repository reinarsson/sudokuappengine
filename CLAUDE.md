# CLAUDE.md — Sudoku Engine

Standing instructions for all agents working in this repo. Keep this file short and current —
it is loaded into every task, so every line costs context.

## What this repo is
The Sudoku-solving engine: a standalone, **pure-Kotlin** library. Input is a 9×9 integer grid;
output is a solve result. Computer vision, the digit model, and the app live in **other** repos
and are out of scope here.

The full contract — API, algorithm, tests — is in **`docs/SPEC.md`**. Treat it as the source of
truth; this file holds only the rules that apply across every task.

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

## Standing constraints (apply to every task)
- **Zero third-party dependencies.** Standard library only. No solver/optimization libraries —
  OR-Tools, Choco-solver, ojAlgo, etc. are explicitly banned (JVM/JNI-only, and they break the
  iOS path). Adding *any* dependency requires explicit human approval first.
- **No platform imports** in production code: no `java.*`, no `android.*`. This keeps the code
  `commonMain`-compatible.
- **Tests use `kotlin.test`** (not JVM-only JUnit APIs).
- **Deterministic, pure, stateless:** identical input → identical output; never mutate a
  caller's input; hold no cross-call state.
- **Never fabricate the test oracle.** The golden-puzzle fixture is produced externally by the
  Python `pulp` solver and committed by the maintainer. Do not generate, invent, or compute
  expected solutions yourself — that makes the tests circular and worthless.

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
