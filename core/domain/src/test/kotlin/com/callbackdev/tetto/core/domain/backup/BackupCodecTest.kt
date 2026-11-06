package com.callbackdev.tetto.core.domain.backup

import com.callbackdev.tetto.core.domain.JPY
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapsLayout
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/** The backup file (PLANNING.md §5, §11 Phase 4): the round trip, and every way a file can be refused. */
class BackupCodecTest {
    private fun ok(text: String): Backup {
        val read = BackupCodec.decode(text)
        assertThat(read).isInstanceOf(BackupRead.Ok::class.java)
        return (read as BackupRead.Ok).backup
    }

    /** The sample's file with [from] replaced by [to], once: a file edited by hand. */
    private fun edited(from: String, to: String): String {
        val text = BackupCodec.encode(sampleBackup())
        assertThat(text).contains(from)
        return text.replaceFirst(from, to)
    }

    @Test
    fun `the round trip gives back every cap, spend, closed line and setting`() {
        val backup = sampleBackup()
        assertThat(ok(BackupCodec.encode(backup))).isEqualTo(backup)
    }

    @Test
    fun `the file says what it is, in words a person can read`() {
        val text = BackupCodec.encode(sampleBackup())
        assertThat(text).contains("\"format\": \"tetto-backup\"")
        assertThat(text).contains("\"version\": 1")
        assertThat(text).contains("\"currency\": \"EUR\"")
        assertThat(text).contains("\"decimals\": 2")
        assertThat(text).contains("\"openStart\": \"2026-11-02\"")
        assertThat(text).contains("\"reminderTime\": \"21:00\"")
        assertThat(text).contains("\"amount\": 4205")
        // What belongs to one phone does not travel.
        assertThat(text).doesNotContain("onboarding")
        assertThat(text).doesNotContain("asked")
    }

    @Test
    fun `the caps keep the reader's order and their spends come oldest first`() {
        val backup = sampleBackup()
        val shuffled = backup.copy(caps = backup.caps.map { it.copy(spends = it.spends.reversed()) })
        val read = ok(BackupCodec.encode(shuffled))
        assertThat(read.caps.map { it.name }).containsExactly("Coffee", "Groceries, market").inOrder()
        assertThat(
            read.caps[0].spends.map {
                it.amount
            },
        ).containsExactly(euros(16), euros(1, 30), euros(2, 70)).inOrder()
    }

    @Test
    fun `a week that follows the phone stays the phone's`() {
        val backup = sampleBackup().let { it.copy(settings = it.settings.copy(weekStart = null)) }
        assertThat(ok(BackupCodec.encode(backup)).settings.weekStart).isNull()
    }

    @Test
    fun `no caps is a backup too`() {
        val backup = sampleBackup().copy(caps = emptyList())
        assertThat(ok(BackupCodec.encode(backup)).caps).isEmpty()
    }

    @Test
    fun `a file cut short is damaged`() {
        val text = BackupCodec.encode(sampleBackup())
        assertThat(BackupCodec.decode(text.substring(0, text.length / 2))).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `a file from a newer Tetto asks for an update`() {
        assertThat(BackupCodec.decode(edited("\"version\": 1", "\"version\": 2"))).isEqualTo(BackupRead.TooNew(2))
    }

    @Test
    fun `anything else is not a Tetto backup`() {
        assertThat(BackupCodec.decode("")).isEqualTo(BackupRead.NotTetto)
        assertThat(BackupCodec.decode("date,time,cap\r\n")).isEqualTo(BackupRead.NotTetto)
        assertThat(BackupCodec.decode("[1, 2, 3]")).isEqualTo(BackupRead.NotTetto)
        assertThat(BackupCodec.decode("""{"format": "other-backup", "version": 1}""")).isEqualTo(BackupRead.NotTetto)
        assertThat(BackupCodec.decode("""{"version": 1, "caps": []}""")).isEqualTo(BackupRead.NotTetto)
    }

    @Test
    fun `a Tetto file without a version, or below the first, is damaged`() {
        assertThat(BackupCodec.decode("""{"format": "tetto-backup"}""")).isEqualTo(BackupRead.Damaged)
        assertThat(BackupCodec.decode("""{"format": "tetto-backup", "version": 0}""")).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `a byte-order mark in front changes nothing`() {
        val text = Char(0xFEFF) + BackupCodec.encode(sampleBackup())
        assertThat(ok(text)).isEqualTo(sampleBackup())
    }

    @Test
    fun `a field this version does not know is skipped`() {
        val read = ok(edited("\"format\": \"tetto-backup\",", "\"format\": \"tetto-backup\", \"note\": \"from 1.4\","))
        assertThat(read).isEqualTo(sampleBackup())
    }

    @Test
    fun `an icon this version does not know is the other one, and a palette the default`() {
        val read = ok(edited("\"icon\": \"coffee\"", "\"icon\": \"teapot\"").replace("\"PAPER\"", "\"NEON\""))
        assertThat(read.caps[0].icon).isEqualTo(CapIcon.OTHER)
        assertThat(read.settings.palette).isEqualTo(AppPalette.VIVID)
    }

    @Test
    fun `a file from before the caps' layout was a setting opens on the tiles`() {
        val read = ok(edited("\"capsLayout\": \"LIST\",", ""))
        assertThat(read.settings.capsLayout).isEqualTo(CapsLayout.TILES)
        assertThat(read.copy(settings = read.settings.copy(capsLayout = CapsLayout.LIST))).isEqualTo(sampleBackup())
    }

    @Test
    fun `a time that cannot be read is the default one`() {
        assertThat(
            ok(edited("\"21:00\"", "\"late\"")).settings.reminderTime,
        ).isEqualTo(UserSettings.DEFAULT_REMINDER_TIME)
    }

    @Test
    fun `a value that decides a figure or a period is never guessed`() {
        val damaged = listOf(
            // A period kind no Tetto has: a newer one would have raised the version.
            edited("\"period\": \"WEEK\"", "\"period\": \"FORTNIGHT\""),
            edited("\"openStart\": \"2026-11-02\"", "\"openStart\": \"2 Nov\""),
            edited("\"amount\": 2000", "\"amount\": 0"),
            // A spend of nothing (below zero is a refund, and reads).
            edited("\"amount\": 130", "\"amount\": 0"),
            edited("\"reserve\": 400", "\"reserve\": -400"),
            edited("\"name\": \"Coffee\"", "\"name\": \"  \""),
            edited("\"monthStartDay\": 27", "\"monthStartDay\": 32"),
            edited("\"weekStart\": \"MONDAY\"", "\"weekStart\": \"MOONDAY\""),
            edited("\"currency\": \"EUR\"", "\"currency\": \"EURO\""),
            edited("\"amount\": 2000", "\"amount\": \"twenty\""),
            // A spend in a period after the open one, a closed line reaching into it, one twice.
            edited("\"periodStart\": \"2026-11-02\"", "\"periodStart\": \"2026-11-09\""),
            edited("\"endExclusive\": \"2026-11-02\"", "\"endExclusive\": \"2026-11-03\""),
            edited("\"endExclusive\": \"2026-11-02\"", "\"endExclusive\": \"2026-10-26\""),
            edited("\"periods\": 1", "\"periods\": 0"),
            edited("\"forgiven\": 3000", "\"forgiven\": -3000"),
        )
        damaged.forEach { assertThat(BackupCodec.decode(it)).isEqualTo(BackupRead.Damaged) }
    }

    @Test
    fun `a closed period written twice is damaged`() {
        val backup = sampleBackup()
        val coffee = backup.caps[0]
        val twice = backup.copy(caps = listOf(coffee.copy(closed = coffee.closed + coffee.closed)))
        assertThat(BackupCodec.decode(BackupCodec.encode(twice))).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `figures too large for any sum are damaged, not a crash later`() {
        val big = Long.MAX_VALUE / 2
        val backup = sampleBackup()
        val coffee = backup.caps[0]
        val huge = backup.copy(
            caps = listOf(coffee.copy(spends = coffee.spends.map { it.copy(amount = Money(big)) })),
        )
        assertThat(BackupCodec.decode(BackupCodec.encode(huge))).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `the week the open periods were made by travels, and a file without it has none`() {
        assertThat(BackupCodec.encode(sampleBackup())).contains("\"weekInForce\": \"MONDAY\"")
        assertThat(ok(edited("\"weekInForce\": \"MONDAY\",", "")).weekInForce).isNull()
        assertThat(BackupCodec.decode(edited("\"weekInForce\": \"MONDAY\"", "\"weekInForce\": \"MOONDAY\"")))
            .isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `an amount the numpad could not type back is damaged, not a crash in the editor`() {
        val backup = sampleBackup()
        val coffee = backup.caps[0]
        val tenDigits = Money(1_000_000_000_00)
        val bigCap = backup.copy(caps = listOf(coffee.copy(amount = tenDigits)))
        assertThat(BackupCodec.decode(BackupCodec.encode(bigCap))).isEqualTo(BackupRead.Damaged)
        val bigSpend = backup.copy(
            caps = listOf(coffee.copy(spends = listOf(coffee.spends[0].copy(amount = tenDigits)))),
        )
        assertThat(BackupCodec.decode(BackupCodec.encode(bigSpend))).isEqualTo(BackupRead.Damaged)
        // The largest the numpad types is an amount like any other.
        val largest = backup.copy(caps = listOf(coffee.copy(amount = Money(999_999_999_99))))
        assertThat(ok(BackupCodec.encode(largest)).caps[0].amount).isEqualTo(Money(999_999_999_99))
    }

    @Test
    fun `a file whose currency had other decimals is read in this phone's, rescaled`() {
        // Written where the yen was counted in hundredths: ¥1,250.40 as 125040.
        val backup = sampleBackup().copy(currency = JPY, settings = UserSettings(currency = "JPY"))
        val text = BackupCodec.encode(backup).replace("\"decimals\": 0", "\"decimals\": 2")
        val read = ok(text)
        assertThat(read.currency).isEqualTo(JPY)
        // €20.00 as 2000 hundredths of a yen is ¥20; 130 hundredths is ¥1.30, ¥1.
        assertThat(read.caps[0].amount).isEqualTo(Money(20))
        assertThat(read.caps[0].spends[1].amount).isEqualTo(Money(1))
    }

    @Test
    fun `a file without its decimals takes its currency's own`() {
        val read = ok(edited("\"decimals\": 2,", ""))
        assertThat(read).isEqualTo(sampleBackup())
        assertThat(read.currency).isEqualTo(AppCurrency("EUR", 2))
    }

    @Test
    fun `the file name is dated`() {
        assertThat(BackupCodec.fileName(LocalDate.of(2026, 11, 5))).isEqualTo("tetto-backup-2026-11-05.json")
    }

    @Test
    fun `a refund travels, a spend of nothing does not`() {
        val backup = sampleBackup()
        val coffee = backup.caps[0]
        val refund = coffee.spends[2].copy(amount = -euros(1, 30))
        val withRefund = backup.copy(caps = listOf(coffee.copy(spends = coffee.spends.dropLast(1) + refund)))
        assertThat(ok(BackupCodec.encode(withRefund)).caps[0].spends.last().amount).isEqualTo(-euros(1, 30))
        val nothing = backup.copy(caps = listOf(coffee.copy(spends = listOf(refund.copy(amount = Money.ZERO)))))
        assertThat(BackupCodec.decode(BackupCodec.encode(nothing))).isEqualTo(BackupRead.Damaged)
    }
}
