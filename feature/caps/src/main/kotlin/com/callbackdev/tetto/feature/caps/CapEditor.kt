package com.callbackdev.tetto.feature.caps

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.padding
import com.callbackdev.tetto.core.designsystem.theme.pageGutter
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapDraft
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import kotlinx.coroutines.launch
import com.callbackdev.tetto.core.designsystem.R as DesignR

/**
 * The cap editor (PLANNING.md §11 Phase 2): a new cap with [capId] null, else that cap. Its fields
 * survive a rotation; they are filled once from the cap, then the reader's.
 */
@Composable
fun CapEditorRoute(capId: Long?, onBack: () -> Unit, viewModel: CapsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state ?: return
    var filled by rememberSaveable { mutableStateOf(capId == null) }
    var draft by rememberSaveable(stateSaver = draftSaver) {
        mutableStateOf(CapDraft("", Money.ZERO, PeriodKind.MONTH, CapIcon.OTHER))
    }
    LaunchedEffect(current, filled) {
        if (!filled) {
            current.cap(capId ?: return@LaunchedEffect)?.cap?.let { cap ->
                draft = CapDraft(cap.name, cap.amount, cap.kind, cap.icon)
            }
            filled = true
        }
    }
    if (!filled) return
    val scope = rememberCoroutineScope()
    CapEditorScreen(
        editing = capId?.let { current.cap(it)?.cap?.kind },
        draft = draft,
        currency = current.currency,
        onChange = { draft = it },
        onSave = {
            scope.launch {
                viewModel.saveCap(capId, draft.name, draft.icon, draft.amount, draft.kind)
                onBack()
            }
        },
        onBack = onBack,
    )
}

/**
 * The editor drawn from its draft. [editing] is the kind the cap has now, null for a new cap: a
 * change of kind says what it does to this period (§4.5).
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
) {
    val format = rememberMoneyFormat(currency)
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
                stringResource(if (editing == null) R.string.new_cap else R.string.editor_edit),
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
                    onClick = { onChange(draft.copy(kind = kind)) },
                    label = { Text(kind.label()) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        if (editing != null && editing != draft.kind) {
            Text(
                stringResource(R.string.editor_kind_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
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
            enabled = draft.name.isNotBlank() && draft.amount.isPositive,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(CapsTags.EDITOR_SAVE),
        ) {
            Text(stringResource(R.string.editor_save))
        }
    }
}

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
    save = { listOf(it.name, it.amount.minor, it.kind.name, it.icon.key) },
    restore = {
        CapDraft(
            name = it[0] as String,
            amount = Money(it[1] as Long),
            kind = PeriodKind.valueOf(it[2] as String),
            icon = CapIcon.fromKey(it[3] as String),
        )
    },
)
