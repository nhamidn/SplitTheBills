import {
  checkedAdd,
  compareParticipantId,
  fail,
  safeNonNegative,
  safePositive,
  uniqueParticipantIds,
} from "./calculation-error.js";

export interface Allocation { participantId: string; amountMinor: number }
export interface Contribution { participantId: string; amountMinor: number }
export type ExactEntry = Allocation;
export interface PercentageEntry { participantId: string; basisPoints: number }
export interface WeightedEntry { participantId: string; shares: number }

export type SplitSpec =
  | { mode: "EQUAL"; entries: ReadonlyArray<{ participantId: string }> }
  | { mode: "EXACT"; entries: ReadonlyArray<ExactEntry> }
  | { mode: "PERCENTAGE"; entries: ReadonlyArray<PercentageEntry> }
  | { mode: "WEIGHTED"; entries: ReadonlyArray<WeightedEntry> };

export function allocate(totalMinor: number, split: SplitSpec): Allocation[] {
  safePositive(totalMinor, "totalMinor");
  switch (split.mode) {
    case "EQUAL":
      return proportional(totalMinor, split.entries.map((entry) => ({ ...entry, shares: 1 })));
    case "EXACT":
      return exact(totalMinor, split.entries);
    case "PERCENTAGE":
      return percentage(totalMinor, split.entries);
    case "WEIGHTED":
      return proportional(totalMinor, split.entries);
  }
}

function exact(totalMinor: number, entries: ReadonlyArray<ExactEntry>): Allocation[] {
  uniqueParticipantIds(entries.map((entry) => entry.participantId));
  let sum = 0;
  const allocations = entries.map((entry) => {
    safeNonNegative(entry.amountMinor, "exact amount");
    sum = checkedAdd(sum, entry.amountMinor, "exact sum");
    return { participantId: entry.participantId, amountMinor: entry.amountMinor };
  });
  if (sum !== totalMinor) fail("INVALID_SPLIT", "Exact amounts must sum to total");
  return allocations.sort((a, b) => compareParticipantId(a.participantId, b.participantId));
}

function percentage(totalMinor: number, entries: ReadonlyArray<PercentageEntry>): Allocation[] {
  uniqueParticipantIds(entries.map((entry) => entry.participantId));
  let sum = 0;
  const weights = entries.map((entry) => {
    safePositive(entry.basisPoints, "basisPoints");
    sum = checkedAdd(sum, entry.basisPoints, "basisPoints sum");
    return { participantId: entry.participantId, shares: entry.basisPoints };
  });
  if (sum !== 10_000) fail("INVALID_SPLIT", "Percentages must sum to 10,000 basis points");
  return proportional(totalMinor, weights);
}

function proportional(totalMinor: number, entries: ReadonlyArray<WeightedEntry>): Allocation[] {
  uniqueParticipantIds(entries.map((entry) => entry.participantId));
  let denominator = 0;
  entries.forEach((entry) => {
    safePositive(entry.shares, "shares");
    denominator = checkedAdd(denominator, entry.shares, "shares sum");
  });

  const denominatorBig = BigInt(denominator);
  const rows = entries.map((entry) => {
    const product = BigInt(totalMinor) * BigInt(entry.shares);
    return {
      participantId: entry.participantId,
      amountMinor: Number(product / denominatorBig),
      remainder: product % denominatorBig,
    };
  });
  let allocated = rows.reduce((sum, row) => checkedAdd(sum, row.amountMinor, "allocation sum"), 0);
  let remainderUnits = totalMinor - allocated;
  const remainderOrder = [...rows].sort((left, right) => {
    if (left.remainder !== right.remainder) return left.remainder > right.remainder ? -1 : 1;
    return compareParticipantId(left.participantId, right.participantId);
  });
  let index = 0;
  while (remainderUnits > 0) {
    const row = remainderOrder[index];
    if (!row) throw new Error("Internal allocation remainder failure");
    row.amountMinor = checkedAdd(row.amountMinor, 1, "allocation");
    allocated = checkedAdd(allocated, 1, "allocation sum");
    remainderUnits--;
    index++;
  }
  if (allocated !== totalMinor) throw new Error("Internal allocation conservation failure");
  return rows
    .sort((a, b) => compareParticipantId(a.participantId, b.participantId))
    .map(({ participantId, amountMinor }) => ({ participantId, amountMinor }));
}
