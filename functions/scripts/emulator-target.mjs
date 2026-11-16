import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";
import { setTimeout as pause } from "node:timers/promises";
import { createConnection, createServer } from "node:net";
import { demoProjectId } from "./demo-data.mjs";

export const repositoryDirectory = resolve(dirname(fileURLToPath(import.meta.url)), "../..");

export function resolveEmulatorTarget(args = [], env = process.env) {
  const { values } = parseArgs({ args, strict: false, allowPositionals: true, options: {
    project: { type: "string", short: "P" }, config: { type: "string", short: "c" }, only: { type: "string" },
  } });
  const projectId = values.project ?? env.GCLOUD_PROJECT ?? env.GOOGLE_CLOUD_PROJECT ?? demoProjectId;
  if (!/^demo-[a-z0-9][a-z0-9-]{0,23}[a-z0-9]$/.test(projectId)) {
    throw new Error("Demo seeding requires a local demo-* project ID. Real Firebase projects are not supported.");
  }
  if (values.only && !["auth", "firestore", "functions"].every((name) => values.only.split(",").some((item) => item.split(":")[0] === name))) {
    throw new Error("Demo startup needs --only auth,firestore,functions. Use the Firebase CLI directly for an unseeded individual emulator.");
  }
  const configPath = resolve(repositoryDirectory, values.config ?? "firebase.json");
  const config = JSON.parse(readFileSync(configPath, "utf8"));
  const endpoint = (service, fallbackPort, variable) => {
    const entry = config.emulators?.[service];
    if (!entry) throw new Error(`Configure the ${service} emulator in ${configPath}.`);
    let host = entry.host ?? "127.0.0.1";
    if (host === "0.0.0.0") host = "127.0.0.1";
    if (host === "::") host = "::1";
    const port = entry.port ?? fallbackPort;
    const url = new URL(`http://${host.includes(":") ? `[${host}]` : host}:${port}`);
    assertLocalUrl(url);
    if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error(`Invalid ${service} emulator port.`);
    if (env[variable] && env[variable] !== url.host && env[variable] !== `localhost:${port}`) {
      throw new Error(`${variable} does not match ${service} in ${configPath}. Refusing to seed a different endpoint.`);
    }
    return { host, port, url: url.origin, address: url.host };
  };
  return { projectId, configPath,
    auth: endpoint("auth", 9099, "FIREBASE_AUTH_EMULATOR_HOST"),
    firestore: endpoint("firestore", 8080, "FIRESTORE_EMULATOR_HOST"),
    functions: endpoint("functions", 5001, "FUNCTIONS_EMULATOR_HOST"),
  };
}

export function assertLocalUrl(value) {
  const url = new URL(value);
  if (url.protocol !== "http:" || !["127.0.0.1", "localhost", "[::1]", "::1"].includes(url.hostname) || url.username || url.password) {
    throw new Error("Demo seeding connects only to local HTTP emulator endpoints.");
  }
}

export async function assertEmulatorPortsAvailable(target) {
  for (const [name, endpoint] of [["Auth", target.auth], ["Firestore", target.firestore], ["Functions", target.functions]]) {
    await new Promise((resolve, reject) => {
      const server = createServer();
      server.once("error", () => reject(new Error(`${name} port ${endpoint.port} is already in use. Stop the existing suite first, or use npm --prefix functions run seed:demo to seed it.`)));
      server.listen({ host: endpoint.host, port: endpoint.port }, () => server.close(resolve));
    });
  }
}

function listening(endpoint, signal) {
  return new Promise((resolve, reject) => {
    const socket = createConnection({ host: endpoint.host, port: endpoint.port, signal });
    socket.setTimeout(1000);
    socket.once("connect", () => { socket.destroy(); resolve(); });
    socket.once("error", reject);
    socket.once("timeout", () => { socket.destroy(); reject(new Error("Emulator is not ready")); });
  });
}

export async function waitForEmulators(target, { signal, timeoutMs = 120_000 } = {}) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    signal?.throwIfAborted();
    try {
      await Promise.all([listening(target.auth, signal), listening(target.firestore, signal)]);
      const response = await fetch(`${target.functions.url}/${target.projectId}/europe-west1/v1ApiInfo`, {
        method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ data: {} }),
        signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(2000)]) : AbortSignal.timeout(2000),
      });
      const body = await response.json();
      if (response.ok && body.result?.apiVersion === "v1" && body.result?.environment === "emulator") return;
    } catch { signal?.throwIfAborted(); }
    await pause(500, undefined, { signal });
  }
  throw new Error("Firebase did not become ready in time. Check Auth, Firestore and the v1ApiInfo Functions handler.");
}
