import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { currencyMetadata } from "./currency.js";
import { appError, requireRevision } from "./errors.js";
import { groupData, requireMember, requireOwner, writeActivity } from "./firestore-helpers.js";
import { idempotentMutation, MutationResponse } from "./idempotency.js";
import {
  booleanValue,
  currencyValue,
  idValue,
  integerValue,
  mutationRequest,
  operationId,
  stringValue,
} from "./validation.js";

export async function createGroup(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(data, ["name", "currency", "participantName", "groupType"], ["name", "currency", "participantName"]);
  const groupType = validatedGroupType(request.groupType);
  const name = stringValue(request, "name", 1, 80);
  const currency = currencyMetadata(currencyValue(request));
  const participantName = stringValue(request, "participantName", 1, 80);
  const firestore = getFirestore();
  const groupRef = firestore.collection("groups").doc();
  const participantRef = groupRef.collection("participants").doc();

  return idempotentMutation("v1CreateGroup", uid, operationId(request), request, async (transaction, committedAt) => {
    transaction.create(groupRef, {
      name,
      groupType,
      currency: currency.code,
      currencyExponent: currency.exponent,
      ownerUid: uid,
      revision: 1,
      financialRecordCount: 0,
      createdAt: committedAt,
      updatedAt: committedAt,
      archivedAt: null,
    });
    transaction.create(groupRef.collection("members").doc(uid), {
      uid,
      role: "OWNER",
      participantId: participantRef.id,
      joinedAt: committedAt,
      updatedAt: committedAt,
      revision: 1,
    });
    transaction.create(participantRef, {
      displayName: participantName,
      linkedUid: uid,
      status: "ACTIVE",
      createdByUid: uid,
      createdAt: committedAt,
      updatedAt: committedAt,
      revision: 1,
    });
    transaction.create(groupRef.collection("balances").doc(participantRef.id), {
      participantId: participantRef.id,
      amountMinor: 0,
      currency: currency.code,
      groupRevision: 1,
      updatedAt: committedAt,
    });
    transaction.set(firestore.doc(`users/${uid}/groups/${groupRef.id}`), {
      groupId: groupRef.id,
      name,
      groupType,
      currency: currency.code,
      role: "OWNER",
      participantId: participantRef.id,
      archivedAt: null,
      groupRevision: 1,
      updatedAt: committedAt,
    });
    writeActivity(
      transaction,
      groupRef.collection("activity"),
      committedAt,
      uid,
      "GROUP_CREATED",
      "GROUP",
      groupRef.id,
      1,
      { name, currency: currency.code },
    );
    return { resourceId: groupRef.id, participantId: participantRef.id, revision: 1 };
  });
}

export async function renameGroup(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(data, ["groupId", "expectedRevision", "name", "groupType"], ["groupId", "expectedRevision", "name"]);
  const groupType = request.groupType === undefined ? undefined : validatedGroupType(request.groupType);
  const groupId = idValue(request, "groupId");
  const expectedRevision = integerValue(request, "expectedRevision", 1);
  const name = stringValue(request, "name", 1, 80);
  const firestore = getFirestore();
  const groupRef = firestore.doc(`groups/${groupId}`);

  return idempotentMutation("v1RenameGroup", uid, operationId(request), request, async (transaction, committedAt) => {
    const [groupSnapshot, memberSnapshot, membersSnapshot] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(groupRef.collection("members").doc(uid)),
      transaction.get(groupRef.collection("members")),
    ]);
    const group = groupData(groupSnapshot);
    requireMember(memberSnapshot);
    requireOwner(group, uid);
    requireRevision(groupId, expectedRevision, group.revision);
    const revision = group.revision + 1;
    transaction.update(groupRef, { name, ...(groupType === undefined ? {} : { groupType }), revision, updatedAt: committedAt });
    membersSnapshot.docs.forEach((member) => {
      transaction.set(firestore.doc(`users/${member.id}/groups/${groupId}`), {
        name,
        ...(groupType === undefined ? {} : { groupType }),
        groupRevision: revision,
        updatedAt: committedAt,
      }, { merge: true });
    });
    writeActivity(transaction, groupRef.collection("activity"), committedAt, uid, "GROUP_RENAMED", "GROUP", groupId, revision, { name });
    return { resourceId: groupId, revision };
  });
}

export async function archiveGroup(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(
    data,
    ["groupId", "expectedRevision", "archived"],
    ["groupId", "expectedRevision", "archived"],
  );
  const groupId = idValue(request, "groupId");
  const expectedRevision = integerValue(request, "expectedRevision", 1);
  if (!booleanValue(request, "archived")) {
    appError("invalid-argument", "INVALID_ARGUMENT", "v1 only supports archiving a group.");
  }
  const firestore = getFirestore();
  const groupRef = firestore.doc(`groups/${groupId}`);

  return idempotentMutation("v1ArchiveGroup", uid, operationId(request), request, async (transaction, committedAt) => {
    const [groupSnapshot, memberSnapshot, membersSnapshot] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(groupRef.collection("members").doc(uid)),
      transaction.get(groupRef.collection("members")),
    ]);
    const group = groupData(groupSnapshot);
    requireMember(memberSnapshot);
    requireOwner(group, uid);
    requireRevision(groupId, expectedRevision, group.revision);
    if (group.archivedAt) appError("failed-precondition", "GROUP_ARCHIVED", "Group is already archived.");
    const revision = group.revision + 1;
    transaction.update(groupRef, { archivedAt: committedAt, revision, updatedAt: committedAt });
    membersSnapshot.docs.forEach((member) => {
      transaction.set(firestore.doc(`users/${member.id}/groups/${groupId}`), {
        archivedAt: committedAt,
        groupRevision: revision,
        updatedAt: committedAt,
      }, { merge: true });
    });
    writeActivity(transaction, groupRef.collection("activity"), committedAt, uid, "GROUP_ARCHIVED", "GROUP", groupId, revision, {});
    return { resourceId: groupId, revision };
  });
}

export async function createParticipant(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(data, ["groupId", "name"], ["groupId", "name"]);
  const groupId = idValue(request, "groupId");
  const name = stringValue(request, "name", 1, 80);
  const firestore = getFirestore();
  const groupRef = firestore.doc(`groups/${groupId}`);
  const participantRef = groupRef.collection("participants").doc();

  return idempotentMutation("v1CreateParticipant", uid, operationId(request), request, async (transaction, committedAt) => {
    const [groupSnapshot, memberSnapshot, participantsSnapshot] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(groupRef.collection("members").doc(uid)),
      transaction.get(groupRef.collection("participants")),
    ]);
    const group = groupData(groupSnapshot);
    requireMember(memberSnapshot);
    requireOwner(group, uid);
    if (participantsSnapshot.size >= 100) {
      appError("resource-exhausted", "INVALID_ARGUMENT", "The group already has 100 participants.");
    }
    const revision = group.revision + 1;
    transaction.create(participantRef, {
      displayName: name,
      linkedUid: null,
      status: "ACTIVE",
      createdByUid: uid,
      createdAt: committedAt,
      updatedAt: committedAt,
      revision: 1,
    });
    transaction.create(groupRef.collection("balances").doc(participantRef.id), {
      participantId: participantRef.id,
      amountMinor: 0,
      currency: group.currency,
      groupRevision: revision,
      updatedAt: committedAt,
    });
    transaction.update(groupRef, { revision, updatedAt: committedAt });
    writeActivity(transaction, groupRef.collection("activity"), committedAt, uid, "PARTICIPANT_CREATED", "PARTICIPANT", participantRef.id, 1, { name });
    return { resourceId: participantRef.id, revision: 1, groupRevision: revision };
  });
}

export function timestampIso(timestamp: Timestamp): string {
  return timestamp.toDate().toISOString();
}

export function validatedGroupType(value: unknown): string {
  if (value === undefined) return "OTHER";
  if (typeof value !== "string" || !["TRIP", "HOME", "FRIENDS", "COUPLE", "OTHER"].includes(value)) {
    appError("invalid-argument", "INVALID_ARGUMENT", "Choose a valid group type.");
  }
  return value as string;
}
