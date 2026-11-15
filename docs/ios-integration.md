# iOS integration note

The future Swift client should treat `shared/contracts/v1/callables.json` and
`docs/api-v1.md` as its wire source of truth. Generate or hand-write `Codable` request and
response types without importing Android models. Keep minor amounts in checked `Int64`, but
reject values outside the v1 JavaScript-safe range before sending.

Use Firebase Auth for identity, Firestore listeners for authorized reads, and callable
Functions in `europe-west1` for shared writes. Send a fresh UUID operation ID for every user
intent and reuse it only when retrying that exact payload. Persist the last server revision
and surface `STALE_REVISION` rather than overwriting.

Invitation documents remain server-only. Owners use `v1ListInvitations` for bounded,
non-secret metadata needed to revoke an older invitation; raw codes/tokens are returned only
by the initial `v1CreateInvitation` response. Persist expense drafts locally per authenticated
account/group/resource, including the operation ID, and remove them only after confirmed
success or an explicit discard.

Participant IDs must remain opaque stable strings. Never link a signed-in user by comparing
name or email to participant text. Render settlements as records of payments made outside
the app. Consume the same JSON allocation fixtures as Kotlin/TypeScript tests before the iOS
client is considered compatible.

Universal Links should target the eventual `/invite` HTTPS route and support manual code
entry. The web domain must host Apple's association file separately from Android's
`assetlinks.json`; neither platform should claim automatic deferred deep linking.

Profile display name is Firebase Auth user metadata, not a participant-linking signal. Mirror
Android’s account-deletion flow by reading fresh owned-group revisions/member choices, sending
one transfer for every owned group, and clearing account-scoped drafts and Firestore persistence
only after `v1DeleteAccount` succeeds. A failed callable must leave local data available for a
retry; it must never be presented as a completed deletion.
