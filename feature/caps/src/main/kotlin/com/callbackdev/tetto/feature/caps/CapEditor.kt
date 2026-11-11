package com.callbackdev.tetto.feature.caps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tetto.core.designsystem.components.ChoiceSegments
import com.callbackdev.tetto.core.designsystem.components.MoneyField
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.icons.label
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.GroupShape
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.balanced
import com.callbackdev.tetto.core.designsystem.theme.padding
import com.callbackdev.tetto.core.designsystem.theme.pageGutter
import com.callbackdev.tetto.core.designsystem.theme.tabular
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapDraft
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.callbackdev.tetto.core.designsystem.R as DesignR

/**
 * The cap editor (PLANNING.md §11 Phase 2): a new cap with [capId] null, else that cap. Its fields
 * survive a rotation; they are filled once from the cap, then the reader's.
 *
 * An ended one-off (owner, 6 Nov 2026) opens here from its sheet's «Again, with new dates»: its
 * name, icon and amount kept, its dates to choose, the picker open at once.
 */
@Composable
fun CapEditorRoute(capId: Long?, onBack: () -> Unit, viewModel: CapsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state ?: return
    var filled by rememberSaveable { mutableStateOf(capId == null) }
    var draft by rememberSaveable(stateSaver = draftSaver) {
        mutableStateOf(CapDraft("", Money.ZERO, PeriodKind.MONTH, CapIcon.OTHER))
    }
    val editing = capId?.let(current::cap)
    LaunchedEffect(current, filled) {
        if (!filled) {
            editing?.cap?.let { cap ->
                draft = CapDraft(cap.name, cap.amount, cap.kind, cap.icon, cap.oneOffDates)
            }
            filled = true
        }
    }
    if (!filled) return
    val scope = rememberCoroutineScope()
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    CapEditorScreen(
        editing = editing?.cap?.kind,
        draft = draft,
        currency = current.currency,
        onChange = { draft = it },
        onSave = {
            scope.launch {
                val dates = draft.dates.takeIf { draft.kind == PeriodKind.ONE_OFF }
                viewModel.saveCap(capId, draft.name, draft.icon, draft.amount, draft.kind, dates)
                onBack()
            }
        },
        onBack = onBack,
        today = current.today,
        reserve = editing?.cap?.reserve ?: Money.ZERO,
        again = editing?.ended == true,
        onDelete = if (editing != null) {
            { confirmingDelete = true }
        } else {
            null
        },
    )
    if (confirmingDelete && editing != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.delete_title, editing.cap.name)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        // Back to the caps first: they say the cap is gone, and offer nothing to undo.
                        onBack()
                        viewModel.deleteCap(editing.cap.id)
                    },
                ) { Text(stringResource(R.string.delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                }) { Text(stringResource(DesignR.string.action_cancel)) }
            },
        )
    }
}

/**
 * The editor drawn from its draft, as the family draws a page that is edited: its title in the bar
 * that stays, with the cap's deletion ([onDelete], an existing cap's) at its end, its fields
 * scrolling under, and Save in a bar of its own at the foot, always within reach.
 *
 * [editing] is the kind the cap has now, null for a new cap: a change of kind says what it does to
 * this period (§4.5). The period is one choice in two shapes: the four that return, side by side,
 * and under them the one that does not, a one-off between two dates, which is also where its dates
 * are read and changed. The dates come from Material's range picker, on a page of its own, said in
 * the locale's patterns; an end before [today] is refused. [reserve] is the cap's, let go when it
 * becomes a one-off, and said so; [again], an ended one-off running again with new dates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapEditorScreen(
    editing: PeriodKind?,
    draft: CapDraft,
    currency: AppCurrency,
    onChange: (CapDraft) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    reserve: Money = Money.ZERO,
    again: Boolean = false,
    onDelete: (() -> Unit)? = null,
) {
    val format = rememberMoneyFormat(currency)
    // An ended one-off, again: the picker opens at once, its dates being what is new.
    var picking by rememberSaveable { mutableStateOf(again && draft.dates == null) }
    val once = draft.kind == PeriodKind.ONE_OFF
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val gutter = pageGutter(sideInsets = false)
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when {
                                again -> R.string.editor_again
                                editing == null -> R.string.new_cap
                                else -> R.string.editor_edit
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TettoIcons.Back, contentDescription = stringResource(DesignR.string.action_back))
                    }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.testTag(CapsTags.EDITOR_DELETE)) {
                            Icon(TettoIcons.Trash, contentDescription = stringResource(R.string.sheet_delete))
                        }
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets.add(gutter.asInsets()),
                scrollBehavior = scroll,
            )
        },
        bottomBar = {
            // The fields always run under Save: its bar wears the tone of a bar scrolled under.
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Button(
                    onClick = onSave,
                    enabled = draft.name.isNotBlank() && draft.amount.isPositive && (!once || draft.dates != null),
                    modifier = Modifier
                        .fillMaxWidth()
                        // Drawn edge to edge, the window does not shrink for the keyboard: the
                        // button rides on it, and the fields above end where it begins.
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                        .padding(gutter)
                        .padding(horizontal = ScreenMargin, vertical = 12.dp)
                        .heightIn(min = 56.dp)
                        .testTag(CapsTags.EDITOR_SAVE),
                ) {
                    Text(stringResource(R.string.editor_save))
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(CapsTags.EDITOR)
                .verticalScroll(rememberScrollState())
                .padding(gutter)
                .padding(horizontal = ScreenMargin)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it.take(NAME_LENGTH))) },
                label = { Text(stringResource(R.string.editor_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth().testTag(CapsTags.EDITOR_NAME),
            )
            Spacer(Modifier.height(12.dp))
            MoneyField(
                amount = draft.amount,
                format = format,
                label = stringResource(R.string.editor_amount),
                onAmount = { onChange(draft.copy(amount = it)) },
                modifier = Modifier.fillMaxWidth().testTag(CapsTags.EDITOR_AMOUNT),
            )

            Section(stringResource(R.string.editor_period))
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val renewing = PeriodKind.entries.filter { it.renews }.map { it to it.label() }
                ChoiceSegments(
                    options = renewing,
                    selected = draft.kind.takeIf { !once },
                    onSelect = { onChange(draft.copy(kind = it)) },
                    modifier = Modifier.testTag(CapsTags.EDITOR_PERIODS),
                )
                OnceRow(
                    selected = once,
                    dates = draft.dates,
                    today = today,
                    onClick = {
                        if (!once) onChange(draft.copy(kind = PeriodKind.ONE_OFF))
                        // Chosen with no dates yet, they are what the choice asks next; chosen
                        // already, a touch is how its dates are changed.
                        if (once || draft.dates == null) picking = true
                    },
                )
            }
            AnimatedVisibility(visible = once) {
                Column {
                    EditorNote(stringResource(R.string.editor_once_note))
                    if (editing != null && editing != PeriodKind.ONE_OFF && reserve.isPositive) {
                        EditorNote(stringResource(R.string.editor_once_reserve_note, format.short(reserve)))
                    }
                }
            }
            if (editing != null && editing != draft.kind && !again) {
                EditorNote(stringResource(R.string.editor_kind_note))
            }

            Section(stringResource(R.string.editor_icon))
            IconGrid(selected = draft.icon, onSelect = { onChange(draft.copy(icon = it)) })
        }
    }
    if (picking) {
        DatesPicker(
            dates = draft.dates,
            today = today,
            onPick = {
                picking = false
                onChange(draft.copy(kind = PeriodKind.ONE_OFF, dates = it))
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun EditorNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.balanced(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = TextInset, end = TextInset, top = 8.dp),
    )
}

/**
 * The period that does not return, a choice beside the four that do: a one-off cap, between two
 * dates. Not chosen, it says what it is; chosen, it wears the chosen segment's ground and reads
 * its dates, "10 – 20 Nov · 11 days", or asks for them, and a touch opens the picker. One of the
 * period's radio buttons for TalkBack, named by its words.
 */
@Composable
private fun OnceRow(selected: Boolean, dates: DateRange?, today: LocalDate, onClick: () -> Unit) {
    val value = dates?.let {
        pluralStringResource(R.plurals.editor_dates_days, it.length, rangeWords(it, today), it.length)
    }
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = GroupShape,
        color = if (selected) colors.secondaryContainer else colors.surfaceContainer,
        contentColor = if (selected) colors.onSecondaryContainer else colors.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(GroupShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag(CapsTags.EDITOR_DATES),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                TettoIcons.Calendar,
                contentDescription = null,
                tint = if (selected) colors.onSecondaryContainer else colors.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(PeriodKind.ONE_OFF.label(), style = MaterialTheme.typography.bodyLarge)
                Text(
                    when {
                        !selected -> stringResource(R.string.editor_once_hint)
                        value != null -> value
                        else -> stringResource(R.string.editor_dates_choose)
                    },
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                )
            }
            // Chosen, the arrow to its dates; not chosen, the empty mark of a choice not made.
            if (selected) {
                Icon(TettoIcons.ChevronRight, contentDescription = null)
            } else {
                RadioButton(selected = false, onClick = null)
            }
        }
    }
}

/**
 * The icons of the palette across the page, their first on the fields' left edge and their last
 * on the right one: as many to a line as fit, six on a phone.
 */
@Composable
private fun IconGrid(selected: CapIcon, onSelect: (CapIcon) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = (maxWidth / IconSize).toInt().coerceIn(1, ICON_COLUMNS)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CapIcon.entries.chunked(columns).forEach { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    line.forEach { icon -> IconChoice(icon, selected = selected == icon) { onSelect(icon) } }
                    // A last line that is not full keeps the columns of the lines above it.
                    repeat(columns - line.size) { Spacer(Modifier.size(IconSize)) }
                }
            }
        }
    }
}

/**
 * Material's range picker on a page of its own, as Material sets a range on a phone: a cross to
 * leave it, Done at the bar's end, and the months scrolling under. The first and the last day, the
 * first today unless the cap has dates already. Any day can start a cap (a trip already under
 * way), but its last day cannot be before today: Done waits, and says why, until it is not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatesPicker(dates: DateRange?, today: LocalDate, onPick: (DateRange) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = (dates?.start ?: today).utcMillis(),
        initialSelectedEndDateMillis = dates?.lastDay?.utcMillis(),
        initialDisplayedMonthMillis = (dates?.start ?: today).utcMillis(),
    )
    val start = state.selectedStartDateMillis?.let(::utcDate)
    val last = state.selectedEndDateMillis?.let(::utcDate)
    val past = last != null && last < today
    val colors = DatePickerDefaults.colors()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = colors.containerColor, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp).heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(TettoIcons.Close, contentDescription = stringResource(DesignR.string.action_cancel))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { if (start != null && last != null) onPick(DateRange(start, last.plusDays(1))) },
                        enabled = start != null && last != null && !past,
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text(stringResource(R.string.editor_dates_done)) }
                }
                if (past) {
                    Text(
                        stringResource(R.string.editor_dates_past),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
                DateRangePicker(
                    state = state,
                    title = {
                        Text(
                            stringResource(R.string.editor_dates_title),
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp),
                        )
                    },
                    colors = colors,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** A day as the picker counts it: its midnight in UTC, whatever the phone's zone. */
private fun LocalDate.utcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun utcDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = TextInset, top = 24.dp, bottom = 8.dp).semantics { heading() },
    )
}

/** One icon of the palette, a choice among the others, named for TalkBack. */
@Composable
private fun IconChoice(icon: CapIcon, selected: Boolean, onClick: () -> Unit) {
    val label = icon.label()
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .size(IconSize)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon.symbol,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/** An icon's circle in the palette, and how many of them a line holds at most. */
private val IconSize: Dp = 52.dp
private const val ICON_COLUMNS = 6

/** How far in from a field's edge the page's own words start, as a section's title in Settings does. */
private val TextInset = 4.dp

/** A name a tile can hold on two lines. */
private const val NAME_LENGTH = 40

private val draftSaver = listSaver<CapDraft, Any>(
    save = {
        listOf(
            it.name,
            it.amount.minor,
            it.kind.name,
            it.icon.key,
            it.dates?.start?.toEpochDay() ?: NO_DATE,
            it.dates?.endExclusive?.toEpochDay() ?: NO_DATE,
        )
    },
    restore = {
        val start = it[4] as Long
        val end = it[5] as Long
        CapDraft(
            name = it[0] as String,
            amount = Money(it[1] as Long),
            kind = PeriodKind.valueOf(it[2] as String),
            icon = CapIcon.fromKey(it[3] as String),
            dates = if (start == NO_DATE) null else DateRange(LocalDate.ofEpochDay(start), LocalDate.ofEpochDay(end)),
        )
    },
)

private const val NO_DATE = Long.MIN_VALUE
