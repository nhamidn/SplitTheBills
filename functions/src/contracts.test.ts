import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { afterEach, describe, test } from "node:test";
import { API_VERSION, getApiInfo } from "./contracts.js";
import { currencyMetadata } from "./backend/currency.js";
import { validatedGroupType } from "./backend/groups.js";

test("group types are backward compatible and reject unsupported values", () => {
  assert.equal(validatedGroupType(undefined), "OTHER");
  for (const value of ["TRIP", "HOME", "FRIENDS", "COUPLE", "OTHER"]) assert.equal(validatedGroupType(value), value);
  for (const value of [null, 5, "trip", "WORK", ""]) assert.throws(() => validatedGroupType(value));
});

test("every currency offered by the client is accepted by the backend", () => {
  const catalog = JSON.parse(readFileSync(
    new URL("../../shared/contracts/v1/currencies.json", import.meta.url), "utf8",
  )) as { apiVersion: string; codes: string[] };
  assert.equal(catalog.apiVersion, API_VERSION);
  assert.deepEqual(catalog.codes, Intl.supportedValuesOf("currency"));
  for (const code of catalog.codes) assert.equal(currencyMetadata(code).code, code);
});

describe("API info", () => {
  afterEach(() => {
    delete process.env.FUNCTIONS_EMULATOR;
  });

  test("returns the v1 compatibility boundary", () => {
    const response = getApiInfo();
    assert.equal(response.apiVersion, API_VERSION);
    assert.equal(response.minimumClientApiVersion, "v1");
  });

  test("reports emulator environment without project details", () => {
    process.env.FUNCTIONS_EMULATOR = "true";
    assert.deepEqual(getApiInfo(), {
      apiVersion: "v1",
      minimumClientApiVersion: "v1",
      environment: "emulator",
    });
  });
});
