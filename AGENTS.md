# AGENTS.md

## Agent Workflow Policy

Use Superpowers for agent planning, debugging, execution, and verification in
this repository.

Do not invoke GSD workflows, create or depend on `.planning/`, or route resume
state through GSD for this repository unless the user explicitly asks for GSD in
the current turn.

Resume context from this file, `docs/project-vision.md`,
`docs/roadmap-current.md`, `docs/session-handoffs.md`, and relevant
`docs/plans/` or `docs/superpowers/` files.

## Branch Policy

- Perform routine development directly on `dev`.
- Do not create branches for individual features, tasks, agents, or review
  fixes.
- Commit coherent work to `dev`, then open a `dev` to `main` pull request
  when there is a meaningful, verified batch ready for promotion.
- After a promotion PR merges, fast-forward `dev` to the new `main` tip and
  push it so the two branches do not diverge.
- Keep `main` as the promoted/release branch; do not commit development work
  directly to it.

## Project Management Loop

For behavior-changing work in this repository:

1. Check alignment with `docs/project-vision.md` before implementation.
2. Update `docs/roadmap-current.md` when priorities or sequencing change.
3. Before claiming completion, run:
   - `./gradlew :app:testDebugUnitTest :app:assembleDebug`
4. Append a handoff entry to `docs/session-handoffs.md` including:
   - what changed
   - why
   - status
   - next 1-3 steps
   - verification commands/results

## Local Environment Notes

When installing or debugging on the Pixel from WSL, use the Windows SDK ADB
binary at `/mnt/e/Android/Sdk/platform-tools/adb.exe` (Windows path:
`E:\Android\Sdk\platform-tools\adb.exe`). The Linux `/usr/bin/adb` may not see
the USB-connected phone.

## Scope Note

This local file defines repo-specific workflow expectations. Platform-level safety and skill instructions still apply.
