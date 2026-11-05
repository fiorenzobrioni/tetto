package com.callbackdev.tetto.core.domain.backup

import com.callbackdev.tetto.core.domain.EUR
import com.callbackdev.tetto.core.domain.JPY
import com.callbackdev.tetto.core.domain.KWD
import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId

/** The spends for a spreadsheet (PLANNING.md §5, §11 Phase 4). */
class CsvExportTest {
    private val rome = ZoneId.of("Europe/Rome")

    private fun lines(csv: String): List<String> = csv.removePrefix(Char(0xFEFF).toString()).split("\r\n").dropLast(1)

    @Test
    fun `one row per spend of every period, oldest first, under a fixed header`() {
        val csv = CsvExport.spends(sampleBackup(), DayOfWeek.MONDAY, rome)
        assertThat(csv).startsWith(Char(0xFEFF).toString())
        assertThat(csv).endsWith("\r\n")
        assertThat(lines(csv)).containsExactly(
            "date,time,cap,amount,currency,period_start,period_end",
            "2026-10-20,18:00,\"Groceries, market\",330.00,EUR,2026-09-27,2026-10-26",
            "2026-10-28,08:30,Coffee,16.00,EUR,2026-10-26,2026-11-01",
            "2026-11-01,11:00,\"Groceries, market\",42.05,EUR,2026-10-27,2026-11-26",
            "2026-11-03,08:15,Coffee,1.30,EUR,2026-11-02,2026-11-08",
            "2026-11-05,16:40,Coffee,2.70,EUR,2026-11-02,2026-11-08",
        ).inOrder()
    }

    @Test
    fun `times are the phone's zone now, the date with them`() {
        val csv = CsvExport.spends(sampleBackup(), DayOfWeek.MONDAY, ZoneId.of("America/New_York"))
        // 08:15 in Rome is 02:15 in New York, the same day; 18:00 on 20 Oct is noon there.
        assertThat(lines(csv)).contains("2026-11-03,02:15,Coffee,1.30,EUR,2026-11-02,2026-11-08")
        assertThat(lines(csv)).contains("2026-10-20,12:00,\"Groceries, market\",330.00,EUR,2026-09-27,2026-10-26")
    }

    @Test
    fun `a week that follows the phone ends where the phone's week does`() {
        val backup = sampleBackup().let { it.copy(settings = it.settings.copy(weekStart = null)) }
        val csv = CsvExport.spends(backup, DayOfWeek.SUNDAY, rome)
        // Monday 2 Nov opened the period; a Sunday week ends it on Saturday 7 Nov.
        assertThat(lines(csv)).contains("2026-11-03,08:15,Coffee,1.30,EUR,2026-11-02,2026-11-07")
    }

    @Test
    fun `amounts in the currency's whole unit, with its decimals`() {
        val coffee = sampleBackup().caps[0]
        fun one(currency: AppCurrency, minor: Long): String {
            val backup = sampleBackup().copy(
                currency = currency,
                caps = listOf(coffee.copy(spends = listOf(coffee.spends[1].copy(amount = Money(minor))))),
            )
            return lines(CsvExport.spends(backup, DayOfWeek.MONDAY, rome))[1].split(",")[3]
        }
        assertThat(one(JPY, 1250)).isEqualTo("1250")
        assertThat(one(KWD, 1250)).isEqualTo("1.250")
        assertThat(one(EUR, 5)).isEqualTo("0.05")
        assertThat(one(EUR, 123_456_789_00)).isEqualTo("123456789.00")
    }

    @Test
    fun `a name is quoted where it must be, and never a formula`() {
        val coffee = sampleBackup().caps[0]
        fun named(name: String): String {
            val backup = sampleBackup().copy(caps = listOf(coffee.copy(name = name, spends = coffee.spends.take(1))))
            return lines(CsvExport.spends(backup, DayOfWeek.MONDAY, rome))[1].split(",", limit = 3)[2]
        }
        assertThat(named("Caffè")).startsWith("Caffè,")
        assertThat(named("Say \"hi\"")).startsWith("\"Say \"\"hi\"\"\",")
        assertThat(named("=SUM(A1)")).startsWith("'=SUM(A1),")
        assertThat(named("-20 a week")).startsWith("'-20 a week,")
        assertThat(named("@home")).startsWith("'@home,")
    }

    @Test
    fun `no spends is a header alone`() {
        val backup = sampleBackup().let { b -> b.copy(caps = b.caps.map { it.copy(spends = emptyList()) }) }
        assertThat(lines(CsvExport.spends(backup, DayOfWeek.MONDAY, rome))).hasSize(1)
    }

    @Test
    fun `a spend in a run of folded periods takes the run's end`() {
        val coffee = sampleBackup().caps[0]
        val folded = coffee.closed[0].copy(
            range = DateRange(date(2026, 10, 12), date(2026, 11, 2)),
            periods = 3,
        )
        val stray = BackupSpend(Money(100), rome("2026-10-20T09:00"), date(2026, 10, 19))
        val backup = sampleBackup().copy(caps = listOf(coffee.copy(closed = listOf(folded), spends = listOf(stray))))
        assertThat(lines(CsvExport.spends(backup, DayOfWeek.MONDAY, rome))[1])
            .isEqualTo("2026-10-20,09:00,Coffee,1.00,EUR,2026-10-19,2026-11-01")
    }

    @Test
    fun `the file name is dated`() {
        assertThat(CsvExport.fileName(date(2026, 11, 5))).isEqualTo("tetto-spends-2026-11-05.csv")
    }
}
