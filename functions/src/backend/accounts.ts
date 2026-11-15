import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { appError, requireRevision } from "./errors.js";
import { groupData, writeActivity } from "./firestore-helpers.js";
import { idempotentMutation, MutationResponse } from "./idempotency.js";
import {
  idValue,
  integerValue,
  mutationRequest,
  objectValue,
  operationId,
  optionalArray,
  strictObject,
  stringValue,
} from "./validation.js";

interface OwnershipTransfer {
  groupId: string;
  newOwnerUid: string;
  expectedRevision: number;
}

export async function deleteAccount(uid: string, data: unknown): Promise<MutationResponse> {
  const request = mutationRequest(data, ["confirmation", "ownershipTransfers"], ["confirmation"]);
  if (stringValue(request, "confirmation", 6, 6) !== "DELETE") {
    appError("invalid-argument", "INVALID_ARGUMENT", "Type DELETE to confirm account deletion.");
  }
  const transfers = optionalArray(request, "ownershipTransfers").map(parseTransfer);
  if (new Set(transfers.map((item) => item.groupId)).size !== transfers.length) {
    appError("invalid-argument", "INVALID_ARGUMENT", "Ownership transfers contain duplicate groups.");
  }
  const firestore = getFirestore();

  const response = await idempotentMutation(
    "v1DeleteAccount",
    uid,
    operationId(request),
    request,
    async (transaction, committedAt) => {
      const [ownedGroups, memberships, linkedParticipants] = await Promise.all([
        transaction.get(firestore.collection("groups").where("ownerUid", "==", uid)),
        transaction.get(firestore.collectionGroup("members").where("uid", "==", uid)),
        transaction.get(firestore.collectionGroup("participants").where("linkedUid", "==", uid)),
      ]);
      const ownedIds = new Set(ownedGroups.docs.map((document) => document.id));
      const transferMap = new Map(transfers.map((item) => [item.groupId, item]));
      if (ownedIds.size !== transferMap.size || [...ownedIds].some((groupId) => !transferMap.has(groupId))) {
        appError(
          "failed-precondition",
          "OWNER_TRANSFER_REQUIRED",
          "Transfer every owned group before deleting the account.",
          { ownedGroupIds: [...ownedIds].sort() },
        );
      }
      const targetMemberRefs = transfers.map((item) => firestore.doc(`groups/${item.groupId}/members/${item.newOwnerUid}`));
      const targetMembers = targetMemberRefs.length > 0 ? await transaction.getAll(...targetMemberRefs) : [];
      targetMembers.forEach((member, index) => {
        if (!member.exists || transfers[index]?.newOwnerUid === uid) {
          appError("invalid-argument", "INVALID_ARGUMENT", "Every new owner must already be another group member.");
        }
      });

      ownedGroups.docs.forEach((groupSnapshot) => {
        const transfer = transferMap.get(groupSnapshot.id);
        if (!transfer) throw new Error("Validated transfer missing");
        const group = groupData(groupSnapshot);
        requireRevision(groupSnapshot.id, transfer.expectedRevision, group.revision);
        const revision = group.revision + 1;
        transaction.update(groupSnapshot.ref, {
          ownerUid: transfer.newOwnerUid,
          revision,
          updatedAt: committedAt,
        });
        transaction.update(firestore.doc(`groups/${groupSnapshot.id}/members/${transfer.newOwnerUid}`), {
          role: "OWNER",
          updatedAt: committedAt,
          revision: FieldValue.increment(1),
        });
        transaction.set(firestore.doc(`users/${transfer.newOwnerUid}/groups/${groupSnapshot.id}`), {
          role: "OWNER",
          groupRevision: revision,
          updatedAt: committedAt,
        }, { merge: true });
        writeActivity(
          transaction,
          groupSnapshot.ref.collection("activity"),
          committedAt,
          uid,
          "OWNERSHIP_TRANSFERRED",
          "GROUP",
          groupSnapshot.id,
          revision,
          { newOwnerUid: transfer.newOwnerUid },
        );
      });

      memberships.docs.forEach((membership) => {
        const groupRef = membership.ref.parent.parent;
        if (!groupRef) return;
        transaction.delete(membership.ref);
        transaction.delete(firestore.doc(`users/${uid}/groups/${groupRef.id}`));
        writeActivity(
          transaction,
          groupRef.collection("activity"),
          committedAt,
          uid,
          "MEMBER_REMOVED_FOR_ACCOUNT_DELETION",
          "MEMBER",
          uid,
          1,
          {},
        );
      });
      linkedParticipants.docs.forEach((participant) => {
        transaction.update(participant.ref, {
          displayName: "Deleted member",
          linkedUid: FieldValue.delete(),
          status: "ANONYMIZED",
          updatedAt: committedAt,
          revision: FieldValue.increment(1),
        });
      });
      transaction.set(firestore.doc(`users/${uid}`), {
        displayName: FieldValue.delete(),
        email: FieldValue.delete(),
        photoUrl: FieldValue.delete(),
        deletionState: "DELETING",
        deletedAt: committedAt,
        updatedAt: committedAt,
      }, { merge: true });
      return { resourceId: uid, revision: 1, transferredGroupIds: [...ownedIds].sort() };
    },
  );

  try {
    await getAuth().deleteUser(uid);
  } catch (error) {
    const code = (error as { code?: string }).code;
    if (code !== "auth/user-not-found") throw error;
  }
  await firestore.doc(`users/${uid}`).set({ deletionState: "DELETED" }, { merge: true });
  return response;
}

function parseTransfer(value: unknown, index: number): OwnershipTransfer {
  const object = strictObject(
    objectValue(value),
    ["groupId", "newOwnerUid", "expectedRevision"],
    ["groupId", "newOwnerUid", "expectedRevision"],
    `ownershipTransfers[${index}]`,
  );
  return {
    groupId: idValue(object, "groupId"),
    newOwnerUid: stringValue(object, "newOwnerUid", 1, 128),
    expectedRevision: integerValue(object, "expectedRevision", 1),
  };
}
