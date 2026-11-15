import { fail, MAX_SAFE_MINOR, safeSigned } from "./calculation-error.js";

export interface CurrencyUnit {
  code: string;
  exponent: number;
}

export function validateCurrency(currency: CurrencyUnit): CurrencyUnit {
  if (!/^[A-Z]{3}$/.test(currency.code) || !Number.isInteger(currency.exponent) ||
      currency.exponent < 0 || currency.exponent > 4) {
    fail("INVALID_CURRENCY", "Currency must have an uppercase ISO code and exponent from 0 to 4");
  }
  return currency;
}

export function parseUnsignedMinor(text: string, currency: CurrencyUnit): number {
  validateCurrency(currency);
  if (text.length > 32) fail("INVALID_AMOUNT", "Decimal amount is too long");
  const match = /^([0-9]+)(?:\.([0-9]+))?$/.exec(text);
  if (!match) fail("INVALID_AMOUNT", "Invalid decimal amount");
  const whole = match[1];
  if (whole === undefined) throw new Error("Internal decimal parser failure");
  const fraction = match[2] ?? "";
  if (fraction.length > currency.exponent) {
    fail("INVALID_AMOUNT", `Amount has more than ${currency.exponent} fractional digits`);
  }
  const scale = 10n ** BigInt(currency.exponent);
  const minor = BigInt(whole) * scale + BigInt(fraction.padEnd(currency.exponent, "0") || "0");
  if (minor > BigInt(MAX_SAFE_MINOR)) fail("OVERFLOW", "Amount exceeds the safe integer range");
  return Number(minor);
}

export function formatMinor(amountMinor: number, currency: CurrencyUnit): string {
  safeSigned(amountMinor, "amountMinor");
  validateCurrency(currency);
  const sign = amountMinor < 0 ? "-" : "";
  const digits = Math.abs(amountMinor).toString();
  if (currency.exponent === 0) return sign + digits;
  const padded = digits.padStart(currency.exponent + 1, "0");
  const splitAt = padded.length - currency.exponent;
  return `${sign}${padded.slice(0, splitAt)}.${padded.slice(splitAt)}`;
}
