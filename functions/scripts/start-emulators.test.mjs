import assert from "node:assert/strict";
import { EventEmitter } from "node:events";
import { test } from "node:test";
import { startEmulators } from "./start-emulators.mjs";
import { resolveEmulatorTarget } from "./emulator-target.mjs";

function harness(seed) {
  const child = new EventEmitter();
  const log = [];
  const commands = [];
  let exited = false;
  child.kill = (signal) => {
    commands.push(signal);
    if (!exited) queueMicrotask(() => child.emit("exit", 0, null));
    return true;
  };
  child.once("exit", () => { exited = true; });
  return { child, log, commands, dependencies: {
    resolveTarget: (args) => resolveEmulatorTarget(args, {}),
    checkPorts: async () => { commands.push("ports checked"); },
    launch: (tool, args, options) => { commands.push({ tool, args, options }); return child; },
    log: (line) => log.push(line), seed: (options) => seed(options, child),
  } };
}

test("startup launches the pinned CLI, seeds, then reports ready, preserving import/export arguments", async () => {
  const run = harness(async ({ target }, child) => {
    assert.equal(target.projectId, "demo-split-the-bills");
    setImmediate(() => child.emit("exit", 0, null));
  });
  const code = await startEmulators(["--only", "auth,firestore,functions", "--import", "/tmp/demo data", "--export-on-exit"], run.dependencies);
  assert.equal(code, 0);
  const command = run.commands.find((entry) => typeof entry === "object");
  assert.equal(run.commands[0], "ports checked");
  assert.equal(command.tool, process.execPath);
  assert.match(command.args[0], /firebase-tools.*firebase\.js$/);
  assert.ok(command.args.includes("/tmp/demo data"));
  assert.ok(command.args.includes("--export-on-exit"));
  assert.equal(command.options.shell, false);
  assert.ok(run.log.some((line) => line.includes("Ready for Android testing")));
});

test("a failed seed shuts down its suite and never reports successful readiness", async () => {
  const run = harness(async () => { throw new Error("fixture creation failed"); });
  await assert.rejects(startEmulators([], run.dependencies), /fixture creation failed/);
  assert.deepEqual(run.commands.filter((entry) => typeof entry === "string"), ["ports checked", "SIGINT"]);
  assert.equal(run.log.some((line) => line.includes("Ready for Android testing")), false);
});

test("an early Firebase exit cancels readiness/seeding instead of hanging", async () => {
  const run = harness(({ signal }, child) => new Promise((resolve, reject) => {
    signal.addEventListener("abort", () => reject(signal.reason), { once: true });
    setImmediate(() => child.emit("exit", 1, null));
  }));
  await assert.rejects(startEmulators([], run.dependencies), /before demo data was ready/);
});

test("a graceful interrupt is forwarded once so Firebase can export its data", async () => {
  const listeners = process.listenerCount("SIGINT");
  const run = harness(async () => { setImmediate(() => process.emit("SIGINT")); });
  assert.equal(await startEmulators([], run.dependencies), 0);
  assert.deepEqual(run.commands.filter((entry) => entry === "SIGINT"), ["SIGINT"]);
  assert.equal(process.listenerCount("SIGINT"), listeners);
});
