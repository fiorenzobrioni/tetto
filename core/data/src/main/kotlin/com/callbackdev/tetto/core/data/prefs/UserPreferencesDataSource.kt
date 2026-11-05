package com.callbackdev.tetto.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.AppFont
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The settings in DataStore, file `settings` (PLANNING.md §5). Only what differs from the default
 * is stored: a field at its default has no key. A value that cannot be read back (out of range, a
 * name from a newer build) reads as its default.
 *
 * Plain storage: what a new week or month start, or a new currency, does to the caps is
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
            theme = enumOr(prefs[Keys.THEME], defaults.theme),
            palette = enumOr(prefs[Keys.PALETTE], defaults.palette),
            font = enumOr(prefs[Keys.FONT], defaults.font),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
            reminderOn = prefs[Keys.REMINDER_ON] ?: defaults.reminderOn,
            reminderTime = prefs[Keys.REMINDER_TIME]?.let(::timeOrNull) ?: defaults.reminderTime,
            askedNotifications = prefs[Keys.ASKED_NOTIFICATIONS] ?: defaults.askedNotifications,
        )
    }

    private fun write(prefs: MutablePreferences, settings: UserSettings) {
        val defaults = UserSettings()
        prefs.putOrRemove(Keys.CURRENCY, settings.currency, defaults.currency)
        prefs.putOrRemove(Keys.MONTH_START_DAY, settings.monthStartDay, defaults.monthStartDay)
        prefs.putOrRemove(Keys.WEEK_START, settings.weekStart?.name, defaults.weekStart?.name)
        prefs.putOrRemove(Keys.THEME, settings.theme.name, defaults.theme.name)
        prefs.putOrRemove(Keys.PALETTE, settings.palette.name, defaults.palette.name)
        prefs.putOrRemove(Keys.FONT, settings.font.name, defaults.font.name)
        prefs.putOrRemove(Keys.DYNAMIC_COLOR, settings.dynamicColor, defaults.dynamicColor)
        prefs.putOrRemove(Keys.ONBOARDING_COMPLETED, settings.onboardingCompleted, defaults.onboardingCompleted)
        prefs.putOrRemove(Keys.REMINDER_ON, settings.reminderOn, defaults.reminderOn)
        prefs.putOrRemove(Keys.REMINDER_TIME, settings.reminderTime.toString(), defaults.reminderTime.toString())
        prefs.putOrRemove(Keys.ASKED_NOTIFICATIONS, settings.askedNotifications, defaults.askedNotifications)
    }

    private fun <T> MutablePreferences.putOrRemove(key: Preferences.Key<T>, value: T?, default: T?) {
        if (value == null || value == default) remove(key) else this[key] = value
    }

    private fun UserSettings.sanitized(): UserSettings = copy(
        currency = currency?.takeIf { AppCurrency.ofOrNull(it) != null },
        monthStartDay = monthStartDay.coerceIn(1, 31),
        // A time of day to the minute: the reminder is set by one, and "20:30" reads back as it.
        reminderTime = reminderTime.withSecond(0).withNano(0),
    )

    /** "20:30", as [write] stores it; anything else reads as the default. */
    private fun timeOrNull(text: String): LocalTime? = try {
        LocalTime.parse(text)
    } catch (_: DateTimeException) {
        null
    }

    /** An enum stored by its name; a name this build does not know (a newer one's) reads as [default]. */
    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val CURRENCY = stringPreferencesKey("currency")
        val MONTH_START_DAY = intPreferencesKey("month_start_day")
        val WEEK_START = stringPreferencesKey("week_start")
        val THEME = stringPreferencesKey("theme")
        val PALETTE = stringPreferencesKey("palette")
        val FONT = stringPreferencesKey("font")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val REMINDER_ON = booleanPreferencesKey("reminder_on")

        /** The local time, as `java.time` writes one: "20:30". */
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val ASKED_NOTIFICATIONS = booleanPreferencesKey("asked_notifications")
    }

    companion object {
        /** Named in the backup's allowlist (`data_extraction_rules.xml`): renaming it drops it from the backup. */
        const val FILE_NAME = "settings"
    }
}
