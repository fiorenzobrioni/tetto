package com.callbackdev.tetto.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * A cap (PLANNING.md §5): the most [name] may spend in one period of [kind], with the [reserve]
 * past periods left over. [openStart] is the first day of its open period; the period's end
 * follows from the rule in force, so a cap never stores a date that a setting could contradict.
 */
data class Cap(
    val id: Long,
    val name: String,
    val icon: CapIcon,
    val amount: Money,
    val kind: PeriodKind,
    val reserve: Money,
    val openStart: LocalDate,
    val position: Int,
)

/**
 * A spend (PLANNING.md §4.3): an amount added to a cap at [addedAt], counting in the period that
 * starts on [periodStart], fixed when it was added: a later change of zone never moves it. Above
 * zero, or below it for a [refund] (owner, 6 Nov 2026): money back, which lowers what the period
 * spent, never below nothing.
 */
data class Spend(val id: Long, val capId: Long, val amount: Money, val addedAt: Instant, val periodStart: LocalDate) {
    /** Money back: a return, a friend's share paid back. */
    val refund: Boolean get() = amount.isNegative

    /** The amount as the reader typed it and reads it, above zero either way. */
    val size: Money get() = if (refund) -amount else amount
}

/**
 * One line of a cap's story (PLANNING.md §4.4): a period closed, or a run of [periods] empty ones
 * folded into one line, with the cap in force, what was spent, the reserve before and after, and
 * the overspend the reserve could not cover, [forgiven]. Never changed once written.
 */
data class ClosedPeriod(
    val capId: Long,
    val range: DateRange,
    val cap: Money,
    val spent: Money,
    val reserveBefore: Money,
    val reserveAfter: Money,
    val forgiven: Money,
    val periods: Int,
) {
    init {
        require(periods >= 1) { "A closed line stands for one period at least" }
    }
}

/**
 * The cap palette's keys (PLANNING.md §5): a curated set, so the caps screen keeps one look. The
 * [key] is what the database and the backup store, never the constant's name, so a constant can
 * be renamed without touching a reader's data. The first twelve are the first run's areas
 * (VISION.md, First run); [OTHER] stays last, where the editor's grid ends; the drawings are
 * `TettoIcons`' (Phase 2). A new key reads as [OTHER] in a build that predates it, so a backup
 * made after an addition still opens in an older Tetto.
 */
enum class CapIcon(val key: String) {
    GROCERIES("groceries"),
    EATING_OUT("eating_out"),
    COFFEE("coffee"),
    SHOPPING("shopping"),
    FUN("fun"),
    TRANSPORT("transport"),
    FUEL("fuel"),
    GIFTS("gifts"),
    PERSONAL_CARE("personal_care"),
    PETS("pets"),
    TRAVEL("travel"),
    HOME("home"),
    BILLS("bills"),
    HEALTH("health"),
    KIDS("kids"),
    CLOTHES("clothes"),
    BOOKS("books"),
    SPORT("sport"),
    CAR("car"),
    TECH("tech"),
    SUBSCRIPTIONS("subscriptions"),
    HOBBIES("hobbies"),
    EDUCATION("education"),
    TAKEAWAY("takeaway"),
    DRINKS("drinks"),
    GAMES("games"),
    EVENTS("events"),
    BANK("bank"),
    RENT("rent"),
    OTHER("other"),
    ;

    companion object {
        private val byKey = entries.associateBy(CapIcon::key)

        /** The icon [key] names; a key this build does not know (a newer backup's) reads as [OTHER]. */
        fun fromKey(key: String): CapIcon = byKey[key] ?: OTHER
    }
}
