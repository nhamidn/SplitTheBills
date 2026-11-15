import { appError } from "./errors.js";

export interface CurrencyMetadata {
  code: string;
  exponent: number;
}

const supportedCurrencies = new Set(Intl.supportedValuesOf("currency"));

/** Resolves ISO currency precision once on group creation; records store the frozen exponent. */
export function currencyMetadata(code: string): CurrencyMetadata {
  if (!supportedCurrencies.has(code)) {
    appError("invalid-argument", "INVALID_ARGUMENT", "Unsupported ISO currency code.", { fields: ["currency"] });
  }
  try {
    const options = new Intl.NumberFormat("en", { style: "currency", currency: code }).resolvedOptions();
    const exponent = options.maximumFractionDigits;
    if (options.currency !== code || options.minimumFractionDigits !== options.maximumFractionDigits ||
        typeof exponent !== "number" || exponent < 0 || exponent > 4) {
      appError("invalid-argument", "INVALID_ARGUMENT", "Unsupported currency precision.", { fields: ["currency"] });
    }
    return { code, exponent };
  } catch {
    appError("invalid-argument", "INVALID_ARGUMENT", "Unsupported ISO currency code.", { fields: ["currency"] });
  }
}
