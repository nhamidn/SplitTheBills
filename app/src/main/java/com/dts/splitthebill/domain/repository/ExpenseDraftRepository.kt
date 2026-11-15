package com.dts.splitthebill.domain.repository

import com.dts.splitthebill.domain.model.ExpenseDraft

interface ExpenseDraftRepository {
    suspend fun load(ownerUid: String, groupId: String, expenseId: String?): ExpenseDraft?
    suspend fun save(draft: ExpenseDraft)
    suspend fun delete(ownerUid: String, groupId: String, expenseId: String?)
    suspend fun deleteForUser(ownerUid: String)
}
