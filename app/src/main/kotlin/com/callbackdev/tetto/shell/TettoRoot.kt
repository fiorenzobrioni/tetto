package com.callbackdev.tetto.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.callbackdev.tetto.R
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.padding
import com.callbackdev.tetto.core.designsystem.theme.pageGutter

/** The app's pages. One for now: the caps screen with no cap yet. */
@Composable
fun TettoRoot() {
    Surface(Modifier.fillMaxSize()) {
        CapsEmptyPage()
    }
}

/**
 * The caps screen before any cap: its sentence, in the place the caps' sentence takes (VISION,
 * «Caps»), and the line that says what a cap is. The "New cap" tile comes with the screen itself
 * (PLANNING.md §11 Phase 2): no button is drawn before it does something.
 */
@Composable
internal fun CapsEmptyPage(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .testTag(ShellTags.PAGE)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
            .padding(pageGutter())
            .padding(horizontal = ScreenMargin, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.caps_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.caps_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** What the shell's tests find the page by. */
internal object ShellTags {
    const val PAGE = "caps_empty_page"
}
