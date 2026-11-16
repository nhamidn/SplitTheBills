import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { test } from "node:test";
import { initializeApp, deleteApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { demoGroups, demoUsers, demoPassword, demoVersion } from "./demo-data.mjs";
import { resolveEmulatorTarget } from "./emulator-target.mjs";
import { demoOperationId, seedDemo } from "./seed-demo.mjs";
import { applyDeltas, expenseDeltas, settlementDeltas } from "../lib/domain/balance.js";

// This test intentionally edits fixtures. Never run it against the shared development suite.
const config = process.env.SPLIT_DEMO_SEED_TEST_CONFIG;
const project = process.env.SPLIT_DEMO_SEED_TEST_PROJECT;
if (!config || !project?.endsWith("-seed-test")) throw new Error("Set SPLIT_DEMO_SEED_TEST_CONFIG and a separate demo-*-seed-test project before running test:seed.");
const target = resolveEmulatorTarget(["--config", config, "--project", project]);

test("real seeding creates valid live data, preserves edits, and resumes without duplicates", { timeout: 120_000 }, async () => {
  await seedDemo({ target, log: () => {} });
  const app = initializeApp({ projectId: project }, `seed-check-${randomUUID()}`);
  const firestore = getFirestore(app);
  const auth = getAuth(app);
  try {
    const before = {};
    for (const group of demoGroups) {
      const root = firestore.doc(`groups/${group.id}`);
      const [stored, members, expenses, settlements, balances, activity] = await Promise.all([
        root.get(), root.collection("members").get(), root.collection("expenses").get(),
        root.collection("settlements").get(), root.collection("balances").get(), root.collection("activity").get(),
      ]);
      assert.equal(stored.get("groupType"), group.type);
      assert.equal(Boolean(stored.get("archivedAt")), Boolean(group.archived));
      assert.equal(members.size, group.members.length);
      assert.equal(expenses.size, group.expenses.length);
      assert.equal(settlements.size, group.settlements.length);
      let calculated = Object.fromEntries(balances.docs.map((item) => [item.id, 0]));
      expenses.docs.forEach((item) => {
        const expense = item.data();
        calculated = applyDeltas(calculated, expenseDeltas({ totalMinor: expense.amountMinor,
          contributions: expense.contributions, allocations: expense.allocations }));
      });
      settlements.docs.forEach((item) => { calculated = applyDeltas(calculated, settlementDeltas(item.data())); });
      assert.deepEqual(Object.fromEntries(balances.docs.map((item) => [item.id, item.get("amountMinor")])), calculated);
      assert.ok(activity.size >= 1 + expenses.size + settlements.size);
      members.docs.forEach((member) => assert.ok(member.get("participantId")));
      before[group.key] = { expenses: expenses.size, settlements: settlements.size };
    }
    for (const user of demoUsers) {
      const response = await fetch(`${target.auth.url}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo-api-key`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: user.email, password: demoPassword, returnSecureToken: true }),
      });
      const result = await response.json();
      assert.equal(result.localId, user.uid);
      const memberships = await firestore.collection(`users/${user.uid}/groups`).get();
      assert.equal(memberships.size, demoGroups.filter((group) => group.members.includes(user.key)).length);
    }

    await firestore.doc("groups/test-user-created-group").set({ sentinel: "keep this data" });
    await auth.updateUser("demo-bob", { displayName: "Bob Test User" });
    const receipt = await firestore.doc(`operationReceipts/demo-bob_${demoOperationId("lisbon/expense/coffee")}`).get();
    const expenseId = receipt.get("response.resourceId");
    const expenseRef = firestore.doc(`groups/demo-lisbon-weekend/expenses/${expenseId}`);
    const original = await expenseRef.get();
    const signIn = await fetch(`${target.auth.url}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo-api-key`, {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: "alice@demo.test", password: demoPassword, returnSecureToken: true }),
    });
    const token = (await signIn.json()).idToken;
    const source = original.data();
    const editedTitle = "Coffee & pastries, edited while testing";
    const edit = await fetch(`${target.functions.url}/${project}/europe-west1/v1UpdateExpense`, {
      method: "POST", headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ data: { apiVersion: "v1", operationId: randomUUID(), groupId: "demo-lisbon-weekend",
        expenseId, expectedRevision: source.revision, expense: { title: editedTitle, amountMinor: source.amountMinor,
          currency: source.currency, contributions: source.contributions, split: source.splitInput,
          date: source.date, category: source.category, note: source.note } } }),
    });
    assert.equal(edit.status, 200, JSON.stringify(await edit.json()));

    // Simulate interruption after the financial writes but before the completion marker.
    await firestore.doc(`emulatorSeeds/${demoVersion}/groups/lisbon`).set({ complete: false }, { merge: true });
    const replay = await seedDemo({ target, log: () => {} });
    assert.equal(replay.initialized, 1);
    assert.equal((await expenseRef.get()).get("title"), editedTitle);
    assert.equal((await expenseRef.get()).get("revision"), source.revision + 1);
    assert.equal((await auth.getUser("demo-bob")).displayName, "Bob Test User");
    assert.equal((await firestore.doc("groups/test-user-created-group").get()).get("sentinel"), "keep this data");
    for (const group of demoGroups) {
      const root = firestore.doc(`groups/${group.id}`);
      assert.equal((await root.collection("expenses").get()).size, before[group.key].expenses);
      assert.equal((await root.collection("settlements").get()).size, before[group.key].settlements);
    }
    assert.equal((await seedDemo({ target, log: () => {} })).initialized, 0);
  } finally {
    await firestore.terminate();
    await deleteApp(app);
  }
});
