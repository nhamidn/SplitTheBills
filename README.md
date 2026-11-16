# Split the Bills

Split the Bills is a native Android shared-expense app with an original “Clear shares,
calmer plans” visual identity. The Firebase backend is designed as a platform-neutral API
so an iOS client can use the same records and business rules.

For a step-by-step local server and Android virtual-device walkthrough, see
[Development and emulator testing](docs/development.md). It includes prerequisites,
server health checks, device routing, manual testing, and troubleshooting.

To test against **real Firebase** on the running Android virtual device, use the `staging`
variant and follow [Real Firebase testing and production deployment](docs/firebase-setup.md).
That guide covers console setup, App Check, two-account testing, release signing, and the
`npm --prefix functions run deploy:firebase -- --project YOUR_PROJECT_ID` deployment script.
Add `--dry-run` to print its plan without running commands or contacting Firebase.

## Current status

Milestones 1 through 6 are implemented: the Android/build foundation, responsive Compose UI,
Hilt/StateFlow wiring, Room draft storage, DataStore provisioning, Firebase SDK/App Check
bootstrap, versioned contracts, shared calculation fixtures, and setup documentation. A pure
Kotlin domain module and matching TypeScript implementation provide exact money parsing, all
split modes, balance deltas/reversal, and deterministic repayment suggestions. Authenticated,
App-Check-protected callable Functions now own group, participant, invitation/linking,
expense, settlement, archive, and account-deletion mutations. They commit authoritative
balances, activity, revisions, and idempotency receipts in Firestore transactions. Rules
deny direct client writes and private backend-collection access.

Android now supports email account creation/sign-in, password reset, Google Credential
Manager sign-in, sign-out, group creation, named participants, live group details,
equal/exact/percentage/weighted expense entry, confirmed expense detail, edit/soft-delete, balances, activity, and
external repayment recording/edit/removal. Search, reminders, notifications, CSV export, group
types/settings, friends, and preferences are connected to their data/system flows; see the
[feature audit](docs/feature-audit.md). Shared writes are connectivity-gated and expose saving,
failure, retry, and actionable stale-revision conflict states. Owners can create and revoke
one-time invitation links/codes and explicitly approve member-to-participant links; signed-in
users can redeem codes or incoming App Links. Activity displays server-written timestamps and
revisions. Expense forms autosave account-scoped Room drafts, restore them after process death,
and retain idempotency IDs across retries. Firestore cache metadata is surfaced for offline views.
A physical-device integration test covers account creation through a server-confirmed expense
against the local emulators. Settings now persist System/Light/Dark appearance, update the
Firebase Auth display name, and provide explicit account deletion with owner-transfer choices,
typed confirmation, server-first cleanup, account-scoped Room cleanup, and Firestore cache
erasure. The current UI uses bottom navigation, long action groups
wrap, important status changes are announced, and release builds run the optimized AGP/R8
pipeline. See [docs/implementation-roadmap.md](docs/implementation-roadmap.md).

## Prerequisites

- Android Studio with Android SDK 37; this checkout pins the Gradle daemon to JDK 25
  and uses a JDK 17 toolchain for `core:domain` (Gradle can provision these).
  The Firebase Emulator Suite separately needs Java 21+ on the terminal's `PATH`.
- Node.js 22 (`.nvmrc` pins 22.13.1) and npm
- An Android emulator; API 24 is the minimum supported version

The Gradle wrapper and Firebase CLI are project-pinned. No global CLI install is required.

## Local setup

1. Run the credential-free Android checks:

   ```bash
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```

   The unsigned release artifact can also be validated locally with
   `./gradlew assembleRelease`. Production installation additionally requires Firebase
   configuration, signing, App Check, and App Link setup; no release signing key is committed.

2. Install and check the backend:

   ```bash
   npm --prefix functions ci
   npm --prefix functions run check
   npm --prefix functions run build
   ```

3. Start Auth, Firestore, Functions, and the Emulator UI:

   On macOS, select JDK 21 for the current terminal first. Android Studio's Gradle JVM does
   not change the Java used by `firebase-tools`:

   ```bash
   export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
   export PATH="$JAVA_HOME/bin:$PATH"
   nvm install
   nvm use
   java -version
   node --version
   npm --prefix functions run emulators
   ```

   Startup builds the backend and seeds four demo users plus shared expense groups. Wait for
   **[demo] Demo data ready**, then sign in with **alice@demo.test / SplitDemo123!**.
   See [Demo accounts and test data](docs/demo-data.md) for all logins and test scenarios.

   `java -version` must report 21 or newer, and Node must report the pinned Node 22 version. These
   exports apply to steps 5 and 6 when they are run in the same terminal. On Linux or
   Windows, point `JAVA_HOME` at an installed JDK 21+ using the platform's normal mechanism.
   An `.nvmrc` is committed, so nvm installs/selects the tested Node version. If nvm is not
   installed, select another compatible Node 22 installation before running the npm command. The
   `emulators` script fails fast on an incompatible runtime.
   Do not rely only on the emulator's final “All emulators ready” banner: the Functions
   section must also list the discovered `v1...` handlers. If startup contains
   `ERR_REQUIRE_ESM` from `jwks-rsa`/`jose`, the shell selected an older Node executable;
   fix `PATH`, stop the suite, and start it again.

   The UI is at `http://127.0.0.1:4000`. This checkout configures Android debug builds
   for `127.0.0.1`, which works with the `adb reverse` commands in step 7. A standalone
   Android emulator without reverse tunnels can instead use
   `-PfirebaseEmulatorHost=10.0.2.2`.

4. Credential-free emulator builds need no Firebase console project or
   `google-services.json`; the debug source set supplies an emulator-only demo configuration.
   To exercise Google sign-in or a real Firebase project, register package
   `com.dts.splitthebill`, place the downloaded file at `app/google-services.json` (gitignored),
   and pass its Web OAuth client ID without a secret:

   ```bash
   ./gradlew :app:installDebug -PgoogleWebClientId=<web-client-id>.apps.googleusercontent.com
   ```

   Select that project explicitly or retain the safe `demo-split-the-bills` alias for local
   emulator work. Never commit service-account keys.

5. Run security-rule tests with the emulator:

   ```bash
   npm --prefix functions exec firebase -- emulators:exec --only firestore \
     "npm --prefix functions run test:rules" \
     --project demo-split-the-bills
   ```

6. Run the two-user Auth/Firestore/Functions integration scenario:

   ```bash
   npm --prefix functions run build
   npm --prefix functions exec firebase -- emulators:exec --only auth,firestore,functions \
     "npm --prefix functions run test:emulator" \
     --project demo-split-the-bills
   ```

7. With the Emulator Suite still running, exercise the Android journey on a connected
   emulator or device. For a USB device, run these commands after each device reconnect or
   reboot, then run the app from Android Studio. The committed Gradle property already uses
   localhost, so Android Studio does not need an extra run argument:

   ```bash
   adb reverse tcp:9099 tcp:9099
   adb reverse tcp:8080 tcp:8080
   adb reverse tcp:5001 tcp:5001
   ./gradlew :app:installDebug
   # Or run the automated journey:
   ./gradlew :app:connectedDebugAndroidTest
   ```

   The test creates disposable users and records only in `demo-split-the-bills`. Some physical
   devices print a harmless `androidx.test.services` app-ops warning before the tests start.

   If the app reports `Failed to connect to 127.0.0.1`, `Failed to connect to 10.0.2.2`, or
   `1 out of 2 underlying tasks failed`, check the USB tunnels first:

   ```bash
   adb reverse --list
   ```

   A USB-device run must list all three mappings above. ADB removes them when the device is
   disconnected, rebooted, or the ADB server restarts; rerun the three `adb reverse` commands
   and retry the action. Also confirm the Emulator Suite still shows ports 9099, 8080, and
   5001 as ready. A response saying that the local Functions backend did not load means the
   suite was started with an unsupported Node version; inspect `node --version` and restart
   it with Node 22.13+.

   Invitation testing uses two signed-in accounts. From an owner account, open a group and
   choose **Manage people** (the person-plus icon), create a code, then sign into the second account
   and choose **Have an invitation?**. After redemption, return to the owner to approve the
   exact member/participant link. For a local explicit App Link test (the committed domain is
   intentionally not verified), target the activity directly with a disposable emulator token:

   ```bash
   adb shell am start -n com.dts.splitthebill/.MainActivity \
     -a android.intent.action.VIEW \
     -d 'https://links.example.invalid/invite?token=<emulator-token>'
   ```

## Project map

- `app/` — Android application and local-only persistence
- `core/domain/` — Android-free Kotlin money and balance business rules
- `functions/` — TypeScript callable Functions and backend tests
- `shared/contracts/v1/` — machine-readable cross-platform contract manifest
- `shared/fixtures/` — calculation examples consumed by Kotlin and TypeScript in milestone 2
- `firestore.rules` / `firestore.indexes.json` — client access boundary and indexes
- `docs/` — architecture, schema, API, iOS, decisions, and roadmap

See [docs/firebase-setup.md](docs/firebase-setup.md) for the exact console, App Check, Google
sign-in, and App Link steps that require project ownership or release signing information.
Its **Test on a real Firebase project** section documents the installable `staging` variant,
cloud deployment commands, App Check debug-token registration, and two-account smoke test.

Production deployment, billing, provider enablement, signing, and real App Link hosting are
deliberately not performed by this repository setup.

The final device/layout and release checklist is in
[docs/release-readiness.md](docs/release-readiness.md).

Calculation rules and their cross-platform implementation requirements are specified in
[docs/calculations-v1.md](docs/calculations-v1.md).
