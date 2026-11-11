package com.callbackdev.tetto.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.tetto.R
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.balanced
import com.callbackdev.tetto.core.designsystem.theme.padding
import com.callbackdev.tetto.core.designsystem.theme.pageGutter

/**
 * What stands in Tetto's place while it waits for the phone's lock (owner, 6 Nov 2026; PLANNING.md
 * §11 Phase 6b): nothing of the reader's, only the lock, the sentence that says what opens it, and
 * the button that asks again after the phone's own prompt was dismissed. Tetto asks once by itself
 * when the page comes on screen; never a prompt that keeps coming back.
 */
@Composable
fun LockedPage(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize().testTag(LockTags.PAGE)) {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(pageGutter())
                .padding(horizontal = ScreenMargin, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            LockedContent(onUnlock, Modifier.widthIn(max = 420.dp))
        }
    }
}

/** The same, in the numpad's sheet over the home screen: [onClose] lets the sheet go. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockedSheet(onUnlock: () -> Unit, onClose: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LockedContent(
            onUnlock,
            Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).padding(bottom = 24.dp).testTag(LockTags.SHEET),
        )
    }
}

@Composable
private fun LockedContent(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                TettoIcons.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.lock_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.lock_body),
            style = MaterialTheme.typography.bodyLarge.balanced(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onUnlock,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(LockTags.UNLOCK),
        ) {
            Icon(TettoIcons.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.lock_unlock))
        }
    }
}

/** What the tests find the locked page by. */
object LockTags {
    const val PAGE = "lock_page"
    const val SHEET = "lock_sheet"
    const val UNLOCK = "lock_unlock"
}
