package com.dts.splitthebill.data

import com.dts.splitthebill.data.remote.CallableTransport
import com.dts.splitthebill.domain.model.ActivityEvent
import com.dts.splitthebill.domain.model.Allocation
import com.dts.splitthebill.domain.model.Contribution
import com.dts.splitthebill.domain.model.Expense
import com.dts.splitthebill.domain.model.ExpenseInput
import com.dts.splitthebill.domain.model.GroupDetail
import com.dts.splitthebill.domain.model.GroupMember
import com.dts.splitthebill.domain.model.GroupSummary
import com.dts.splitthebill.domain.model.InvitationSecret
import com.dts.splitthebill.domain.model.InvitationSummary
import com.dts.splitthebill.domain.model.MutationResult
import com.dts.splitthebill.domain.model.OwnershipCandidate
import com.dts.splitthebill.domain.model.OwnershipOptions
import com.dts.splitthebill.domain.model.OwnershipTransfer
import com.dts.splitthebill.domain.model.Participant
import com.dts.splitthebill.domain.model.ParticipantBalance
import com.dts.splitthebill.domain.model.Settlement
import com.dts.splitthebill.domain.model.SettlementInput
import com.dts.splitthebill.domain.model.SplitMode
import com.dts.splitthebill.domain.repository.GroupsRepository
import com.dts.splitthebill.data.remote.awaitResult
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class FirebaseGroupsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val callableTransport: CallableTransport,
) : GroupsRepository {
    override fun observeGroups(uid: String): Flow<List<GroupSummary>> =
        observeCollection(firestore.collection("users/$uid/groups")).mapSnapshots { snapshot ->
            snapshot.documents.mapNotNull(::groupSummary).sortedBy { it.name.lowercase() }
        }

    override fun observeGroup(groupId: String, uid: String): Flow<GroupDetail> {
        val group = firestore.document("groups/$groupId")
        return combine(
            observeDocument(group),
            observeDocument(group.collection("members").document(uid)),
            observeCollection(group.collection("members")),
            observeCollection(group.collection("participants")),
            observeCollection(group.collection("expenses")),
            observeCollection(group.collection("settlements")),
            observeCollection(group.collection("balances")),
            observeCollection(group.collection("activity")),
        ) { values ->
            val root = values[0] as DocumentSnapshot
            val member = values[1] as DocumentSnapshot
            val members = (values[2] as QuerySnapshot).documents.mapNotNull(::member)
                .sortedWith(compareBy<GroupMember> { it.role != "OWNER" }.thenBy { it.uid })
            val participants = (values[3] as QuerySnapshot).documents.mapNotNull(::participant)
                .sortedBy { it.displayName.lowercase() }
            val expenses = (values[4] as QuerySnapshot).documents.mapNotNull(::expense)
                .sortedWith(compareByDescending<Expense> { it.date }.thenBy { it.id })
            val settlements = (values[5] as QuerySnapshot).documents.mapNotNull(::settlement)
                .sortedWith(compareByDescending<Settlement> { it.date }.thenBy { it.id })
            val balances = (values[6] as QuerySnapshot).documents.map {
                ParticipantBalance(it.id, it.getLong("amountMinor") ?: 0L)
            }
            val activity = (values[7] as QuerySnapshot).documents.mapNotNull(::activity)
                .sortedByDescending { it.occurredAtEpochMillis }
            GroupDetail(
                id = root.id,
                name = root.getString("name").orEmpty(),
                currency = root.getString("currency").orEmpty(),
                currencyExponent = (root.getLong("currencyExponent") ?: 2L).toInt(),
                ownerUid = root.getString("ownerUid").orEmpty(),
                revision = root.getLong("revision") ?: 0L,
                archived = root.getTimestamp("archivedAt") != null,
                currentMemberParticipantId = member.getString("participantId"),
                members = members,
                participants = participants,
                expenses = expenses,
                settlements = settlements,
                balances = balances,
                activity = activity,
                fromCache = values.any { snapshot ->
                    when (snapshot) {
                        is DocumentSnapshot -> snapshot.metadata.isFromCache
                        is QuerySnapshot -> snapshot.metadata.isFromCache
                        else -> false
                    }
                },
            )
        }
    }

    override suspend fun createGroup(name: String, currency: String, participantName: String) =
        call("v1CreateGroup", mapOf("name" to name, "currency" to currency, "participantName" to participantName))

    override suspend fun createParticipant(groupId: String, name: String) =
        call("v1CreateParticipant", mapOf("groupId" to groupId, "name" to name))

    override suspend fun createExpense(groupId: String, expense: ExpenseInput, operationId: String) =
        call("v1CreateExpense", mapOf("groupId" to groupId, "expense" to expensePayload(expense)), operationId)

    override suspend fun updateExpense(
        groupId: String,
        expenseId: String,
        expectedRevision: Long,
        expense: ExpenseInput,
        operationId: String,
    ) = call(
        "v1UpdateExpense",
        mapOf(
            "groupId" to groupId,
            "expenseId" to expenseId,
            "expectedRevision" to expectedRevision,
            "expense" to expensePayload(expense),
        ),
        operationId,
    )

    override suspend fun deleteExpense(groupId: String, expenseId: String, expectedRevision: Long) =
        call(
            "v1DeleteExpense",
            mapOf("groupId" to groupId, "expenseId" to expenseId, "expectedRevision" to expectedRevision),
        )

    override suspend fun createSettlement(groupId: String, settlement: SettlementInput) =
        call(
            "v1CreateSettlement",
            mapOf(
                "groupId" to groupId,
                "settlement" to mapOf(
                    "fromParticipantId" to settlement.fromParticipantId,
                    "toParticipantId" to settlement.toParticipantId,
                    "amountMinor" to settlement.amountMinor,
                    "currency" to settlement.currency,
                    "date" to settlement.date,
                    "note" to settlement.note,
                ),
            ),
        )

    override suspend fun createInvitation(
        groupId: String,
        expiresInSeconds: Long,
        maxUses: Long,
        operationId: String,
    ): InvitationSecret {
        val data = callData(
            "v1CreateInvitation",
            mapOf("groupId" to groupId, "expiresInSeconds" to expiresInSeconds, "maxUses" to maxUses),
            operationId,
        )
        return InvitationSecret(
            id = data.requiredString("resourceId"),
            revision = data.requiredLong("revision"),
            groupRevision = data.requiredLong("groupRevision"),
            expiresAt = data.requiredString("expiresAt"),
            token = data["token"] as? String ?: "",
            code = data["code"] as? String ?: "",
            secretAlreadyIssued = data["secretAlreadyIssued"] as? Boolean ?: false,
        )
    }

    override suspend fun listInvitations(groupId: String): List<InvitationSummary> {
        val data = callData("v1ListInvitations", mapOf("groupId" to groupId))
        return (data["invitations"] as? List<*>)?.mapNotNull { row ->
            val value = row as? Map<*, *> ?: return@mapNotNull null
            InvitationSummary(
                id = value["invitationId"] as? String ?: return@mapNotNull null,
                revision = (value["revision"] as? Number)?.toLong() ?: return@mapNotNull null,
                createdAt = value["createdAt"] as? String ?: "",
                expiresAt = value["expiresAt"] as? String ?: "",
                revokedAt = value["revokedAt"] as? String,
                maxUses = (value["maxUses"] as? Number)?.toLong() ?: 0,
                useCount = (value["useCount"] as? Number)?.toLong() ?: 0,
            )
        }.orEmpty()
    }

    override suspend fun revokeInvitation(
        groupId: String,
        invitationId: String,
        expectedRevision: Long,
        operationId: String,
    ) = call(
        "v1RevokeInvitation",
        mapOf("groupId" to groupId, "invitationId" to invitationId, "expectedRevision" to expectedRevision),
        operationId,
    )

    override suspend fun redeemInvitation(token: String?, code: String?, operationId: String) =
        call(
            "v1RedeemInvitation",
            buildMap {
                token?.let { put("token", it) }
                code?.let { put("code", it) }
            },
            operationId,
        )

    override suspend fun approveParticipantLink(
        groupId: String,
        participantId: String,
        memberUid: String,
        expectedRevision: Long,
        operationId: String,
    ) = call(
        "v1ApproveParticipantLink",
        mapOf(
            "groupId" to groupId,
            "participantId" to participantId,
            "memberUid" to memberUid,
            "expectedRevision" to expectedRevision,
        ),
        operationId,
    )

    override suspend fun ownershipOptions(groupId: String): OwnershipOptions {
        val group = firestore.document("groups/$groupId")
        val root = group.get().awaitResult()
        val members = group.collection("members").get().awaitResult().documents
        val participants = group.collection("participants").get().awaitResult().documents
        val participantNames = participants.associate { it.id to it.getString("displayName") }
        val candidates = members.mapNotNull { document ->
            val uid = document.getString("uid") ?: return@mapNotNull null
            OwnershipCandidate(uid, document.getString("participantId")?.let(participantNames::get))
        }.sortedWith(compareBy<OwnershipCandidate> { it.displayName ?: it.uid }.thenBy { it.uid })
        return OwnershipOptions(root.getLong("revision") ?: 0L, candidates)
    }

    override suspend fun deleteAccount(transfers: List<OwnershipTransfer>, operationId: String) = call(
        "v1DeleteAccount",
        mapOf(
            "confirmation" to "DELETE",
            "ownershipTransfers" to transfers.map {
                mapOf(
                    "groupId" to it.groupId,
                    "newOwnerUid" to it.newOwnerUid,
                    "expectedRevision" to it.expectedRevision,
                )
            },
        ),
        operationId,
    )

    override suspend fun clearLocalCache() {
        firestore.terminate().awaitResult()
        firestore.clearPersistence().awaitResult()
    }

    private suspend fun call(
        name: String,
        body: Map<String, Any?>,
        operationId: String = UUID.randomUUID().toString(),
    ): MutationResult {
        val data = callData(name, body, operationId)
        return MutationResult(
            resourceId = data.requiredString("resourceId"),
            revision = data.requiredLong("revision"),
        )
    }

    private suspend fun callData(
        name: String,
        body: Map<String, Any?>,
        operationId: String = UUID.randomUUID().toString(),
    ): Map<*, *> {
        val request = buildMap<String, Any?> {
            put("apiVersion", "v1")
            put("operationId", operationId)
            putAll(body)
        }
        return callableTransport.call(name, request)
    }

    private fun expensePayload(expense: ExpenseInput): Map<String, Any?> = mapOf(
        "title" to expense.title,
        "amountMinor" to expense.amountMinor,
        "currency" to expense.currency,
        "contributions" to listOf(
            mapOf("participantId" to expense.payerParticipantId, "amountMinor" to expense.amountMinor),
        ),
        "split" to mapOf(
            "mode" to expense.splitMode.name,
            "entries" to expense.splitEntries.map { entry ->
                buildMap<String, Any> {
                    put("participantId", entry.participantId)
                    entry.value?.let { value ->
                        put(
                            when (expense.splitMode) {
                                SplitMode.EXACT -> "amountMinor"
                                SplitMode.PERCENTAGE -> "basisPoints"
                                SplitMode.WEIGHTED -> "shares"
                                SplitMode.EQUAL -> return@let
                            },
                            value,
                        )
                    }
                }
            },
        ),
        "date" to expense.date,
        "category" to expense.category,
        "note" to expense.note,
    )

    private fun groupSummary(document: DocumentSnapshot): GroupSummary? {
        val name = document.getString("name") ?: return null
        return GroupSummary(
            id = document.id,
            name = name,
            currency = document.getString("currency").orEmpty(),
            role = document.getString("role").orEmpty(),
            participantId = document.getString("participantId"),
            revision = document.getLong("groupRevision") ?: 0L,
            archived = document.getTimestamp("archivedAt") != null,
        )
    }

    private fun participant(document: DocumentSnapshot): Participant? {
        val name = document.getString("displayName") ?: return null
        return Participant(
            id = document.id,
            displayName = name,
            linkedUid = document.getString("linkedUid"),
            active = document.getString("status") == "ACTIVE",
        )
    }

    private fun member(document: DocumentSnapshot): GroupMember? {
        val uid = document.getString("uid") ?: return null
        return GroupMember(
            uid = uid,
            role = document.getString("role").orEmpty(),
            participantId = document.getString("participantId"),
        )
    }

    private fun expense(document: DocumentSnapshot): Expense? {
        if (document.getTimestamp("deletedAt") != null) return null
        val splitInput = document.get("splitInput") as? Map<*, *>
        val splitMode = runCatching {
            SplitMode.valueOf((splitInput?.get("mode") as? String).orEmpty())
        }.getOrDefault(SplitMode.EQUAL)
        return Expense(
            id = document.id,
            title = document.getString("title") ?: return null,
            amountMinor = document.getLong("amountMinor") ?: return null,
            currency = document.getString("currency").orEmpty(),
            contributions = mapMoneyList(document.get("contributions")) { id, amount -> Contribution(id, amount) },
            allocations = mapMoneyList(document.get("allocations")) { id, amount -> Allocation(id, amount) },
            splitMode = splitMode,
            splitEntries = mapSplitEntries(splitInput?.get("entries"), splitMode),
            date = document.getString("date").orEmpty(),
            category = document.getString("category").orEmpty(),
            note = document.getString("note"),
            createdByUid = document.getString("createdByUid").orEmpty(),
            revision = document.getLong("revision") ?: 1L,
        )
    }

    private fun settlement(document: DocumentSnapshot): Settlement? {
        if (document.getTimestamp("deletedAt") != null) return null
        return Settlement(
            id = document.id,
            fromParticipantId = document.getString("fromParticipantId") ?: return null,
            toParticipantId = document.getString("toParticipantId") ?: return null,
            amountMinor = document.getLong("amountMinor") ?: return null,
            currency = document.getString("currency").orEmpty(),
            date = document.getString("date").orEmpty(),
            note = document.getString("note"),
            createdByUid = document.getString("createdByUid").orEmpty(),
            revision = document.getLong("revision") ?: 1L,
        )
    }

    private fun activity(document: DocumentSnapshot): ActivityEvent? {
        val type = document.getString("type") ?: return null
        val summary = document.get("summary") as? Map<*, *>
        val description = summary?.get("title")?.toString()
            ?: summary?.get("name")?.toString()
            ?: type.lowercase().replace('_', ' ')
        return ActivityEvent(
            id = document.id,
            type = type,
            resourceType = document.getString("resourceType").orEmpty(),
            summary = description,
            actorUid = document.getString("actorUid"),
            resourceRevision = document.getLong("resourceRevision") ?: 0L,
            occurredAtEpochMillis = (document.get("occurredAt") as? Timestamp)?.toDate()?.time ?: 0L,
        )
    }

    private fun mapSplitEntries(value: Any?, mode: SplitMode): List<com.dts.splitthebill.domain.model.SplitEntryInput> =
        (value as? List<*>)?.mapNotNull { row ->
            val map = row as? Map<*, *> ?: return@mapNotNull null
            val id = map["participantId"] as? String ?: return@mapNotNull null
            val rawValue = when (mode) {
                SplitMode.EQUAL -> null
                SplitMode.EXACT -> map["amountMinor"]
                SplitMode.PERCENTAGE -> map["basisPoints"]
                SplitMode.WEIGHTED -> map["shares"]
            }
            com.dts.splitthebill.domain.model.SplitEntryInput(id, (rawValue as? Number)?.toLong())
        }.orEmpty()

    private inline fun <T> mapMoneyList(
        value: Any?,
        transform: (String, Long) -> T,
    ): List<T> = (value as? List<*>)?.mapNotNull { row ->
        val map = row as? Map<*, *> ?: return@mapNotNull null
        val id = map["participantId"] as? String ?: return@mapNotNull null
        val amount = (map["amountMinor"] as? Number)?.toLong() ?: return@mapNotNull null
        transform(id, amount)
    }.orEmpty()
}

private fun Map<*, *>.requiredString(key: String): String =
    this[key] as? String ?: error("Missing $key")

private fun Map<*, *>.requiredLong(key: String): Long =
    (this[key] as? Number)?.toLong() ?: error("Missing $key")

private fun <T, R> Flow<T>.mapSnapshots(transform: (T) -> R): Flow<R> =
    map(transform)

private fun observeDocument(reference: DocumentReference): Flow<DocumentSnapshot> = callbackFlow {
    val registration = reference.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
        when {
            error != null -> close(error)
            snapshot != null -> trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}

private fun observeCollection(reference: CollectionReference): Flow<QuerySnapshot> = callbackFlow {
    val registration = reference.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
        when {
            error != null -> close(error)
            snapshot != null -> trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}
