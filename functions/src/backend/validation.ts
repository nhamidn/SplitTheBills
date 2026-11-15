import { appError } from "./errors.js";

export type InputObject = Record<string, unknown>;

const operationIdPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const idPattern = /^[A-Za-z0-9_-]{1,128}$/;

export function objectValue(value: unknown, field = "request"): InputObject {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    invalid(`${field} must be an object`, field);
  }
  return value as InputObject;
}

export function strictObject(
  value: unknown,
  allowedKeys: readonly string[],
  requiredKeys: readonly string[],
  field = "request",
): InputObject {
  const result = objectValue(value, field);
  const allowed = new Set(allowedKeys);
  const unknown = Object.keys(result).filter((key) => !allowed.has(key));
  if (unknown.length > 0) invalid(`${field} contains unknown fields`, unknown.join(","));
  requiredKeys.forEach((key) => {
    if (!(key in result)) invalid(`${field}.${key} is required`, key);
  });
  return result;
}

export function mutationRequest(
  value: unknown,
  operationKeys: readonly string[],
  requiredOperationKeys: readonly string[],
): InputObject {
  const request = strictObject(
    value,
    ["apiVersion", "operationId", ...operationKeys],
    ["apiVersion", "operationId", ...requiredOperationKeys],
  );
  if (request.apiVersion !== "v1") invalid("apiVersion must be v1", "apiVersion");
  const operationId = stringValue(request, "operationId", 36, 36);
  if (!operationIdPattern.test(operationId)) invalid("operationId must be a UUID", "operationId");
  return request;
}

export function stringValue(
  object: InputObject,
  field: string,
  minimumLength = 1,
  maximumLength = 128,
): string {
  const value = object[field];
  if (typeof value !== "string") invalid(`${field} must be a string`, field);
  const length = [...value].length;
  if (length < minimumLength || length > maximumLength || value.trim() !== value) {
    invalid(`${field} length or whitespace is invalid`, field);
  }
  return value;
}

export function optionalString(
  object: InputObject,
  field: string,
  maximumLength: number,
): string | null {
  const value = object[field];
  if (value === undefined || value === null || value === "") return null;
  return stringValue(object, field, 1, maximumLength);
}

export function idValue(object: InputObject, field: string): string {
  const value = stringValue(object, field, 1, 128);
  if (!idPattern.test(value)) invalid(`${field} has an invalid ID format`, field);
  return value;
}

export function integerValue(
  object: InputObject,
  field: string,
  minimum = 0,
  maximum = Number.MAX_SAFE_INTEGER,
): number {
  const value = object[field];
  if (!Number.isSafeInteger(value) || (value as number) < minimum || (value as number) > maximum) {
    invalid(`${field} must be an integer from ${minimum} to ${maximum}`, field);
  }
  return value as number;
}

export function booleanValue(object: InputObject, field: string): boolean {
  const value = object[field];
  if (typeof value !== "boolean") invalid(`${field} must be a boolean`, field);
  return value;
}

export function arrayValue(object: InputObject, field: string, maximum = 100): unknown[] {
  const value = object[field];
  if (!Array.isArray(value) || value.length < 1 || value.length > maximum) {
    invalid(`${field} must contain between 1 and ${maximum} entries`, field);
  }
  return value;
}

export function optionalArray(object: InputObject, field: string, maximum = 100): unknown[] {
  const value = object[field];
  if (value === undefined) return [];
  if (!Array.isArray(value) || value.length > maximum) invalid(`${field} must be an array`, field);
  return value;
}

export function currencyValue(object: InputObject, field = "currency"): string {
  const value = stringValue(object, field, 3, 3);
  if (!/^[A-Z]{3}$/.test(value)) invalid(`${field} must be an uppercase ISO currency code`, field);
  return value;
}

export function dateValue(object: InputObject, field = "date"): string {
  const value = stringValue(object, field, 10, 10);
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) invalid(`${field} must be YYYY-MM-DD`, field);
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const date = new Date(Date.UTC(year, month - 1, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) {
    invalid(`${field} is not a valid calendar date`, field);
  }
  return value;
}

export function operationId(request: InputObject): string {
  return request.operationId as string;
}

export function invalid(message: string, field?: string): never {
  appError("invalid-argument", "INVALID_ARGUMENT", message, field ? { fields: [field] } : {});
}
