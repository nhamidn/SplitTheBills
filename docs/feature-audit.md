# Feature audit — 2026-11-16

This audit covers the visible Android controls and the unfinished changes retained in the
workspace. The data boundary remains Firebase callable mutations and authorized Firestore
reads; exports and sharing use Android system services.

| Feature | Completed behavior and integration |
| --- | --- |
| Home balances | Observe every membership, retain both credit/debt totals, and display currencies separately. Loading, incomplete results, cached data, and retry are visible. |
| Search | Search groups and transactions across loaded memberships; filter groups/expenses/payments and open the correct group/record. |
| Notifications | Show other members' server-written group activity with an unread badge, persisted per-account read timestamp, and expense/repayment/group navigation. |
| Remind | Derive current amounts owed to the signed-in participant, review/edit a message, and open Android's share chooser. Live record changes invalidate obsolete amounts. Inconsistent or cached balances cannot create actionable reminders. |
| Add expense | Home and Groups open a group picker, then the selected group's form and account-scoped draft. |
| Expense splits | Equal, exact, percentage, and weighted shares; percentages convert to basis points, and previews distribute every cent with the shared calculator. Invalid splits fail before a callable is sent. |
| Expense notes and drafts | Edit/save notes, restore Room drafts, discard changes, and retain draft identity on retry/conflict. |
| Expense changes | Creator/owner edit and confirmed removal; soft deletion reverses balances in the backend. |
| Repayments | Choose/prefill a live debt or record a repayment, open its detail, edit, and confirm removal. All three callable mutations are connected; retries retain operation IDs, and stale edits can reload the current revision. |
| Export | Save quoted UTF-8 CSV through Android's document picker, including expense shares, repayments, dates, and notes. Spreadsheet formula prefixes are neutralized. Cancel/error/success states are distinct. |
| Group types and filters | Persist Trip/Home/Friends/Couple/Other on the group and member projections; existing records default to Other. Filters and sorting use real group data. |
| Group settings | Owner-only rename/type updates and confirmed archive with revision conflicts. Archived financial history remains available. |
| Participants and invitations | Named participants, bounded invitations, sharing/revocation/redemption, and owner-approved account links use existing callable authorization. |
| Members and history | Separate tabs expose participant balances and the server activity history. |
| Global activity | All-group expenses/repayments, type/date filters, and a monthly share chart with separate currencies; records open their details. |
| Profile and friends | Auth display name, membership-derived friend list/counts, group links, and settled-payment counts. |
| Preferences | DataStore persists appearance, default currency for new groups, debt simplification, biometric lock, and per-account inbox read state. |
| App lock | System biometric/device credential authentication gates access and lock changes. The window is protected while locked; the activity theme supports the pinned library's older-Android fingerprint fallback. |
| Help and account actions | Expandable answers, browser support link with failure handling, sign-out, and the existing confirmed account-deletion/ownership-transfer flow. |
| Navigation | Explicit root destination state and guarded group subscriptions keep cross-group actions attached to the selected group. |

Reminders are reviewed messages shared using an app chosen by the user. The notification
inbox reads group activity; its read state is local to each account on the device. Neither
flow reports delivery merely because a chooser or screen opened. Google sign-in, verified
production invitation links, and production deployment retain the setup prerequisites in
[firebase-setup.md](firebase-setup.md).

## Verification

On 2026-11-16, all 44 JVM tests, all 20 Android device tests, backend checks (12 contract/domain
and 9 deployment-script tests), and the Firebase integration scenario passed. The Android
financial journey additionally verified real percentage/repayment writes, balance reversal,
and CSV document-result handling with a read-back of the saved file. The document location is
supplied by an instrumentation monitor; the app's real result callback and file write run.
Debug lint, staging compilation, and the optimized unsigned release build also passed.

Run the commands and manual scenarios in [development.md](development.md). Unit coverage
includes percentage conversion, exact/weighted/equal previews, rounding, multi-currency
aggregation, stale balance suppression, CSV escaping, cross-group subscriptions, repayment
retry IDs/conflicts, root navigation, export cancellation, and account-specific read state.
Device coverage exercises the visible actions, Android share intent, durable preferences,
fingerprint fallback theme inflation, and the Firebase-backed financial journey. Biometric
acceptance with enrolled hardware/device credentials and the manual accessibility matrix
remain device-dependent checks in [release-readiness.md](release-readiness.md).
