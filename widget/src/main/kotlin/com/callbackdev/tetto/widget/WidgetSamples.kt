package com.callbackdev.tetto.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.tetto.core.data.time.PhoneDefaults
import com.callbackdev.tetto.core.domain.cap.CapChanges
import com.callbackdev.tetto.core.domain.cap.CapStates
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.core.model.WidgetLook
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Sample caps, for what draws a card without the reader's: the picker's generated previews, and
 * the card's settings screen before any cap exists. A preview is a drawing of the product, never a
 * reading: names in the reader's language, figures in the phone's currency, on a fixed Wednesday.
 */
object WidgetSamples {
    val FourByOne = DpSize(340.dp, 85.dp)
    val FourByTwo = DpSize(340.dp, 189.dp)
    val TwoByTwo = DpSize(159.dp, 189.dp)

    /** Wednesday 11 November 2026: a week half gone, a month a third gone. */
    val Today: LocalDate = LocalDate.of(2026, 11, 11)

    /** The sample card: «In words» on its first cap ([words]), «At a glance» on all five. */
    fun model(
        context: Context,
        words: Boolean = false,
        currency: AppCurrency = PhoneDefaults.System.currency(),
        look: WidgetLook = WidgetLook(),
        settings: UserSettings = UserSettings(),
    ): WidgetModel = WidgetModel(
        look = if (words && look.wordsCap == null) look.copy(wordsCap = 1L) else look,
        settings = settings,
        currency = currency,
        today = Today,
        caps = caps(context, currency),
    )

    /** Five caps: one at ease, one ahead of the days, one over, as a reader's week holds them. */
    fun caps(context: Context, currency: AppCurrency, today: LocalDate = Today): List<CapState> {
        val res = context.resources
        return listOf(
            Sample(1, R.string.widget_preview_cap1, CapIcon.COFFEE, PeriodKind.WEEK, 20, 8),
            Sample(2, R.string.widget_preview_cap2, CapIcon.GROCERIES, PeriodKind.MONTH, 300, 238),
            Sample(3, R.string.widget_preview_cap3, CapIcon.EATING_OUT, PeriodKind.MONTH, 120, 30),
            Sample(4, R.string.widget_preview_cap4, CapIcon.TRANSPORT, PeriodKind.MONTH, 60, 18),
            Sample(5, R.string.widget_preview_cap5, CapIcon.FUN, PeriodKind.WEEK, 40, 46),
        ).mapIndexed { index, sample ->
            val unit = currency.unit.minor
            val rule = PeriodRule(sample.kind, DayOfWeek.MONDAY, 1)
            CapStates.of(
                cap = Cap(
                    id = sample.id,
                    name = res.getString(sample.name),
                    icon = sample.icon,
                    amount = Money(sample.amount * unit),
                    kind = sample.kind,
                    reserve = Money.ZERO,
                    openStart = CapChanges.openStartForNew(rule, today),
                    position = index,
                ),
                spent = Money(sample.spent * unit),
                rule = rule,
                today = today,
                currency = currency,
            )
        }
    }

    private data class Sample(
        val id: Long,
        val name: Int,
        val icon: CapIcon,
        val kind: PeriodKind,
        val amount: Long,
        val spent: Long,
    )
}
