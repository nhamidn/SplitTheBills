# Firebase, real-project testing, and App Link setup

These steps require project ownership and are intentionally not automated or deployed.

## Android Firebase registration

1. Create or select a Firebase project in the Firebase console.
2. Add an Android app with package name `com.dts.splitthebill`.
3. Add the debug and eventual release SHA-1 and SHA-256 certificate fingerprints. Obtain the
   local values with `./gradlew signingReport`; never commit a release keystore.
4. Download `google-services.json` to `app/google-services.json`. The path is gitignored and
   activates the Google Services Gradle plugin automatically.
5. Run emulators with that project ID (`--project <project-id>`) or keep the committed
   `demo-split-the-bills` project for rule/backend tests. Use one project ID consistently in
   a test run.

## Deploy (push) the Firebase backend

You do **not** push the Android application code to Firebase, and a GitHub push is not required
for Firebase deployment. In this project, "push to Firebase" means deploying these backend
artifacts with the Firebase CLI:

- callable Cloud Functions from `functions/`;
- Firestore access rules from `firestore.rules`;
- Firestore indexes from `firestore.indexes.json`.

First authenticate the CLI and identify the exact non-production project you intend to change:

```bash
npx firebase-tools login
npx firebase-tools projects:list
```

Verify the code locally before deployment:

```bash
npm --prefix functions install
npm --prefix functions run check
./gradlew test lintDebug assembleStaging
```

Deploy the rules and indexes first, then the functions:

```bash
npx firebase-tools deploy --only firestore:rules,firestore:indexes \
  --project <staging-project-id>

npx firebase-tools deploy --only functions \
  --project <staging-project-id>
```

The functions command may require the Firebase project to use the Blaze plan. These commands
change cloud state, so this repository never runs them automatically. Always include the
explicit `--project <staging-project-id>` argument: the committed `.firebaserc` default is the
local-only `demo-split-the-bills` project and is intentionally not a deployment target.

Run the relevant deployment command again whenever functions, Firestore rules, or indexes
change. Android-only UI changes do not require a Firebase deployment; build and install a new
APK instead. The Android app itself would normally be distributed through Google Play (or an
explicitly configured app-distribution service), not by `firebase deploy`.

## Test on a real Firebase project

The Android variants intentionally have different network behavior:

- `debug` always uses the local Auth, Firestore, and Functions emulators.
- `staging` is locally debug-signed and talks directly to the Firebase project in
  `app/google-services.json`.
- `release` talks directly to Firebase, uses Play Integrity App Check, and remains unsigned
  until release signing is explicitly configured.

Use a separate Firebase project for staging data. Do not point routine development builds at
the production project.

1. Complete **Android Firebase registration** and **Authentication** below. Keep
   `app/google-services.json` local; it is gitignored.
2. Create the Firestore database in the Firebase console. Choose the region deliberately;
   changing it later is not a routine operation.
3. Install the Firebase CLI, authenticate, and confirm the staging project ID:

   ```bash
   npx firebase-tools login
   npx firebase-tools projects:list
   ```

   Keep passing `--project <staging-project-id>` to cloud-changing commands. Do not replace
   the safe committed `demo-split-the-bills` default in `.firebaserc`.
4. Verify locally before deploying:

   ```bash
   npm --prefix functions install
   npm --prefix functions run check
   ./gradlew test lintDebug assembleStaging
   ```

5. Follow **Deploy (push) the Firebase backend** above, using the explicit staging project ID.

6. Build and install the real-Firebase test app. The Web client ID is shown in Firebase
   Authentication / Google provider configuration or Google Cloud credentials:

   ```bash
   ./gradlew :app:installStaging \
     -PgoogleWebClientId=<web-client-id>.apps.googleusercontent.com
   ```

   No `adb reverse` command and no Emulator Suite process should be needed for `staging`.
   `debug` and `staging` intentionally share the production package name required by Firebase.
   When switching between them, uninstall the existing app first if you need a clean test;
   uninstalling removes local drafts and cached data.
7. On the first staging launch, filter Logcat for `DebugAppCheckProvider`. Copy the printed
   debug token into Firebase console → App Check → Apps → your Android app → Manage debug
   tokens. Never commit or share the token. Restart the app after registering it.
8. Test Email/Password first, then Google sign-in, group creation, a second account accepting
   an invitation, an expense, balances, a repayment record, and account deletion. Use two
   devices or two separately installed/test-user environments for the two-account flow.

If authentication succeeds but a shared mutation fails with `APP_CHECK_REQUIRED`, the staging
debug token is missing or registered in a different Firebase project. If a callable is
`NOT_FOUND`, confirm the Functions deployment region is `europe-west1` and the APK was built
from the same `google-services.json` project.

## Authentication

1. In Authentication → Sign-in method, enable Email/Password.
2. Enable Google, choose the support email, and confirm the Android SHA fingerprints above.
3. The Android client uses Credential Manager plus the Google ID library. Pass the Web OAuth
   client ID as `-PgoogleWebClientId=<id>`; do not embed a web client secret. The client ID is
   an identifier, not a secret.
4. Configure authorized domains only when a real web invitation landing page exists.

Email/password, reset, Google Credential Manager, and sign-out UI are implemented. Email
authentication works against the Auth emulator without console credentials. Google sign-in
requires the registered Android app, SHA fingerprints, and Web OAuth client ID above and is
therefore not exercised by the credential-free emulator integration test.

## App Check

1. Register the Android app in App Check with Play Integrity before a release build talks to
   production.
2. Credential-free `debug` builds target the Emulator Suite and intentionally do not install
   an App Check provider because there is no App Check emulator. The `staging` build targets
   real Firebase and installs the App Check debug provider; register its one-time Logcat token
   in the staging Firebase project. Never commit that token.
3. Turn on enforcement for Firestore only after registered test and release builds have been
   verified. Mutation Functions set `enforceAppCheck: true` outside the Emulator Suite; the
   public metadata-only `v1ApiInfo` call is the sole exception. Emulator execution disables
   callable App Check enforcement because the local client has no production attestation.
4. Debug mutation calls use the same versioned callable HTTP contract directly and attach the
   Auth emulator ID token. This deliberately avoids the Android Functions SDK's production
   Firebase Installations/Instance ID lookup for the credential-free demo project. Release
   mutations continue to use the official Firebase Functions SDK and App Check token path.

## HTTPS App Links

1. Replace `links.example.invalid` in `AndroidManifest.xml` with a controlled HTTPS domain.
2. Host `https://<domain>/.well-known/assetlinks.json` with package
   `com.dts.splitthebill` and the SHA-256 fingerprints of every allowed signing certificate.
3. Keep the route `/invite`; it should show a safe web explanation and the manual invite code
   when the app is unavailable.
4. Verify with Android's App Links assistant and `adb shell pm get-app-links
   com.dts.splitthebill`.

This is direct App/Universal Link behavior only. The product must not promise automatic
deferred deep linking after an install.

## Production backend (future explicit authorization)

Select the project with the Firebase CLI, create required indexes, choose retention/TTL for
operation receipts, configure secret/rate-limit infrastructure, and run the full two-user
emulator suite before any deploy. Cloud Functions deployment may require billing. Do not run
`firebase deploy`, enable billing, or enable App Check enforcement as part of local setup.

## Release signing and verification

The release variant enables AGP’s optimized R8/resource pipeline and does not define a signing
configuration. Before distributing it, provide a keystore through a local or CI secret store,
add its SHA-256 certificate to Firebase Auth, App Check, and `assetlinks.json`, then run:

```bash
./gradlew clean testDebugUnitTest lintDebug assembleRelease
npm --prefix functions run check
```

Never place keystore passwords, service-account JSON, App Check debug tokens, or OAuth client
secrets in Gradle properties committed to this repository. A release without
`app/google-services.json` is compile-verifiable only and intentionally cannot initialize the
production Firebase runtime.
