package com.dts.splitthebill.domain.model

data class SignedInUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
)

data class GroupSummary(
    val id: String,
    val name: String,
    val currency: String,
    val role: String,
    val participantId: String?,
    val revision: Long,
    val archived: Boolean,
    val type: GroupType = GroupType.OTHER,
)

enum class GroupType { TRIP, HOME, FRIENDS, COUPLE, OTHER }

data class Participant(
    val id: String,
    val displayName: String,
    val linkedUid: String?,
    val active: Boolean,
)

data class GroupMember(
    val uid: String,
    val role: String,
    val participantId: String?,
)

data class OwnershipCandidate(
    val uid: String,
    val displayName: String?,
)

data class OwnershipOptions(
    val groupRevision: Long,
    val candidates: List<OwnershipCandidate>,
)

data class OwnershipTransfer(
    val groupId: String,
    val newOwnerUid: String,
    val expectedRevision: Long,
)

data class Contribution(val participantId: String, val amountMinor: Long)
data class Allocation(val participantId: String, val amountMinor: Long)

enum class SplitMode { EQUAL, EXACT, PERCENTAGE, WEIGHTED }

data class Expense(
    val id: String,
    val title: String,
    val amountMinor: Long,
    val currency: String,
    val contributions: List<Contribution>,
    val allocations: List<Allocation>,
    val splitMode: SplitMode,
    val splitEntries: List<SplitEntryInput>,
    val date: String,
    val category: String,
    val note: String?,
    val createdByUid: String,
    val revision: Long,
)

data class Settlement(
    val id: String,
    val fromParticipantId: String,
    val toParticipantId: String,
    val amountMinor: Long,
    val currency: String,
    val date: String,
    val note: String?,
    val createdByUid: String,
    val revision: Long,
)

data class ParticipantBalance(
    val participantId: String,
    val amountMinor: Long,
)

data class ActivityEvent(
    val id: String,
    val type: String,
    val resourceType: String,
    val summary: String,
    val actorUid: String?,
    val resourceRevision: Long,
    val occurredAtEpochMillis: Long,
    val resourceId: String? = null,
    val subject: String? = null,
)

data class GroupDetail(
    val id: String,
    val name: String,
    val currency: String,
    val currencyExponent: Int,
    val ownerUid: String,
    val revision: Long,
    val archived: Boolean,
    val currentMemberParticipantId: String?,
    val members: List<GroupMember>,
    val participants: List<Participant>,
    val expenses: List<Expense>,
    val settlements: List<Settlement>,
    val balances: List<ParticipantBalance>,
    val activity: List<ActivityEvent>,
    val fromCache: Boolean,
    val type: GroupType = GroupType.OTHER,
)

data class InvitationSecret(
    val id: String,
    val revision: Long,
    val groupRevision: Long,
    val expiresAt: String,
    val token: String,
    val code: String,
    val secretAlreadyIssued: Boolean,
)

data class InvitationSummary(
    val id: String,
    val revision: Long,
    val createdAt: String,
    val expiresAt: String,
    val revokedAt: String?,
    val maxUses: Long,
    val useCount: Long,
)

data class ExpenseDraft(
    val ownerUid: String,
    val groupId: String,
    val expenseId: String?,
    val operationId: String,
    val expectedRevision: Long?,
    val title: String,
    val amount: String,
    val payerParticipantId: String,
    val selectedParticipantIds: Set<String>,
    val splitMode: SplitMode,
    val entryValues: Map<String, String>,
    val date: String,
    val category: String,
    val note: String,
    val updatedAtEpochMillis: Long,
)

data class ExpenseInput(
    val title: String,
    val amountMinor: Long,
    val currency: String,
    val payerParticipantId: String,
    val splitMode: SplitMode,
    val splitEntries: List<SplitEntryInput>,
    val date: String,
    val category: String,
    val note: String?,
)

data class SplitEntryInput(
    val participantId: String,
    val value: Long? = null,
)

data class SettlementInput(
    val fromParticipantId: String,
    val toParticipantId: String,
    val amountMinor: Long,
    val currency: String,
    val date: String,
    val note: String?,
)

data class MutationResult(
    val resourceId: String,
    val revision: Long,
)
