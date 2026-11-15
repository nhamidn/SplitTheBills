import { initializeApp } from "firebase-admin/app";
import { setGlobalOptions } from "firebase-functions/v2";
import { CallableRequest, onCall } from "firebase-functions/v2/https";
import { deleteAccount } from "./backend/accounts.js";
import { appError } from "./backend/errors.js";
import { createExpense, deleteExpense, updateExpense } from "./backend/expenses.js";
import { archiveGroup, createGroup, createParticipant, renameGroup } from "./backend/groups.js";
import {
  approveParticipantLink,
  createInvitation,
  listInvitations,
  redeemInvitation,
  revokeInvitation,
} from "./backend/invitations.js";
import { createSettlement, deleteSettlement, updateSettlement } from "./backend/settlements.js";
import { getApiInfo } from "./contracts.js";

initializeApp();

setGlobalOptions({
  region: "europe-west1",
  maxInstances: 10,
  memory: "256MiB",
});

/**
 * Non-mutating bootstrap endpoint. Financial mutations added in later milestones must
 * require authentication, App Check, an operation ID, and transactional receipts.
 */
export const v1ApiInfo = onCall(
  { enforceAppCheck: false },
  () => getApiInfo(),
);

const mutationOptions = {
  enforceAppCheck: process.env.FUNCTIONS_EMULATOR !== "true",
} as const;

export const v1CreateGroup = onCall(mutationOptions, (request) => createGroup(authenticatedUid(request), request.data));
export const v1RenameGroup = onCall(mutationOptions, (request) => renameGroup(authenticatedUid(request), request.data));
export const v1ArchiveGroup = onCall(mutationOptions, (request) => archiveGroup(authenticatedUid(request), request.data));
export const v1CreateParticipant = onCall(mutationOptions, (request) => createParticipant(authenticatedUid(request), request.data));
export const v1CreateInvitation = onCall(mutationOptions, (request) => createInvitation(authenticatedUid(request), request.data));
export const v1ListInvitations = onCall(mutationOptions, (request) => listInvitations(authenticatedUid(request), request.data));
export const v1RevokeInvitation = onCall(mutationOptions, (request) => revokeInvitation(authenticatedUid(request), request.data));
export const v1RedeemInvitation = onCall(mutationOptions, (request) => redeemInvitation(
  authenticatedUid(request),
  request.data,
  request.rawRequest.ip,
));
export const v1ApproveParticipantLink = onCall(mutationOptions, (request) => approveParticipantLink(authenticatedUid(request), request.data));
export const v1CreateExpense = onCall(mutationOptions, (request) => createExpense(authenticatedUid(request), request.data));
export const v1UpdateExpense = onCall(mutationOptions, (request) => updateExpense(authenticatedUid(request), request.data));
export const v1DeleteExpense = onCall(mutationOptions, (request) => deleteExpense(authenticatedUid(request), request.data));
export const v1CreateSettlement = onCall(mutationOptions, (request) => createSettlement(authenticatedUid(request), request.data));
export const v1UpdateSettlement = onCall(mutationOptions, (request) => updateSettlement(authenticatedUid(request), request.data));
export const v1DeleteSettlement = onCall(mutationOptions, (request) => deleteSettlement(authenticatedUid(request), request.data));
export const v1DeleteAccount = onCall(mutationOptions, (request) => deleteAccount(authenticatedUid(request), request.data));

function authenticatedUid(request: CallableRequest<unknown>): string {
  if (!request.auth?.uid) appError("unauthenticated", "UNAUTHENTICATED", "Sign in is required.");
  return request.auth.uid;
}
