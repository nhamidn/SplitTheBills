import {
  CollectionReference,
  DocumentReference,
  DocumentSnapshot,
  QuerySnapshot,
  Timestamp,
  Transaction,
} from "firebase-admin/firestore";
import { applyDeltas, BalanceDelta, DeltaDirection } from "../domain/balance.js";
import { appError } from "./errors.js";

export interface GroupData {
  name: string;
  currency: string;
  currencyExponent: number;
  ownerUid: string;
  revision: number;
  financialRecordCount: number;
  archivedAt?: Timestamp | null;
}

export function groupData(snapshot: DocumentSnapshot): GroupData {
  if (!snapshot.exists) appError("not-found", "NOT_FOUND", "Group not found.");
  const value = snapshot.data() as Partial<GroupData>;
  const archivedAtIsValid = value.archivedAt === null || value.archivedAt === undefined ||
    value.archivedAt instanceof Timestamp;
  if (typeof value.name !== "string" || !/^[A-Z]{3}$/.test(value.currency ?? "") ||
      !Number.isSafeInteger(value.currencyExponent) || (value.currencyExponent as number) < 0 ||
      (value.currencyExponent as number) > 4 || !Number.isSafeInteger(value.revision) ||
      (value.revision as number) < 1 || !Number.isSafeInteger(value.financialRecordCount) ||
      (value.financialRecordCount as number) < 0 || typeof value.ownerUid !== "string" ||
      !archivedAtIsValid) {
    appError("internal", "BALANCE_INVARIANT_FAILED", "Stored group data is invalid.");
  }
  return value as GroupData;
}

export function requireMember(snapshot: DocumentSnapshot): Record<string, unknown> {
  if (!snapshot.exists) appError("permission-denied", "NOT_A_MEMBER", "You are not a member of this group.");
  return snapshot.data() ?? {};
}

export function requireOwner(group: GroupData, uid: string): void {
  if (group.ownerUid !== uid) appError("permission-denied", "OWNER_REQUIRED", "Only the group owner may do this.");
}

export function requireFinanciallyActive(group: GroupData): void {
  if (group.archivedAt) {
    appError("failed-precondition", "GROUP_ARCHIVED", "Archived groups reject financial changes.");
  }
}

export async function requireParticipants(
  transaction: Transaction,
  participants: CollectionReference,
  participantIds: readonly string[],
): Promise<void> {
  const unique = [...new Set(participantIds)];
  const refs = unique.map((id) => participants.doc(id));
  const snapshots = refs.length > 0 ? await transaction.getAll(...refs) : [];
  snapshots.forEach((snapshot) => {
    if (!snapshot.exists || snapshot.get("status") !== "ACTIVE") {
      appError("invalid-argument", "INVALID_ARGUMENT", "A participant is unavailable in this group.");
    }
  });
}

export function writeActivity(
  transaction: Transaction,
  activity: CollectionReference,
  committedAt: Timestamp,
  actorUid: string | null,
  type: string,
  resourceType: string,
  resourceId: string,
  resourceRevision: number,
  summary: Record<string, unknown>,
): void {
  const ref = activity.doc();
  transaction.create(ref, {
    eventId: ref.id,
    type,
    actorUid,
    resourceType,
    resourceId,
    resourceRevision,
    summary,
    occurredAt: committedAt,
  });
}

export function writeBalanceDeltas(
  transaction: Transaction,
  balanceSnapshot: QuerySnapshot,
  balancesCollection: CollectionReference,
  deltas: readonly BalanceDelta[],
  currency: string,
  groupRevision: number,
  committedAt: Timestamp,
  direction: DeltaDirection = "APPLY",
): void {
  const before = balanceRecord(balanceSnapshot);
  let after: Record<string, number>;
  try {
    after = applyDeltas(before, deltas, direction);
  } catch (error) {
    appError("internal", "BALANCE_INVARIANT_FAILED", "Balance conservation failed.", {
      cause: error instanceof Error ? error.message : "unknown",
    });
  }
  writeBalances(transaction, balancesCollection, after, currency, groupRevision, committedAt);
}

export function balanceRecord(balanceSnapshot: QuerySnapshot): Record<string, number> {
  const balances: Record<string, number> = {};
  balanceSnapshot.docs.forEach((snapshot) => {
    const amount = snapshot.get("amountMinor");
    if (!Number.isSafeInteger(amount)) {
      appError("internal", "BALANCE_INVARIANT_FAILED", "Stored balance is invalid.");
    }
    balances[snapshot.id] = amount as number;
  });
  return balances;
}

export function writeBalances(
  transaction: Transaction,
  balancesCollection: CollectionReference,
  balances: Record<string, number>,
  currency: string,
  groupRevision: number,
  committedAt: Timestamp,
): void {
  Object.entries(balances).forEach(([participantId, amountMinor]) => {
    transaction.set(balancesCollection.doc(participantId), {
      participantId,
      amountMinor,
      currency,
      groupRevision,
      updatedAt: committedAt,
    });
  });
}

export function canEditRecord(record: DocumentSnapshot, uid: string, ownerUid: string): void {
  if (record.get("createdByUid") !== uid && ownerUid !== uid) {
    appError("permission-denied", "RECORD_EDIT_FORBIDDEN", "Only the creator or owner may edit this record.");
  }
}

export function existingRecord(snapshot: DocumentSnapshot, type: string): Record<string, unknown> {
  if (!snapshot.exists || snapshot.get("deletedAt")) {
    appError("not-found", "NOT_FOUND", `${type} not found.`);
  }
  return snapshot.data() ?? {};
}

export function documentRef(collection: CollectionReference, id: string): DocumentReference {
  return collection.doc(id);
}
