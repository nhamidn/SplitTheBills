import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, test } from "node:test";
import {
  RulesTestEnvironment,
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc } from "firebase/firestore";

const projectId = "demo-split-the-bills";
let environment: RulesTestEnvironment;

before(async () => {
  environment = await initializeTestEnvironment({
    projectId,
    firestore: {
      rules: readFileSync("../firestore.rules", "utf8"),
    },
  });
});

beforeEach(async () => {
  await environment.clearFirestore();
  await environment.withSecurityRulesDisabled(async (context) => {
    const firestore = context.firestore();
    await setDoc(doc(firestore, "groups/group-a"), { name: "Weekend", revision: 1 });
    await setDoc(doc(firestore, "groups/group-a/members/alice"), { role: "owner" });
    await setDoc(doc(firestore, "groups/group-a/expenses/expense-a"), { title: "Train" });
    await setDoc(doc(firestore, "users/alice/groups/group-a"), { role: "OWNER" });
    await setDoc(doc(firestore, "invitations/invite-a"), { tokenHash: "server-only" });
    await setDoc(doc(firestore, "operationReceipts/alice_operation-a"), { response: {} });
    await setDoc(doc(firestore, "invitationRateLimits/rate-a"), { attempts: 1 });
  });
});

after(async () => {
  await environment.cleanup();
});

describe("Firestore deny-by-default scaffold", () => {
  test("allows a member to read group data", async () => {
    const firestore = environment.authenticatedContext("alice").firestore();
    await assertSucceeds(getDoc(doc(firestore, "groups/group-a/expenses/expense-a")));
  });

  test("rejects anonymous and non-member reads", async () => {
    const anonymous = environment.unauthenticatedContext().firestore();
    const outsider = environment.authenticatedContext("mallory").firestore();
    await assertFails(getDoc(doc(anonymous, "groups/group-a")));
    await assertFails(getDoc(doc(outsider, "groups/group-a")));
    await assertFails(getDoc(doc(outsider, "groups/group-a/expenses/expense-a")));
  });

  test("allows only the signed-in user to read their projection", async () => {
    const alice = environment.authenticatedContext("alice").firestore();
    const mallory = environment.authenticatedContext("mallory").firestore();
    await assertSucceeds(getDoc(doc(alice, "users/alice/groups/group-a")));
    await assertFails(getDoc(doc(mallory, "users/alice/groups/group-a")));
  });

  test("rejects every direct shared write, including owner writes", async () => {
    const firestore = environment.authenticatedContext("alice").firestore();
    await assertFails(
      setDoc(doc(firestore, "groups/group-a/expenses/expense-b"), { title: "Lunch" }),
    );
  });

  test("keeps all private backend collections server-only", async () => {
    const firestore = environment.authenticatedContext("alice").firestore();
    await assertFails(getDoc(doc(firestore, "invitations/invite-a")));
    await assertFails(getDoc(doc(firestore, "operationReceipts/alice_operation-a")));
    await assertFails(getDoc(doc(firestore, "invitationRateLimits/rate-a")));
    await assertFails(setDoc(doc(firestore, "groups/group-a/members/bob"), { role: "MEMBER" }));
    await assertFails(setDoc(doc(firestore, "groups/group-a/balances/alice"), { amountMinor: 10 }));
  });
});
