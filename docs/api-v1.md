# Callable API v1

This document is the normative human-readable v1 contract. The companion manifest is
[`shared/contracts/v1/callables.json`](../shared/contracts/v1/callables.json). Callable names
are versioned so incompatible changes can coexist.

The exact arithmetic and tie-breaking specification is in
[`calculations-v1.md`](calculations-v1.md).

## Common envelope

Every mutation request contains:

```json
{
  "apiVersion": "v1",
  "operationId": "UUID generated once per user intent",
  "groupId": "opaque ID when group-scoped",
  "expectedRevision": 4
}
```

`operationId` is required and idempotent. Retrying the same ID and canonical request returns
the stored response; reusing it for a different payload returns `OPERATION_ID_REUSED`.
`expectedRevision` is required for edit/delete/archive/owner-management operations. Revisions
start at 1. Create calls omit it unless stated otherwise.

Successful mutations return:

```json
{
  "apiVersion": "v1",
  "operationId": "same UUID",
  "resourceId": "opaque ID",
  "revision": 5,
  "committedAt": "server RFC 3339 timestamp",
  "replayed": false
}
```

Firestore timestamps are returned as RFC 3339 strings at the callable boundary. Dates that
represent the user's chosen calendar day are `YYYY-MM-DD` strings and are not converted by
timezone.

Authentication and App Check failures rejected by Firebase before the callable handler may
use Firebase's standard error payload without the v1 `details` object. Clients should handle
those transport/authentication failures before decoding the stable application error codes.

## Money and split input

`amountMinor` is a non-negative JSON integer no larger than `9,007,199,254,740,991`.
Currency is an uppercase ISO 4217 code. The group currency is authoritative; each financial
request repeats it to make accidental cross-currency writes fail loudly.

The expense payload is:

```json
{
  "title": "Groceries",
  "amountMinor": 4250,
  "currency": "EUR",
  "contributions": [{ "participantId": "p_alice", "amountMinor": 4250 }],
  "split": {
    "mode": "WEIGHTED",
    "entries": [
      { "participantId": "p_alice", "shares": 1 },
      { "participantId": "p_bob", "shares": 2 }
    ]
  },
  "date": "2026-09-07",
  "category": "GROCERIES",
  "note": "optional"
}
```

The MVP requires exactly one positive contribution, but the persisted array and invariant
support multiple payers later. Contribution sum and server allocation sum must both equal
the expense total.

Split entry fields by mode:

- `EQUAL`: participant ID only.
- `EXACT`: `amountMinor`; entries must sum to total.
- `PERCENTAGE`: integer `basisPoints`; entries must sum to 10,000.
- `WEIGHTED`: positive integer `shares`; checked multiplication/sums must stay safe.

For proportional modes the server calculates floors and distributes leftover minor units
by largest fractional remainder, then ascending participant ID for exact ties. The client
may preview but never overrides server allocations.

## Callable catalog

| Callable | Required role | Operation-specific input |
|---|---|---|
| `v1ApiInfo` | none | none; metadata only, no mutation |
| `v1CreateGroup` | signed in | `name`, `currency`, owner `participantName` |
| `v1RenameGroup` | owner | `groupId`, `expectedRevision`, `name` |
| `v1ArchiveGroup` | owner | `groupId`, `expectedRevision`, `archived: true` |
| `v1CreateParticipant` | owner | `groupId`, `name` |
| `v1CreateInvitation` | owner | `groupId`, `expiresInSeconds`, `maxUses` |
| `v1ListInvitations` | owner | `groupId`; returns bounded safe metadata, never secrets or hashes |
| `v1RevokeInvitation` | owner | `groupId`, `invitationId`, `expectedRevision` |
| `v1RedeemInvitation` | signed in | raw `token` or normalized `code` |
| `v1ApproveParticipantLink` | owner | `groupId`, `participantId`, `memberUid`, group revision |
| `v1CreateExpense` | member | `groupId`, expense payload |
| `v1UpdateExpense` | creator or owner | `groupId`, `expenseId`, resource revision, full payload |
| `v1DeleteExpense` | creator or owner | `groupId`, `expenseId`, resource revision |
| `v1CreateSettlement` | member | from/to participant IDs, amount, currency, date, note |
| `v1UpdateSettlement` | creator or owner | ID, resource revision, full payload |
| `v1DeleteSettlement` | creator or owner | ID and resource revision |
| `v1DeleteAccount` | signed in | explicit confirmation and optional ownership transfers |

Account deletion requires `confirmation: "DELETE"`. `ownershipTransfers` must contain
exactly one `{ groupId, newOwnerUid, expectedRevision }` object for every group currently
owned by the caller, and each target must already be a different member of that group. The
transaction transfers ownership, revokes the caller’s memberships, removes user projections,
anonymizes (but does not merge or delete) linked participant records, and writes activity.
After the transaction the Auth user and personal profile fields are removed. The response also
contains `transferredGroupIds` in ascending group-ID order. `OWNER_TRANSFER_REQUIRED` includes
the authoritative `ownedGroupIds`; stale group revisions use the normal `STALE_REVISION`
contract. Clients must remove account-scoped local drafts and cached authorized data only after
the callable succeeds.

Create responses return server-assigned IDs. Invitation creation is the only response that
returns a raw secret, once; stored invitation documents contain only its hash. Redemption is
rate-limited by normalized code/token hash and caller/IP-derived server signals, expires,
can be revoked, and consumes usage in the membership transaction.

Archived groups reject expense and settlement creation/edit/deletion with `GROUP_ARCHIVED`.
Rename, invitation/member management, ownership transfer, reads, and explicit unarchive
policy may remain available to the owner; v1 initially supports archive only.

## Error contract

Functions throw a Firebase callable `HttpsError`; stable app behavior uses `details`:

```json
{
  "apiVersion": "v1",
  "errorCode": "STALE_REVISION",
  "message": "The expense changed on another device.",
  "retryable": false,
  "resourceId": "expense_123",
  "expectedRevision": 4,
  "actualRevision": 5
}
```

| Stable code | Callable status | Meaning/action |
|---|---|---|
| `UNAUTHENTICATED` | `unauthenticated` | Sign in again. |
| `APP_CHECK_REQUIRED` | `failed-precondition` | Obtain a valid App Check token. |
| `INVALID_ARGUMENT` | `invalid-argument` | Correct field errors supplied in `details.fields`. |
| `NOT_A_MEMBER` | `permission-denied` | The caller cannot access the group. |
| `OWNER_REQUIRED` | `permission-denied` | Only the current owner may perform this action. |
| `RECORD_EDIT_FORBIDDEN` | `permission-denied` | Caller is neither creator nor owner. |
| `NOT_FOUND` | `not-found` | Resource is absent or deliberately hidden. |
| `STALE_REVISION` | `aborted` | Reload; preserve local edits and let the user compare/retry. |
| `OPERATION_ID_REUSED` | `already-exists` | Generate a new ID only for a new user intent. |
| `GROUP_ARCHIVED` | `failed-precondition` | Financial mutations are disabled. |
| `CURRENCY_LOCKED` | `failed-precondition` | Existing financial history prevents currency changes. |
| `BALANCE_INVARIANT_FAILED` | `internal` | Do not retry blindly; preserve the draft and report. |
| `INVITATION_EXPIRED` | `deadline-exceeded` | Ask the owner for a new invitation. |
| `INVITATION_REVOKED` | `failed-precondition` | Ask the owner for a new invitation. |
| `INVITATION_EXHAUSTED` | `resource-exhausted` | Usage limit was reached. |
| `GROUP_FULL` | `resource-exhausted` | Active participant places are already assigned or reserved by accepted members. The owner must add a participant before another account can join. |
| `RATE_LIMITED` | `resource-exhausted` | Retry only after `retryAfterSeconds`. |
| `OWNER_TRANSFER_REQUIRED` | `failed-precondition` | Transfer every owned group before account deletion. |

Limits at v1: names 1–80 Unicode code points, titles 1–120, notes 0–1,000, no more than
100 active participants per group, 100 split/contribution entries per record, and invitation
lifetime 5 minutes–30 days. Unknown fields are rejected to detect client/server drift.
