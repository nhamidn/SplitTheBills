import { FunctionsErrorCode, HttpsError } from "firebase-functions/v2/https";

export interface ErrorDetails extends Record<string, unknown> {
  apiVersion: "v1";
  errorCode: string;
  retryable: boolean;
}

export function appError(
  status: FunctionsErrorCode,
  errorCode: string,
  message: string,
  details: Record<string, unknown> = {},
): never {
  const payload: ErrorDetails = {
    apiVersion: "v1",
    errorCode,
    retryable: false,
    ...details,
  };
  throw new HttpsError(status, message, payload);
}

export function staleRevision(
  resourceId: string,
  expectedRevision: number,
  actualRevision: number,
): never {
  appError("aborted", "STALE_REVISION", "The record changed on another device.", {
    resourceId,
    expectedRevision,
    actualRevision,
  });
}

export function requireRevision(resourceId: string, expected: number, actual: unknown): void {
  if (!Number.isSafeInteger(actual) || actual !== expected) {
    staleRevision(resourceId, expected, Number.isSafeInteger(actual) ? actual as number : 0);
  }
}
