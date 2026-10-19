# Team module integration implementation plan

Goal: Preserve the full version, then close the real frontend-to-database workflow on a separate branch.
Architecture: Retain reviewed domain contracts and ImportService. Reuse persistent ports and the live model adapter from the preserved version. One executor owns schedule writes; the teammate backend is a reference for atomic batch semantics, not a second runtime.
Tech stack: Java 21 / Spring Boot 3.5, Vue 3 / TypeScript, Flyway, H2 and MySQL 8.

- [x] Create and push a full-version snapshot including uncommitted calendar work; preserve the original working tree.
- [x] Create a managed worktree from main; merge preserved adapters with original histories.
- [x] Write failing integration tests for batch preview/apply/retry/undo and audited course/block/preference writes.
- [x] Add pure CalendarMutations preparation; route CalendarService through ActionExecutor while retaining frontend endpoint compatibility.
- [x] Add batch preview and apply to ActionExecutor and ExecutorController, retaining stable ActionSpec and shared constraints.
- [x] Add live TEXT import adapter on the existing ImportModelPort; keep screenshot/audio manual review honest; test provider failures and import confirmation.
- [x] Reuse teammate status/conflict components against real responses and verify frontend typecheck/build/tests.
- [x] Start isolated MySQL 8 from installed binaries, initialize a dedicated data directory, and run database integration and restart tests without existing database credentials.
- [x] Run browser end-to-end flows and start the integrated preview on separate ports.
- [x] Record provenance, API contracts, test evidence and remaining limits. Publish the resulting integration branch for review after the final diff check.
