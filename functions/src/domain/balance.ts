import {
  checkedAdd,
  compareParticipantId,
  fail,
  participantId,
  safeNonNegative,
  safePositive,
  safeSigned,
  uniqueParticipantIds,
} from "./calculation-error.js";
import { Allocation, Contribution } from "./split.js";

export interface BalanceDelta { participantId: string; amountMinor: number }
export interface ExpenseFinancials {
  totalMinor: number;
  contributions: ReadonlyArray<Contribution>;
  allocations: ReadonlyArray<Allocation>;
}
export interface SettlementFinancials {
  fromParticipantId: string;
  toParticipantId: string;
  amountMinor: number;
}
export type DeltaDirection = "APPLY" | "REVERSE";
export type Balances = Readonly<Record<string, number>>;

export function expenseDeltas(expense: ExpenseFinancials): BalanceDelta[] {
  safePositive(expense.totalMinor, "expense total");
  validateContributions(expense);
  validateAllocations(expense);
  const deltas = new Map<string, number>();
  expense.contributions.forEach((item) => addDelta(deltas, item.participantId, item.amountMinor));
  expense.allocations.forEach((item) => addDelta(deltas, item.participantId, -item.amountMinor));
  return normalizedDeltas(deltas);
}

export function settlementDeltas(settlement: SettlementFinancials): BalanceDelta[] {
  const from = participantId(settlement.fromParticipantId);
  const to = participantId(settlement.toParticipantId);
  if (from === to) fail("INVALID_PARTICIPANT", "Settlement endpoints must differ");
  safePositive(settlement.amountMinor, "settlement amount");
  return [
    { participantId: from, amountMinor: settlement.amountMinor },
    { participantId: to, amountMinor: -settlement.amountMinor },
  ].sort((a, b) => compareParticipantId(a.participantId, b.participantId));
}

export function applyDeltas(
  balances: Balances,
  deltas: ReadonlyArray<BalanceDelta>,
  direction: DeltaDirection = "APPLY",
): Record<string, number> {
  assertConserved(balances);
  const updated: Record<string, number> = { ...balances };
  deltas.forEach((delta) => {
    const id = participantId(delta.participantId);
    safeSigned(delta.amountMinor, "delta");
    const directed = direction === "APPLY" ? delta.amountMinor : -delta.amountMinor;
    updated[id] = checkedAdd(updated[id] ?? 0, directed, "participant balance");
  });
  assertConserved(updated);
  return Object.fromEntries(
    Object.entries(updated).sort(([left], [right]) => compareParticipantId(left, right)),
  );
}

export function replaceExpense(
  balances: Balances,
  previous: ExpenseFinancials,
  replacement: ExpenseFinancials,
): Record<string, number> {
  return applyDeltas(
    applyDeltas(balances, expenseDeltas(previous), "REVERSE"),
    expenseDeltas(replacement),
  );
}

export function assertConserved(balances: Balances): void {
  let sum = 0n;
  Object.entries(balances).forEach(([id, amount]) => {
    participantId(id);
    safeSigned(amount, "balance");
    sum += BigInt(amount);
  });
  if (sum !== 0n) fail("BALANCES_NOT_CONSERVED", "Group balances must sum to zero");
}

function validateContributions(expense: ExpenseFinancials): void {
  uniqueParticipantIds(expense.contributions.map((item) => item.participantId));
  let sum = 0;
  expense.contributions.forEach((item) => {
    safePositive(item.amountMinor, "contribution");
    sum = checkedAdd(sum, item.amountMinor, "contribution sum");
  });
  if (sum !== expense.totalMinor) {
    fail("CONTRIBUTIONS_DO_NOT_SUM", "Contributions must sum to the expense total");
  }
}

function validateAllocations(expense: ExpenseFinancials): void {
  uniqueParticipantIds(expense.allocations.map((item) => item.participantId));
  let sum = 0;
  expense.allocations.forEach((item) => {
    safeNonNegative(item.amountMinor, "allocation");
    sum = checkedAdd(sum, item.amountMinor, "allocation sum");
  });
  if (sum !== expense.totalMinor) {
    fail("ALLOCATIONS_DO_NOT_SUM", "Allocations must sum to the expense total");
  }
}

function addDelta(target: Map<string, number>, id: string, amount: number): void {
  participantId(id);
  target.set(id, checkedAdd(target.get(id) ?? 0, amount, "expense delta"));
}

function normalizedDeltas(deltas: Map<string, number>): BalanceDelta[] {
  assertConserved(Object.fromEntries(deltas));
  return [...deltas.entries()]
    .filter(([, amount]) => amount !== 0)
    .sort(([left], [right]) => compareParticipantId(left, right))
    .map(([id, amountMinor]) => ({ participantId: id, amountMinor }));
}
