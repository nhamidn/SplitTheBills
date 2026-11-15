# Firestore schema v1

All IDs are opaque and immutable. `createdAt`, `updatedAt`, `deletedAt`, and `occurredAt` are
server timestamps. Financial documents retain participant IDs after unlinking or account
deletion. Fields marked projection are server-maintained and never trusted as mutation input.

## User scope

`users/{uid}` is the server-managed account/privacy lifecycle document: it may contain a
mirrored `displayName`, normalized `email`, `photoUrl`, `createdAt`, and `updatedAt`, plus
`deletionState`. Firebase Auth is the v1 source for the signed-in client’s current profile;
clients never write this document. Account deletion removes any mirrored personal fields and
retains only the deletion lifecycle metadata.

`users/{uid}/groups/{groupId}` is a read-optimized projection: `groupId`, `name`, `currency`,
`role`, `participantId?`, `archivedAt?`, `groupRevision`, `updatedAt`. The canonical member
record controls access.

## Group root and access

`groups/{groupId}`: `name`, `currency`, `currencyExponent`, `ownerUid`, `revision`,
`financialRecordCount`, `createdAt`, `updatedAt`, `archivedAt?`.

`groups/{groupId}/members/{uid}`: `uid`, `role` (`OWNER` or `MEMBER`), `participantId?`,
`joinedAt`, `updatedAt`, `revision`. A member and participant may be temporarily unlinked.

`groups/{groupId}/participants/{participantId}`: `displayName`, `linkedUid?`, `status`
(`ACTIVE` or `ANONYMIZED`), `createdByUid`, `createdAt`, `updatedAt`, `revision`. Linking is
unique within a group and owner-approved. Names are display data, never identifiers.

## Financial records

`groups/{groupId}/expenses/{expenseId}`:

- `title`, `amountMinor`, `currency`, `date`, `category`, `note?`
- `contributions[]`: `{participantId, amountMinor}`
- `splitInput`: normalized mode-specific entries retained for editing/audit
- `allocations[]`: server result `{participantId, amountMinor}` ordered by participant ID
- `createdByUid`, `createdAt`, `updatedAt`, `deletedAt?`, `revision`

`groups/{groupId}/settlements/{settlementId}`: `fromParticipantId`, `toParticipantId`,
`amountMinor`, `currency`, `date`, `note?`, `createdByUid`, timestamps, `deletedAt?`, `revision`.
“From” is the participant who paid externally; the balance formula adds that amount to from
and subtracts it from to.

`groups/{groupId}/balances/{participantId}`: `participantId`, `amountMinor`, `currency`,
`updatedAt`, `groupRevision`. For every group, the checked integer sum must be exactly zero.
The amount equals expense contributions minus allocations plus repayments sent minus
repayments received across active records.

`groups/{groupId}/activity/{eventId}`: `eventId`, `type`, `actorUid?`, `resourceType`,
`resourceId`, `resourceRevision`, redacted `summary`, `occurredAt`. Only the server writes
events. Account deletion may null/anonymize `actorUid` while preserving audit meaning.

## Server-only collections

`invitations/{invitationId}`: `groupId`, `tokenHash`, normalized `codeHash`, `createdByUid`,
`createdAt`, `expiresAt`, `revokedAt?`, `maxUses`, `useCount`, `revision`, and bounded
rate-limit metadata. Raw tokens/codes are never stored or logged.

`operationReceipts/{uid_operationId}`: caller UID, operation ID, callable name,
canonical-request hash, committed response, and expiry timestamp. The key prevents two
callers from colliding. Receipts are written in the same transaction as their mutation and
may later be TTL-deleted after the documented retry window.

The Admin SDK may also use private rate-limit documents. They remain outside all client read
rules. No client query is permitted to infer whether a particular invitation exists.
