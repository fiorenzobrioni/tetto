package com.callbackdev.tetto.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The settings in DataStore, file `settings` (PLANNING.md §5). Only what differs from the default
 * is stored: a field at its default has no key. A value that cannot be read back (out of range, a
 * name from a newer build) reads as its default.
 *
 * Plain storage: what a new week or month start does to the caps' open periods is
 * `CapsRepository`'s, the only caller allowed to change them.
 */
@Singleton
class UserPreferencesDataSource
@Inject
constructor(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<UserSettings> = dataStore.data
        // An unreadable file is not a reason to stop showing the caps: the defaults stand in.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map(::read)
        .distinctUntilChanged()

    suspend fun current(): UserSettings = settings.first()

    /** Applies [transform] to the stored settings atomically; returns the settings now stored. */
    suspend fun update(transform: (UserSettings) -> UserSettings): UserSettings {
        var updated = UserSettings()
        dataStore.edit { prefs ->
            val new = transform(read(prefs)).sanitized()
            write(prefs, new)
            updated = new
        }
        return updated
    }

    private fun read(prefs: Preferences): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            currency = prefs[Keys.CURRENCY]?.takeIf { AppCurrency.ofOrNull(it) != null },
            monthStartDay = prefs[Keys.MONTH_START_DAY]?.takeIf { it in 1..31 } ?: defaults.monthStartDay,
            weekStart = prefs[Keys.WEEK_START]?.let { name -> DayOfWeek.entries.firstOrNull { it.name == name } },
        )
    }

    private fun write(prefs: MutablePreferences, settings: UserSettings) {
        val defaults = UserSettings()
        prefs.putOrRemove(Keys.CURRENCY, settings.currency, defaults.currency)
        prefs.putOrRemove(Keys.MONTH_START_DAY, settings.monthStartDay, defaults.monthStartDay)
        prefs.putOrRemove(Keys.WEEK_START, settings.weekStart?.name, defaults.weekStart?.name)
    }

    private fun <T> MutablePreferences.putOrRemove(key: Preferences.Key<T>, value: T?, default: T?) {
        if (value == null || value == default) remove(key) else this[key] = value
    }

    private fun UserSettings.sanitized(): UserSettings = copy(
        currency = currency?.takeIf { AppCurrency.ofOrNull(it) != null },
        monthStartDay = monthStartDay.coerceIn(1, 31),
    )

    private object Keys {
        val CURRENCY = stringPreferencesKey("currency")
        val MONTH_START_DAY = intPreferencesKey("month_start_day")
        val WEEK_START = stringPreferencesKey("week_start")
    }

    companion object {
        /** Named in the backup's allowlist (`data_extraction_rules.xml`): renaming it drops it from the backup. */
        const val FILE_NAME = "settings"
    }
}
