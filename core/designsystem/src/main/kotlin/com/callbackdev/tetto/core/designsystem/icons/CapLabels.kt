package com.callbackdev.tetto.core.designsystem.icons

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.callbackdev.tetto.core.designsystem.R
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.PeriodKind

/** What an icon stands for, in the reader's language: TalkBack's name for it, a first run's area. */
@Composable
fun CapIcon.label(): String = stringResource(labelRes)

@get:StringRes
val CapIcon.labelRes: Int
    get() = when (this) {
        CapIcon.GROCERIES -> R.string.cap_icon_groceries
        CapIcon.EATING_OUT -> R.string.cap_icon_eating_out
        CapIcon.COFFEE -> R.string.cap_icon_coffee
        CapIcon.SHOPPING -> R.string.cap_icon_shopping
        CapIcon.FUN -> R.string.cap_icon_fun
        CapIcon.TRANSPORT -> R.string.cap_icon_transport
        CapIcon.FUEL -> R.string.cap_icon_fuel
        CapIcon.GIFTS -> R.string.cap_icon_gifts
        CapIcon.PERSONAL_CARE -> R.string.cap_icon_personal_care
        CapIcon.PETS -> R.string.cap_icon_pets
        CapIcon.TRAVEL -> R.string.cap_icon_travel
        CapIcon.HOME -> R.string.cap_icon_home
        CapIcon.BILLS -> R.string.cap_icon_bills
        CapIcon.HEALTH -> R.string.cap_icon_health
        CapIcon.KIDS -> R.string.cap_icon_kids
        CapIcon.CLOTHES -> R.string.cap_icon_clothes
        CapIcon.BOOKS -> R.string.cap_icon_books
        CapIcon.SPORT -> R.string.cap_icon_sport
        CapIcon.CAR -> R.string.cap_icon_car
        CapIcon.TECH -> R.string.cap_icon_tech
        CapIcon.SUBSCRIPTIONS -> R.string.cap_icon_subscriptions
        CapIcon.HOBBIES -> R.string.cap_icon_hobbies
        CapIcon.EDUCATION -> R.string.cap_icon_education
        CapIcon.TAKEAWAY -> R.string.cap_icon_takeaway
        CapIcon.DRINKS -> R.string.cap_icon_drinks
        CapIcon.GAMES -> R.string.cap_icon_games
        CapIcon.EVENTS -> R.string.cap_icon_events
        CapIcon.BANK -> R.string.cap_icon_bank
        CapIcon.RENT -> R.string.cap_icon_rent
        CapIcon.OTHER -> R.string.cap_icon_other
    }

/** A period's name: Day, Week, Month, Year. */
@Composable
fun PeriodKind.label(): String = stringResource(
    when (this) {
        PeriodKind.DAY -> R.string.period_day
        PeriodKind.WEEK -> R.string.period_week
        PeriodKind.MONTH -> R.string.period_month
        PeriodKind.YEAR -> R.string.period_year
    },
)
