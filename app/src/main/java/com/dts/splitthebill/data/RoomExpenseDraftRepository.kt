package com.dts.splitthebill.data

import com.dts.splitthebill.data.local.ExpenseDraftDao
import com.dts.splitthebill.data.local.ExpenseDraftEntity
import com.dts.splitthebill.domain.model.ExpenseDraft
import com.dts.splitthebill.domain.model.SplitMode
import com.dts.splitthebill.domain.repository.ExpenseDraftRepository
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class RoomExpenseDraftRepository @Inject constructor(
    private val dao: ExpenseDraftDao,
) : ExpenseDraftRepository {
    override suspend fun load(ownerUid: String, groupId: String, expenseId: String?): ExpenseDraft? =
        dao.get(ownerUid, groupId, expenseId)?.toDraft()

    override suspend fun save(draft: ExpenseDraft) {
        dao.upsert(
            ExpenseDraftEntity(
                draftId = draftKey(draft.ownerUid, draft.groupId, draft.expenseId),
                ownerUid = draft.ownerUid,
                groupId = draft.groupId,
                expenseId = draft.expenseId,
                payloadJson = draft.toJson().toString(),
                updatedAtEpochMillis = draft.updatedAtEpochMillis,
            ),
        )
    }

    override suspend fun delete(ownerUid: String, groupId: String, expenseId: String?) =
        dao.delete(ownerUid, groupId, expenseId)

    override suspend fun deleteForUser(ownerUid: String) = dao.deleteForUser(ownerUid)
}

private fun ExpenseDraftEntity.toDraft(): ExpenseDraft? = runCatching {
    val value = JSONObject(payloadJson)
    val selected = value.getJSONArray("selectedParticipantIds").strings().toSet()
    val entriesJson = value.getJSONObject("entryValues")
    val entries = entriesJson.keys().asSequence().associateWith(entriesJson::getString)
    ExpenseDraft(
        ownerUid = ownerUid,
        groupId = groupId,
        expenseId = expenseId,
        operationId = value.getString("operationId"),
        expectedRevision = value.optLongOrNull("expectedRevision"),
        title = value.optString("title"),
        amount = value.optString("amount"),
        payerParticipantId = value.optString("payerParticipantId"),
        selectedParticipantIds = selected,
        splitMode = SplitMode.valueOf(value.optString("splitMode", SplitMode.EQUAL.name)),
        entryValues = entries,
        date = value.optString("date"),
        category = value.optString("category", "FOOD"),
        note = value.optString("note"),
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}.getOrNull()

private fun ExpenseDraft.toJson(): JSONObject = JSONObject()
    .put("operationId", operationId)
    .put("expectedRevision", expectedRevision ?: JSONObject.NULL)
    .put("title", title)
    .put("amount", amount)
    .put("payerParticipantId", payerParticipantId)
    .put("selectedParticipantIds", JSONArray(selectedParticipantIds.sorted()))
    .put("splitMode", splitMode.name)
    .put("entryValues", JSONObject(entryValues))
    .put("date", date)
    .put("category", category)
    .put("note", note)

private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }

private fun JSONObject.optLongOrNull(name: String): Long? =
    if (isNull(name) || !has(name)) null else getLong(name)

private fun draftKey(ownerUid: String, groupId: String, expenseId: String?): String =
    "$ownerUid|$groupId|${expenseId ?: "new"}"
