export const API_VERSION = "v1" as const;

export interface ApiInfoResponse {
  apiVersion: typeof API_VERSION;
  minimumClientApiVersion: typeof API_VERSION;
  environment: "emulator" | "firebase";
}

export function getApiInfo(): ApiInfoResponse {
  return {
    apiVersion: API_VERSION,
    minimumClientApiVersion: API_VERSION,
    environment: process.env.FUNCTIONS_EMULATOR === "true" ? "emulator" : "firebase",
  };
}
