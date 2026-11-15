# Architecture

## Trust boundary

Android and future iOS clients read authorized Firestore projections. They never write
shared group, member, participant, expense, settlement, balance, or activity documents
directly. Every shared mutation is a versioned callable Function. The Admin SDK transaction
validates the actor and revision, computes authoritative allocations and balance deltas,
writes activity, then records the operation receipt atomically.

Local Room records are account-scoped drafts only. Firestore's persistent client cache supports offline viewing, but
the UI must require connectivity for shared writes and must show server confirmation before
using “saved”. Expense editor changes debounce into Room, retain their operation ID across
retries, and are deleted only after server confirmation or an explicit discard. DataStore
holds device preferences such as theme, not shared truth.

## Android layers

- UI: stateless Compose screens render immutable state and emit actions.
- Presentation: Hilt-created ViewModels reduce actions into `StateFlow` state and launch
  coroutines for use cases.
- Domain: pure Kotlin money, split, balance, and repayment logic; no Android/Firebase types.
- Data: repository implementations adapt Room, DataStore, Firebase Auth, Firestore reads,
  and callable responses into domain models.

The UI shell follows UI → action → ViewModel → state. The Android-free `core:domain` module
owns checked money parsing, allocations, balance deltas, reversal, and repayment suggestions.
Matching TypeScript code under `functions/src/domain` is used by the authoritative
transactional callable implementation under `functions/src/backend`. Both platforms are
verified against the same committed JSON fixtures.

## Backend flow

```text
callable request
  -> authenticate + App Check
  -> parse v1 contract and enforce limits
  -> authorize membership/ownership and group status
  -> Firestore transaction
       -> reject reused operation ID with different payload
       -> reject stale expected revision with current snapshot metadata
       -> calculate allocations and reverse prior deltas where needed
       -> assert all integer and zero-sum invariants
       -> write resource, balances, activity, user projection, receipt
  -> return v1 response
```

Functions use the `europe-west1` region. Android requests must select that same region.

## Security posture

Firestore rules are deny-by-default. Authenticated users may read their own user document
and group projections, and members may read data below their groups. Even owners cannot
write shared data directly. Invitations and operation receipts are server-only. Emulator
tests lock down these guarantees, including member/non-member access, private collections,
cross-group references, creator/owner checks, and direct-write denial.

Credential-free debug builds use Firebase emulators and intentionally install no App Check
provider because the Emulator Suite has no App Check emulator. Release builds use Play
Integrity and never select emulators. Local databases, drafts, preferences, and shared
preferences are excluded from Android backup so account deletion cannot be undone by restore.

Account deletion is server-first. Android obtains fresh group revisions and eligible ownership
targets from authorized Firestore reads, calls `v1DeleteAccount`, and only after confirmation
cancels listeners, removes account-scoped Room drafts, signs out, terminates Firestore, and
clears its persistent cache. Android then clears the application’s local data (including Auth,
Room, DataStore, and any remaining SDK caches) and closes the process. Theme is a device
preference and contains no account identity, but is reset as part of that complete local wipe.
