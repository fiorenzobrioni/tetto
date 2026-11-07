package com.callbackdev.tetto.core.domain.backup

import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId

/**
 * The one-off cap in the backup file and the CSV (owner, 6 Nov 2026; PLANNING.md §11 Phase 6b): its
 * end travels as an added field, the version stays 1, and a file that says something no Tetto
 * could have written of one is damaged.
 */
class OneOffBackupTest {
    /** The sample, with a Lisbon trip still open and a Paris weekend that ended. */
    private fun withOneOffs(): Backup {
        val base = sampleBackup()
        return base.copy(
            caps = base.caps + listOf(
                BackupCap(
                    name = "Lisbon trip",
                    icon = CapIcon.TRAVEL,
                    amount = euros(800),
                    kind = PeriodKind.ONE_OFF,
                    reserve = Money.ZERO,
                    openStart = date(2026, 11, 4),
                    createdAt = rome("2026-11-01T10:00"),
                    spends = listOf(BackupSpend(euros(120), rome("2026-11-04T09:00"), date(2026, 11, 4))),
                    closed = emptyList(),
                    endExclusive = date(2026, 11, 15),
                ),
                BackupCap(
                    name = "Paris weekend",
                    icon = CapIcon.TRAVEL,
                    amount = euros(300),
                    kind = PeriodKind.ONE_OFF,
                    reserve = Money.ZERO,
                    openStart = date(2026, 10, 26),
                    createdAt = rome("2026-10-20T10:00"),
                    spends = listOf(BackupSpend(euros(320), rome("2026-10-24T20:00"), date(2026, 10, 23))),
                    closed = listOf(
                        BackupClosed(
                            range = DateRange(date(2026, 10, 23), date(2026, 10, 26)),
                            cap = euros(300),
                            spent = euros(320),
                            reserveBefore = Money.ZERO,
                            reserveAfter = Money.ZERO,
                            forgiven = euros(20),
                            periods = 1,
                        ),
                    ),
                    endExclusive = date(2026, 10, 26),
                ),
            ),
        )
    }

    private fun ok(text: String): Backup = (BackupCodec.decode(text) as BackupRead.Ok).backup

    @Test
    fun `a one-off's end travels, open or ended, and the version stays the first`() {
        val backup = withOneOffs()
        val text = BackupCodec.encode(backup)
        assertThat(text).contains("\"version\": 1")
        assertThat(text).contains("\"period\": \"ONE_OFF\"")
        assertThat(text).contains("\"endExclusive\": \"2026-11-15\"")
        assertThat(ok(text)).isEqualTo(backup)
    }

    @Test
    fun `a file from before one-off caps still reads, its caps without an end`() {
        val text = BackupCodec.encode(sampleBackup())
        assertThat(ok(text).caps.map { it.endExclusive }).containsExactly(null, null)
    }

    @Test
    fun `a one-off that says what no Tetto writes is damaged`() {
        val text = BackupCodec.encode(withOneOffs())
        val damaged = listOf(
            // A one-off without its end, an end on a weekly cap.
            text.replace("\"endExclusive\": \"2026-11-15\"", "\"other\": \"2026-11-15\""),
            text.replaceFirst("\"period\": \"WEEK\",", "\"period\": \"WEEK\", \"endExclusive\": \"2026-11-09\","),
            // Starting after its end, or with a reserve.
            text.replace("\"endExclusive\": \"2026-11-15\"", "\"endExclusive\": \"2026-11-03\""),
            text.replace(
                "\"amount\": 80000,\n      \"period\": \"ONE_OFF\",\n      \"reserve\": 0",
                "\"amount\": 80000,\n      \"period\": \"ONE_OFF\",\n      \"reserve\": 500",
            ),
        )
        damaged.forEach { assertThat(BackupCodec.decode(it)).isEqualTo(BackupRead.Damaged) }
        assertThat(damaged[3]).isNotEqualTo(text)
    }

    @Test
    fun `the CSV gives a one-off's spends its own dates, open or ended`() {
        val csv = CsvExport.spends(withOneOffs(), DayOfWeek.MONDAY, ZoneId.of("Europe/Rome"))
        val rows = csv.removePrefix(Char(0xFEFF).toString()).split("\r\n")
        assertThat(rows).contains("2026-11-04,09:00,Lisbon trip,120.00,EUR,2026-11-04,2026-11-14")
        assertThat(rows).contains("2026-10-24,20:00,Paris weekend,320.00,EUR,2026-10-23,2026-10-25")
    }
}
