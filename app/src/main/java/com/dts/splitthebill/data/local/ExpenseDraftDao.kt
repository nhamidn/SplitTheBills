package com.dts.splitthebill.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDraftDao {
    @Query("SELECT * FROM expense_drafts ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<ExpenseDraftEntity>>

    @Query(
        """SELECT * FROM expense_drafts
           WHERE ownerUid = :ownerUid AND groupId = :groupId
           AND ((expenseId IS NULL AND :expenseId IS NULL) OR expenseId = :expenseId)
           LIMIT 1""",
    )
    suspend fun get(ownerUid: String, groupId: String, expenseId: String?): ExpenseDraftEntity?

    @Upsert
    suspend fun upsert(draft: ExpenseDraftEntity)

    @Query("DELETE FROM expense_drafts WHERE draftId = :draftId")
    suspend fun delete(draftId: String)

    @Query(
        """DELETE FROM expense_drafts
           WHERE ownerUid = :ownerUid AND groupId = :groupId
           AND ((expenseId IS NULL AND :expenseId IS NULL) OR expenseId = :expenseId)""",
    )
    suspend fun delete(ownerUid: String, groupId: String, expenseId: String?)

    @Query("DELETE FROM expense_drafts WHERE ownerUid = :ownerUid")
    suspend fun deleteForUser(ownerUid: String)

    @Query("DELETE FROM expense_drafts")
    suspend fun deleteAll()
}
