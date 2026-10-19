# PR 7 executor repair plan

Goal: Fix the four reproduced executor defects without changing the complete frontend.
Architecture: Keep backend-hjh isolated. Validate before simulating; bind idempotency to a complete ordered request; protect locked blocks; compare undo snapshots under the user lock.
Tech stack: Java 21, Spring Boot, JUnit 5, Mockito, MySQL/Flyway.

## Constraints
- Append migrations; do not modify V1/V2.
- Do not touch the running frontend or its working tree.
- Do not treat mock tests as database integration verification.

## Tasks
- [x] Change the four reproduction assertions into acceptance tests and verify all four fail.
- [x] Update ActionExecutor and add RequestFingerprint; add AgentAction.requestHash and V3 migration. Guard replay, terminal keys and failure audit records. Verify exact retries still work.
- [x] Reject locked block updates and deletes in DefaultConstraintValidator, covering unlock attempts and unlocked success.
- [x] Lock before reading undo records and reject stale entity snapshots for CREATE/UPDATE across task, step and block; preserve valid undo behavior.
- [x] Run the full backend-hjh test suite, review the diff and record unresolved integration blockers in PR7-review.md.
- [x] Prepare the isolated repair branch and document why PR 7 must remain unmerged. Publication follows the successful checks.
