import { spawn } from "node:child_process";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { assertEmulatorPortsAvailable, repositoryDirectory, resolveEmulatorTarget } from "./emulator-target.mjs";
import { seedDemo } from "./seed-demo.mjs";

const firebaseCli = fileURLToPath(new URL("../node_modules/firebase-tools/lib/bin/firebase.js", import.meta.url));

export async function startEmulators(args = [], { launch = spawn, seed = seedDemo, checkPorts = assertEmulatorPortsAvailable, resolveTarget = resolveEmulatorTarget, log = console.log } = {}) {
  const target = resolveTarget(args);
  await checkPorts(target);
  const abort = new AbortController();
  log(`[dev] Starting ${target.projectId}; demo users and records will be prepared automatically.`);
  const child = launch(process.execPath, [firebaseCli, "emulators:start", ...args,
    "--project", target.projectId, "--config", target.configPath], {
    cwd: repositoryDirectory, stdio: "inherit", shell: false, detached: process.platform !== "win32",
  });
  const finished = new Promise((resolve, reject) => {
    child.once("error", reject);
    child.once("exit", (code, signal) => resolve({ code, signal }));
  });
  let stopping = false;
  const stop = () => {
    if (stopping) return;
    stopping = true;
    abort.abort(new Error("Emulator startup was stopped."));
    // Firebase receives one graceful interrupt, allowing --export-on-exit to complete.
    child.kill("SIGINT");
  };
  process.once("SIGINT", stop);
  process.once("SIGTERM", stop);
  try {
    const first = await Promise.race([
      finished.then((result) => ({ exited: result })),
      seed({ target, signal: abort.signal, log }).then(() => ({ ready: true })),
    ]);
    if (first.exited) {
      abort.abort(new Error("Firebase exited before demo seeding completed."));
      if (stopping) return first.exited.code ?? 0;
      throw new Error(`Firebase stopped before demo data was ready (exit ${first.exited.code ?? first.exited.signal}).`);
    }
    log("[dev] Ready for Android testing. Leave this terminal running; press Ctrl+C to stop.");
    const result = await finished;
    return result.code ?? (stopping ? 0 : 1);
  } catch (error) {
    const requested = stopping;
    stop();
    await finished.catch(() => {});
    if (requested) return 0;
    throw error;
  } finally {
    abort.abort();
    process.removeListener("SIGINT", stop);
    process.removeListener("SIGTERM", stop);
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  startEmulators(process.argv.slice(2)).then((code) => { process.exitCode = code; }).catch((error) => {
    console.error(`[dev] ${error.message}`);
    process.exitCode = 1;
  });
}
