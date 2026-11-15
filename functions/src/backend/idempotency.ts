import { createHash } from "node:crypto";
import { getFirestore, Timestamp, Transaction } from "firebase-admin/firestore";
import { appError } from "./errors.js";

export interface MutationResponse extends Record<string, unknown> {
  apiVersion: "v1";
  operationId: string;
  committedAt: string;
  replayed: boolean;
}

interface ReceiptData {
  callableName: string;
  requestHash: string;
  response: MutationResponse;
}

interface IdempotencyOptions {
  receiptResponse?: (response: MutationResponse) => MutationResponse;
  replayResponse?: (stored: MutationResponse) => MutationResponse;
}

export async function idempotentMutation(
  callableName: string,
  uid: string,
  operationId: string,
  requestData: unknown,
  action: (transaction: Transaction, committedAt: Timestamp) => Promise<Record<string, unknown>>,
  options: IdempotencyOptions = {},
): Promise<MutationResponse> {
  const firestore = getFirestore();
  const receiptRef = firestore.collection("operationReceipts").doc(`${uid}_${operationId}`);
  const requestHash = createHash("sha256").update(stableJson(requestData)).digest("hex");

  return firestore.runTransaction(async (transaction) => {
    const receiptSnapshot = await transaction.get(receiptRef);
    if (receiptSnapshot.exists) {
      const receipt = receiptSnapshot.data() as ReceiptData;
      if (receipt.callableName !== callableName || receipt.requestHash !== requestHash) {
        appError("already-exists", "OPERATION_ID_REUSED", "The operation ID was used for another request.");
      }
      const replayed = { ...receipt.response, replayed: true };
      return options.replayResponse ? options.replayResponse(replayed) : replayed;
    }

    const committedAt = Timestamp.now();
    const result = await action(transaction, committedAt);
    const response: MutationResponse = {
      apiVersion: "v1",
      operationId,
      committedAt: committedAt.toDate().toISOString(),
      replayed: false,
      ...result,
    };
    const storedResponse = options.receiptResponse ? options.receiptResponse(response) : response;
    transaction.create(receiptRef, {
      uid,
      operationId,
      callableName,
      requestHash,
      response: storedResponse,
      createdAt: committedAt,
      expiresAt: Timestamp.fromMillis(committedAt.toMillis() + 30 * 24 * 60 * 60 * 1000),
    });
    return response;
  });
}

function stableJson(value: unknown): string {
  if (value === null || typeof value !== "object") return JSON.stringify(value);
  if (Array.isArray(value)) return `[${value.map(stableJson).join(",")}]`;
  const object = value as Record<string, unknown>;
  return `{${Object.keys(object).sort().map((key) => `${JSON.stringify(key)}:${stableJson(object[key])}`).join(",")}}`;
}
