# Demo accounts and test data

The local Firebase Emulator Suite creates these fictional accounts and shared plans
automatically. Use the Android **debug** build with the local servers running.

## Sign in

Every demo account uses the password **`SplitDemo123!`**. Choose **Sign in**, rather than
Create account, and enter one of these emails:

| Account | Email | What to test |
| --- | --- | --- |
| Alice Morgan | `alice@demo.test` | Start here. Owns Lisbon weekend and the archived Mountain day trip; shares the apartment and movie group. Has people to remind and amounts to repay. |
| Bob Chen | `bob@demo.test` | Owns Shared apartment and Weekend plans. Receives home repayments while owing money in the trip and movie groups. |
| Cara Silva | `cara@demo.test` | Owns Friday movie night. Tests USD credit separately from EUR debts, plus member permissions in other groups. |
| Dana Patel | `dana@demo.test` | Starts without memberships for empty screens and invitation redemption. An unlinked Dana participant already exists in Lisbon weekend. |

These credentials belong to the local demo project, not staging or production Firebase.
The seed script restores their documented password on each run, while retaining display-name
edits, existing group records, and changes made through the app.

## Start the server

From the repository root, with Node 22 selected:

```bash
npm --prefix functions ci
npm --prefix functions run emulators -- --only auth,firestore,functions
```

Startup compiles the backend, starts Auth/Firestore/Functions, waits for the callable API,
and runs the seed script. Wait for both **All emulators ready** and **[demo] Demo data ready**.
The console prints the available logins and shared password. The backend administration
UI is at <http://127.0.0.1:4000>.

If the terminal uses a different Node runtime, the existing pinned-runtime fallback works:

```bash
npm exec --yes --package=node@22.13.1 -- \
  npm --prefix functions run emulators -- --only auth,firestore,functions
```

To add the demo data to a suite that is already running:

```bash
npm --prefix functions run seed:demo
```

The standalone script uses the local ports/project from `firebase.json`. Both commands accept
`--config PATH` and `--project demo-PROJECT_ID` when using a separate local emulator suite.
Seeding requires Auth, Firestore, and the Functions API together. Individual rules-only
emulator commands documented in [development.md](development.md) remain unseeded.

## What is included

There are **4 users, 5 groups, 11 expenses, and 4 repayments**. Dates are relative to the
first seed run, with some expenses dated that day so the monthly chart has data. Groups
retain their own currencies; the app does not add EUR, USD, and GBP together.

The initial balances below use `+` for money a participant receives and `−` for money owed:

| Group | Type / currency | Alice | Bob | Cara |
| --- | --- | --- | --- | --- |
| Lisbon weekend | Trip / EUR | +101.50 | −14.25 | −87.25 |
| Shared apartment | Home / EUR | −38.00 | +73.00 | −35.00 |
| Friday movie night | Friends / USD | −4.00 | −18.00 | +22.00 |
| Mountain day trip | Archived trip / EUR | 0.00 | 0.00 | — |
| Weekend plans | Couple / GBP, no expenses yet | — | 0.00 | 0.00 |

Lisbon weekend includes equal apartment/dinner splits, weighted airport transfers, exact
museum ticket prices, percentage-based coffee, and Bob's partial repayment. Dana's unlinked
participant has a zero balance and is excluded from these existing expenses.

Shared apartment includes groceries, internet, cleaning supplies, and Alice's partial
repayment. Friday movie night has cinema tickets, snacks, and a partial repayment from Bob.
Mountain day trip is fully settled and archived for read-only history testing. Weekend plans
provides a real empty group where Bob or Cara can add an expense.

## Suggested walkthrough

1. **Alice:** Home shows EUR credit/debt and USD debt separately. Open **Remind** to review
   Bob's EUR 14.25 or Cara's EUR 87.25 trip balance. Sharing opens Android's chooser; choose
   the intended recipient in your messaging app.
2. **Bob:** Open Shared apartment to see money owed to you. Open Lisbon weekend and record
   a repayment to Alice, then edit or remove it and check the balance changes.
3. **Cara:** Switch between EUR and USD on Home/Activity. Search for `Cinema tickets`,
   `Airport transfers`, or a note such as `coast`. Inspect the other members' notifications.
4. **Alice:** Open Lisbon weekend, inspect the four split modes, export its CSV, and try
   group settings. Other members can view the settings, while owner actions remain restricted.
5. **Invitations:** As Alice, open Lisbon weekend → **Manage people**, create an invitation,
   and note/share its code. Sign out, sign in as Dana, and join with that code. Return to
   Alice to approve Dana's link to the existing Dana participant.
6. **Empty/archive states:** Dana initially has no groups. Weekend plans has no expenses.
   Mountain day trip has history and zero balances, with financial changes disabled.

Use Dana or a new disposable account for account-deletion testing. Seeding does not undo
membership removal, ownership transfers, archived groups, or edited/deleted financial records.

## Reruns and persistence

Rerunning the seeder against the same running or imported database keeps completed groups,
uses stable operation IDs to resume interrupted financial writes, and does not duplicate
expenses or repayments. The marker lives in the server-only `emulatorSeeds` collection.
App-created accounts/groups outside these demo fixtures are left untouched.

Firebase's normal in-memory behavior still applies: a fresh start **without import** creates
the original demo data again. To retain your testing changes across server restarts, use
Firebase's existing import/export options:

```bash
npm --prefix functions run emulators -- --only auth,firestore,functions \
  --import="$HOME/.local/share/split-the-bills-emulator-data" --export-on-exit
```

Create the first export using the command in [development.md](development.md#7-edit-restart-and-keep-test-data).
Imported fixture edits are preserved; restarting does not restore their original balances.
Deleting private seed markers or operation receipts manually is not a reset workflow.

## Developer checks

`npm --prefix functions run check` includes fixture arithmetic, account-guide consistency,
local-target validation, startup sequencing, failure cleanup, and graceful shutdown tests.

For the integration test, start the separate test suite in one terminal:

```bash
npm --prefix functions run emulators -- --only auth,firestore,functions \
  --project demo-split-the-bills-seed-test --config firebase.seed-test.json
```

After its demo-ready message, run this from the repository root in another terminal:

```bash
SPLIT_DEMO_SEED_TEST_CONFIG="$PWD/firebase.seed-test.json" \
SPLIT_DEMO_SEED_TEST_PROJECT=demo-split-the-bills-seed-test \
  npm --prefix functions run test:seed
```

This uses ports 19099/18080/15001 and a separate project. The test verifies logins, balances,
rerun idempotency, interrupted-seed recovery, and preservation of user edits. It deliberately
edits a test fixture and refuses to run without a separate `*-seed-test` project. Stop the
test suite with Ctrl+C afterward; your regular development suite stays running.
