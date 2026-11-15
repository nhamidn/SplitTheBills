import assert from "node:assert/strict";
import { afterEach, describe, test } from "node:test";
import { API_VERSION, getApiInfo } from "./contracts.js";

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
