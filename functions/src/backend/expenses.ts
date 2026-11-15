import { getFirestore } from "firebase-admin/firestore";
import {
  applyDeltas,
  expenseDeltas,
  ExpenseFinancials,
} from "../domain/balance.js";
import { CalculationError } from "../domain/calculation-error.js";
import {
  allocate,
  Allocation,
  Contribution,
  SplitSpec,
} from "../domain/split.js";
import { appError, requireRevision } from "./errors.js";
import {
  canEditRecord,
  balanceRecord,
  existingRecord,
  groupData,
  requireFinanciallyActive,
  requireMember,
  requireParticipants,
  writeActivity,
  writeBalances,
  writeBalanceDeltas,
} from "./firestore-helpers.js";
import { idempotentMutation, MutationResponse } from "./idempotency.js";
import {
  arrayValue,
  currencyValue,
  dateValue,
  idValue,
  integerValue,
  mutationRequest,
  objectValue,
  operationId,
  optionalString,
  strictObject,
  stringValue,
} from "./validation.js";

const categories = new Set([
  "FOOD", "GROCERIES", "TRANSPORT", "LODGING", "ENTERTAINMENT",
  "SHOPPING", "HEALTH", "OTHER",
]);

interface ParsedExpense {
  title: string;
  totalMinor: number;
  currency: string;
  contributions: Contribution[];
  splitInput: SplitSpec;
  allocations: Allocation[];
  date: string;
  category: string;
  note: string | null;
}

export async function createExpense(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(data, ["groupId", "expense"], ["groupId", "expense"]);
  const groupId = idValue(request, "groupId");
  const parsed = parseExpense(request.expense);
  const firestore = getFirestore();
  const groupRef = firestore.doc(`groups/${groupId}`);
  const expenseRef = groupRef.collection("expenses").doc();
  const financials: ExpenseFinancials = parsed;
  const deltas = calculateExpenseDeltas(financials);

  return idempotentMutation("v1CreateExpense", uid, operationId(request), request, async (transaction, committedAt) => {
    const [groupSnapshot, memberSnapshot, balanceSnapshot] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(groupRef.collection("members").doc(uid)),
      transaction.get(groupRef.collection("balances")),
    ]);
    const group = groupData(groupSnapshot);
    requireMember(memberSnapshot);
    requireFinanciallyActive(group);
    if (group.currency !== parsed.currency) {
      appError("invalid-argument", "INVALID_ARGUMENT", "Expense currency must match the group.", { fields: ["currency"] });
    }
    await requireParticipants(
      transaction,
      groupRef.collection("participants"),
      [...parsed.contributions, ...parsed.allocations].map((item) => item.participantId),
    );
    const groupRevision = group.revision + 1;
    transaction.create(expenseRef, {
      title: parsed.title,
      amountMinor: parsed.totalMinor,
      currency: parsed.currency,
      contributions: parsed.contributions,
      splitInput: parsed.splitInput,
      allocations: parsed.allocations,
      date: parsed.date,
      category: parsed.category,
      note: parsed.note,
      createdByUid: uid,
      createdAt: committedAt,
      updatedAt: committedAt,
      deletedAt: null,
      revision: 1,
    });
    writeBalanceDeltas(
      transaction,
      balanceSnapshot,
      groupRef.collection("balances"),
      deltas,
      group.currency,
      groupRevision,
      committedAt,
    );
    transaction.update(groupRef, {
      revision: groupRevision,
      financialRecordCount: group.financialRecordCount + 1,
      updatedAt: committedAt,
    });
    writeActivity(transaction, groupRef.collection("activity"), committedAt, uid, "EXPENSE_CREATED", "EXPENSE", expenseRef.id, 1, { title: parsed.title, amountMinor: parsed.totalMinor, currency: parsed.currency });
    return { resourceId: expenseRef.id, revision: 1, groupRevision, allocations: parsed.allocations };
  });
}

export async function updateExpense(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(
    data,
    ["groupId", "expenseId", "expectedRevision", "expense"],
    ["groupId", "expenseId", "expectedRevision", "expense"],
  );
  const groupId = idValue(request, "groupId");
  const expenseId = idValue(request, "expenseId");
  const expectedRevision = integerValue(request, "expectedRevision", 1);
  const parsed = parseExpense(request.expense);
  const replacement: ExpenseFinancials = parsed;
  const replacementDeltas = calculateExpenseDeltas(replacement);
  const firestore = getFirestore();
  const groupRef = firestore.doc(`groups/${groupId}`);
  const expenseRef = groupRef.collection("expenses").doc(expenseId);

  return idempotentMutation("v1UpdateExpense", uid, operationId(request), request, async (transaction, committedAt) => {
    const [groupSnapshot, memberSnapshot, expenseSnapshot, balanceSnapshot] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(groupRef.collection("members").doc(uid)),
      transaction.get(expenseRef),
      transaction.get(groupRef.collection("balances")),
    ]);
    const group = groupData(groupSnapshot);
    requireMember(memberSnapshot);
    requireFinanciallyActive(group);
    existingRecord(expenseSnapshot, "Expense");
    canEditRecord(expenseSnapshot, uid, group.ownerUid);
    requireRevision(expenseId, expectedRevision, expenseSnapshot.get("revision"));
    if (group.currency !== parsed.currency) {
      appError("invalid-argument", "INVALID_ARGUMENT", "Expense currency must match the group.", { fields: ["currency"] });
    }
    await requireParticipants(
      transaction,
      groupRef.collection("participants"),
      [...parsed.contributions, ...parsed.allocations].map((item) => item.participantId),
    );
    const previousDeltas = calculateExpenseDeltas(storedFinancials(expenseSnapshot.data() ?? {}));
    const afterReverse = balanceRecord(balanceSnapshot);
    const reversed = applyDeltas(afterReverse, previousDeltas, "REVERSE");
    const finalBalances = applyDeltas(reversed, replacementDeltas);
    const revision = expectedRevision + 1;
    const groupRevision = group.revision + 1;
    transaction.update(expenseRef, {
      title: parsed.title,
      amountMinor: parsed.totalMinor,
      currency: parsed.currency,
      contributions: parsed.contributions,
      splitInput: parsed.splitInput,
      allocations: parsed.allocations,
      date: parsed.date,
      category: parsed.category,
      note: parsed.note,
      updatedAt: committedAt,
      revision,
    });
    writeBalances(transaction, groupRef.collection("balances"), finalBalances, group.currency, groupRevision, committedAt);
    transaction.update(groupRef, { revision: groupRevision, updatedAt: committedAt });
    writeActivity(transaction, groupRef.collection("activity"), committedAt, uid, "EXPENSE_UPDATED", "EXPENSE", expenseId, revision, { title: parsed.title, amountMinor: parsed.totalMinor, currency: parsed.currency });
    return { resourceId: expenseId, revision, groupRevision, allocations: parsed.allocations };
  });
}

export async function deleteExpense(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(
    data,
    ["groupId", "expenseId", "expectedRevision"],
    ["groupId", "expenseId", "expectedRevision"],
  );
  const groupId = idValue(request, "groupId");
  const expenseId = idValue(request, "expenseId");
  const expectedRevision = integerValue(request, "expectedRevision", 1);
  const firestore = getFirestore();
  const groupRef = firestore.doc(`groups/${groupId}`);
  const expenseRef = groupRef.collection("expenses").doc(expenseId);

  return idempotentMutation("v1DeleteExpense", uid, operationId(request), request, async (transaction, committedAt) => {
    const [groupSnapshot, memberSnapshot, expenseSnapshot, balanceSnapshot] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(groupRef.collection("members").doc(uid)),
      transaction.get(expenseRef),
      transaction.get(groupRef.collection("balances")),
    ]);
    const group = groupData(groupSnapshot);
    requireMember(memberSnapshot);
    requireFinanciallyActive(group);
    existingRecord(expenseSnapshot, "Expense");
    canEditRecord(expenseSnapshot, uid, group.ownerUid);
    requireRevision(expenseId, expectedRevision, expenseSnapshot.get("revision"));
    const deltas = calculateExpenseDeltas(storedFinancials(expenseSnapshot.data() ?? {}));
    const revision = expectedRevision + 1;
    const groupRevision = group.revision + 1;
    transaction.update(expenseRef, { deletedAt: committedAt, updatedAt: committedAt, revision });
    writeBalanceDeltas(transaction, balanceSnapshot, groupRef.collection("balances"), deltas, group.currency, groupRevision, committedAt, "REVERSE");
    transaction.update(groupRef, { revision: groupRevision, updatedAt: committedAt });
    writeActivity(transaction, groupRef.collection("activity"), committedAt, uid, "EXPENSE_DELETED", "EXPENSE", expenseId, revision, {});
    return { resourceId: expenseId, revision, groupRevision };
  });
}

function parseExpense(value: unknown): ParsedExpense {
  const object = strictObject(
    value,
    ["title", "amountMinor", "currency", "contributions", "split", "date", "category", "note"],
    ["title", "amountMinor", "currency", "contributions", "split", "date", "category"],
    "expense",
  );
  const title = stringValue(object, "title", 1, 120);
  const totalMinor = integerValue(object, "amountMinor", 1);
  const currency = currencyValue(object);
  const contributions = arrayValue(object, "contributions").map((entry, index) => {
    const item = strictObject(entry, ["participantId", "amountMinor"], ["participantId", "amountMinor"], `contributions[${index}]`);
    return { participantId: idValue(item, "participantId"), amountMinor: integerValue(item, "amountMinor", 1) };
  });
  if (contributions.length !== 1) {
    appError("invalid-argument", "INVALID_ARGUMENT", "The MVP requires exactly one payer contribution.");
  }
  const splitInput = parseSplit(object.split);
  let allocations: Allocation[];
  try {
    allocations = allocate(totalMinor, splitInput);
    expenseDeltas({ totalMinor, contributions, allocations });
  } catch (error) {
    calculationInputError(error);
  }
  const category = stringValue(object, "category", 1, 32);
  if (!categories.has(category)) {
    appError("invalid-argument", "INVALID_ARGUMENT", "Unsupported expense category.", { fields: ["category"] });
  }
  return {
    title,
    totalMinor,
    currency,
    contributions,
    splitInput,
    allocations: allocations as Allocation[],
    date: dateValue(object),
    category,
    note: optionalString(object, "note", 1000),
  };
}

function parseSplit(value: unknown): SplitSpec {
  const object = objectValue(value, "split");
  const mode = stringValue(object, "mode", 1, 16);
  const rawEntries = arrayValue(object, "entries");
  switch (mode) {
    case "EQUAL":
      strictObject(object, ["mode", "entries"], ["mode", "entries"], "split");
      return { mode, entries: rawEntries.map((entry, index) => {
        const item = strictObject(entry, ["participantId"], ["participantId"], `split.entries[${index}]`);
        return { participantId: idValue(item, "participantId") };
      }) };
    case "EXACT":
      strictObject(object, ["mode", "entries"], ["mode", "entries"], "split");
      return { mode, entries: rawEntries.map((entry, index) => {
        const item = strictObject(entry, ["participantId", "amountMinor"], ["participantId", "amountMinor"], `split.entries[${index}]`);
        return { participantId: idValue(item, "participantId"), amountMinor: integerValue(item, "amountMinor") };
      }) };
    case "PERCENTAGE":
      strictObject(object, ["mode", "entries"], ["mode", "entries"], "split");
      return { mode, entries: rawEntries.map((entry, index) => {
        const item = strictObject(entry, ["participantId", "basisPoints"], ["participantId", "basisPoints"], `split.entries[${index}]`);
        return { participantId: idValue(item, "participantId"), basisPoints: integerValue(item, "basisPoints", 1, 10_000) };
      }) };
    case "WEIGHTED":
      strictObject(object, ["mode", "entries"], ["mode", "entries"], "split");
      return { mode, entries: rawEntries.map((entry, index) => {
        const item = strictObject(entry, ["participantId", "shares"], ["participantId", "shares"], `split.entries[${index}]`);
        return { participantId: idValue(item, "participantId"), shares: integerValue(item, "shares", 1) };
      }) };
    default:
      appError("invalid-argument", "INVALID_ARGUMENT", "Unsupported split mode.", { fields: ["split.mode"] });
  }
}

function storedFinancials(record: Record<string, unknown>): ExpenseFinancials {
  return {
    totalMinor: record.amountMinor as number,
    contributions: record.contributions as Contribution[],
    allocations: record.allocations as Allocation[],
  };
}

function calculateExpenseDeltas(expense: ExpenseFinancials) {
  try {
    return expenseDeltas(expense);
  } catch (error) {
    calculationInputError(error);
  }
}

function calculationInputError(error: unknown): never {
  if (error instanceof CalculationError) {
    appError("invalid-argument", "INVALID_ARGUMENT", error.message, { calculationCode: error.code });
  }
  throw error;
}
