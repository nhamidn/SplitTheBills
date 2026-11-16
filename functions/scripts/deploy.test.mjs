import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { tmpdir } from "node:os";
import { resolve } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";
import { createDeploymentPlan, parseDeploymentArgs, runDeployment } from "./deploy.mjs";

const options = (args = []) => parseDeploymentArgs(["--project", "split-bills-staging", ...args]);

test("deployment requires an explicit real project instead of the demo default", () => {
  for (const args of [[], ["--project", "demo-split-the-bills"], ["--project", "<project-id>"], ["--project", "bad;command"]]) {
    assert.throws(() => parseDeploymentArgs(args), /project/i);
  }
});

test("unknown options, unsupported targets, and positional arguments fail closed", () => {
  for (const args of [["--force"], ["--only", "hosting"], ["another-project"], ["--project"]]) {
    assert.throws(() => options(args));
  }
  assert.equal(parseDeploymentArgs(["--help"]).help, true);
});

test("checks precede rules/indexes and Functions; all deployments pin the project and config", () => {
  const steps = createDeploymentPlan(options(["--non-interactive"]));
  assert.equal(steps.length, 3);
  assert.equal(steps[0].tool, "npm");
  assert.deepEqual(steps[0].args, ["run", "check"]);
  assert.deepEqual(steps.slice(1).map((step) => step.args[2]), ["firestore:rules,firestore:indexes", "functions"]);
  for (const step of steps.slice(1)) {
    assert.equal(step.args[step.args.indexOf("--project") + 1], "split-bills-staging");
    assert.equal(step.args[step.args.indexOf("--config") + 1], resolve(step.cwd, "firebase.json"));
    assert.ok(step.args.includes("--non-interactive"));
    assert.ok(!step.args.includes("--force"));
  }
});

test("partial scopes deploy only the requested backend resources", () => {
  for (const [scope, target] of [["functions", "functions"], ["firestore", "firestore:rules,firestore:indexes"]]) {
    const steps = createDeploymentPlan(options(["--only", scope]));
    assert.equal(steps.length, 2);
    assert.equal(steps[1].args[2], target);
  }
});

test("dry run prints a plan without invoking checks or Firebase", () => {
  const messages = [];
  runDeployment(options(["--dry-run"]), {
    run: () => assert.fail("A dry run must never execute a command"),
    log: (message) => messages.push(message),
  });
  assert.match(messages.join("\n"), /Plan only/);
  assert.match(messages.join("\n"), /firestore:rules,firestore:indexes/);
});

test("CLI preview works outside the repo without dependencies or npm execution context", () => {
  const result = spawnSync(process.execPath, [
    fileURLToPath(new URL("./deploy.mjs", import.meta.url)),
    "--project", "split-bills-production", "--dry-run",
  ], {
    cwd: tmpdir(),
    env: { ...process.env, npm_execpath: "/nonexistent/npm-cli.js" },
    encoding: "utf8",
  });
  assert.ifError(result.error);
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /split-bills-production/);
  assert.match(result.stdout, /Plan only/);
});

test("a failed check prevents every cloud deployment", () => {
  const executed = [];
  assert.throws(() => runDeployment(options(), {
    run: (step) => { executed.push(step.tool); throw new Error("check failed"); },
    log: () => {},
  }), /check failed/);
  assert.deepEqual(executed, ["npm"]);
});

test("a Firestore deployment failure prevents Functions deployment", () => {
  const executed = [];
  assert.throws(() => runDeployment(options(), {
    run: (step) => {
      executed.push(step.tool);
      if (step.tool === "firebase") throw new Error("rules deployment failed");
    },
    log: () => {},
  }), /rules deployment failed/);
  assert.deepEqual(executed, ["npm", "firebase"]);
});

test("successful execution follows the same order as the preview", () => {
  const configuration = options();
  const executed = [];
  runDeployment(configuration, { run: (step) => executed.push(step), log: () => {} });
  assert.deepEqual(executed, createDeploymentPlan(configuration));
});
