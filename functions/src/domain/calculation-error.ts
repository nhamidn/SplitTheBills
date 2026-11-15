export const MAX_SAFE_MINOR = Number.MAX_SAFE_INTEGER;

export type CalculationErrorCode =
  | "INVALID_AMOUNT"
  | "INVALID_CURRENCY"
  | "INVALID_PARTICIPANT"
  | "INVALID_SPLIT"
  | "DUPLICATE_PARTICIPANT"
  | "CONTRIBUTIONS_DO_NOT_SUM"
  | "ALLOCATIONS_DO_NOT_SUM"
  | "BALANCES_NOT_CONSERVED"
  | "OVERFLOW";

export class CalculationError extends Error {
  constructor(
    readonly code: CalculationErrorCode,
    message: string,
  ) {
    super(message);
    this.name = "CalculationError";
  }
}

export function fail(code: CalculationErrorCode, message: string): never {
  throw new CalculationError(code, message);
}

export function participantId(value: string): string {
  if (!/^[A-Za-z0-9_-]{1,128}$/.test(value)) {
    fail("INVALID_PARTICIPANT", "Invalid participant ID");
  }
  return value;
}

export function safeNonNegative(value: number, field: string): number {
  if (!Number.isSafeInteger(value) || value < 0) {
    fail("INVALID_AMOUNT", `${field} must be a non-negative safe integer`);
  }
  return value;
}

export function safePositive(value: number, field: string): number {
  safeNonNegative(value, field);
  if (value === 0) fail("INVALID_AMOUNT", `${field} must be positive`);
  return value;
}

export function safeSigned(value: number, field: string): number {
  if (!Number.isSafeInteger(value)) {
    fail("OVERFLOW", `${field} is outside the safe integer range`);
  }
  return value;
}

export function checkedAdd(left: number, right: number, field: string): number {
  safeSigned(left, field);
  safeSigned(right, field);
  const result = left + right;
  if (!Number.isSafeInteger(result)) fail("OVERFLOW", `${field} exceeds the safe integer range`);
  return result;
}

export function uniqueParticipantIds(ids: readonly string[]): void {
  if (ids.length < 1 || ids.length > 100) {
    fail("INVALID_SPLIT", "A split needs between 1 and 100 participants");
  }
  ids.forEach(participantId);
  if (new Set(ids).size !== ids.length) {
    fail("DUPLICATE_PARTICIPANT", "Participant IDs must be unique");
  }
}

export function compareParticipantId(left: string, right: string): number {
  return left < right ? -1 : left > right ? 1 : 0;
}
