import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { describe, test } from "node:test";
import {
  applyDeltas,
  expenseDeltas,
  ExpenseFinancials,
  replaceExpense,
  settlementDeltas,
} from "./balance.js";
import { CalculationError, CalculationErrorCode, MAX_SAFE_MINOR } from "./calculation-error.js";
import { formatMinor, parseUnsignedMinor } from "./money.js";
import { suggestRepayments } from "./repayment.js";
import { allocate, SplitSpec } from "./split.js";

interface AllocationFixture { participantId: string; amountMinor: number }
interface MoneyTextFixture {
  id: string;
  code: string;
  exponent: number;
  input: string;
  expectedMinor: number;
  expectedFormatted: string;
}
interface SplitFixture {
  id: string;
  amountMinor: number;
  split: SplitSpec;
  expectedAllocations: AllocationFixture[];
}
interface BalanceFixture {
  id: string;
  expense: ExpenseFinancials;
  expectedExpenseBalances: Record<string, number>;
  settlement: { fromParticipantId: string; toParticipantId: string; amountMinor: number };
  expectedAfterSettlement: Record<string, number>;
}
interface RepaymentFixture {
  id: string;
  balances: Record<string, number>;
  expected: Array<{ fromParticipantId: string; toParticipantId: string; amountMinor: number }>;
}
interface FixtureRoot {
  moneyTextCases: MoneyTextFixture[];
  cases: SplitFixture[];
  balanceCases: BalanceFixture[];
  repaymentCases: RepaymentFixture[];
}

const fixture = JSON.parse(
  readFileSync(new URL("../../../shared/fixtures/splits-v1.json", import.meta.url), "utf8"),
) as FixtureRoot;

describe("shared calculation fixtures", () => {
  test("money text matches Kotlin fixtures", () => {
    fixture.moneyTextCases.forEach((item) => {
      const currency = { code: item.code, exponent: item.exponent };
      const minor = parseUnsignedMinor(item.input, currency);
      assert.equal(minor, item.expectedMinor, item.id);
      assert.equal(formatMinor(minor, currency), item.expectedFormatted, item.id);
    });
  });

  test("all split modes match Kotlin fixtures", () => {
    fixture.cases.forEach((item) => {
      assert.deepEqual(allocate(item.amountMinor, item.split), item.expectedAllocations, item.id);
    });
  });

  test("expense, settlement, reversal, and repayment direction match Kotlin fixtures", () => {
    fixture.balanceCases.forEach((item) => {
      const expenseBalances = applyDeltas({}, expenseDeltas(item.expense));
      assert.deepEqual(expenseBalances, item.expectedExpenseBalances, item.id);
      const deltas = settlementDeltas(item.settlement);
      const settled = applyDeltas(expenseBalances, deltas);
      assert.deepEqual(settled, item.expectedAfterSettlement, item.id);
      assert.deepEqual(applyDeltas(settled, deltas, "REVERSE"), expenseBalances, item.id);
    });
    fixture.repaymentCases.forEach((item) => {
      assert.deepEqual(suggestRepayments(item.balances), item.expected, item.id);
    });
  });
});

describe("calculation validation", () => {
  test("rejects invalid percentage, weight, exact, and duplicate inputs", () => {
    assertCode("INVALID_SPLIT", () => allocate(100, {
      mode: "PERCENTAGE",
      entries: [{ participantId: "p-a", basisPoints: 5_000 }, { participantId: "p-b", basisPoints: 4_999 }],
    }));
    assertCode("INVALID_AMOUNT", () => allocate(100, {
      mode: "WEIGHTED",
      entries: [{ participantId: "p-a", shares: 0 }],
    }));
    assertCode("INVALID_SPLIT", () => allocate(100, {
      mode: "EXACT",
      entries: [{ participantId: "p-a", amountMinor: 99 }],
    }));
    assertCode("DUPLICATE_PARTICIPANT", () => allocate(100, {
      mode: "EQUAL",
      entries: [{ participantId: "p-a" }, { participantId: "p-a" }],
    }));
  });

  test("uses exact bigint intermediates at the safe integer boundary", () => {
    assert.deepEqual(
      allocate(MAX_SAFE_MINOR, {
        mode: "WEIGHTED",
        entries: [
          { participantId: "p-a", shares: MAX_SAFE_MINOR - 1 },
          { participantId: "p-b", shares: 1 },
        ],
      }),
      [
        { participantId: "p-a", amountMinor: MAX_SAFE_MINOR - 1 },
        { participantId: "p-b", amountMinor: 1 },
      ],
    );
  });

  test("conserves allocations independently of input order", () => {
    [1, 2, 7, 101, 10_001, MAX_SAFE_MINOR].forEach((totalMinor) => {
      const forward = allocate(totalMinor, {
        mode: "WEIGHTED",
        entries: [
          { participantId: "p-a", shares: 7 },
          { participantId: "p-b", shares: 3 },
          { participantId: "p-c", shares: 11 },
        ],
      });
      const reversed = allocate(totalMinor, {
        mode: "WEIGHTED",
        entries: [
          { participantId: "p-c", shares: 11 },
          { participantId: "p-b", shares: 3 },
          { participantId: "p-a", shares: 7 },
        ],
      });
      assert.equal(forward.reduce((sum, item) => sum + item.amountMinor, 0), totalMinor);
      assert.deepEqual(reversed, forward);
    });
  });

  test("reverses old expense effects for edits and soft deletion", () => {
    const previous: ExpenseFinancials = {
      totalMinor: 100,
      contributions: [{ participantId: "p-a", amountMinor: 100 }],
      allocations: [
        { participantId: "p-a", amountMinor: 50 },
        { participantId: "p-b", amountMinor: 50 },
      ],
    };
    const replacement: ExpenseFinancials = {
      totalMinor: 120,
      contributions: [{ participantId: "p-b", amountMinor: 120 }],
      allocations: [
        { participantId: "p-a", amountMinor: 40 },
        { participantId: "p-b", amountMinor: 80 },
      ],
    };
    const current = applyDeltas({}, expenseDeltas(previous));
    assert.deepEqual(replaceExpense(current, previous, replacement), { "p-a": -40, "p-b": 40 });
    assert.deepEqual(applyDeltas(current, expenseDeltas(previous), "REVERSE"), { "p-a": 0, "p-b": 0 });
  });

  test("rejects overflow, excessive precision, and non-conserved financials", () => {
    assertCode("OVERFLOW", () => allocate(100, {
      mode: "WEIGHTED",
      entries: [
        { participantId: "p-a", shares: MAX_SAFE_MINOR },
        { participantId: "p-b", shares: 1 },
      ],
    }));
    assertCode("INVALID_AMOUNT", () => parseUnsignedMinor("1.001", { code: "EUR", exponent: 2 }));
    assertCode("OVERFLOW", () => parseUnsignedMinor("90071992547410.00", { code: "EUR", exponent: 2 }));
    assertCode("CONTRIBUTIONS_DO_NOT_SUM", () => expenseDeltas({
      totalMinor: 100,
      contributions: [{ participantId: "p-a", amountMinor: 99 }],
      allocations: [{ participantId: "p-a", amountMinor: 100 }],
    }));
    assertCode("BALANCES_NOT_CONSERVED", () => suggestRepayments({ "p-a": 10, "p-b": -9 }));
  });
});

function assertCode(code: CalculationErrorCode, block: () => unknown): void {
  assert.throws(block, (error: unknown) => error instanceof CalculationError && error.code === code);
}
