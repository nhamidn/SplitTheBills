import { spawnSync } from "node:child_process";
import { existsSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { parseArgs } from "node:util";
import { fileURLToPath } from "node:url";

const functionsDirectory = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const repositoryDirectory = resolve(functionsDirectory, "..");
const firebaseCli = resolve(functionsDirectory, "node_modules/firebase-tools/lib/bin/firebase.js");

const help = `Usage (from the repository root):
  npm --prefix functions run deploy:firebase -- --project REAL_PROJECT_ID [options]

Options:
  --project ID         Required Firebase project ID; demo projects are rejected.
  --only SCOPE         backend (default), firestore, or functions.
  --dry-run            Print the plan only. Runs no commands and makes no cloud requests.
  --non-interactive    Pass non-interactive mode to Firebase for configured CI runners.
  --help               Show this help.

Runs backend checks first, then deploys Firestore rules/indexes before Functions.
Uses the repository's pinned Firebase CLI and an explicit project on every deploy.
See docs/firebase-setup.md for console setup, Android testing, and release steps.`;

export function parseDeploymentArgs(args) {
  const { values } = parseArgs({
    args,
    strict: true,
    allowPositionals: false,
    options: {
      project: { type: "string" },
      only: { type: "string", default: "backend" },
      "dry-run": { type: "boolean", default: false },
      "non-interactive": { type: "boolean", default: false },
      help: { type: "boolean", default: false },
    },
  });
  if (!values.help) validateOptions(values);
  return values;
}

function validateOptions(options) {
  if (!options.project || !/^[a-z][a-z0-9-]{4,28}[a-z0-9]$/.test(options.project)) {
    throw new Error("Pass --project with the actual Firebase project ID (6–30 lowercase letters, digits, or hyphens).");
  }
  if (options.project.startsWith("demo-")) {
    throw new Error("demo-* projects are for local emulators. Pass a real Firebase project ID.");
  }
  if (!["backend", "firestore", "functions"].includes(options.only)) {
    throw new Error("--only must be backend, firestore, or functions.");
  }
}

export function createDeploymentPlan(options) {
  validateOptions(options);
  const steps = [{
    label: "Backend lint, build, and tests",
    tool: "npm",
    args: ["run", "check"],
    cwd: functionsDirectory,
  }];
  const deploy = (target) => ({
    label: `Deploy ${target} to ${options.project}`,
    tool: "firebase",
    args: [
      "deploy", "--only", target,
      "--project", options.project,
      "--config", resolve(repositoryDirectory, "firebase.json"),
      ...(options["non-interactive"] ? ["--non-interactive"] : []),
    ],
    cwd: repositoryDirectory,
  });
  if (options.only !== "functions") steps.push(deploy("firestore:rules,firestore:indexes"));
  if (options.only !== "firestore") steps.push(deploy("functions"));
  return steps;
}

function executeStep(step) {
  // npm_execpath is supplied by `npm run`; using Node avoids platform-specific .cmd shells.
  const script = step.tool === "npm" ? process.env.npm_execpath : firebaseCli;
  if (!script || !existsSync(script)) {
    throw new Error("Run npm --prefix functions ci, then launch deployment using npm run deploy:firebase.");
  }
  const result = spawnSync(process.execPath, [script, ...step.args], {
    cwd: step.cwd,
    stdio: "inherit",
    shell: false,
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(`${step.label} failed (${result.signal ?? `exit ${result.status}`}). Deployment stopped.`);
  }
}

export function runDeployment(options, { run = executeStep, log = console.log } = {}) {
  const steps = createDeploymentPlan(options);
  log(`Firebase project: ${options.project}; scope: ${options.only}`);
  if (options["dry-run"]) {
    log("Plan only: checks and deployment commands will NOT run.");
    for (const step of steps) {
      const command = step.tool === "npm"
        ? ["npm", "--prefix", functionsDirectory, ...step.args]
        : [process.execPath, firebaseCli, ...step.args];
      // JSON quoting here is for display only; execution always passes an argument array.
      log(command.map((argument) => JSON.stringify(argument)).join(" "));
    }
    return;
  }
  for (const step of steps) {
    log(step.label);
    run(step);
  }
  log("Deployment commands completed. Verify index readiness and the staging smoke test before rollout.");
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const options = parseDeploymentArgs(process.argv.slice(2));
    if (options.help) console.log(help);
    else runDeployment(options);
  } catch (error) {
    console.error(error instanceof Error ? error.message : String(error));
    process.exitCode = 1;
  }
}
