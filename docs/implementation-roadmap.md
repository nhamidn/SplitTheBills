# Implementation roadmap

- [x] 1. Scaffold builds, emulators, contracts, App Check, rules boundary, and documentation.
- [x] 2. Implement pure Kotlin and TypeScript money/splitting logic from shared fixtures.
- [x] 3. Implement transactional mutations and comprehensive Functions/rules emulator tests.
- [x] 4. Implement authentication and the complete Android expense journey against emulators.
- [x] 5. Connect invitations/activity to Android and add conflict UX, offline viewing, and draft restoration.
- [x] 6. Add persisted profile/theme/account-deletion settings; verify accessibility,
  compact/expanded layouts, large fonts, lint, and optimized release configuration.

Each mutation lands with authorization, idempotency, stale-revision, reversal, conservation,
and cross-group-reference tests. Production deployment remains a separate explicitly
authorized activity.
