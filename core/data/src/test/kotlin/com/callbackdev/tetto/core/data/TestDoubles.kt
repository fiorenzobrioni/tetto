package com.callbackdev.tetto.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.callbackdev.tetto.core.data.time.PhoneDefaults
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** A Preferences DataStore on its own file in [directory], and the scope to cancel after the test. */
internal class TestDataStore(directory: File) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(directory, "settings.preferences_pb") },
    )
}

/** A clock the test sets: an instant and the zone in effect, both movable. */
internal class FakeToday(@Volatile var zone: ZoneId) : TodaySource {
    @Volatile var instant: Instant = Instant.EPOCH

    override fun now(): Instant = instant

    override fun zone(): ZoneId = zone

    /** Sets the clock to [local], a local date and time in the zone in effect. */
    fun at(local: String) {
        instant = LocalDateTime.parse(local).atZone(zone).toInstant()
    }
}

/** A phone in the euro area whose weeks start on Monday. */
internal class FakePhone(
    private val currency: AppCurrency = AppCurrency("EUR", 2),
    private val weekStart: DayOfWeek = DayOfWeek.MONDAY,
) : PhoneDefaults {
    override fun currency(): AppCurrency = currency

    override fun weekStart(): DayOfWeek = weekStart
}

internal fun euros(whole: Long, cents: Long = 0): Money = Money(whole * 100 + cents)

internal fun date(year: Int, month: Int, day: Int): LocalDate = LocalDate.of(year, month, day)
