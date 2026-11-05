package com.callbackdev.tetto.feature.settings

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tetto.core.designsystem.components.CapTile
import com.callbackdev.tetto.core.designsystem.components.CurrencyPickerDialog
import com.callbackdev.tetto.core.designsystem.components.GroupDivider
import com.callbackdev.tetto.core.designsystem.components.GroupHeader
import com.callbackdev.tetto.core.designsystem.components.InfoRow
import com.callbackdev.tetto.core.designsystem.components.RadioDialog
import com.callbackdev.tetto.core.designsystem.components.SettingsGroup
import com.callbackdev.tetto.core.designsystem.components.SwitchRow
import com.callbackdev.tetto.core.designsystem.components.ValueRow
import com.callbackdev.tetto.core.designsystem.components.currencyNamed
import com.callbackdev.tetto.core.designsystem.format.Currencies
import com.callbackdev.tetto.core.designsystem.format.MoneyFormat
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.GroupShape
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.pageGutter
import com.callbackdev.tetto.core.designsystem.theme.tabular
import com.callbackdev.tetto.core.domain.money.Rescale
import com.callbackdev.tetto.core.domain.period.PeriodCalendar
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.AppFont
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import com.callbackdev.tetto.core.model.Share
import com.callbackdev.tetto.core.model.ThemeMode
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Formatter
import java.util.Locale
import com.callbackdev.tetto.core.designsystem.R as DesignR

@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsScreen(
        state = state,
        onBack = onBack,
        actions = SettingsActions(
            setCurrency = viewModel::setCurrency,
            setMonthStartDay = viewModel::setMonthStartDay,
            setWeekStart = viewModel::setWeekStart,
            setTheme = viewModel::setTheme,
            setPalette = viewModel::setPalette,
            setFont = viewModel::setFont,
            setDynamicColor = viewModel::setDynamicColor,
            // The system's per-app language page: one place for it, the same in every app.
            openLanguage = {
                runCatching { context.startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, appUri(context))) }
            },
            // The browser opens it: Tetto itself has no network access. No browser, no crash.
            openUrl = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } },
        ),
    )
}

/** What the list can ask for, as functions: the screen is a plain composable a test can draw. */
class SettingsActions(
    val setCurrency: (String) -> Unit = {},
    val setMonthStartDay: (Int) -> Unit = {},
    val setWeekStart: (DayOfWeek?) -> Unit = {},
    val setTheme: (ThemeMode) -> Unit = {},
    val setPalette: (AppPalette) -> Unit = {},
    val setFont: (AppFont) -> Unit = {},
    val setDynamicColor: (Boolean) -> Unit = {},
    val openLanguage: () -> Unit = {},
    val openUrl: (String) -> Unit = {},
)

/**
 * Settings (PLANNING.md §11 Phase 3): every group on one rounded ground under a header in the
 * accent; the money first, then a live preview of the appearance above the choices that change it,
 * the language, the privacy statement, about and the credits. The reminder and your data join with
 * Phase 4, the guide's card with Phase 6: no entry is drawn before what it opens exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState?,
    onBack: () -> Unit,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TettoIcons.Back, contentDescription = stringResource(DesignR.string.action_back))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets.add(pageGutter(sideInsets = false).asInsets()),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        // Until the store's first answer the list is not drawn: a screen of defaults that may be
        // about to change is a lie with good intentions.
        if (state != null) SettingsList(state, actions, Modifier.fillMaxSize().padding(padding))
    }
}

private enum class Dialog {
    CURRENCY,
    MONTH_START,
    WEEK_START,
    THEME,
    PALETTE,
    FONT,
}

@Composable
private fun SettingsList(state: SettingsUiState, actions: SettingsActions, modifier: Modifier) {
    val settings = state.settings
    val locale = LocalConfiguration.current.locales[0]
    var dialog by rememberSaveable { mutableStateOf<Dialog?>(null) }
    var confirmingCurrency by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.testTag(SettingsTags.LIST),
        contentPadding = pageGutter(sideInsets = false).contentPadding(bottom = 32.dp),
    ) {
        item { GroupHeader(stringResource(R.string.settings_group_money)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(DesignR.string.currency_title),
                    value = currencyNamed(state.currency.code),
                    onClick = { dialog = Dialog.CURRENCY },
                    modifier = Modifier.testTag(SettingsTags.CURRENCY),
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_month_start),
                    value = stringResource(
                        R.string.settings_month_start_value,
                        dayNumber(settings.monthStartDay, locale),
                        rangeWords(thisMonth(state), state.today),
                    ),
                    onClick = { dialog = Dialog.MONTH_START },
                    modifier = Modifier.testTag(SettingsTags.MONTH_START),
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_week_start),
                    value = if (settings.weekStart == null) {
                        stringResource(R.string.settings_week_start_phone, dayName(state.phoneWeekStart, locale))
                    } else {
                        dayName(state.weekStart, locale)
                    },
                    onClick = { dialog = Dialog.WEEK_START },
                    modifier = Modifier.testTag(SettingsTags.WEEK_START),
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_appearance)) }
        item { AppearancePreview(state.currency) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_theme),
                    value = themeLabel(settings.theme),
                    onClick = { dialog = Dialog.THEME },
                    modifier = Modifier.testTag(SettingsTags.THEME),
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_palette),
                    value = paletteLabel(settings.palette),
                    onClick = { dialog = Dialog.PALETTE },
                    enabled = !settings.dynamicColor,
                )
                GroupDivider()
                ValueRow(stringResource(R.string.settings_font), fontLabel(settings.font), { dialog = Dialog.FONT })
                GroupDivider()
                SwitchRow(
                    label = stringResource(R.string.settings_dynamic_color),
                    note = stringResource(R.string.settings_dynamic_color_note),
                    checked = settings.dynamicColor,
                    onChange = actions.setDynamicColor,
                    modifier = Modifier.testTag(SettingsTags.DYNAMIC_COLOR),
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_language)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_language),
                    value = currentLanguageLabel(),
                    trailing = true,
                    onClick = actions.openLanguage,
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_privacy)) }
        item { PrivacyCard() }

        item { GroupHeader(stringResource(R.string.settings_group_about)) }
        item {
            SettingsGroup {
                InfoRow(stringResource(R.string.settings_version), state.version)
                GroupDivider()
                InfoRow(stringResource(R.string.settings_developer), stringResource(R.string.settings_developer_note))
                GroupDivider()
                InfoRow(stringResource(R.string.settings_copyright), stringResource(R.string.settings_copyright_note))
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_license),
                    value = stringResource(R.string.settings_license_note),
                    trailing = true,
                    onClick = { actions.openUrl(LICENSE_URL) },
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_source_code),
                    value = stringResource(R.string.settings_source_code_note),
                    trailing = true,
                    onClick = { actions.openUrl(SOURCE_URL) },
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_credits)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_credit_font),
                    value = fontCreditNote(settings.font),
                    trailing = true,
                    onClick = { actions.openUrl(fontCreditUrl(settings.font)) },
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_credit_icons),
                    value = stringResource(R.string.settings_credit_icons_note),
                    trailing = true,
                    onClick = { actions.openUrl(ICONS_URL) },
                )
            }
        }
    }

    val close = { dialog = null }
    when (dialog) {
        Dialog.CURRENCY -> CurrencyPickerDialog(
            selected = state.currency.code,
            explanation = stringResource(R.string.settings_currency_explanation),
            onPick = { code ->
                dialog = null
                when {
                    code == state.currency.code -> Unit

                    // Caps to relabel: the reader is told what happens to them before it does.
                    state.capCount > 0 -> confirmingCurrency = code

                    else -> actions.setCurrency(code)
                }
            },
            onDismiss = close,
        )

        Dialog.MONTH_START -> MonthStartDialog(
            selected = settings.monthStartDay,
            onSelect = { day ->
                dialog = null
                if (day != settings.monthStartDay) actions.setMonthStartDay(day)
            },
            onDismiss = close,
        )

        Dialog.WEEK_START -> RadioDialog(
            title = stringResource(R.string.settings_week_start),
            explanation = stringResource(R.string.settings_week_start_explanation),
            options = listOf<Pair<DayOfWeek?, String>>(
                null to stringResource(R.string.settings_week_start_phone, dayName(state.phoneWeekStart, locale)),
            ) + (0L until DAYS_IN_WEEK).map { state.phoneWeekStart.plus(it) }.map { it to dayName(it, locale) },
            selected = settings.weekStart,
            onSelect = { day ->
                dialog = null
                if (day != settings.weekStart) actions.setWeekStart(day)
            },
            onDismiss = close,
        )

        Dialog.THEME -> RadioDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it to themeLabel(it) },
            selected = settings.theme,
            onSelect = { theme ->
                dialog = null
                actions.setTheme(theme)
            },
            onDismiss = close,
        )

        Dialog.PALETTE -> RadioDialog(
            title = stringResource(R.string.settings_palette),
            explanation = stringResource(R.string.settings_palette_note),
            options = AppPalette.entries.map { it to paletteLabel(it) },
            selected = settings.palette,
            onSelect = { palette ->
                dialog = null
                actions.setPalette(palette)
            },
            onDismiss = close,
        )

        Dialog.FONT -> RadioDialog(
            title = stringResource(R.string.settings_font),
            explanation = stringResource(R.string.settings_font_note),
            options = AppFont.entries.map { it to fontLabel(it) },
            selected = settings.font,
            onSelect = { font ->
                dialog = null
                actions.setFont(font)
            },
            onDismiss = close,
        )

        null -> Unit
    }

    val to = confirmingCurrency?.let(AppCurrency::ofOrNull)
    if (to != null) {
        CurrencyConfirmDialog(
            from = state.currency,
            to = to,
            onConfirm = {
                confirmingCurrency = null
                actions.setCurrency(to.code)
            },
            onDismiss = { confirmingCurrency = null },
        )
    }
}

/**
 * Before a change of currency relabels the reader's caps (PLANNING.md §4.1): it says that it does
 * not convert them, and, where the new currency has fewer decimals, how an amount is rounded, by
 * an example in the two currencies ("€12.55 becomes ¥13").
 */
@Composable
private fun CurrencyConfirmDialog(from: AppCurrency, to: AppCurrency, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val name = remember(to, locale) { Currencies.inSentence(to.code, locale) }
    val relabel = stringResource(R.string.settings_currency_confirm_body)
    val body = if (to.fractionDigits < from.fractionDigits) {
        val sample = roundingSample(from)
        val before = MoneyFormat(from, locale).full(sample)
        val after = MoneyFormat(to, locale).full(Rescale.amount(sample, from.fractionDigits, to.fractionDigits))
        relabel + " " + stringResource(R.string.settings_currency_confirm_rounded, before, after)
    } else {
        relabel
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_currency_confirm_title, name)) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(SettingsTags.CURRENCY_CONFIRM)) {
                Text(stringResource(R.string.settings_currency_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.action_cancel)) } },
    )
}

/** Twelve and as many fives as [currency] has decimals: an amount every rounding changes (12.55, 12.555). */
private fun roundingSample(currency: AppCurrency): Money {
    val unit = currency.unit.minor
    return Money(SAMPLE_UNITS * unit + (unit - 1) * 5 / 9)
}

private const val SAMPLE_UNITS = 12L

/**
 * The day a month starts, 1 to 31, as the days of a month laid out: a choice among them, which
 * TalkBack reads as radio buttons. What the day does, and what a change does to the caps, above.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthStartDialog(selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_month_start)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.settings_month_start_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                FlowRow(
                    modifier = Modifier.selectableGroup().testTag(SettingsTags.MONTH_DAYS),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    (1..31).forEach { day ->
                        DayChoice(dayNumber(day, locale), day == selected, Modifier.testTag(SettingsTags.day(day))) {
                            onSelect(day)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.action_cancel)) } },
    )
}

@Composable
private fun DayChoice(number: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 6.dp),
    ) {
        Text(
            number,
            style = MaterialTheme.typography.bodyLarge.tabular(),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The appearance as the caps screen will wear it: two tiles, one under its cap in the accent and
 * one over it in the over ink, captioned as an example so no sample reads as the reader's money.
 * Every choice below changes it the moment it is made. A picture, silent to a screen reader: the
 * rows say every choice in words.
 */
@Composable
private fun AppearancePreview(currency: AppCurrency) {
    val format = rememberMoneyFormat(currency)
    val unit = currency.unit.minor
    val oneColumn = LocalDensity.current.fontScale >= LARGE_TEXT
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenMargin, end = ScreenMargin, bottom = 12.dp)
            .testTag(SettingsTags.PREVIEW)
            .clearAndSetSemantics { },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.settings_preview_caption),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PreviewTile(
                    icon = CapIcon.COFFEE,
                    name = stringResource(R.string.settings_preview_under_name),
                    left = stringResource(R.string.settings_preview_left, format.short(Money(12 * unit))),
                    detail = stringResource(R.string.settings_preview_detail, format.short(Money(20 * unit))),
                    bar = Share(8, 20),
                    over = false,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                if (!oneColumn) {
                    PreviewTile(
                        icon = CapIcon.SHOPPING,
                        name = stringResource(R.string.settings_preview_over_name),
                        left = stringResource(R.string.settings_preview_over, format.short(Money(30 * unit))),
                        detail = stringResource(R.string.settings_preview_detail_over, format.short(Money(100 * unit))),
                        bar = Share(130, 100),
                        over = true,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewTile(
    icon: CapIcon,
    name: String,
    left: String,
    detail: String,
    bar: Share,
    over: Boolean,
    modifier: Modifier,
) {
    CapTile(
        icon = icon.symbol,
        name = name,
        left = left,
        leftValue = 0,
        detail = detail,
        bar = bar,
        mark = Share(4, 7),
        over = over,
        ahead = false,
        description = "",
        clickLabel = "",
        longClickLabel = "",
        onClick = {},
        onLongClick = {},
        modifier = modifier,
    )
}

private const val LARGE_TEXT = 1.5f

@Composable
private fun PrivacyCard() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(16.dp)) {
            Icon(TettoIcons.Shield, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.settings_privacy_headline), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.settings_privacy_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** This month as the month start day makes it: the period of a monthly cap that holds today. */
private fun thisMonth(state: SettingsUiState): DateRange =
    PeriodCalendar.containing(state.today, PeriodRule(PeriodKind.MONTH, state.weekStart, state.settings.monthStartDay))

/** A range of days as the locale writes one: "27 Oct – 26 Nov", "1 – 30 Nov"; the year only when it is not this one. */
@Composable
private fun rangeWords(range: DateRange, today: LocalDate): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val last = range.lastDay
    var flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    flags = flags or if (range.start.year == today.year && last.year == today.year) {
        DateUtils.FORMAT_NO_YEAR
    } else {
        DateUtils.FORMAT_SHOW_YEAR
    }
    val start = range.start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    // Noon of the last day: a range ending at a midnight would read as ending the day before.
    val end = last.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    return DateUtils.formatDateRange(context, Formatter(StringBuilder(), locale), start, end, flags, "UTC").toString()
}

/** A day of the month in the locale's digits. */
private fun dayNumber(day: Int, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(day)

/** "Monday", "Lunedì": a weekday standing alone, its first letter up. */
private fun dayName(day: DayOfWeek, locale: Locale): String =
    day.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
}

@Composable
private fun paletteLabel(palette: AppPalette): String = when (palette) {
    AppPalette.PAPER -> stringResource(R.string.settings_palette_paper)
    AppPalette.VIVID -> stringResource(R.string.settings_palette_vivid)
}

@Composable
private fun fontLabel(font: AppFont): String = when (font) {
    AppFont.GOOGLE_SANS -> stringResource(R.string.settings_font_google_sans)
    AppFont.INTER -> stringResource(R.string.settings_font_inter)
    AppFont.SYSTEM -> stringResource(R.string.settings_font_system)
}

/** Both bundled faces travel in the APK, so both are credited; the note says which one is on screen. */
@Composable
private fun fontCreditNote(font: AppFont): String {
    val inUse = when (font) {
        AppFont.SYSTEM -> stringResource(R.string.settings_credit_font_inuse_system)
        else -> stringResource(R.string.settings_credit_font_inuse, fontLabel(font))
    }
    return stringResource(R.string.settings_credit_font_note) + " · " + inUse
}

private fun fontCreditUrl(font: AppFont): String = when (font) {
    AppFont.INTER -> "https://rsms.me/inter/"
    AppFont.GOOGLE_SANS -> "https://fonts.google.com/specimen/Google+Sans"
    AppFont.SYSTEM -> "https://openfontlicense.org"
}

/** What the app is speaking: the reader's pick in the system's picker, or the phone's language. */
@Composable
private fun currentLanguageLabel(): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val appLocales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
    if (appLocales == null || appLocales.isEmpty) return stringResource(R.string.settings_language_system)
    val chosen = appLocales[0]
    return chosen.getDisplayLanguage(chosen).replaceFirstChar { it.titlecase(locale) }
}

/** This app's own page in the system's settings, for its language. */
private fun appUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)

private const val DAYS_IN_WEEK = 7L
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
private const val SOURCE_URL = "https://github.com/fiorenzobrioni/tetto"
private const val ICONS_URL = "https://github.com/google/material-design-icons"

/** Hooks for the UI tests. */
object SettingsTags {
    const val LIST = "settings_list"
    const val CURRENCY = "settings_currency"
    const val CURRENCY_CONFIRM = "settings_currency_confirm"
    const val MONTH_START = "settings_month_start"
    const val MONTH_DAYS = "settings_month_days"
    const val WEEK_START = "settings_week_start"
    const val THEME = "settings_theme"
    const val DYNAMIC_COLOR = "settings_dynamic_color"
    const val PREVIEW = "settings_preview"

    fun day(day: Int) = "settings_day_$day"
}
