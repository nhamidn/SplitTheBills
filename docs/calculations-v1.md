# Calculation rules v1

This document is normative for Android, Functions, and the future iOS implementation. The
executable examples are in `shared/fixtures/splits-v1.json`.

## Numeric boundary

Wire amounts, weights, basis points, allocations, deltas, and individual balances are JSON
integers in `[-9,007,199,254,740,991, 9,007,199,254,740,991]`; expense, allocation,
contribution, settlement, weight, and percentage inputs are non-negative or positive as
their field requires. Every input and arithmetic result is checked.

Kotlin and TypeScript use `BigInteger`/`bigint` only for internal products, remainders, and
conservation sums. Results are converted back only after safe-range validation. Swift should
use full-width integer division or an arbitrary-precision helper for the same intermediate
operations. Floating-point and decimal-to-binary conversion are forbidden.

Participant IDs used for calculation ordering match `[A-Za-z0-9_-]{1,128}`. This ASCII
restriction makes ascending tie-break order identical on all clients.

## Currency text

`CurrencyUnit` carries both the uppercase three-letter code and an exponent from 0 through
4. Parsing accepts an unsigned plain decimal without separators, exponent notation, signs,
or implicit rounding. Fewer fractional digits are padded; too many are rejected. Formatting
always emits exactly the configured number of fractional digits. The backend will own the
code-to-exponent catalog; clients must not infer exponent 2.

## Allocation

All modes reject empty lists, more than 100 entries, invalid IDs, and duplicate IDs.

- Equal assigns a weight of 1 to every participant.
- Exact accepts non-negative minor amounts whose checked sum equals the total.
- Percentage accepts positive integer basis points whose checked sum is 10,000.
- Weighted accepts positive integer shares whose checked sum remains a safe integer.

For equal, percentage, and weighted modes, for each entry:

```text
product   = totalMinor × entryUnits          // arbitrary-precision intermediate
base      = product / totalUnits             // integer floor
remainder = product % totalUnits
```

After summing bases, leftover units are awarded by descending remainder. Exact remainder
ties use ascending participant ID. Results are returned in ascending participant-ID order,
so input order never affects allocation. The output sum must equal the total exactly.

## Balances and reversal

An expense first validates:

```text
sum(contributions) = sum(allocations) = expense total
```

Its per-participant delta is contributions minus allocations. An external settlement from
participant A to participant B adds its amount to A and subtracts it from B. Applying every
delta must leave the group sum at zero.

An edit reverses every delta from the currently active record before applying the complete
replacement record. Soft deletion performs only the reversal. Settlement edits/deletions
use the same pattern. Zero balance entries may remain as stable projections.

## Suggested repayments

Suggestions require conserved balances. Positive participants are creditors and negative
participants are debtors. On every iteration, select the largest remaining debt and largest
remaining credit; ties use ascending participant ID. Suggest the smaller remaining amount,
update both sides, and repeat until all balances are zero.

This process is deterministic and directionally correct, but the product must not claim that
it globally minimizes transfer count.
