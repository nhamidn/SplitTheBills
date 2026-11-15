package com.dts.splitthebill.domain.repository

import com.dts.splitthebill.domain.model.ExpenseInput
import com.dts.splitthebill.domain.model.GroupDetail
import com.dts.splitthebill.domain.model.GroupSummary
import com.dts.splitthebill.domain.model.InvitationSecret
import com.dts.splitthebill.domain.model.InvitationSummary
import com.dts.splitthebill.domain.model.MutationResult
import com.dts.splitthebill.domain.model.OwnershipCandidate
import com.dts.splitthebill.domain.model.OwnershipOptions
import com.dts.splitthebill.domain.model.OwnershipTransfer
import com.dts.splitthebill.domain.model.SettlementInput
import kotlinx.coroutines.flow.Flow

interface GroupsRepository {
    fun observeGroups(uid: String): Flow<List<GroupSummary>>
    fun observeGroup(groupId: String, uid: String): Flow<GroupDetail>
    suspend fun createGroup(name: String, currency: String, participantName: String): MutationResult
    suspend fun createParticipant(groupId: String, name: String): MutationResult
    suspend fun createExpense(groupId: String, expense: ExpenseInput, operationId: String): MutationResult
    suspend fun updateExpense(
        groupId: String,
        expenseId: String,
        expectedRevision: Long,
        expense: ExpenseInput,
        operationId: String,
    ): MutationResult
    suspend fun deleteExpense(groupId: String, expenseId: String, expectedRevision: Long): MutationResult
    suspend fun createSettlement(groupId: String, settlement: SettlementInput): MutationResult
    suspend fun createInvitation(
        groupId: String,
        expiresInSeconds: Long,
        maxUses: Long,
        operationId: String,
    ): InvitationSecret
    suspend fun listInvitations(groupId: String): List<InvitationSummary>
    suspend fun revokeInvitation(
        groupId: String,
        invitationId: String,
        expectedRevision: Long,
        operationId: String,
    ): MutationResult
    suspend fun redeemInvitation(token: String?, code: String?, operationId: String): MutationResult
    suspend fun approveParticipantLink(
        groupId: String,
        participantId: String,
        memberUid: String,
        expectedRevision: Long,
        operationId: String,
    ): MutationResult
    suspend fun ownershipOptions(groupId: String): OwnershipOptions
    suspend fun deleteAccount(transfers: List<OwnershipTransfer>, operationId: String): MutationResult
    suspend fun clearLocalCache()
}

class BackendException(
    val errorCode: String,
    message: String,
    val details: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
) : Exception(message, cause)
