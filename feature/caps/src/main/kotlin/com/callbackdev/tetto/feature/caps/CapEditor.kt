package com.callbackdev.tetto.feature.caps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tetto.core.designsystem.components.MoneyField
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.icons.label
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.GroupShape
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
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
    )
}

/**
 * The editor drawn from its draft. [editing] is the kind the cap has now, null for a new cap: a
 * change of kind says what it does to this period (§4.5). A one-off's dates (owner, 6 Nov 2026)
 * come from Material's range picker, said in the locale's patterns; an end before [today] is
 * refused. [reserve] is the cap's, let go when it becomes a one-off, and said so; [again], an
 * ended one-off running again with new dates.
 */
@OptIn(ExperimentalLayoutApi::class)
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
) {
    val format = rememberMoneyFormat(currency)
    // An ended one-off, again: the picker opens at once, its dates being what is new.
    var picking by rememberSaveable { mutableStateOf(again && draft.dates == null) }
    Column(
        modifier
            .fillMaxSize()
            .testTag(CapsTags.EDITOR)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
            .imePadding()
            .padding(pageGutter())
            .padding(horizontal = ScreenMargin, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(TettoIcons.Back, contentDescription = stringResource(DesignR.string.action_back))
            }
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(
                    when {
                        again -> R.string.editor_again
                        editing == null -> R.string.new_cap
                        else -> R.string.editor_edit
                    },
                ),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
        }
        Spacer(Modifier.height(16.dp))
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
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodKind.entries.forEach { kind ->
                FilterChip(
                    selected = draft.kind == kind,
                    onClick = {
                        onChange(draft.copy(kind = kind))
                        // Once, with no dates yet: they are what the choice asks next.
                        if (kind == PeriodKind.ONE_OFF && draft.dates == null) picking = true
                    },
                    label = { Text(kind.label()) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        val once = draft.kind == PeriodKind.ONE_OFF
        AnimatedVisibility(visible = once) {
            Column {
                Spacer(Modifier.height(12.dp))
                DatesRow(draft.dates, today, onClick = { picking = true })
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
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CapIcon.entries.forEach { icon ->
                IconChoice(icon, selected = draft.icon == icon) { onChange(draft.copy(icon = icon)) }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSave,
            enabled = draft.name.isNotBlank() && draft.amount.isPositive && (!once || draft.dates != null),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(CapsTags.EDITOR_SAVE),
        ) {
            Text(stringResource(R.string.editor_save))
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
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/**
 * A one-off's dates, as a row that opens the picker: "10 – 20 Nov · 11 days", or the words that ask
 * for them. The whole row is the touch, named by its words for TalkBack.
 */
@Composable
private fun DatesRow(dates: DateRange?, today: LocalDate, onClick: () -> Unit) {
    val value = dates?.let {
        pluralStringResource(R.plurals.editor_dates_days, it.length, rangeWords(it, today), it.length)
    }
    Surface(
        onClick = onClick,
        shape = GroupShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(CapsTags.EDITOR_DATES),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(TettoIcons.Calendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.editor_dates),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    value ?: stringResource(R.string.editor_dates_choose),
                    style = MaterialTheme.typography.bodyLarge.tabular(),
                    color = if (value ==
                        null
                    ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            Icon(TettoIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Material's range picker in its dialog: the first and the last day, the first today unless the
 * cap has dates already. Any day can start a cap (a trip already under way), but its last day
 * cannot be before today: Done waits, and says why, until it is not.
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
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { if (start != null && last != null) onPick(DateRange(start, last.plusDays(1))) },
                enabled = start != null && last != null && !past,
            ) { Text(stringResource(R.string.editor_dates_done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.action_cancel)) }
        },
    ) {
        DateRangePicker(
            state = state,
            title = {
                Text(
                    stringResource(R.string.editor_dates_title),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
            modifier = Modifier.weight(1f),
        )
        if (past) {
            Text(
                stringResource(R.string.editor_dates_past),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
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
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp).semantics { heading() },
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
            .size(52.dp)
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
