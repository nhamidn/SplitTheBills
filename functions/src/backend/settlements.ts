import { getFirestore } from "firebase-admin/firestore";
import { applyDeltas, settlementDeltas, SettlementFinancials } from "../domain/balance.js";
import { CalculationError } from "../domain/calculation-error.js";
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
  currencyValue,
  dateValue,
  idValue,
  integerValue,
  mutationRequest,
  operationId,
  optionalString,
  strictObject,
} from "./validation.js";

interface ParsedSettlement extends SettlementFinancials {
  currency: string;
  date: string;
  note: string | null;
}

export async function createSettlement(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(data, ["groupId", "settlement"], ["groupId", "settlement"]);
  const groupId = idValue(request, "groupId");
  const parsed = parseSettlement(request.settlement);
  const deltas = calculateDeltas(parsed);
  const firestore = getFirestore();
  const groupRef = firestore.doc(`groups/${groupId}`);
  const settlementRef = groupRef.collection("settlements").doc();

  return idempotentMutation("v1CreateSettlement", uid, operationId(request), request, async (transaction, committedAt) => {
    const [groupSnapshot, memberSnapshot, balanceSnapshot] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(groupRef.collection("members").doc(uid)),
      transaction.get(groupRef.collection("balances")),
    ]);
    const group = groupData(groupSnapshot);
    requireMember(memberSnapshot);
    requireFinanciallyActive(group);
    if (group.currency !== parsed.currency) appError("invalid-argument", "INVALID_ARGUMENT", "Settlement currency must match the group.");
    await requireParticipants(transaction, groupRef.collection("participants"), [parsed.fromParticipantId, parsed.toParticipantId]);
    const groupRevision = group.revision + 1;
    transaction.create(settlementRef, {
      ...parsed,
      createdByUid: uid,
      createdAt: committedAt,
      updatedAt: committedAt,
      deletedAt: null,
      revision: 1,
    });
    writeBalanceDeltas(transaction, balanceSnapshot, groupRef.collection("balances"), deltas, group.currency, groupRevision, committedAt);
    transaction.update(groupRef, {
      revision: groupRevision,
      financialRecordCount: group.financialRecordCount + 1,
      updatedAt: committedAt,
    });
    writeActivity(transaction, groupRef.collection("activity"), committedAt, uid, "SETTLEMENT_CREATED", "SETTLEMENT", settlementRef.id, 1, { amountMinor: parsed.amountMinor, currency: parsed.currency });
    return { resourceId: settlementRef.id, revision: 1, groupRevision };
  });
}

export async function updateSettlement(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(
    data,
    ["groupId", "settlementId", "expectedRevision", "settlement"],
    ["groupId", "settlementId", "expectedRevision", "settlement"],
  );
  const groupId = idValue(request, "groupId");
  const settlementId = idValue(request, "settlementId");
  const expectedRevision = integerValue(request, "expectedRevision", 1);
  const parsed = parseSettlement(request.settlement);
  const firestore = getFirestore();
  const groupRef = firestore.doc(`groups/${groupId}`);
  const settlementRef = groupRef.collection("settlements").doc(settlementId);

  return idempotentMutation("v1UpdateSettlement", uid, operationId(request), request, async (transaction, committedAt) => {
    const [groupSnapshot, memberSnapshot, settlementSnapshot, balanceSnapshot] = await Promise.all([
      transaction.get(groupRef), transaction.get(groupRef.collection("members").doc(uid)),
      transaction.get(settlementRef), transaction.get(groupRef.collection("balances")),
    ]);
    const group = groupData(groupSnapshot);
    requireMember(memberSnapshot);
    requireFinanciallyActive(group);
    existingRecord(settlementSnapshot, "Settlement");
    canEditRecord(settlementSnapshot, uid, group.ownerUid);
    requireRevision(settlementId, expectedRevision, settlementSnapshot.get("revision"));
    if (group.currency !== parsed.currency) appError("invalid-argument", "INVALID_ARGUMENT", "Settlement currency must match the group.");
    await requireParticipants(transaction, groupRef.collection("participants"), [parsed.fromParticipantId, parsed.toParticipantId]);
    const oldDeltas = calculateDeltas(storedSettlement(settlementSnapshot.data() ?? {}));
    const revision = expectedRevision + 1;
    const groupRevision = group.revision + 1;
    const reversed = applyDeltas(balanceRecord(balanceSnapshot), oldDeltas, "REVERSE");
    const finalBalances = applyDeltas(reversed, calculateDeltas(parsed));
    writeBalances(transaction, groupRef.collection("balances"), finalBalances, group.currency, groupRevision, committedAt);
    transaction.update(settlementRef, { ...parsed, updatedAt: committedAt, revision });
    transaction.update(groupRef, { revision: groupRevision, updatedAt: committedAt });
    writeActivity(transaction, groupRef.collection("activity"), committedAt, uid, "SETTLEMENT_UPDATED", "SETTLEMENT", settlementId, revision, { amountMinor: parsed.amountMinor, currency: parsed.currency });
    return { resourceId: settlementId, revision, groupRevision };
  });
}

export async function deleteSettlement(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(
    data,
    ["groupId", "settlementId", "expectedRevision"],
    ["groupId", "settlementId", "expectedRevision"],
  );
  const groupId = idValue(request, "groupId");
  const settlementId = idValue(request, "settlementId");
  const expectedRevision = integerValue(request, "expectedRevision", 1);
  const firestore = getFirestore();
  const groupRef = firestore.doc(`groups/${groupId}`);
  const settlementRef = groupRef.collection("settlements").doc(settlementId);

  return idempotentMutation("v1DeleteSettlement", uid, operationId(request), request, async (transaction, committedAt) => {
    const [groupSnapshot, memberSnapshot, settlementSnapshot, balanceSnapshot] = await Promise.all([
      transaction.get(groupRef), transaction.get(groupRef.collection("members").doc(uid)),
      transaction.get(settlementRef), transaction.get(groupRef.collection("balances")),
    ]);
    const group = groupData(groupSnapshot);
    requireMember(memberSnapshot);
    requireFinanciallyActive(group);
    existingRecord(settlementSnapshot, "Settlement");
    canEditRecord(settlementSnapshot, uid, group.ownerUid);
    requireRevision(settlementId, expectedRevision, settlementSnapshot.get("revision"));
    const deltas = calculateDeltas(storedSettlement(settlementSnapshot.data() ?? {}));
    const revision = expectedRevision + 1;
    const groupRevision = group.revision + 1;
    transaction.update(settlementRef, { deletedAt: committedAt, updatedAt: committedAt, revision });
    writeBalanceDeltas(transaction, balanceSnapshot, groupRef.collection("balances"), deltas, group.currency, groupRevision, committedAt, "REVERSE");
    transaction.update(groupRef, { revision: groupRevision, updatedAt: committedAt });
    writeActivity(transaction, groupRef.collection("activity"), committedAt, uid, "SETTLEMENT_DELETED", "SETTLEMENT", settlementId, revision, {});
    return { resourceId: settlementId, revision, groupRevision };
  });
}

function parseSettlement(value: unknown): ParsedSettlement {
  const object = strictObject(
    value,
    ["fromParticipantId", "toParticipantId", "amountMinor", "currency", "date", "note"],
    ["fromParticipantId", "toParticipantId", "amountMinor", "currency", "date"],
    "settlement",
  );
  const result = {
    fromParticipantId: idValue(object, "fromParticipantId"),
    toParticipantId: idValue(object, "toParticipantId"),
    amountMinor: integerValue(object, "amountMinor", 1),
    currency: currencyValue(object),
    date: dateValue(object),
    note: optionalString(object, "note", 1000),
  };
  calculateDeltas(result);
  return result;
}

function calculateDeltas(settlement: SettlementFinancials) {
  try {
    return settlementDeltas(settlement);
  } catch (error) {
    if (error instanceof CalculationError) {
      appError("invalid-argument", "INVALID_ARGUMENT", error.message, { calculationCode: error.code });
    }
    throw error;
  }
}

function storedSettlement(record: Record<string, unknown>): SettlementFinancials {
  return {
    fromParticipantId: record.fromParticipantId as string,
    toParticipantId: record.toParticipantId as string,
    amountMinor: record.amountMinor as number,
  };
}
