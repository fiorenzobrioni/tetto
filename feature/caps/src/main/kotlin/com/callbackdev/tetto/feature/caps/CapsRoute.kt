package com.callbackdev.tetto.feature.caps

import android.content.res.Resources
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tetto.core.designsystem.format.MoneyFormat
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import kotlinx.coroutines.flow.collectLatest
import com.callbackdev.tetto.core.designsystem.R as DesignR

/**
 * The caps screen with its numpad, its sheets and its notices (PLANNING.md §11 Phase 2). Each
 * change is said after it in a snackbar with Undo, for as long as Material's accessible timing
 * gives (the reader's accessibility timeout, longer with TalkBack).
 */
@Composable
fun CapsRoute(
    onNewCap: () -> Unit,
    onEditCap: (Long) -> Unit,
    onChangeOrder: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    viewModel: CapsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val numpad by viewModel.numpad.collectAsStateWithLifecycle()
    val numpadAmounts by viewModel.numpadAmounts.collectAsStateWithLifecycle()
    val current = state ?: return
    val format = rememberMoneyFormat(current.currency)
    val snackbars = remember { SnackbarHostState() }
    val resources = LocalResources.current
    var confirmingDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(viewModel, format, resources) {
        // The latest notice replaces the one on screen, never waits behind it: three spends added
        // in a row offer the third's Undo at once (owner, 6 Nov 2026). The one replaced is
        // dismissed as its wait is cancelled.
        viewModel.notices.collectLatest { notice ->
            val result = snackbars.showSnackbar(
                message = noticeWords(notice, format, resources),
                actionLabel = if (notice.undoable) resources.getString(R.string.action_undo) else null,
                withDismissAction = false,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(notice)
        }
    }

    // The page keeps its own insets; the snackbar stands above the navigation bar.
    Scaffold(
        snackbarHost = { SnackbarHost(snackbars, Modifier.navigationBarsPadding()) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        CapsScreen(
            state = current,
            onAddSpend = { viewModel.openNumpad(it) },
            onOpenCap = viewModel::openSheet,
            onNewCap = onNewCap,
            onChangeOrder = onChangeOrder,
            onOpenSettings = onOpenSettings,
            onOpenGuide = {
                viewModel.onTipSeen()
                onOpenGuide()
            },
            onDismissTip = viewModel::onTipSeen,
            contentPadding = padding,
        )
    }

    val sheetCap = sheet?.capId?.let(current::cap)
    if (sheetCap != null) {
        CapSheet(
            state = sheetCap,
            data = sheet,
            currency = current.currency,
            today = current.today,
            actions = CapSheetActions(
                onCorrect = { spend -> viewModel.openNumpad(spend.capId, spend) },
                onDelete = viewModel::deleteSpend,
                onClearReserve = { viewModel.clearReserve(sheetCap.cap.id) },
                onEdit = {
                    viewModel.closeSheet()
                    onEditCap(sheetCap.cap.id)
                },
                onDeleteCap = { confirmingDelete = sheetCap.cap.id },
            ),
            onDismiss = viewModel::closeSheet,
        )
    }

    val target = numpad
    val numpadCap = target?.capId?.let(current::cap)
    if (target != null && numpadCap != null) {
        NumpadSheet(
            state = numpadCap,
            currency = current.currency,
            correcting = target.correcting?.amount,
            amounts = numpadAmounts,
            onConfirm = { amount -> viewModel.confirmNumpad(target, amount) },
            onDismiss = viewModel::closeNumpad,
            // From the numpad's header to the cap's sheet; not while correcting, which came from it.
            onOpenCap = if (target.correcting == null) {
                {
                    viewModel.closeNumpad()
                    viewModel.openSheet(target.capId)
                }
            } else {
                null
            },
        )
    }

    val deleting = confirmingDelete?.let(current::cap)
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = null },
            title = { Text(stringResource(R.string.delete_title, deleting.cap.name)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = null
                    viewModel.deleteCap(deleting.cap.id)
                }) { Text(stringResource(R.string.delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = null }) { Text(stringResource(DesignR.string.action_cancel)) }
            },
        )
    }
}

private val CapsNotice.undoable: Boolean
    get() = this is CapsNotice.Added || this is CapsNotice.Corrected || this is CapsNotice.Deleted ||
        this is CapsNotice.ReserveCleared

/** A notice's words, outside composition: the snackbar is shown from a coroutine. */
internal fun noticeWords(notice: CapsNotice, format: MoneyFormat, resources: Resources): String = when (notice) {
    is CapsNotice.Added -> resources.getString(R.string.notice_added, format.full(notice.spend.amount), notice.capName)

    is CapsNotice.Corrected -> resources.getString(R.string.notice_corrected, format.full(notice.amount))

    is CapsNotice.Deleted -> resources.getString(
        R.string.notice_deleted,
        format.full(notice.spend.amount),
        notice.capName,
    )

    is CapsNotice.ReserveCleared -> resources.getString(R.string.notice_reserve_cleared, notice.capName)

    is CapsNotice.CapDeleted -> resources.getString(R.string.notice_cap_deleted, notice.capName)

    CapsNotice.PeriodClosed -> resources.getString(R.string.notice_period_closed)
}
