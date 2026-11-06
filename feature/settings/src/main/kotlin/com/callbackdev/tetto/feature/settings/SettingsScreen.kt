package com.callbackdev.tetto.feature.settings

import android.Manifest
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tetto.core.data.widget.HomeCard
import com.callbackdev.tetto.core.designsystem.components.CapRow
import com.callbackdev.tetto.core.designsystem.components.CapTile
import com.callbackdev.tetto.core.designsystem.components.CurrencyPickerDialog
import com.callbackdev.tetto.core.designsystem.components.DENSE_TARGETS_TAG
import com.callbackdev.tetto.core.designsystem.components.GroupDivider
import com.callbackdev.tetto.core.designsystem.components.GroupHeader
import com.callbackdev.tetto.core.designsystem.components.InfoRow
import com.callbackdev.tetto.core.designsystem.components.RadioDialog
import com.callbackdev.tetto.core.designsystem.components.SettingsGroup
import com.callbackdev.tetto.core.designsystem.components.StatusCard
import com.callbackdev.tetto.core.designsystem.components.StatusTone
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
import com.callbackdev.tetto.core.model.CapsLayout
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import com.callbackdev.tetto.core.model.Share
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.core.reminder.ReminderAccess
import com.callbackdev.tetto.core.reminder.ReminderBlock
import com.callbackdev.tetto.feature.settings.data.DataActions
import com.callbackdev.tetto.feature.settings.data.DataDialogs
import com.callbackdev.tetto.feature.settings.data.DataGroup
import com.callbackdev.tetto.feature.settings.data.DataUiState
import com.callbackdev.tetto.feature.settings.data.DataViewModel
import com.callbackdev.tetto.feature.settings.data.rememberDataFiles
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Formatter
import java.util.Locale
import com.callbackdev.tetto.core.designsystem.R as DesignR

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenGuide: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    dataViewModel: DataViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data by dataViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val asked = state?.settings?.askedNotifications ?: false
    fun readReach(askedBefore: Boolean) = ReminderReach.of(
        block = ReminderAccess.block(context),
        granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED,
        askedBefore = askedBefore,
        showRationale = activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true,
    )
    var reach by remember { mutableStateOf(readReach(asked)) }
    // Read again whenever the page comes back: from the permission's question, from the system's
    // notification settings, or from anywhere the reader may have turned them off.
    LifecycleResumeEffect(asked) {
        reach = readReach(asked)
        viewModel.reminderAccessMayHaveChanged()
        onPauseOrDispose { }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        reach = readReach(askedBefore = true)
        viewModel.reminderAccessMayHaveChanged()
    }
    val askPermission = {
        viewModel.noteNotificationsAsked()
        runCatching { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
    SettingsScreen(
        state = state,
        onBack = onBack,
        reach = reach,
        data = data,
        dataActions = remember(dataViewModel) {
            DataActions(
                writeBackup = dataViewModel::writeBackup,
                writeSpends = dataViewModel::writeSpends,
                readBackup = dataViewModel::readBackup,
                confirmRestore = dataViewModel::confirmRestore,
                cancelRestore = dataViewModel::cancelRestore,
                erase = dataViewModel::erase,
                dismissOutcome = dataViewModel::dismissOutcome,
                backupFileName = dataViewModel::backupFileName,
                spendsFileName = dataViewModel::spendsFileName,
            )
        },
        actions = SettingsActions(
            setCurrency = viewModel::setCurrency,
            setMonthStartDay = viewModel::setMonthStartDay,
            setWeekStart = viewModel::setWeekStart,
            setTheme = viewModel::setTheme,
            setPalette = viewModel::setPalette,
            setFont = viewModel::setFont,
            setCapsLayout = viewModel::setCapsLayout,
            setDynamicColor = viewModel::setDynamicColor,
            // Turned on, the reminder asks for the permission where Android still lets it ask;
            // where it does not, the card over the group says so, and opens the page that fixes it.
            setReminder = { on ->
                viewModel.setReminder(on)
                if (on && reach.block == ReminderBlock.APP && reach.askable) askPermission()
            },
            setReminderTime = viewModel::setReminderTime,
            pinCard = viewModel::pinCard,
            fixNotifications = {
                if (reach.block == ReminderBlock.APP && reach.askable) {
                    askPermission()
                } else {
                    runCatching { context.startActivity(ReminderAccess.settingsIntent(context, reach.block)) }
                }
            },
            // The system's per-app language page: one place for it, the same in every app.
            openLanguage = {
                runCatching { context.startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, appUri(context))) }
            },
            // The browser opens it: Tetto itself has no network access. No browser, no crash.
            openUrl = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } },
            openGuide = onOpenGuide,
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
    val setCapsLayout: (CapsLayout) -> Unit = {},
    val setDynamicColor: (Boolean) -> Unit = {},
    val setReminder: (Boolean) -> Unit = {},
    val setReminderTime: (LocalTime) -> Unit = {},
    val pinCard: (HomeCard) -> Unit = {},
    val fixNotifications: () -> Unit = {},
    val openLanguage: () -> Unit = {},
    val openUrl: (String) -> Unit = {},
    val openGuide: () -> Unit = {},
)

/**
 * Where the reminder's notifications stand, as the route reads them from Android: what blocks them,
 * if anything, and whether Android will still ask the reader for the permission.
 */
data class ReminderReach(val block: ReminderBlock, val askable: Boolean) {
    companion object {
        val Clear = ReminderReach(ReminderBlock.NONE, askable = false)

        /**
         * Android answers "should the app explain first?" with no both before its first question
         * and after a refusal it will not let the app repeat: only whether Tetto ever asked
         * ([askedBefore]) tells the two apart.
         */
        fun of(block: ReminderBlock, granted: Boolean, askedBefore: Boolean, showRationale: Boolean) =
            ReminderReach(block, askable = !granted && (!askedBefore || showRationale))
    }
}

/**
 * Settings (PLANNING.md §11 Phases 3, 4 and 6): the guide's card first; then every group on one
 * rounded ground under a header in the accent: the money, the evening reminder, a live preview of
 * the appearance above the choices that change it, the home screen's cards (where the launcher can
 * place one), the language, your data (the privacy statement, the backup, the restore, the spends
 * for a spreadsheet, erasing everything), about and the credits.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState?,
    onBack: () -> Unit,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    reach: ReminderReach = ReminderReach.Clear,
    data: DataUiState = DataUiState(),
    dataActions: DataActions = DataActions(),
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
        if (state != null) {
            SettingsList(state, actions, reach, data, dataActions, Modifier.fillMaxSize().padding(padding))
        }
    }
}

private enum class Dialog {
    CURRENCY,
    MONTH_START,
    WEEK_START,
    THEME,
    PALETTE,
    FONT,
    CAPS_LAYOUT,
    REMINDER_TIME,
}

@Composable
private fun SettingsList(
    state: SettingsUiState,
    actions: SettingsActions,
    reach: ReminderReach,
    data: DataUiState,
    dataActions: DataActions,
    modifier: Modifier,
) {
    val settings = state.settings
    val locale = LocalConfiguration.current.locales[0]
    var dialog by rememberSaveable { mutableStateOf<Dialog?>(null) }
    var confirmingCurrency by rememberSaveable { mutableStateOf<String?>(null) }
    // Out of the lazy list: a picker's answer must not leave with a row scrolled away.
    val files = rememberDataFiles(dataActions)

    LazyColumn(
        modifier = modifier.testTag(SettingsTags.LIST),
        contentPadding = pageGutter(sideInsets = false).contentPadding(bottom = 32.dp),
    ) {
        item { GuideCard(actions.openGuide) }
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

        item { GroupHeader(stringResource(R.string.settings_group_reminder)) }
        item {
            ReminderGroup(
                settings = settings,
                reach = reach,
                onTurn = actions.setReminder,
                onPickTime = { dialog = Dialog.REMINDER_TIME },
                onFix = actions.fixNotifications,
            )
        }

        item { GroupHeader(stringResource(R.string.settings_group_appearance)) }
        item { AppearancePreview(state.currency, settings.capsLayout) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_caps_layout),
                    value = capsLayoutLabel(settings.capsLayout),
                    onClick = { dialog = Dialog.CAPS_LAYOUT },
                    modifier = Modifier.testTag(SettingsTags.CAPS_LAYOUT),
                )
                GroupDivider()
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

        // The home screen's cards, where the launcher can place one on request (PLANNING.md §8):
        // a row that could not do what it says is not drawn.
        if (state.canPinCards) {
            item { GroupHeader(stringResource(R.string.settings_group_home)) }
            item {
                SettingsGroup {
                    ValueRow(
                        label = stringResource(R.string.settings_card_glance),
                        value = stringResource(R.string.settings_card_glance_note),
                        icon = TettoIcons.Widgets,
                        onClick = { actions.pinCard(HomeCard.AT_A_GLANCE) },
                        modifier = Modifier.testTag(SettingsTags.CARD_GLANCE),
                    )
                    GroupDivider()
                    ValueRow(
                        label = stringResource(R.string.settings_card_words),
                        value = stringResource(R.string.settings_card_words_note),
                        icon = TettoIcons.Widgets,
                        onClick = { actions.pinCard(HomeCard.IN_WORDS) },
                        modifier = Modifier.testTag(SettingsTags.CARD_WORDS),
                    )
                }
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

        item { GroupHeader(stringResource(R.string.settings_group_data)) }
        item { PrivacyCard() }
        item {
            Box(Modifier.padding(top = 12.dp).testTag(SettingsTags.DATA)) {
                DataGroup(data, files, dataActions.dismissOutcome)
            }
        }

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

        Dialog.CAPS_LAYOUT -> RadioDialog(
            title = stringResource(R.string.settings_caps_layout),
            explanation = stringResource(R.string.settings_caps_layout_note),
            options = CapsLayout.entries.map { it to capsLayoutLabel(it) },
            selected = settings.capsLayout,
            onSelect = { layout ->
                dialog = null
                actions.setCapsLayout(layout)
            },
            onDismiss = close,
        )

        Dialog.REMINDER_TIME -> TimeDialog(
            title = stringResource(R.string.settings_reminder),
            initial = settings.reminderTime,
            note = stringResource(R.string.settings_reminder_time_note),
            onSave = { time ->
                dialog = null
                if (time != settings.reminderTime) actions.setReminderTime(time)
            },
            onDismiss = close,
        )

        null -> Unit
    }

    DataDialogs(data, files, dataActions)

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

/**
 * The evening reminder (PLANNING.md §8): the switch, saying what it does and when; its time. Turned
 * on while Android would drop it, a card over the group says why and holds the one button that
 * fixes it, so a switch that is on never promises what Android drops.
 */
@Composable
private fun ReminderGroup(
    settings: UserSettings,
    reach: ReminderReach,
    onTurn: (Boolean) -> Unit,
    onPickTime: () -> Unit,
    onFix: () -> Unit,
) {
    val time = clockTime(settings.reminderTime)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (settings.reminderOn && reach.block != ReminderBlock.NONE) {
            val askable = reach.block == ReminderBlock.APP && reach.askable
            StatusCard(
                icon = TettoIcons.Bell,
                title = stringResource(
                    when {
                        askable -> R.string.settings_reminder_ask_title
                        reach.block == ReminderBlock.CHANNEL -> R.string.settings_reminder_blocked_channel_title
                        else -> R.string.settings_reminder_blocked_title
                    },
                ),
                body = stringResource(
                    if (askable) R.string.settings_reminder_ask_body else R.string.settings_reminder_blocked_body,
                ),
                // Not yet asked is a step to take, not a fault: the quieter ground.
                tone = if (askable) StatusTone.CHOICE else StatusTone.PROBLEM,
                action = stringResource(
                    if (askable) R.string.settings_reminder_ask_action else R.string.settings_reminder_blocked_action,
                ),
                onAction = onFix,
                modifier = Modifier.padding(horizontal = ScreenMargin).testTag(SettingsTags.REMINDER_BLOCKED),
            )
        }
        SettingsGroup {
            SwitchRow(
                label = stringResource(R.string.settings_reminder),
                note = stringResource(R.string.settings_reminder_note, time),
                checked = settings.reminderOn,
                onChange = onTurn,
                modifier = Modifier.testTag(SettingsTags.REMINDER),
            )
            GroupDivider()
            // Always open: a time chosen while the reminder is off is the one it starts with.
            ValueRow(
                label = stringResource(R.string.settings_reminder_time),
                value = time,
                onClick = onPickTime,
                modifier = Modifier.testTag(SettingsTags.REMINDER_TIME),
            )
        }
    }
}

/** A time of day on Material's dial, 12 or 24 hours as the phone shows them; saved on confirm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    title: String,
    initial: LocalTime,
    note: String,
    onSave: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val picker = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                // At twice the text size the dial and its note scroll rather than push the buttons off.
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                // Material's dial: its hours a little under 48dp apart, set by a drag as much as a tap.
                TimePicker(state = picker, modifier = Modifier.testTag(DENSE_TARGETS_TAG))
                Text(
                    note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(LocalTime.of(picker.hour, picker.minute)) },
                modifier = Modifier.testTag(SettingsTags.REMINDER_TIME_SAVE),
            ) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.action_cancel)) } },
    )
}

/** A time of day as the phone writes one: its locale's order, 24-hour or not as the reader set it. */
@Composable
private fun clockTime(time: LocalTime): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
    return remember(time, locale, skeleton) {
        time.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
    }
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
 * The appearance as the caps screen will wear it: two caps, one under its cap in the accent and
 * one over it in the over ink, as tiles or as rows ([layout]), captioned as an example so no
 * sample reads as the reader's money. Every choice below changes it the moment it is made. A
 * picture, silent to a screen reader: the rows say every choice in words.
 */
@Composable
private fun AppearancePreview(currency: AppCurrency, layout: CapsLayout) {
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
            if (layout == CapsLayout.LIST) {
                PreviewRow(
                    icon = CapIcon.COFFEE,
                    name = stringResource(R.string.settings_preview_under_name),
                    figure = format.short(Money(12 * unit)),
                    detail = stringResource(R.string.settings_preview_detail, format.short(Money(20 * unit))),
                    bar = Share(8, 20),
                    over = false,
                )
                PreviewRow(
                    icon = CapIcon.SHOPPING,
                    name = stringResource(R.string.settings_preview_over_name),
                    figure = format.short(Money(30 * unit)),
                    detail = stringResource(R.string.settings_preview_detail_over, format.short(Money(100 * unit))),
                    bar = Share(130, 100),
                    over = true,
                )
                return@Column
            }
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PreviewTile(
                    icon = CapIcon.COFFEE,
                    name = stringResource(R.string.settings_preview_under_name),
                    figure = format.short(Money(12 * unit)),
                    detail = stringResource(R.string.settings_preview_of, format.short(Money(20 * unit))),
                    dateLine = stringResource(R.string.settings_preview_until),
                    bar = Share(8, 20),
                    over = false,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                if (!oneColumn) {
                    PreviewTile(
                        icon = CapIcon.SHOPPING,
                        name = stringResource(R.string.settings_preview_over_name),
                        figure = format.short(Money(30 * unit)),
                        detail = stringResource(R.string.settings_preview_of, format.short(Money(100 * unit))),
                        dateLine = stringResource(R.string.settings_preview_again),
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
    figure: String,
    detail: String,
    dateLine: String,
    bar: Share,
    over: Boolean,
    modifier: Modifier,
) {
    CapTile(
        icon = icon.symbol,
        name = name,
        figure = figure,
        figureValue = 0,
        detail = detail,
        dateLine = dateLine,
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
        enabled = false,
    )
}

@Composable
private fun PreviewRow(icon: CapIcon, name: String, figure: String, detail: String, bar: Share, over: Boolean) {
    CapRow(
        icon = icon.symbol,
        name = name,
        figure = figure,
        figureValue = 0,
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
        modifier = Modifier.fillMaxWidth(),
        enabled = false,
    )
}

private const val LARGE_TEXT = 1.5f

/**
 * The way to the guide, first in the list (PLANNING.md §11 Phase 6): the place a reader comes back
 * to the day the question arrives, which the tip shown once on the caps screen could never be.
 */
@Composable
private fun GuideCard(onOpenGuide: () -> Unit) {
    Surface(
        onClick = onOpenGuide,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = GroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenMargin, end = ScreenMargin, top = 8.dp)
            .testTag(SettingsTags.GUIDE),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(TettoIcons.Info, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_guide), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.settings_guide_note), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(TettoIcons.ChevronRight, contentDescription = null)
        }
    }
}

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
private fun capsLayoutLabel(layout: CapsLayout): String = when (layout) {
    CapsLayout.TILES -> stringResource(R.string.settings_caps_layout_tiles)
    CapsLayout.LIST -> stringResource(R.string.settings_caps_layout_list)
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
    const val GUIDE = "settings_guide"
    const val CURRENCY = "settings_currency"
    const val CURRENCY_CONFIRM = "settings_currency_confirm"
    const val MONTH_START = "settings_month_start"
    const val MONTH_DAYS = "settings_month_days"
    const val WEEK_START = "settings_week_start"
    const val CAPS_LAYOUT = "settings_caps_layout"
    const val THEME = "settings_theme"
    const val DYNAMIC_COLOR = "settings_dynamic_color"
    const val PREVIEW = "settings_preview"
    const val REMINDER = "settings_reminder"
    const val REMINDER_TIME = "settings_reminder_time"
    const val REMINDER_TIME_SAVE = "settings_reminder_time_save"
    const val REMINDER_BLOCKED = "settings_reminder_blocked"
    const val DATA = "settings_data"
    const val CARD_GLANCE = "settings_card_glance"
    const val CARD_WORDS = "settings_card_words"

    fun day(day: Int) = "settings_day_$day"
}
