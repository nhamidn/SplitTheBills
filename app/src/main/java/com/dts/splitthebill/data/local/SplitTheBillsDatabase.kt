package com.dts.splitthebill.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ExpenseDraftEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class SplitTheBillsDatabase : RoomDatabase() {
    abstract fun expenseDraftDao(): ExpenseDraftDao
}
