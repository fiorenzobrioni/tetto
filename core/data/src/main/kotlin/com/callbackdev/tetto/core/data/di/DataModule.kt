package com.callbackdev.tetto.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.callbackdev.tetto.core.data.db.TettoDatabase
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.data.time.PhoneDefaults
import com.callbackdev.tetto.core.data.time.TodaySource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    /** Never `fallbackToDestructiveMigration`: the database is a contract (PLANNING.md §5). */
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): TettoDatabase =
        Room.databaseBuilder(context, TettoDatabase::class.java, TettoDatabase.NAME).build()

    /**
     * One instance per process, as DataStore requires. A corrupt file is replaced by an empty one:
     * the reader loses their settings, not their caps.
     */
    @Provides
    @Singleton
    fun preferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile(UserPreferencesDataSource.FILE_NAME) },
        )

    @Provides
    fun todaySource(): TodaySource = TodaySource.System

    @Provides
    fun phoneDefaults(): PhoneDefaults = PhoneDefaults.System
}
