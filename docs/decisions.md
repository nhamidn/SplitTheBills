# Implementation decisions

1. **Minor units are safe integers.** The v1 wire contract accepts JSON integers only in
   `0..9,007,199,254,740,991`. Backend arithmetic must check every multiply/add before it
   occurs. This range is intentionally compatible with TypeScript `number`; persisted
   Firestore integer values remain exactly representable by every supported client.
2. **Currency precision is server-owned.** ISO 4217 code and server-known exponent are
   stored on a group. A client may format optimistically, but the Function validates the
   code/exponent. Currency becomes immutable after the first non-deleted financial record.
3. **Participants and members stay separate.** A member grants access; a participant owns
   historical financial identity. Linking is an explicit owner-approved operation and
   names are never used as identity evidence.
4. **Payers are modeled as contributions.** The Android MVP editor emits exactly one
   contribution, while the persisted expense and contract use an array whose sum equals
   the total.
5. **Soft deletion preserves IDs.** Expenses, settlements, and referenced participant IDs
   are retained. Deletion reverses the active record's balance delta in one transaction.
6. **Settlement means an external repayment record.** Product copy never says the app sent
   or transferred funds.
7. **Deterministic suggestions are greedy, not advertised as globally minimal.** Debtors
   and creditors are ordered by remaining amount, then stable participant ID.
8. **Invitation secrets are not Firestore-readable.** The server stores only a token hash,
   expiry, revocation, usage bounds, and rate-limit metadata. HTTPS App Links are used once
   a real domain hosts `assetlinks.json`; a short code remains available. Deferred deep
   linking is not promised.
9. **No silent production fallback.** Debug builds always target the Emulator Suite and can
   bootstrap a credential-free demo Firebase app. Their mutation transport implements the
   callable JSON protocol directly with an Auth emulator ID token: the Android callable SDK
   otherwise requests a production Firebase Installations/Instance ID token that a demo
   project cannot obtain. Release builds retain the official callable SDK and require real
   Firebase configuration plus Play Integrity registration.
10. **Deletion is a workflow, not a button-only Auth call.** Ownership must first transfer
    or block deletion; membership is revoked, personal data and local caches are removed,
    and retained participant history is anonymized.
11. **Drafts retain mutation identity.** An expense draft is scoped by account, group, and
    optional expense ID. Its UUID operation ID survives process death and retry, preventing
    a lost callable response from creating a duplicate expense. A stale edit keeps the local
    draft until the user explicitly reloads the server version or successfully submits it.
