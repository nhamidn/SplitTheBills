import { assertConserved, Balances } from "./balance.js";
import { compareParticipantId } from "./calculation-error.js";

export interface RepaymentSuggestion {
  fromParticipantId: string;
  toParticipantId: string;
  amountMinor: number;
}

/** Deterministic largest-balance-first suggestions; no minimum-count claim is made. */
export function suggestRepayments(balances: Balances): RepaymentSuggestion[] {
  assertConserved(balances);
  const creditors = new Map(Object.entries(balances).filter(([, amount]) => amount > 0));
  const debtors = new Map(Object.entries(balances).filter(([, amount]) => amount < 0)
    .map(([id, amount]) => [id, -amount] as const));
  const suggestions: RepaymentSuggestion[] = [];

  while (creditors.size > 0 && debtors.size > 0) {
    const creditor = largestThenId(creditors);
    const debtor = largestThenId(debtors);
    const amount = Math.min(creditor[1], debtor[1]);
    suggestions.push({ fromParticipantId: debtor[0], toParticipantId: creditor[0], amountMinor: amount });
    updateRemaining(creditors, creditor[0], creditor[1] - amount);
    updateRemaining(debtors, debtor[0], debtor[1] - amount);
  }
  if (creditors.size !== 0 || debtors.size !== 0) throw new Error("Internal repayment conservation failure");
  return suggestions;
}

function largestThenId(values: Map<string, number>): [string, number] {
  const entry = [...values.entries()].sort((left, right) => {
    if (left[1] !== right[1]) return left[1] > right[1] ? -1 : 1;
    return compareParticipantId(left[0], right[0]);
  })[0];
  if (!entry) throw new Error("Expected a non-empty balance map");
  return entry;
}

function updateRemaining(values: Map<string, number>, id: string, amount: number): void {
  if (amount === 0) values.delete(id);
  else values.set(id, amount);
}
