# Release readiness

## Implemented safeguards

- The Compose shell uses bottom navigation. Page/form bodies are centered with bounded widths
  and remain vertically scrollable.
- Forms apply IME insets. Action groups that are likely to overflow use wrapping rows; dedicated
  previews cover standard screen sizes, a phone at 200% font scale, and a tablet.
- The back affordance has a spoken label, screen/form titles are semantic headings, the offline
  banner is a polite live region, and standard Material controls retain 48 dp touch targets.
- Loading, empty, error/retry, offline, saving, conflict, restored-draft, and destructive
  confirmation states are represented. “Saved” is shown only after a callable confirms.
- Local app data is excluded from cloud backup/device transfer. Account deletion clears Room
  drafts and Firestore persistence, then invokes Android’s application-data wipe and closes the
  process so Auth state, preferences, and SDK caches cannot survive deletion.
- Release builds use Play Integrity App Check and the official callable SDK. Emulator hosts and
  cleartext traffic exist only in the debug source set/manifest.

## Manual device matrix before distribution

Run TalkBack traversal and switch access on a compact phone and tablet; verify portrait and
landscape at 100%, 150%, and 200% font size/display size. Exercise keyboard traversal for every
form, offline-to-online recovery, stale expense conflict, owner deletion with zero/one/multiple
transfer candidates, and deletion blocked for an owned group with no other member.

Follow [Real Firebase testing and production deployment](firebase-setup.md) to replace the
placeholder App Link host, configure production Firebase/App Check and signing, verify staging,
and deploy a reviewed backend commit. The deployment script requires an explicit real project
ID; preparing the repository does not change billing, enable providers, or create signing keys.

## Verification snapshot — 2026-09-08

- `./gradlew test lintDebug assembleRelease :app:installDebug`: passed; 19 JVM tests, no
  lint issues, optimized unsigned release APK produced, and debug APK installed on an SM-S928B.
- `./gradlew :app:connectedDebugAndroidTest`: passed both device tests, including account
  creation through a server-confirmed expense. The test signs out before and after without
  deleting pre-existing emulator records or drafts.
- `npm --prefix functions run check`: passed ESLint, TypeScript build, and 10 contract/domain
  tests.
- The backend emulator integration scenario passed against Auth/Firestore/Functions, including
  authorization, idempotency, conflicts, reversals, invitation failures, archived mutations,
  account-deletion blocking/anonymization, and successful ownership transfer.
- Firestore rule sources were unchanged in milestone 6. Their dedicated suite was not rerun
  against the already-running shared emulator because its setup clears emulator data; use the
  isolated `emulators:exec` command in the README before release.
