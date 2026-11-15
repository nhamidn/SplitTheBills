package com.dts.splitthebill.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dts.splitthebill.data.local.ExpenseDraftDao
import com.dts.splitthebill.data.local.SplitTheBillsDatabase
import com.dts.splitthebill.data.remote.CallableTransport
import com.dts.splitthebill.data.remote.PlatformCallableTransport
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SplitTheBillsDatabase =
        Room.databaseBuilder(
            context,
            SplitTheBillsDatabase::class.java,
            "split-the-bills.db",
        ).addMigrations(MIGRATION_1_2).build()

    @Provides
    fun provideExpenseDraftDao(database: SplitTheBillsDatabase): ExpenseDraftDao =
        database.expenseDraftDao()

    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("preferences") },
        )

    @Provides
    @Singleton
    fun provideFirebaseApp(): FirebaseApp = FirebaseApp.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAuth(firebaseApp: FirebaseApp): FirebaseAuth = FirebaseAuth.getInstance(firebaseApp)

    @Provides
    @Singleton
    fun provideFirestore(firebaseApp: FirebaseApp): FirebaseFirestore = FirebaseFirestore.getInstance(firebaseApp)

    @Provides
    @Singleton
    fun provideCallableTransport(transport: PlatformCallableTransport): CallableTransport = transport
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expense_drafts ADD COLUMN ownerUid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE expense_drafts ADD COLUMN expenseId TEXT")
        db.execSQL("DELETE FROM expense_drafts WHERE ownerUid = ''")
    }
}
