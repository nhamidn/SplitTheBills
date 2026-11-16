# Implementation roadmap

- [x] 1. Scaffold builds, emulators, contracts, App Check, rules boundary, and documentation.
- [x] 2. Implement pure Kotlin and TypeScript money/splitting logic from shared fixtures.
- [x] 3. Implement transactional mutations and comprehensive Functions/rules emulator tests.
- [x] 4. Implement authentication and the complete Android expense journey against emulators.
- [x] 5. Connect invitations/activity to Android and add conflict UX, offline viewing, and draft restoration.
- [x] 6. Add persisted profile/theme/account-deletion settings; verify accessibility,
  compact/expanded layouts, large fonts, lint, and optimized release configuration.
- [x] 7. Complete the remaining designed actions: search, reminders, notifications, CSV export,
  group types/settings, friends/help, currency/debt/lock preferences, cross-group navigation,
  all four split modes, and repayment detail/edit/removal. See the [feature audit](feature-audit.md)
  and current verification in [development.md](development.md).

Each mutation lands with authorization, idempotency, stale-revision, reversal, conservation,
and cross-group-reference tests. Production deployment remains a separate explicitly
authorized activity.
