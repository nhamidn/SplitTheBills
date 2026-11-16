import { createHash, randomUUID } from "node:crypto";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { setTimeout as pause } from "node:timers/promises";
import { demoGroups, demoPassword, demoUsers, demoVersion, expensePayload, participantId, settlementPayload, demoDate } from "./demo-data.mjs";
import { assertLocalUrl, resolveEmulatorTarget, waitForEmulators } from "./emulator-target.mjs";

export function demoOperationId(key) {
  const hash = createHash("sha256").update(`${demoVersion}/${key}`).digest("hex");
  return `${hash.slice(0, 8)}-${hash.slice(8, 12)}-5${hash.slice(13, 16)}-8${hash.slice(17, 20)}-${hash.slice(20, 32)}`;
}

async function callable(target, tokens, actor, name, key, payload, signal) {
  const body = JSON.stringify({ data: { apiVersion: "v1", operationId: demoOperationId(key), ...payload } });
  for (let attempt = 0; attempt < 3; attempt++) {
    signal?.throwIfAborted();
    try {
      const response = await fetch(`${target.functions.url}/${target.projectId}/europe-west1/${name}`, {
        method: "POST", headers: { "Content-Type": "application/json", Authorization: `Bearer ${tokens.get(actor)}` }, body,
        signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(30_000)]) : AbortSignal.timeout(30_000),
      });
      const result = await response.json();
      if (response.ok && result.result) return result.result;
      const failure = new Error(`${name}: ${result.error?.message ?? `HTTP ${response.status}`}`);
      if (response.status < 500) { failure.permanent = true; throw failure; }
      throw failure;
    } catch (error) {
      if (error.permanent || attempt === 2) throw error;
      await pause(500 * (attempt + 1), undefined, { signal });
    }
  }
}

export async function seedDemo({ target = resolveEmulatorTarget(), log = console.log, signal } = {}) {
  // Validate again before initializing SDKs, including when called programmatically.
  if (!target.projectId?.startsWith("demo-")) throw new Error("Demo seeding requires a demo-* project.");
  [target.auth, target.firestore, target.functions].forEach((endpoint) => assertLocalUrl(endpoint.url));
  await waitForEmulators(target, { signal });
  process.env.FIREBASE_AUTH_EMULATOR_HOST = target.auth.address;
  process.env.FIRESTORE_EMULATOR_HOST = target.firestore.address;
  const { initializeApp, deleteApp } = await import("firebase-admin/app");
  const { getAuth } = await import("firebase-admin/auth");
  const { getFirestore, Timestamp } = await import("firebase-admin/firestore");
  const app = initializeApp({ projectId: target.projectId }, `demo-seed-${randomUUID()}`);
  const auth = getAuth(app);
  const firestore = getFirestore(app);
  const users = new Map(demoUsers.map((user) => [user.key, user]));
  const tokens = new Map();
  let createdGroups = 0;
  try {
    log("[demo] Preparing demo accounts and shared plans…");
    for (const user of demoUsers) {
      signal?.throwIfAborted();
      let existing;
      try { existing = await auth.getUser(user.uid); }
      catch (error) { if (error.code !== "auth/user-not-found") throw error; }
      if (existing && existing.email !== user.email) throw new Error(`The demo UID ${user.uid} belongs to another email. No changes were made to that account.`);
      if (!existing) {
        try { await auth.createUser({ uid: user.uid, email: user.email, displayName: user.displayName, password: demoPassword, emailVerified: true }); }
        catch (error) {
          if (error.code !== "auth/uid-already-exists" || (await auth.getUser(user.uid)).email !== user.email) throw error;
        }
      }
      // These dedicated test identities always keep the documented login working.
      // Preserve display-name edits and all financial records on subsequent starts.
      await auth.updateUser(user.uid, { password: demoPassword, disabled: false, emailVerified: true });
      const response = await fetch(`${target.auth.url}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo-api-key`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: user.email, password: demoPassword, returnSecureToken: true }),
        signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(10_000)]) : AbortSignal.timeout(10_000),
      });
      const signedIn = await response.json();
      if (!response.ok || signedIn.localId !== user.uid || !signedIn.idToken) throw new Error(`Could not sign in as ${user.email}: ${signedIn.error?.message ?? response.status}`);
      tokens.set(user.key, signedIn.idToken);
    }
    const seedRef = firestore.doc(`emulatorSeeds/${demoVersion}`);
    const referenceDate = await firestore.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(seedRef);
      if (snapshot.exists) return snapshot.get("referenceDate");
      const date = new Date().toISOString().slice(0, 10);
      transaction.create(seedRef, { version: demoVersion, referenceDate: date, createdAt: Timestamp.now() });
      return date;
    });
    if (!/^\d{4}-\d{2}-\d{2}$/.test(referenceDate)) throw new Error("The saved demo reference date is invalid.");
    const createdAt = Timestamp.fromDate(new Date(`${demoDate(referenceDate, 14)}T09:00:00Z`));
    for (const group of demoGroups) {
      signal?.throwIfAborted();
      const root = firestore.doc(`groups/${group.id}`);
      const progress = seedRef.collection("groups").doc(group.key);
      const status = await progress.get();
      const existing = await root.get();
      if (existing.exists && existing.get("demoSeedVersion") !== demoVersion) throw new Error(`Group ${group.id} already exists and is not a demo fixture.`);
      if (status.get("complete")) {
        if (!existing.exists) throw new Error(`Demo group ${group.id} was removed. Use a fresh emulator instance to restore the original fixtures.`);
        log(`[demo] Keeping ${group.name} (already seeded).`);
        continue;
      }
      // Bootstrap the known fictional identities atomically. All financial changes below
      // use the authenticated callable API, so its calculations and audit writes remain authoritative.
      await firestore.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(root);
        if (snapshot.exists) {
          if (snapshot.get("demoSeedVersion") !== demoVersion) throw new Error(`Group ${group.id} is not a demo fixture.`);
          return;
        }
        transaction.create(root, { name: group.name, groupType: group.type, currency: group.currency, currencyExponent: 2,
          ownerUid: users.get(group.owner).uid, revision: 1, financialRecordCount: 0,
          createdAt, updatedAt: createdAt, archivedAt: null, demoSeedVersion: demoVersion });
        for (const person of [...group.members, ...group.guests]) {
          const user = users.get(person);
          const linked = group.members.includes(person);
          const id = participantId(group, person);
          transaction.create(root.collection("participants").doc(id), { displayName: user.displayName.split(" ")[0],
            linkedUid: linked ? user.uid : null, status: "ACTIVE", createdByUid: users.get(group.owner).uid,
            createdAt, updatedAt: createdAt, revision: 1 });
          transaction.create(root.collection("balances").doc(id), { participantId: id, amountMinor: 0,
            currency: group.currency, groupRevision: 1, updatedAt: createdAt });
          if (linked) {
            const role = person === group.owner ? "OWNER" : "MEMBER";
            transaction.create(root.collection("members").doc(user.uid), { uid: user.uid, role, participantId: id,
              joinedAt: createdAt, updatedAt: createdAt, revision: 1 });
            transaction.create(firestore.doc(`users/${user.uid}/groups/${group.id}`), { groupId: group.id,
              name: group.name, groupType: group.type, currency: group.currency, role, participantId: id,
              archivedAt: null, groupRevision: 1, updatedAt: createdAt });
          }
        }
        transaction.create(root.collection("activity").doc("demo-group-created"), { eventId: "demo-group-created",
          type: "GROUP_CREATED", actorUid: users.get(group.owner).uid, resourceType: "GROUP", resourceId: group.id,
          resourceRevision: 1, summary: { name: group.name, currency: group.currency }, occurredAt: createdAt });
      });
      for (const expense of group.expenses) {
        await callable(target, tokens, expense.payer, "v1CreateExpense", `${group.key}/expense/${expense.key}`,
          { groupId: group.id, expense: expensePayload(group, expense, referenceDate) }, signal);
      }
      for (const settlement of group.settlements) {
        await callable(target, tokens, settlement.from, "v1CreateSettlement", `${group.key}/settlement/${settlement.key}`,
          { groupId: group.id, settlement: settlementPayload(group, settlement, referenceDate) }, signal);
      }
      if (group.archived && !(await root.get()).get("archivedAt")) {
        await callable(target, tokens, group.owner, "v1ArchiveGroup", `${group.key}/archive`,
          { groupId: group.id, expectedRevision: (await root.get()).get("revision"), archived: true }, signal);
      }
      await progress.set({ complete: true, groupId: group.id, completedAt: Timestamp.now() });
      createdGroups++;
      log(`[demo] Seeded ${group.name}.`);
    }
    log(`[demo] Demo data ready: ${demoUsers.length} users, ${demoGroups.length} groups (${createdGroups} initialized this run).`);
    log(`[demo] Sign in with ${demoUsers.map((user) => user.email).join(", ")}`);
    log(`[demo] Shared password: ${demoPassword}`);
    log("[demo] Account guide: docs/demo-data.md");
    return { users: demoUsers.length, groups: demoGroups.length, initialized: createdGroups };
  } finally {
    await firestore.terminate();
    await deleteApp(app);
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    await seedDemo({ target: resolveEmulatorTarget(process.argv.slice(2)) });
  } catch (error) {
    console.error(`[demo] Seeding failed: ${error.message}`);
    process.exitCode = 1;
  }
}
