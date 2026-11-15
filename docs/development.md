# Development and emulator testing

## What this project runs

Split the Bills is a native Android shared-expense application. Its development server is
the Firebase Local Emulator Suite: Auth, Firestore, and callable Cloud Functions. There is
no separate web frontend server. The browser UI on port 4000 is a backend administration
tool; use the Android application to test the product.

| Location | Responsibility |
| --- | --- |
| `app/` | Kotlin/Compose Android UI, Hilt ViewModel/repositories, Firebase client integration |
| `core/domain/` | Pure Kotlin money parsing, splits, balances, repayment suggestions |
| `functions/src/backend/` | Authoritative TypeScript mutations and Firestore transactions |
| `functions/src/domain/` | TypeScript calculations matching the Kotlin implementation |
| `shared/contracts/v1/` | Versioned callable API manifest |
| `shared/fixtures/` | Calculation examples used by both implementations |
| `firestore.rules`, `firestore.indexes.json` | Read permissions and query indexes |
| `firebase.json`, `.firebaserc` | Local service ports and `demo-split-the-bills` project |

Clients authenticate with Firebase Auth, read permitted Firestore documents, and send shared
changes through `v1...` callable Functions in `europe-west1`. Functions check permissions,
operation IDs, and revisions, calculate allocations, and commit records and balances in
transactions. Clients cannot write shared Firestore records directly. Room stores local
expense drafts; DataStore stores device preferences. See [architecture.md](architecture.md),
[api-v1.md](api-v1.md), and [firestore-schema.md](firestore-schema.md) for the full design.

## 1. Prepare the tools

Run commands from the repository root. On the current Linux workstation:

```bash
cd /home/nhamid/StudioProjects/SplitTheBills
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

On another machine, use its checkout and Android SDK paths. Android Studio writes `sdk.dir`
in the ignored `local.properties`; check it points to the installed SDK.

Required tools:

- Android SDK Platform 37 and an Android virtual device running API 24 or newer.
  Accept the SDK licenses and install missing SDK components through Android Studio.
- The Gradle wrapper in this repository. `gradle/gradle-daemon-jvm.properties` pins its
  daemon to **JDK 25**; `core/domain/build.gradle.kts` requests a **JDK 17** toolchain.
  Gradle can download the configured JDKs on the first build. This is separate from the
  app's Java 11 source/target compatibility in `app/build.gradle.kts`.
- **Java 21+** on the terminal's `PATH` for Firebase's Firestore emulator. JDK 25 also
  satisfies this requirement. Android Studio's selected Gradle JVM does not change the
  terminal Java used by Firebase.
- **Node.js 22 and npm**. `functions/package.json` declares Node 22; `.nvmrc` pins 22.13.1.
  If nvm is installed, select the pinned runtime:

  ```bash
  nvm install
  nvm use
  ```

  If nvm is unavailable, npm can select the pinned runtime for a backend command without
  a global Node installation change. For example, the server command in step 3 becomes:

  ```bash
  npm exec --yes --package=node@22.13.1 -- \
    npm --prefix functions run emulators -- --only auth,firestore,functions
  ```

  The same prefix can wrap the backend `ci`, `run build`, and `run check` commands.

Confirm the tools and identify the already-running virtual device:

```bash
java -version
node --version
npm --version
adb devices -l
```

The device used for this walkthrough is `emulator-5554` (API 37). Use the serial reported
by `adb devices -l` if yours differs. Keep using the existing virtual device; a second one
is unnecessary. Initial dependency, JDK, and Firebase emulator downloads require internet
access. The Firebase CLI is pinned in `functions/package-lock.json`; no global install is needed.

## 2. Install and compile the backend

```bash
npm --prefix functions ci
npm --prefix functions run build
```

`ci` installs the lockfile versions. `build` compiles `functions/src/` to the ignored
`functions/lib/` directory. The Functions emulator loads `lib/index.js`, not TypeScript
source. The `emulators` script currently checks Node's version but does not build the code.

Local testing with the committed demo project needs no Firebase login, billing, service
account, OAuth client ID, or `app/google-services.json`. If you already have that file,
debug initialization uses its project ID; use a consistent project ID for the suite and
client rather than mixing a real project configuration with this demo walkthrough.

## 3. Start the development server (terminal A)

```bash
npm --prefix functions run emulators -- --only auth,firestore,functions
```

Leave this terminal running. Wait for **All emulators ready** and the **Loaded functions
definitions from source** line listing the `v1...` handlers. Merely seeing the ready banner
does not prove that the Functions code loaded.

| Service | Host address | Purpose |
| --- | --- | --- |
| Emulator UI | `http://127.0.0.1:4000` | Inspect local users, documents, and function logs |
| Auth | `127.0.0.1:9099` | Email/password test accounts |
| Firestore | `127.0.0.1:8080` | Local group, expense, balance, and activity data |
| Functions | `127.0.0.1:5001` | Callable API |
| Emulator hub / logging / Firestore websocket | `4400` / `4500` / `9150` | Supporting services |

In another terminal, check the public, read-only API metadata endpoint:

```bash
curl --fail-with-body --silent --show-error \
  -H 'Content-Type: application/json' \
  -d '{"data":{}}' \
  http://127.0.0.1:5001/demo-split-the-bills/europe-west1/v1ApiInfo
```

Expect JSON containing a `result` with API version `v1`. Callable endpoints require the
`{"data": ...}` envelope; opening a URL in a browser is not a callable API test.

## 4. Connect and launch the Android app (terminal B)

Repeat the SDK/PATH setup from step 1 in this terminal if needed. The committed
`gradle.properties` sets `firebaseEmulatorHost=127.0.0.1`. On Android, that address is the
device itself, so forward all three app ports to the host:

```bash
adb -s emulator-5554 reverse tcp:9099 tcp:9099
adb -s emulator-5554 reverse tcp:8080 tcp:8080
adb -s emulator-5554 reverse tcp:5001 tcp:5001
adb -s emulator-5554 reverse --list
```

Build, install, and launch on the selected device:

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :app:installDebug
adb -s emulator-5554 shell am start -n com.dts.splitthebill/.MainActivity
```

Alternatively, in Android Studio select the **app** run configuration, **debug** build
variant, and the running virtual device, then click **Run**. Keep terminal A running.

| Android variant | Backend behavior |
| --- | --- |
| `debug` | Always local Auth/Firestore/Functions emulators; cleartext HTTP permitted |
| `staging` | Real Firebase, debug signing, App Check debug provider |
| `release` | Real Firebase, Play Integrity App Check, release signing not configured |

For direct host access from the standard Android emulator without reverse tunnels, rebuild
with `./gradlew :app:installDebug -PfirebaseEmulatorHost=10.0.2.2`. The fallback in
`app/build.gradle.kts` is `10.0.2.2`, but the committed Gradle property overrides it with
`127.0.0.1`. Physical USB devices should use the reverse tunnels.

## 5. Test a complete expense manually

1. Choose **Create account**. Use a disposable address such as `dev@example.test` and a
   password with at least six characters. Email/password works entirely against local Auth;
   Google sign-in needs additional real-project setup.
2. Open **Groups** in the bottom navigation, then **Create a group**. Enter a name, open
   **Currency**, and search by code or currency name. The selector offers 162 currencies;
   choose EUR for this example. Enter your participant name (for example, Alex) and submit
   **Create group**.
3. Open **Manage people** using the person-plus icon in the group toolbar. Add Blair as a
   named participant, then return with the toolbar back button. Let the **Participant
   added** confirmation disappear before tapping the floating **Add expense** button.
4. Choose **Add expense**, enter `Shared lunch` and `12.00`, select the payer and both
   participants, keep **Equally**, choose the date using the calendar, and save. Expenses and
   repayments use calendar-only date selection; the stored API value remains `YYYY-MM-DD`.
5. Confirm **Confirmed shares** appears with EUR 6.00 per participant and EUR 12.00 total.
   Open the group's **Balances** tab and inspect the corresponding debt and credit.
6. Inspect the local users and documents at `http://127.0.0.1:4000`. Edit the expense and
   confirm the allocation/balance changes; **Remove** soft-deletes it and reverses its delta.

Expense cards show overlapping initials for the payer(s) and split participants, plus an
overflow count for larger splits. People in the group who are not involved in that expense
are excluded. Currency choices are packaged from `shared/contracts/v1/currencies.json`,
and a backend contract test verifies that every offered code is accepted. Server-provided
currency precision controls money display, including zero- and three-decimal currencies.

For a second-account invitation test, create an invitation in **Manage people**, copy the
code, sign out via **Profile**, and create/sign into a second local account. In **Groups**,
choose **Have an invitation? Join a group** and redeem it. Return to the owner account to
approve the member-to-participant link. A named participant and an authenticated member
are distinct identities; invitation capacity depends on available participant places.

Current UI limits: the expense selector exposes **Equally**, **Percent**, and **Custom**;
the domain/API also supports weighted shares. Some visible controls (including search,
Export, and Remind) have empty click handlers. Do not use these as evidence of a backend
failure. See the source and [implementation-roadmap.md](implementation-roadmap.md) alongside
the older implementation/release snapshots when checking feature coverage.

## 6. Run the existing automated checks

Local checks that do not need running Firebase emulators:

```bash
npm --prefix functions run check
./gradlew :core:domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

With terminal A still running and device forwarding configured, run Android tests on the
existing emulator:

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest
```

The expense journey creates disposable local users/groups and signs out afterward. It
follows the current group/people screens and waits for the confirmation snackbar to clear.
Reports are written below
`app/build/reports/androidTests/connected/`; JVM reports are under each module's
`build/reports/tests/`.

The connected test runner may remove its app installation during cleanup. Repeat
`:app:installDebug` and the activity launch command from step 4 afterward to resume manual
testing.

To run the backend integration scenario against the already-running suite:

```bash
npm --prefix functions run build
FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 \
FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 \
GCLOUD_PROJECT=demo-split-the-bills \
  npm --prefix functions run test:emulator
```

This checks permissions, invitation flows, idempotency, stale revisions, expense/settlement
reversals, and account deletion using disposable local records.

Run the rules suite separately: its setup **clears Firestore emulator data**. Stop terminal
A first so an isolated suite can bind port 8080, and do not import your saved development data:

```bash
npm --prefix functions run build
npm --prefix functions exec firebase -- emulators:exec --only firestore \
  --project demo-split-the-bills "npm --prefix functions run test:rules"
```

## 7. Edit, restart, and keep test data

After changing backend TypeScript, run `npm --prefix functions run build`. The Functions
emulator watches the compiled output and reloads it. For continuous compilation, run this
in an additional terminal while the suite is running:

```bash
npm --prefix functions run build -- --watch
```

After Android changes, run the app again in Android Studio or repeat `:app:installDebug`
and the launch command. After reconnecting/rebooting the device or restarting ADB, recreate
the three reverse mappings.

Stop the suite with **Ctrl+C** in terminal A. Its normal startup command does not persist
Auth/Firestore data across restarts. To save local emulator data outside the repository:

```bash
npm --prefix functions exec firebase -- emulators:export \
  "$HOME/.local/share/split-the-bills-emulator-data" --project demo-split-the-bills
```

On subsequent starts, import that directory and export updates at shutdown:

```bash
npm --prefix functions run emulators -- --only auth,firestore,functions \
  --import="$HOME/.local/share/split-the-bills-emulator-data" --export-on-exit
```

The export contains local test accounts and group records. Android's installed app retains
its own Auth session, cached Firestore data, and Room drafts independently. If the server
starts fresh, create/sign into a local account again; cached records alone do not confirm
that those records exist on the restarted server.

## Troubleshooting

| Symptom | Check / fix |
| --- | --- |
| `adb: command not found` | Add the SDK's `platform-tools` directory to this terminal's `PATH`. |
| Device missing or `offline` | Check Android Studio's running device and `adb devices -l`; use the reported serial. |
| Connection refused to `127.0.0.1` | Keep terminal A alive and check all three `adb ... reverse --list` entries. |
| Auth succeeds, but creating a group fails | Check Functions loaded the `v1...` handlers and the API metadata curl succeeds. Auth and Functions are separate services. |
| `lib/index.js` missing / changes ignored | Run the TypeScript build; restart Functions if it failed to load initially. |
| `ERR_REQUIRE_ESM`, `jwks-rsa`, or `jose` at startup | Select `.nvmrc`'s Node 22.13.1 and restart the suite. |
| npm engine warning with Node 24 | Select Node 22, matching the backend's declared runtime. |
| Java error starting Firestore | Check terminal `java -version` and `JAVA_HOME`, independently of Android Studio; use Java 21+. |
| Gradle requests JDK 25 or 17 | Allow its configured toolchain provisioning or install those JDKs; see the daemon/toolchain files described above. |
| Address already in use | Reuse or stop the existing suite before starting another. `emulators:exec` does not attach to an already-running suite. |
| Demo/real project mismatch | Check `app/google-services.json`, `.firebaserc`, and the suite's selected project ID. |

Real Firebase, Google sign-in, staging, and deployment are documented separately in
[firebase-setup.md](firebase-setup.md).

## Verified on 2026-11-15

On Linux with Node 22.13.1, Java 25, and the existing Pixel 9 API 37 virtual device
(`emulator-5554`):

- Auth, Firestore, Functions, and the Emulator UI started; all 17 callable handlers loaded.
- The `v1ApiInfo` health check returned `apiVersion: v1` and `environment: emulator`.
- Debug APK build and installation passed; 19 Kotlin/JVM tests passed.
- Both Android device tests passed, including account creation through a confirmed expense.
- Backend lint/build and all 10 contract/domain tests passed.
- The backend integration scenario against the running suite passed.
- Android `lintDebug` passed with existing warnings. The destructive rules suite was not
  run against this development instance.
