package com.dts.splitthebill.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local-only, opaque editor state. It is never treated as an authoritative expense. */
@Entity(tableName = "expense_drafts")
data class ExpenseDraftEntity(
    @PrimaryKey val draftId: String,
    val ownerUid: String,
    val groupId: String,
    val expenseId: String?,
    val payloadJson: String,
    val updatedAtEpochMillis: Long,
)
