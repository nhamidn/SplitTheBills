# Real Firebase testing and production deployment

This runbook covers this repository's Android app and Firebase backend. Run terminal commands
from the repository root. Examples use Bash/Zsh; replace project IDs and OAuth client IDs with
your own values. Preparing these files does not deploy anything or enable billing.

## 1. Choose the right environment

| Android variant | Auth / Firestore / Functions | App Check | Use |
| --- | --- | --- | --- |
| `debug` | Local Emulator Suite | No production attestation | Credential-free development |
| `staging` | Real project in `app/google-services.json` | Debug provider; register the device token | Real Firebase testing on the running AVD or a phone |
| `release` | Real project in `app/google-services.json` | Play Integrity | Signed production distribution |

Use **staging** to test real Firebase. Supplying `google-services.json` does not change a
`debug` build into a cloud-connected build. Staging needs internet access, but no local backend
process, Firebase Emulator Suite, or `adb reverse` tunnels. Firebase runs the deployed server.
The local Android virtual device and the Firebase server emulators are separate things.

Use separate Firebase projects for staging and production. Keep `.firebaserc` pointing to the
safe `demo-split-the-bills` default; pass the actual cloud project ID explicitly when deploying.
The deployment script rejects `demo-*` IDs. Local emulator accounts/data are not copied to
real Firebase. Keep test data in staging.

All variants currently use package `com.dts.splitthebill`. Debug and staging share the local
debug signing certificate. Installing staging can replace the debug app and retain local
cached state. For a clean environment switch, use a fresh AVD or uninstall the existing app
first; uninstalling removes its drafts, settings, and cached sign-in. Release certificates
usually differ, so switching between staging and release may also require uninstalling.

## 2. Prepare the Firebase staging project

In the [Firebase console](https://console.firebase.google.com/):

1. Create a staging project and note its **project ID**, not its display name or project number.
2. Link billing using the **Blaze plan** for Cloud Functions deployment. Configure billing
   alerts and review usage; [budget alerts do not cap charges](https://docs.cloud.google.com/billing/docs/how-to/budgets).
   This project uses second
   generation Functions on Node 22 in `europe-west1`, with `maxInstances: 10` per function.
   See [Functions setup](https://firebase.google.com/docs/functions/get-started).
3. Create the **default Cloud Firestore database**, using the Firebase/native API (Standard
   edition). Choose its location deliberately, preferably near the Functions region.
   Start with restricted production rules; the repository deploys its own rules next.
4. Under Authentication → Sign-in method, enable **Email/Password**. Enable **Google** and
   choose a support email if you want to test Google sign-in too.
5. Under Project settings → Your apps, register an Android app with package
   **`com.dts.splitthebill`**. Obtain the staging/debug certificate fingerprints:

   ```bash
   ./gradlew :app:signingReport
   ```

   Add that certificate's **SHA-1 and SHA-256** to the Android app registration. Each developer
   can have a different debug certificate. Later, register the production app-signing
   certificate separately.
6. Download the updated `google-services.json` into **`app/google-services.json`**. This exact
   root-of-module path activates the conditional Google Services plugin in
   `app/build.gradle.kts`. The file is gitignored. Do not put it only in `app/src/staging/`:
   the current plugin activation checks the root file.
7. For Google sign-in, copy the **Web OAuth client ID** from the Google provider configuration
   or Google Cloud → APIs & Services → Credentials. Use the Web application client ID ending
   in `.apps.googleusercontent.com`, not the Android client ID. No OAuth client secret is
   needed. See [Google sign-in setup](https://firebase.google.com/docs/auth/android/google-signin).
8. Register the Android app under App Check. For staging you will authorize a debug token in
   step 5 below. Cloud mutation Functions already enforce App Check; Firestore's console
   enforcement setting is separate.

There are no custom runtime secrets or `.env` values required by the current backend.
Deployed Functions use their runtime service identity, not a service-account JSON bundled in
the APK. Do not loosen Firestore rules to make setup errors disappear: clients read authorized
records, and callable Functions own shared writes.

## 3. Install tools and authenticate

Requirements: Node 22 (`.nvmrc` pins 22.13.1), npm, Android SDK 37, and the project's Gradle/JDK
setup described in [development.md](development.md). Local security/integration tests also
need Java 21+ on the terminal `PATH` for the Firebase Emulator Suite.

```bash
# If using nvm; otherwise select the matching Node installation yourself.
nvm install
nvm use
npm --prefix functions ci

# Use the CLI pinned in functions/package-lock.json; no global install is needed.
./functions/node_modules/.bin/firebase login
./functions/node_modules/.bin/firebase projects:list
```

The repository already contains `firebase.json`, Functions, rules, and indexes. You do not
need to run `firebase init` or regenerate those files to connect a real project.

On a headless machine, the pinned CLI supports `login --no-localhost`. CI should use
[Application Default Credentials](https://firebase.google.com/docs/cli#cli-ci-systems)
with a deployment identity authorized for the target project. Prefer short-lived credentials;
never commit service-account keys or login tokens. `--non-interactive` does not grant access.

Set these terminal variables to your staging values:

```bash
export STB_STAGING_PROJECT_ID="replace-with-your-staging-project-id"
export STB_STAGING_WEB_CLIENT_ID="replace-with-your-web-client-id.apps.googleusercontent.com"

# Print only the project ID from the downloaded Android configuration.
node -e 'const fs = require("node:fs"); console.log(JSON.parse(fs.readFileSync("app/google-services.json", "utf8")).project_info.project_id)'
```

The printed ID must match `STB_STAGING_PROJECT_ID`. The deployment script deliberately does
not infer its target from this file: backend-only CI deployments need no Android configuration.

## 4. Deploy the staging backend

Preview the plan, then execute it:

```bash
npm --prefix functions run deploy:firebase -- \
  --project "$STB_STAGING_PROJECT_ID" --dry-run

npm --prefix functions run deploy:firebase -- \
  --project "$STB_STAGING_PROJECT_ID"
```

[`functions/scripts/deploy.mjs`](../functions/scripts/deploy.mjs) runs backend lint, the
TypeScript build, contract/domain tests, and deployment-script tests. Only if they pass does
it deploy Firestore rules/indexes, followed by Functions, using the pinned CLI and root
`firebase.json`. It stops on failure and does not pass `--force`. The Firebase CLI may prompt
about first-deployment APIs, permissions, or artifact cleanup; review those for your project.

Our `--dry-run` only prints the commands. It does **not** run checks, validate cloud access,
or call Firebase. This differs from the Firebase CLI's own dry-run, which can enable APIs
(see the pinned CLI's `firebase deploy --help`).

The script deploys neither Android binaries nor Auth providers, App Check registrations,
billing settings, domain configuration, or a web invitation page. A Git commit/push alone
also does not deploy Firebase. No GitHub Actions deployment is configured.

In the console, confirm Functions are present in **`europe-west1`** and wait for Firestore
indexes to finish building. Then check the public metadata callable:

```bash
curl --fail-with-body \
  "https://europe-west1-${STB_STAGING_PROJECT_ID}.cloudfunctions.net/v1ApiInfo" \
  -H 'Content-Type: application/json' --data '{"data":{}}'
```

Expected response:

```json
{"result":{"apiVersion":"v1","minimumClientApiVersion":"v1","environment":"firebase"}}
```

This verifies the deployed bootstrap endpoint, not authentication, database authorization,
or App Check. Verify those through the app next.

## 5. Use the running Android virtual device

Ensure the AVD has internet access. For Google sign-in, use a Google Play-enabled AVD with a
Google account, or a compatible physical device. Email/password is the simplest first test.
In Android Studio you can select the **staging** build variant and the running device.

For an exact terminal install target:

```bash
adb devices -l
# The current AVD is typically emulator-5554; use the serial shown above if different.
./gradlew :app:assembleStaging \
  -PgoogleWebClientId="$STB_STAGING_WEB_CLIENT_ID"
adb -s emulator-5554 install -r app/build/outputs/apk/staging/app-staging.apk
adb -s emulator-5554 shell am start -n com.dts.splitthebill/.MainActivity
```

If `adb` is not on `PATH`, use the binary under your Android SDK's `platform-tools` directory.
For email/password-only testing, the Web client ID property can be omitted. If only the
intended device is connected, `./gradlew :app:installStaging` also builds and installs; pass
the same Web client ID property when testing Google sign-in.

Authorize this installation's App Check debug token:

1. Open Logcat and filter for `DebugAppCheckProvider`, or run:

   ```bash
   adb -s emulator-5554 logcat -s DebugAppCheckProvider
   ```

2. Sign in and attempt a group action so the SDK requests an App Check token. An initial
   failure before registration is expected. Copy the generated debug secret from Logcat.
3. In the **staging project**, go to App Check → Apps → the Android app → Manage debug
   tokens, and register it. Treat this token as a secret; never commit or share the log.
4. Restart the app, allowing time for registration to propagate:

   ```bash
   adb -s emulator-5554 shell am force-stop com.dts.splitthebill
   adb -s emulator-5554 shell am start -n com.dts.splitthebill/.MainActivity
   ```

Repeat for a second AVD/device used in testing. Reinstallation or clearing app data can
generate a new token. See [App Check debug provider](https://firebase.google.com/docs/app-check/android/debug-provider).

## 6. Real Firebase smoke test

Use two disposable accounts, Alice and Bob, on two app installations. Register both App Check
debug tokens. Confirm the users appear in **staging Authentication**, and records appear in
**staging Firestore**, rather than the local Emulator UI at port 4000.

| Action | Expected result |
| --- | --- |
| Create/sign in Alice and Bob; test reset email using a mailbox you control | Auth users are in the staging console; email delivery works |
| Alice creates a EUR group and adds a participant named Bob | Group loads after restart; creation has a server-confirmed result |
| Alice creates an invitation; Bob redeems its manual code | Bob becomes a member; Alice approves linking his account to the named participant |
| Alice adds a EUR 120 expense, paid by Alice and split equally with Bob | Each share is EUR 60; Alice is owed EUR 60; Bob owes EUR 60 |
| Open the expense, then edit it to EUR 140 | Both devices show the update and EUR 70 balances; participant indicators and activity agree |
| Record Bob paying Alice EUR 70 externally | Both balances become zero; this records a repayment and does not transfer money |
| In a second group, create and delete a disposable expense before any repayment | Balances return to zero; history reflects the deletion |
| Create other expenses with exact/percentage splits, currencies, and the date picker | Totals and dates survive restart; amounts remain in their group currency |
| Try a revoked/expired invite and an unrelated third account | Invalid invitations fail; unrelated users cannot read private group data |
| Load data, disable device networking, and try saving a shared change | Cached/offline status appears; no false success; reconnect and retry without duplicate records |
| Change the same expense from both devices | A stale revision produces a conflict/reload path rather than silently overwriting |
| Sign out/in and test Google sign-in if configured | Account boundaries hold and the intended Firebase user is used |
| Delete a disposable account, including owner-transfer/blocking cases | Server cleanup completes; local account data is cleared; other members retain valid financial history |

For invitation testing use the **manual code** until the placeholder link domain is replaced.
Use only throwaway data for deletion tests. Check both light/dark appearance and the home
balance bar while exercising the flows. Automated emulator tests are not proof that cloud
configuration, Google OAuth, real App Check, or email delivery work.

Settlement edit/delete callables exist in the backend but are not exposed in the current
Android payment-history UI. Their reversals are covered by the backend emulator integration
suite; do not expect edit/delete controls when following this app walkthrough.

## 7. Production release procedure

### Prepare production separately

1. Repeat the Firebase project, default database, Android registration, Auth provider, and
   billing setup for the **production** project. Complete the staging smoke test first.
2. Configure Play Integrity App Check for the production Android app, including its SHA-256
   **app-signing certificate**. Link the production Cloud project in Play Console as required.
   For Google Play distribution, obtain this certificate from Play Console → App integrity;
   the upload key and app-signing key can differ. Add the appropriate SHA-1/SHA-256 to Firebase
   Auth registration as well.
3. Exercise a signed release through Google Play **internal testing**, preferably on a
   physical device. Staging's debug token on an emulator does not test Play Integrity.
   Review the provider's app recognition/licensing settings for your distribution channel.
   See [Play Integrity setup](https://firebase.google.com/docs/app-check/android/play-integrity-provider).
4. Review App Check metrics before enabling Firestore enforcement. Mutation callables already
   require App Check immediately outside emulators; `v1ApiInfo` is the public metadata
   exception. Do not ship the debug provider or register development tokens as a production
   workaround.
5. Replace **both** occurrences of `links.example.invalid` in the manifest and invitation
   URL builder (`app/src/main/java/com/dts/splitthebill/ui/SplitTheBillsApp.kt`). Host
   `https://YOUR_DOMAIN/.well-known/assetlinks.json` with package `com.dts.splitthebill` and the
   allowed app-signing SHA-256 certificates. Keep the `/invite` route and provide a web
   fallback with manual-code instructions. Verify with Android's App Links tools and
   `adb shell pm get-app-links com.dts.splitthebill`. No invitation website is deployed by
   `firebase.json`; Hosting and deferred deep linking are not configured here.
6. Decide database backup/restore and retention policies before storing real financial data.
   Operation receipts are part of retry/idempotency behavior: do not add arbitrary TTLs that
   allow old operations to execute twice. Review privacy/account deletion, monitoring, usage
   alerts, IAM access, and the [release device matrix](release-readiness.md).

### Run the release checks

```bash
npm --prefix functions ci
npm --prefix functions run check
./gradlew :core:domain:test :app:testDebugUnitTest :app:lintStaging :app:lintRelease
```

Run the two emulator suites in a disposable local environment with Java 21+ and no other
Emulator Suite occupying the configured ports. The rules suite clears the demo Firestore
database; never aim it at a shared dev session. These commands use the demo project only:

```bash
./functions/node_modules/.bin/firebase emulators:exec --project demo-split-the-bills \
  --only firestore "npm --prefix functions run test:rules"

npm --prefix functions run build
./functions/node_modules/.bin/firebase emulators:exec --project demo-split-the-bills \
  --only auth,firestore,functions "npm --prefix functions run test:emulator"
```

See [development.md](development.md) for Android device tests. Follow the manual device matrix
and retain the staging smoke-test results and tested Git commit for the release.

### Deploy the production backend

From the same reviewed commit that passed staging, with credentials authorized for production:

```bash
export STB_PRODUCTION_PROJECT_ID="replace-with-your-production-project-id"
npm --prefix functions run deploy:firebase -- \
  --project "$STB_PRODUCTION_PROJECT_ID" --dry-run
npm --prefix functions run deploy:firebase -- \
  --project "$STB_PRODUCTION_PROJECT_ID"
```

Wait for indexes to be ready, check `v1ApiInfo` using the production project ID, and verify
the signed client before rolling out broadly. Deployments are not atomic across services:
if Functions fail after Firestore succeeds, the new rules/indexes remain deployed. Inspect
the error, preserve client/API compatibility, and rerun the required scope after fixing it.
The script neither migrates existing financial records nor automatically deletes old functions.

### Sign and distribute the Android app

1. Replace the local `app/google-services.json` with the **production** download and confirm
   its project ID. Select the production Web OAuth client ID too. Keep staging/production
   downloads outside Git and check which one is active before every build.
2. Increment `versionCode` and set the intended `versionName` in `app/build.gradle.kts`.
3. Configure release signing using a keystore outside this repository and passwords in a local
   or CI secret store. This checkout has **no release signing configuration**. Android Studio's
   **Build → Generate Signed App Bundle / APK → Android App Bundle** wizard can build a signed
   release without committing the private key. Supply the production `googleWebClientId`
   property to that build (for example through your local Gradle user properties).
4. The command below checks the release bundle build, but it is **unsigned with the current
   Gradle configuration**. For reproducible CI publishing, configure the release signing
   configuration with CI secrets before treating its output as uploadable:

   ```bash
   export STB_PRODUCTION_WEB_CLIENT_ID="replace-with-your-production-web-client-id.apps.googleusercontent.com"
   ./gradlew :app:bundleRelease \
     -PgoogleWebClientId="$STB_PRODUCTION_WEB_CLIENT_ID"
   # Output: app/build/outputs/bundle/release/app-release.aab
   ```

   An `.aab` is uploaded to Play Console; it is not installed with `adb install`.
5. Upload the signed bundle to Play internal testing, verify Auth/Google sign-in/App Check and
   the complete expense flow, then promote using a controlled rollout. A Firebase backend
   deploy does not publish the APK/AAB to Google Play. See [Android app signing](https://developer.android.com/studio/publish/app-signing).

Never commit keystores, passwords, service-account JSON, OAuth client secrets, or App Check
debug tokens. A release compiled without `app/google-services.json` cannot initialize its real
Firebase runtime. The existing unsigned release build is a compile check, not a deployable app.

## 8. Updates, failures, and rollback

| Change | Action |
| --- | --- |
| Android UI only | Build/test/distribute a new Android version; no backend deployment needed |
| Functions only | `npm --prefix functions run deploy:firebase -- --project "$STB_STAGING_PROJECT_ID" --only functions` |
| Firestore rules/indexes only | `npm --prefix functions run deploy:firebase -- --project "$STB_STAGING_PROJECT_ID" --only firestore` |
| Backend and Firestore | Default `deploy:firebase` workflow; stage before production |
| Auth, App Check, billing, or signing | Console/secret-store steps above; not managed by this script |

For configured CI, the same deployment command accepts `--non-interactive`; install with
`npm ci` first and provide deployment credentials through the CI identity/secret mechanism.
Do not add automatic production deployment to every branch push without deciding the release
policy. The script does not run destructive emulator rule tests during a deploy.

Inspect cloud Functions logs with the pinned CLI:

```bash
./functions/node_modules/.bin/firebase functions:log \
  --project "$STB_STAGING_PROJECT_ID" --only v1CreateExpense
```

| Symptom | Check |
| --- | --- |
| No cloud records, or connection attempts to port 8080/9099 | Install **staging**, not debug; verify the Android configuration's project ID |
| Missing Firebase initialization/configuration | Put the downloaded file at `app/google-services.json`, sync/rebuild, and reinstall |
| Auth `OPERATION_NOT_ALLOWED` | Enable that sign-in provider in the same project as the APK |
| Google sign-in fails | Web client ID, registered signing SHA-1/SHA-256, Google provider, AVD Google Play support/account |
| `APP_CHECK_REQUIRED` or unauthenticated mutation despite signed-in UI | Register the staging debug token in the correct project; on release check Play Integrity metrics/distribution/signing |
| Callable `NOT_FOUND` | Deployment succeeded, matching project, region `europe-west1`, function name/version |
| Firestore `PERMISSION_DENIED` | Correct signed-in user and approved membership, deployed rules, App Check metrics; direct client shared writes are intentionally denied |
| Query requires an index | Confirm `firestore.indexes.json` deployed and indexes reached ready state |
| Deploy permission/billing/API error | CLI account/CI identity roles, target project, Blaze billing, and required enabled APIs; do not grant the Android client admin credentials |
| Deploy quota error (429/500) | Use smaller named-function deployment batches with the pinned CLI and explicit project; see [Functions deployment guidance](https://firebase.google.com/docs/functions/manage-functions#deploy_functions) |
| Installing release fails with a certificate mismatch | Existing staging/debug app has a different signature; use a fresh device/profile or deliberately uninstall it first |

For rollback, use a clean checkout/worktree of the previously tested commit, install its locked
dependencies, and redeploy its Functions and/or rules with an explicit project ID. Review
schema/API compatibility first. This is a code/configuration rollback: it does **not** reverse
Firestore writes, restore deleted accounts, remove newly created indexes/functions, or roll
back the Android app automatically. Restore data only through a separately planned recovery
procedure. Keep logs free of invitation secrets and account credentials.
