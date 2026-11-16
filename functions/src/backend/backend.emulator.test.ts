import assert from "node:assert/strict";
import { createHash, randomUUID } from "node:crypto";
import { after, before, test } from "node:test";
import { initializeApp as initializeAdminApp } from "firebase-admin/app";
import { getFirestore as getAdminFirestore, Timestamp } from "firebase-admin/firestore";
import { FirebaseApp, deleteApp, initializeApp } from "firebase/app";
import {
  Auth,
  connectAuthEmulator,
  createUserWithEmailAndPassword,
  getAuth,
} from "firebase/auth";
import {
  Firestore,
  collection,
  connectFirestoreEmulator,
  doc,
  getDoc,
  getDocs,
  getFirestore,
  setDoc,
} from "firebase/firestore";
import {
  Functions,
  connectFunctionsEmulator,
  getFunctions,
  httpsCallable,
} from "firebase/functions";

const projectId = "demo-split-the-bills";

interface Client {
  app: FirebaseApp;
  auth: Auth;
  firestore: Firestore;
  functions: Functions;
  uid: string;
}

let owner: Client;
let member: Client;
let outsider: Client;

before(async () => {
  initializeAdminApp({ projectId });
  const runId = randomUUID();
  owner = await createClient(`owner-${runId}@example.test`);
  member = await createClient(`member-${runId}@example.test`);
  outsider = await createClient(`outsider-${runId}@example.test`);
});

after(async () => {
  await Promise.all([owner.app, member.app, outsider.app].map(deleteApp));
});

test("two-user financial journey is authorized, atomic, idempotent, and conflict-aware", async () => {
  await assertErrorCode(call(owner, "v1CreateGroup", {
    apiVersion: "v1",
    operationId: randomUUID(),
    name: "Invalid currency group",
    currency: "ZZZ",
    participantName: "Alex",
  }), "INVALID_ARGUMENT");

  const createGroupOperation = randomUUID();
  const createGroupRequest = {
    apiVersion: "v1",
    operationId: createGroupOperation,
    name: "Coast weekend",
    groupType: "TRIP",
    currency: "EUR",
    participantName: "Alex",
  };
  const createdGroup = await call(owner, "v1CreateGroup", createGroupRequest);
  const groupId = createdGroup.resourceId as string;
  const ownerParticipantId = createdGroup.participantId as string;
  assert.equal((await group(owner.firestore, groupId)).groupType, "TRIP");
  assert.equal(createdGroup.replayed, false);

  const replayedGroup = await call(owner, "v1CreateGroup", createGroupRequest);
  assert.equal(replayedGroup.resourceId, groupId);
  assert.equal(replayedGroup.replayed, true);
  await assertErrorCode(
    call(owner, "v1CreateGroup", { ...createGroupRequest, name: "Changed retry" }),
    "OPERATION_ID_REUSED",
  );

  const participant = await call(owner, "v1CreateParticipant", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    name: "Blair",
  });
  const memberParticipantId = participant.resourceId as string;

  const invitation = await call(owner, "v1CreateInvitation", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    expiresInSeconds: 600,
    maxUses: 2,
  });
  const revocableInvitation = await call(owner, "v1CreateInvitation", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    expiresInSeconds: 600,
    maxUses: 1,
  });
  assert.equal(typeof invitation.token, "string");
  const redeemed = await call(member, "v1RedeemInvitation", {
    apiVersion: "v1",
    operationId: randomUUID(),
    token: invitation.token,
  });
  assert.equal(redeemed.resourceId, groupId);
  assert.equal((await getDoc(doc(member.firestore, `users/${member.uid}/groups/${groupId}`))).data()?.groupType, "TRIP");
  await assertErrorCode(call(outsider, "v1RedeemInvitation", {
    apiVersion: "v1",
    operationId: randomUUID(),
    token: invitation.token,
  }), "GROUP_FULL");
  await assertErrorCode(call(owner, "v1CreateInvitation", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    expiresInSeconds: 600,
    maxUses: 1,
  }), "GROUP_FULL");
  const invitationList = await call(owner, "v1ListInvitations", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
  });
  const listedInvitation = (invitationList.invitations as Array<Record<string, unknown>>)
    .find((entry) => entry.invitationId === invitation.resourceId);
  assert.equal(listedInvitation?.revision, 2);
  assert.equal(listedInvitation?.useCount, 1);
  assert.equal("token" in (listedInvitation ?? {}), false);
  assert.equal("tokenHash" in (listedInvitation ?? {}), false);
  await assertErrorCode(call(member, "v1ListInvitations", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
  }), "OWNER_REQUIRED");

  const groupBeforeLink = await group(owner.firestore, groupId);
  await call(owner, "v1ApproveParticipantLink", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    participantId: memberParticipantId,
    memberUid: member.uid,
    expectedRevision: groupBeforeLink.revision,
  });

  const outsiderGroup = await call(outsider, "v1CreateGroup", {
    apiVersion: "v1",
    operationId: randomUUID(),
    name: "Other group",
    currency: "EUR",
    participantName: "Morgan",
  });
  const foreignParticipantId = outsiderGroup.participantId as string;
  await assertErrorCode(call(member, "v1CreateExpense", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    expense: expensePayload(ownerParticipantId, foreignParticipantId, 100),
  }), "INVALID_ARGUMENT");

  await assertErrorCode(call(outsider, "v1CreateExpense", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    expense: expensePayload(ownerParticipantId, memberParticipantId, 100),
  }), "NOT_A_MEMBER");

  const expenseOperationId = randomUUID();
  const expenseRequest = {
    apiVersion: "v1",
    operationId: expenseOperationId,
    groupId,
    expense: expensePayload(ownerParticipantId, memberParticipantId, 100),
  };
  const createdExpense = await call(member, "v1CreateExpense", expenseRequest);
  const expenseId = createdExpense.resourceId as string;
  assert.deepEqual(createdExpense.allocations, [
    { participantId: [ownerParticipantId, memberParticipantId].sort()[0], amountMinor: 50 },
    { participantId: [ownerParticipantId, memberParticipantId].sort()[1], amountMinor: 50 },
  ]);
  assert.deepEqual(await balances(owner.firestore, groupId), {
    [ownerParticipantId]: 50,
    [memberParticipantId]: -50,
  });
  assert.equal((await call(member, "v1CreateExpense", expenseRequest)).replayed, true);
  assert.deepEqual(await balances(owner.firestore, groupId), {
    [ownerParticipantId]: 50,
    [memberParticipantId]: -50,
  });

  const competingEdits = await Promise.allSettled([
    call(member, "v1UpdateExpense", {
      apiVersion: "v1",
      operationId: randomUUID(),
      groupId,
      expenseId,
      expectedRevision: 1,
      expense: expensePayload(ownerParticipantId, memberParticipantId, 120),
    }),
    call(owner, "v1UpdateExpense", {
      apiVersion: "v1",
      operationId: randomUUID(),
      groupId,
      expenseId,
      expectedRevision: 1,
      expense: expensePayload(ownerParticipantId, memberParticipantId, 140),
    }),
  ]);
  assert.equal(competingEdits.filter((result) => result.status === "fulfilled").length, 1);
  assert.equal(competingEdits.filter((result) => result.status === "rejected").length, 1);
  const rejectedEdit = competingEdits.find((result) => result.status === "rejected");
  assert.equal(
    (rejectedEdit as PromiseRejectedResult).reason.details?.errorCode,
    "STALE_REVISION",
  );
  const winningAmount = competingEdits[0].status === "fulfilled" ? 120 : 140;
  const winningShare = winningAmount / 2;
  assert.deepEqual(await balances(owner.firestore, groupId), {
    [ownerParticipantId]: winningShare,
    [memberParticipantId]: -winningShare,
  });

  const settlement = await call(member, "v1CreateSettlement", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    settlement: settlementPayload(memberParticipantId, ownerParticipantId, 20),
  });
  const settlementId = settlement.resourceId as string;
  assert.deepEqual(await balances(owner.firestore, groupId), {
    [ownerParticipantId]: winningShare - 20,
    [memberParticipantId]: -(winningShare - 20),
  });
  const editedSettlement = await call(member, "v1UpdateSettlement", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    settlementId,
    expectedRevision: 1,
    settlement: settlementPayload(memberParticipantId, ownerParticipantId, 30),
  });
  assert.equal(editedSettlement.revision, 2);
  assert.deepEqual(await balances(owner.firestore, groupId), {
    [ownerParticipantId]: winningShare - 30,
    [memberParticipantId]: -(winningShare - 30),
  });
  await call(owner, "v1DeleteSettlement", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    settlementId,
    expectedRevision: 2,
  });
  assert.deepEqual(await balances(owner.firestore, groupId), {
    [ownerParticipantId]: winningShare,
    [memberParticipantId]: -winningShare,
  });
  await call(owner, "v1DeleteExpense", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    expenseId,
    expectedRevision: 2,
  });
  assert.deepEqual(await balances(owner.firestore, groupId), {
    [ownerParticipantId]: 0,
    [memberParticipantId]: 0,
  });

  const expiredToken = "expired-token-that-is-long-enough";
  await getAdminFirestore().collection("invitations").doc("expired-invite").set({
    groupId,
    tokenHash: createHash("sha256").update(expiredToken).digest("hex"),
    codeHash: "unused",
    createdByUid: owner.uid,
    createdAt: Timestamp.fromMillis(Date.now() - 10_000),
    expiresAt: Timestamp.fromMillis(Date.now() - 1_000),
    revokedAt: null,
    maxUses: 1,
    useCount: 0,
    revision: 1,
  });
  await assertErrorCode(call(outsider, "v1RedeemInvitation", {
    apiVersion: "v1",
    operationId: randomUUID(),
    token: expiredToken,
  }), "INVITATION_EXPIRED");

  await call(owner, "v1RevokeInvitation", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    invitationId: revocableInvitation.resourceId,
    expectedRevision: 1,
  });
  await assertErrorCode(call(outsider, "v1RedeemInvitation", {
    apiVersion: "v1",
    operationId: randomUUID(),
    code: revocableInvitation.code,
  }), "INVITATION_REVOKED");

  const beforeRename = await group(owner.firestore, groupId);
  await assertErrorCode(call(member, "v1RenameGroup", {
    apiVersion: "v1", operationId: randomUUID(), groupId, expectedRevision: beforeRename.revision,
    name: "Not allowed", groupType: "HOME",
  }), "OWNER_REQUIRED");
  await call(owner, "v1RenameGroup", {
    apiVersion: "v1", operationId: randomUUID(), groupId, expectedRevision: beforeRename.revision,
    name: "Weekend at home", groupType: "HOME",
  });
  assert.equal((await group(owner.firestore, groupId)).groupType, "HOME");
  const updatedProjection = (await getDoc(doc(member.firestore, `users/${member.uid}/groups/${groupId}`))).data();
  assert.equal(updatedProjection?.groupType, "HOME");
  assert.equal(updatedProjection?.name, "Weekend at home");
  const currentGroup = await group(owner.firestore, groupId);
  await call(owner, "v1ArchiveGroup", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    expectedRevision: currentGroup.revision,
    archived: true,
  });
  await assertErrorCode(call(member, "v1CreateExpense", {
    apiVersion: "v1",
    operationId: randomUUID(),
    groupId,
    expense: expensePayload(ownerParticipantId, memberParticipantId, 100),
  }), "GROUP_ARCHIVED");

  const activity = await getDocs(collection(owner.firestore, `groups/${groupId}/activity`));
  assert.ok(activity.size >= 10);
  await assert.rejects(getDoc(doc(owner.firestore, `invitations/${invitation.resourceId as string}`)));
  await assert.rejects(setDoc(doc(owner.firestore, `groups/${groupId}/expenses/direct-write`), { title: "Denied" }));

  await assertErrorCode(call(owner, "v1DeleteAccount", {
    apiVersion: "v1",
    operationId: randomUUID(),
    confirmation: "DELETE",
    ownershipTransfers: [],
  }), "OWNER_TRANSFER_REQUIRED");
  await call(member, "v1DeleteAccount", {
    apiVersion: "v1",
    operationId: randomUUID(),
    confirmation: "DELETE",
    ownershipTransfers: [],
  });
  const adminFirestore = getAdminFirestore();
  assert.equal((await adminFirestore.doc(`users/${member.uid}`).get()).get("deletionState"), "DELETED");
  assert.equal((await adminFirestore.doc(`groups/${groupId}/members/${member.uid}`).get()).exists, false);
  const anonymizedParticipant = await adminFirestore.doc(
    `groups/${groupId}/participants/${memberParticipantId}`,
  ).get();
  assert.equal(anonymizedParticipant.get("status"), "ANONYMIZED");
  assert.equal(anonymizedParticipant.get("displayName"), "Deleted member");
  assert.equal(anonymizedParticipant.get("linkedUid"), undefined);

  const departingOwner = await createClient(`departing-${randomUUID()}@example.test`);
  try {
    const transferGroup = await call(departingOwner, "v1CreateGroup", {
      apiVersion: "v1",
      operationId: randomUUID(),
      name: "Transferred home",
      currency: "USD",
      participantName: "Departing owner",
    });
    await call(departingOwner, "v1CreateParticipant", {
      apiVersion: "v1",
      operationId: randomUUID(),
      groupId: transferGroup.resourceId,
      name: "Future owner",
    });
    const transferInvitation = await call(departingOwner, "v1CreateInvitation", {
      apiVersion: "v1",
      operationId: randomUUID(),
      groupId: transferGroup.resourceId,
      expiresInSeconds: 600,
      maxUses: 1,
    });
    await call(outsider, "v1RedeemInvitation", {
      apiVersion: "v1",
      operationId: randomUUID(),
      code: transferInvitation.code,
    });
    const beforeTransfer = await group(departingOwner.firestore, transferGroup.resourceId as string);
    const deletion = await call(departingOwner, "v1DeleteAccount", {
      apiVersion: "v1",
      operationId: randomUUID(),
      confirmation: "DELETE",
      ownershipTransfers: [{
        groupId: transferGroup.resourceId,
        newOwnerUid: outsider.uid,
        expectedRevision: beforeTransfer.revision,
      }],
    });
    assert.deepEqual(deletion.transferredGroupIds, [transferGroup.resourceId]);
    const transferred = await adminFirestore.doc(`groups/${transferGroup.resourceId}`).get();
    assert.equal(transferred.get("ownerUid"), outsider.uid);
    assert.equal(
      (await adminFirestore.doc(`groups/${transferGroup.resourceId}/members/${outsider.uid}`).get()).get("role"),
      "OWNER",
    );
    assert.equal(
      (await adminFirestore.doc(`groups/${transferGroup.resourceId}/members/${departingOwner.uid}`).get()).exists,
      false,
    );
    assert.equal((await adminFirestore.doc(`users/${departingOwner.uid}`).get()).get("deletionState"), "DELETED");
  } finally {
    await deleteApp(departingOwner.app);
  }
});

async function createClient(email: string): Promise<Client> {
  const app = initializeApp({ apiKey: "demo-api-key", projectId, appId: randomUUID() }, randomUUID());
  const auth = getAuth(app);
  connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });
  const firestore = getFirestore(app);
  connectFirestoreEmulator(firestore, "127.0.0.1", 8080);
  const functions = getFunctions(app, "europe-west1");
  connectFunctionsEmulator(functions, "127.0.0.1", 5001);
  const credential = await createUserWithEmailAndPassword(auth, email, "correct horse battery staple");
  return { app, auth, firestore, functions, uid: credential.user.uid };
}

async function call(client: Client, name: string, data: unknown): Promise<Record<string, unknown>> {
  const result = await httpsCallable<unknown, Record<string, unknown>>(client.functions, name)(data);
  return result.data;
}

function expensePayload(payer: string, other: string, amountMinor: number) {
  return {
    title: "Shared lunch",
    amountMinor,
    currency: "EUR",
    contributions: [{ participantId: payer, amountMinor }],
    split: { mode: "EQUAL", entries: [{ participantId: other }, { participantId: payer }] },
    date: "2026-09-07",
    category: "FOOD",
    note: "Emulator scenario",
  };
}

function settlementPayload(fromParticipantId: string, toParticipantId: string, amountMinor: number) {
  return {
    fromParticipantId,
    toParticipantId,
    amountMinor,
    currency: "EUR",
    date: "2026-09-07",
    note: "Paid outside the app",
  };
}

async function group(firestore: Firestore, groupId: string): Promise<Record<string, unknown>> {
  const snapshot = await getDoc(doc(firestore, `groups/${groupId}`));
  return snapshot.data() ?? {};
}

async function balances(firestore: Firestore, groupId: string): Promise<Record<string, number>> {
  const snapshot = await getDocs(collection(firestore, `groups/${groupId}/balances`));
  return Object.fromEntries(snapshot.docs.map((document) => [document.id, document.get("amountMinor") as number]));
}

async function assertErrorCode(promise: Promise<unknown>, errorCode: string): Promise<void> {
  await assert.rejects(promise, (error: unknown) => {
    const details = (error as { details?: { errorCode?: string } }).details;
    assert.equal(details?.errorCode, errorCode);
    return true;
  });
}
