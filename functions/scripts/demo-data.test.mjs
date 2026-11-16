import assert from "node:assert/strict";
import { readFileSync, mkdtempSync, writeFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { test } from "node:test";
import { demoGroups, demoUsers, demoPassword, demoDate, participantId, expensePayload, settlementPayload } from "./demo-data.mjs";
import { assertLocalUrl, resolveEmulatorTarget, repositoryDirectory } from "./emulator-target.mjs";
import { demoOperationId } from "./seed-demo.mjs";
import { allocate } from "../lib/domain/split.js";
import { applyDeltas, expenseDeltas, settlementDeltas } from "../lib/domain/balance.js";

test("the demo covers real split modes, currencies, live debts, repayments, and a settled archive", () => {
  const expected = {
    lisbon: { alice: 10150, bob: -1425, cara: -8725, dana: 0 },
    home: { alice: -3800, bob: 7300, cara: -3500 },
    movies: { alice: -400, bob: -1800, cara: 2200 },
    mountains: { alice: 0, bob: 0 }, weekend: { bob: 0, cara: 0 },
  };
  const modes = new Set();
  for (const group of demoGroups) {
    assert.ok(group.members.includes(group.owner));
    let balances = Object.fromEntries([...group.members, ...group.guests].map((key) => [participantId(group, key), 0]));
    for (const record of group.expenses) {
      const expense = expensePayload(group, record, "2026-11-16");
      modes.add(expense.split.mode);
      balances = applyDeltas(balances, expenseDeltas({ totalMinor: expense.amountMinor,
        contributions: expense.contributions, allocations: allocate(expense.amountMinor, expense.split) }));
    }
    for (const record of group.settlements) balances = applyDeltas(balances, settlementDeltas(settlementPayload(group, record, "2026-11-16")));
    assert.deepEqual(balances, Object.fromEntries(Object.entries(expected[group.key]).map(([key, value]) => [participantId(group, key), value])));
  }
  assert.deepEqual([...modes].sort(), ["EQUAL", "EXACT", "PERCENTAGE", "WEIGHTED"]);
  assert.equal(demoGroups.filter((group) => group.archived).length, 1);
  assert.equal(demoGroups.flatMap((group) => group.expenses).length, 11);
  assert.equal(demoGroups.flatMap((group) => group.settlements).length, 4);
  assert.equal(demoGroups.some((group) => group.members.includes("dana")), false);
});

test("seed dates and idempotency IDs remain stable across retries", () => {
  assert.equal(demoDate("2026-03-01", 1), "2026-02-28");
  assert.equal(demoOperationId("lisbon/expense/apartment"), demoOperationId("lisbon/expense/apartment"));
  assert.match(demoOperationId("lisbon/expense/apartment"), /^[a-f0-9]{8}-[a-f0-9]{4}-5[a-f0-9]{3}-8[a-f0-9]{3}-[a-f0-9]{12}$/);
  assert.notEqual(demoOperationId("lisbon/expense/apartment"), demoOperationId("home/expense/groceries"));
});

test("the published account guide includes every login and group", () => {
  const guide = readFileSync(join(repositoryDirectory, "docs/demo-data.md"), "utf8");
  demoUsers.forEach((user) => assert.ok(guide.includes(user.email)));
  demoGroups.forEach((group) => assert.ok(guide.includes(group.name)));
  assert.ok(guide.includes(demoPassword));
});

test("seeding rejects real projects, remote endpoints, and conflicting emulator environment", () => {
  assert.throws(() => resolveEmulatorTarget(["--project", "split-the-bills-prod"], {}), /demo-/);
  assert.throws(() => resolveEmulatorTarget([], { GCLOUD_PROJECT: "split-the-bills-prod" }), /demo-/);
  assert.throws(() => resolveEmulatorTarget(["--only", "firestore"], {}), /auth,firestore,functions/);
  assert.throws(() => resolveEmulatorTarget([], { FIRESTORE_EMULATOR_HOST: "remote.example:8080" }), /does not match/);
  ["https://127.0.0.1:9099", "http://example.com:9099", "http://owner@127.0.0.1:9099"].forEach((url) => assert.throws(() => assertLocalUrl(url), /local HTTP/));
  ["http://127.0.0.1:9099", "http://localhost:9099", "http://[::1]:9099"].forEach((url) => assert.doesNotThrow(() => assertLocalUrl(url)));
});

test("the target follows explicit local ports and configuration instead of silently using defaults", () => {
  const directory = mkdtempSync(join(tmpdir(), "split-demo-target-"));
  try {
    const config = join(directory, "firebase.json");
    writeFileSync(config, JSON.stringify({ emulators: {
      auth: { host: "0.0.0.0", port: 19099 }, firestore: { port: 18080 }, functions: { port: 15001 },
    } }));
    const target = resolveEmulatorTarget(["--project=demo-seed-test", "--config", config], {});
    assert.equal(target.projectId, "demo-seed-test");
    assert.equal(target.auth.address, "127.0.0.1:19099");
    assert.equal(target.firestore.address, "127.0.0.1:18080");
    assert.equal(target.functions.url, "http://127.0.0.1:15001");
  } finally { rmSync(directory, { recursive: true, force: true }); }
});
