import { createHash, randomBytes } from "node:crypto";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { appError, requireRevision } from "./errors.js";
import { groupData, requireMember, requireOwner, writeActivity } from "./firestore-helpers.js";
import { idempotentMutation, MutationResponse } from "./idempotency.js";
import {
  idValue,
  integerValue,
  mutationRequest,
  operationId,
  stringValue,
} from "./validation.js";

const RATE_WINDOW_MILLIS = 10 * 60 * 1000;
const RATE_ATTEMPTS = 20;
const INVITATION_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

export async function listInvitations(uid: string, data: unknown): Promise<Record<string, unknown>> {
  const request = mutationRequest(data, ["groupId"], ["groupId"]);
  operationId(request);
  const groupId = idValue(request, "groupId");
  const firestore = getFirestore();
  const groupRef = firestore.doc(`groups/${groupId}`);
  const [groupSnapshot, memberSnapshot, invitationSnapshot] = await Promise.all([
    groupRef.get(),
    groupRef.collection("members").doc(uid).get(),
    firestore.collection("invitations").where("groupId", "==", groupId).limit(100).get(),
  ]);
  const group = groupData(groupSnapshot);
  requireMember(memberSnapshot);
  requireOwner(group, uid);
  const invitations = invitationSnapshot.docs.map((snapshot) => {
    const value = snapshot.data();
    return {
      invitationId: snapshot.id,
      revision: value.revision,
      createdAt: value.createdAt instanceof Timestamp ? value.createdAt.toDate().toISOString() : null,
      expiresAt: value.expiresAt instanceof Timestamp ? value.expiresAt.toDate().toISOString() : null,
      revokedAt: value.revokedAt instanceof Timestamp ? value.revokedAt.toDate().toISOString() : null,
      maxUses: value.maxUses,
      useCount: value.useCount,
    };
  }).sort((left, right) => String(right.createdAt).localeCompare(String(left.createdAt)));
  return { apiVersion: "v1", invitations };
}

export async function createInvitation(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(
    data,
    ["groupId", "expiresInSeconds", "maxUses"],
    ["groupId", "expiresInSeconds", "maxUses"],
  );
  const groupId = idValue(request, "groupId");
  const expiresInSeconds = integerValue(request, "expiresInSeconds", 300, 30 * 24 * 60 * 60);
  const maxUses = integerValue(request, "maxUses", 1, 20);
  const token = randomBytes(32).toString("base64url");
  const codeRaw = Array.from(
    randomBytes(10),
    (byte) => INVITATION_CODE_ALPHABET[byte % INVITATION_CODE_ALPHABET.length],
  ).join("");
  const code = `${codeRaw.slice(0, 5)}-${codeRaw.slice(5)}`;
  const firestore = getFirestore();
  const groupRef = firestore.doc(`groups/${groupId}`);
  const invitationRef = firestore.collection("invitations").doc();

  return idempotentMutation(
    "v1CreateInvitation",
    uid,
    operationId(request),
    request,
    async (transaction, committedAt) => {
      const [groupSnapshot, memberSnapshot, membersSnapshot, participantsSnapshot] = await Promise.all([
        transaction.get(groupRef),
        transaction.get(groupRef.collection("members").doc(uid)),
        transaction.get(groupRef.collection("members")),
        transaction.get(groupRef.collection("participants").where("status", "==", "ACTIVE")),
      ]);
      const group = groupData(groupSnapshot);
      requireMember(memberSnapshot);
      requireOwner(group, uid);
      requireAvailableGroupSeat(membersSnapshot.size, participantsSnapshot.docs.map((document) => document.get("linkedUid")));
      const revision = group.revision + 1;
      const expiresAt = Timestamp.fromMillis(committedAt.toMillis() + expiresInSeconds * 1000);
      transaction.create(invitationRef, {
        groupId,
        tokenHash: secretHash(token),
        codeHash: secretHash(normalizeCode(code)),
        createdByUid: uid,
        createdAt: committedAt,
        expiresAt,
        revokedAt: null,
        maxUses,
        useCount: 0,
        revision: 1,
      });
      transaction.update(groupRef, { revision, updatedAt: committedAt });
      writeActivity(transaction, groupRef.collection("activity"), committedAt, uid, "INVITATION_CREATED", "INVITATION", invitationRef.id, 1, { expiresAt: expiresAt.toDate().toISOString(), maxUses });
      return {
        resourceId: invitationRef.id,
        revision: 1,
        groupRevision: revision,
        expiresAt: expiresAt.toDate().toISOString(),
        token,
        code,
      };
    },
    {
      receiptResponse: (response) => {
        const safe = { ...response };
        delete safe.token;
        delete safe.code;
        return { ...safe, secretAlreadyIssued: true };
      },
    },
  );
}

export async function revokeInvitation(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(
    data,
    ["groupId", "invitationId", "expectedRevision"],
    ["groupId", "invitationId", "expectedRevision"],
  );
  const groupId = idValue(request, "groupId");
  const invitationId = idValue(request, "invitationId");
  const expectedRevision = integerValue(request, "expectedRevision", 1);
  const firestore = getFirestore();
  const groupRef = firestore.doc(`groups/${groupId}`);
  const invitationRef = firestore.doc(`invitations/${invitationId}`);

  return idempotentMutation("v1RevokeInvitation", uid, operationId(request), request, async (transaction, committedAt) => {
    const [groupSnapshot, memberSnapshot, invitationSnapshot] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(groupRef.collection("members").doc(uid)),
      transaction.get(invitationRef),
    ]);
    const group = groupData(groupSnapshot);
    requireMember(memberSnapshot);
    requireOwner(group, uid);
    if (!invitationSnapshot.exists || invitationSnapshot.get("groupId") !== groupId) {
      appError("not-found", "NOT_FOUND", "Invitation not found.");
    }
    requireRevision(invitationId, expectedRevision, invitationSnapshot.get("revision"));
    if (invitationSnapshot.get("revokedAt")) {
      appError("failed-precondition", "INVITATION_REVOKED", "Invitation is already revoked.");
    }
    const invitationRevision = expectedRevision + 1;
    const groupRevision = group.revision + 1;
    transaction.update(invitationRef, { revokedAt: committedAt, revision: invitationRevision });
    transaction.update(groupRef, { revision: groupRevision, updatedAt: committedAt });
    writeActivity(transaction, groupRef.collection("activity"), committedAt, uid, "INVITATION_REVOKED", "INVITATION", invitationId, invitationRevision, {});
    return { resourceId: invitationId, revision: invitationRevision, groupRevision };
  });
}

export async function redeemInvitation(
  uid: string,
  data: unknown,
  remoteAddress = "unknown",
): Promise<MutationResponse> {
  const request = mutationRequest(data, ["token", "code"], []);
  const tokenValue = request.token === undefined ? null : stringValue(request, "token", 20, 200);
  const codeValue = request.code === undefined ? null : normalizeCode(stringValue(request, "code", 8, 20));
  if ((tokenValue === null) === (codeValue === null)) {
    appError("invalid-argument", "INVALID_ARGUMENT", "Provide exactly one invitation token or code.");
  }
  await enforceRateLimit(uid, remoteAddress);
  const field = tokenValue === null ? "codeHash" : "tokenHash";
  const hash = secretHash(tokenValue ?? codeValue as string);
  const firestore = getFirestore();

  return idempotentMutation("v1RedeemInvitation", uid, operationId(request), request, async (transaction, committedAt) => {
    const invitationQuery = firestore.collection("invitations").where(field, "==", hash).limit(1);
    const invitationQuerySnapshot = await transaction.get(invitationQuery);
    const invitationSnapshot = invitationQuerySnapshot.docs[0];
    if (!invitationSnapshot) appError("not-found", "NOT_FOUND", "Invitation not found.");
    const invitation = invitationSnapshot.data();
    const groupId = invitation.groupId as string;
    const groupRef = firestore.doc(`groups/${groupId}`);
    const memberRef = groupRef.collection("members").doc(uid);
    const [groupSnapshot, currentMember, membersSnapshot, participantsSnapshot] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(memberRef),
      transaction.get(groupRef.collection("members")),
      transaction.get(groupRef.collection("participants").where("status", "==", "ACTIVE")),
    ]);
    const group = groupData(groupSnapshot);
    if (invitation.revokedAt) appError("failed-precondition", "INVITATION_REVOKED", "Invitation was revoked.");
    if (!(invitation.expiresAt instanceof Timestamp) || invitation.expiresAt.toMillis() <= committedAt.toMillis()) {
      appError("deadline-exceeded", "INVITATION_EXPIRED", "Invitation has expired.");
    }
    if (!Number.isSafeInteger(invitation.useCount) || !Number.isSafeInteger(invitation.maxUses) ||
        invitation.useCount >= invitation.maxUses) {
      appError("resource-exhausted", "INVITATION_EXHAUSTED", "Invitation usage limit was reached.");
    }
    if (currentMember.exists) {
      return { resourceId: groupId, revision: group.revision, participantId: currentMember.get("participantId") ?? null };
    }
    requireAvailableGroupSeat(membersSnapshot.size, participantsSnapshot.docs.map((document) => document.get("linkedUid")));
    const groupRevision = group.revision + 1;
    transaction.create(memberRef, {
      uid,
      role: "MEMBER",
      participantId: null,
      joinedAt: committedAt,
      updatedAt: committedAt,
      revision: 1,
    });
    transaction.set(firestore.doc(`users/${uid}/groups/${groupId}`), {
      groupId,
      name: group.name,
      groupType: group.groupType ?? "OTHER",
      currency: group.currency,
      role: "MEMBER",
      participantId: null,
      archivedAt: group.archivedAt ?? null,
      groupRevision,
      updatedAt: committedAt,
    });
    transaction.update(invitationSnapshot.ref, {
      useCount: invitation.useCount + 1,
      revision: (invitation.revision as number) + 1,
      lastUsedAt: committedAt,
    });
    transaction.update(groupRef, { revision: groupRevision, updatedAt: committedAt });
    writeActivity(transaction, groupRef.collection("activity"), committedAt, uid, "MEMBER_JOINED", "MEMBER", uid, 1, {});
    return { resourceId: groupId, revision: groupRevision, participantId: null };
  });
}

export async function approveParticipantLink(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(
    data,
    ["groupId", "participantId", "memberUid", "expectedRevision"],
    ["groupId", "participantId", "memberUid", "expectedRevision"],
  );
  const groupId = idValue(request, "groupId");
  const participantId = idValue(request, "participantId");
  const memberUid = stringValue(request, "memberUid", 1, 128);
  const expectedRevision = integerValue(request, "expectedRevision", 1);
  const firestore = getFirestore();
  const groupRef = firestore.doc(`groups/${groupId}`);
  const participantRef = groupRef.collection("participants").doc(participantId);
  const memberRef = groupRef.collection("members").doc(memberUid);

  return idempotentMutation("v1ApproveParticipantLink", uid, operationId(request), request, async (transaction, committedAt) => {
    const [groupSnapshot, ownerMember, targetMember, participantSnapshot, existingLinks] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(groupRef.collection("members").doc(uid)),
      transaction.get(memberRef),
      transaction.get(participantRef),
      transaction.get(groupRef.collection("participants").where("linkedUid", "==", memberUid).limit(1)),
    ]);
    const group = groupData(groupSnapshot);
    requireMember(ownerMember);
    requireOwner(group, uid);
    requireRevision(groupId, expectedRevision, group.revision);
    requireMember(targetMember);
    if (!participantSnapshot.exists || participantSnapshot.get("status") !== "ACTIVE") {
      appError("not-found", "NOT_FOUND", "Participant not found.");
    }
    if (participantSnapshot.get("linkedUid") || !existingLinks.empty || targetMember.get("participantId")) {
      appError("failed-precondition", "INVALID_ARGUMENT", "Member or participant is already linked.");
    }
    const groupRevision = group.revision + 1;
    const participantRevision = (participantSnapshot.get("revision") as number) + 1;
    const memberRevision = (targetMember.get("revision") as number) + 1;
    transaction.update(participantRef, { linkedUid: memberUid, revision: participantRevision, updatedAt: committedAt });
    transaction.update(memberRef, { participantId, revision: memberRevision, updatedAt: committedAt });
    transaction.set(firestore.doc(`users/${memberUid}/groups/${groupId}`), {
      participantId,
      groupRevision,
      updatedAt: committedAt,
    }, { merge: true });
    transaction.update(groupRef, { revision: groupRevision, updatedAt: committedAt });
    writeActivity(transaction, groupRef.collection("activity"), committedAt, uid, "PARTICIPANT_LINKED", "PARTICIPANT", participantId, participantRevision, { memberUid });
    return { resourceId: participantId, revision: participantRevision, groupRevision };
  });
}

async function enforceRateLimit(uid: string, remoteAddress: string): Promise<void> {
  const firestore = getFirestore();
  const key = secretHash(`${uid}:${remoteAddress}`);
  const ref = firestore.collection("invitationRateLimits").doc(key);
  const allowed = await firestore.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(ref);
    const now = Timestamp.now();
    const windowStartedAt = snapshot.get("windowStartedAt") as Timestamp | undefined;
    const expired = !windowStartedAt || now.toMillis() - windowStartedAt.toMillis() >= RATE_WINDOW_MILLIS;
    const attempts = expired ? 1 : (snapshot.get("attempts") as number ?? 0) + 1;
    transaction.set(ref, {
      uidHash: secretHash(uid),
      windowStartedAt: expired ? now : windowStartedAt,
      attempts,
      updatedAt: now,
      expiresAt: Timestamp.fromMillis(now.toMillis() + RATE_WINDOW_MILLIS),
    });
    return attempts <= RATE_ATTEMPTS;
  });
  if (!allowed) {
    appError("resource-exhausted", "RATE_LIMITED", "Too many invitation attempts.", { retryAfterSeconds: 600 });
  }
}

function secretHash(value: string): string {
  return createHash("sha256").update(value).digest("hex");
}

function normalizeCode(value: string): string {
  const normalized = value.toUpperCase().replace(/-/g, "");
  if (!/^[A-Z0-9]{8,12}$/.test(normalized)) {
    appError("invalid-argument", "INVALID_ARGUMENT", "Invitation code is invalid.", { fields: ["code"] });
  }
  return normalized;
}

function requireAvailableGroupSeat(memberCount: number, linkedUids: unknown[]): void {
  const activeParticipantCount = linkedUids.length;
  const allParticipantsAssigned = activeParticipantCount > 0 && linkedUids.every(
    (linkedUid) => typeof linkedUid === "string" && linkedUid.length > 0,
  );
  if (activeParticipantCount === 0 || memberCount >= activeParticipantCount || allParticipantsAssigned) {
    appError(
      "resource-exhausted",
      "GROUP_FULL",
      "Every participant place in this group is already assigned or reserved by a member.",
      { memberCount, participantLimit: activeParticipantCount },
    );
  }
}
