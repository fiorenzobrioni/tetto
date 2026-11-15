package com.callbackdev.tetto.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.callbackdev.tetto.R
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.data.widget.HomeCard
import com.callbackdev.tetto.core.data.widget.HomeScreenCards
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.theme.GroupShape
import com.callbackdev.tetto.core.designsystem.theme.balanced
import com.callbackdev.tetto.core.designsystem.theme.padding
import com.callbackdev.tetto.core.designsystem.theme.pageGutter
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.widget.config.HomeCardPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject
import com.callbackdev.tetto.feature.onboarding.R as OnboardingR
import com.callbackdev.tetto.widget.R as WidgetR

/** What the first run's last page shows: the reader's new caps, as a card will draw them. */
data class HomeOfferState(
    val caps: List<CapState>,
    val settings: UserSettings,
    val currency: AppCurrency,
    val today: LocalDate,
)

/**
 * The first run's last page (PLANNING.md §8, the pin request): offered only where the launcher can
 * place a card on request and none of Tetto's is there yet, after a run that made caps.
 */
@HiltViewModel
class HomeOfferViewModel
@Inject
constructor(
    caps: CapsRepository,
    settings: SettingsRepository,
    private val cards: HomeScreenCards,
    private val clock: TodaySource,
) : ViewModel() {
    val state: StateFlow<HomeOfferState?> =
        combine(caps.states, settings.settings, settings.currency) { states, stored, currency ->
            HomeOfferState(states, stored, currency, clock.today())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Whether the page has anything to offer: a launcher that takes the request, and no card yet. */
    fun worthOffering(): Boolean = cards.canPin() && !cards.placed()

    fun pin(card: HomeCard): Boolean = cards.pin(card)

    fun placed(): Boolean = cards.placed()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

@Composable
fun HomeOfferRoute(
    onLeave: () -> Unit,
    where: String? = null,
    last: Boolean = true,
    viewModel: HomeOfferViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var placed by remember { mutableStateOf(viewModel.placed()) }
    // The launcher's own dialog decides; back from it, the page reads the home screen again.
    LifecycleResumeEffect(Unit) {
        placed = viewModel.placed()
        onPauseOrDispose { }
    }
    HomeOfferPage(
        state = state,
        placed = placed,
        onPin = { card -> viewModel.pin(card) },
        onLeave = onLeave,
        where = where,
        last = last,
    )
}

/**
 * «Your caps on the home screen», in the first run's frame (no icon, one sentence, one wide button):
 * the two cards to choose from, as the family's first runs offer theirs (owner, 15 Nov 2026), «At a
 * glance» first as in the launcher's list, each drawn from the reader's own caps, with its line and
 * an «Add» that hands the request to the launcher, whose dialog places it. A new reader sees one cap
 * on the first and every cap on the second, and picks. Once a card is there the page says so. The
 * wide button moves on: «Done» on the run's last page, «Continue» when the reminder's page follows.
 * [where] is the run's step, said on the heading as the run's pages say it.
 */
@Composable
fun HomeOfferPage(
    state: HomeOfferState?,
    placed: Boolean,
    onPin: (HomeCard) -> Unit,
    onLeave: () -> Unit,
    where: String? = null,
    last: Boolean = true,
) {
    val gutter = pageGutter()
    BackHandler(onBack = onLeave)
    Surface(Modifier.fillMaxSize().testTag(HomeOfferTags.PAGE)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            BoxWithConstraints(Modifier.weight(1f)) {
                val room = maxHeight
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = room)
                        .padding(gutter)
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_offer_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.firstRunHeading(where),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.home_offer_body),
                        style = MaterialTheme.typography.bodyLarge.balanced(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CardChoice(
                            card = HomeCard.AT_A_GLANCE,
                            title = stringResource(WidgetR.string.widget_words_label),
                            body = stringResource(R.string.home_offer_glance_body),
                            state = state,
                            onPin = onPin,
                        )
                        CardChoice(
                            card = HomeCard.LIST,
                            title = stringResource(WidgetR.string.widget_glance_label),
                            body = stringResource(R.string.home_offer_list_body),
                            state = state,
                            onPin = onPin,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.home_offer_note),
                        style = MaterialTheme.typography.bodySmall.balanced(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(gutter)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (placed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                        }.testTag(HomeOfferTags.PLACED),
                    ) {
                        Icon(
                            TettoIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(R.string.home_offer_added),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                Button(
                    onClick = onLeave,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(HomeOfferTags.PRIMARY),
                ) {
                    Text(
                        if (last) {
                            stringResource(R.string.home_offer_done)
                        } else {
                            stringResource(OnboardingR.string.onboarding_next)
                        },
                    )
                }
            }
        }
    }
}

/**
 * One card to choose, in the family's choice group: its name, its line, the card as the reader's
 * caps will stand on it (once they are read), and «Add», whose spoken label names the card.
 */
@Composable
private fun CardChoice(
    card: HomeCard,
    title: String,
    body: String,
    state: HomeOfferState?,
    onPin: (HomeCard) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().testTag(HomeOfferTags.choice(card)),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.balanced(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state != null) {
                Box(Modifier.padding(top = 6.dp).testTag(HomeOfferTags.preview(card))) {
                    HomeCardPreview(card, state.caps, state.currency, state.settings, state.today, inset = 0.dp)
                }
            }
            val label = stringResource(R.string.home_offer_add_a11y, title)
            FilledTonalButton(
                onClick = { onPin(card) },
                modifier = Modifier
                    .padding(top = 6.dp)
                    .semantics { contentDescription = label }
                    .testTag(HomeOfferTags.pin(card)),
            ) {
                Text(stringResource(R.string.home_offer_add))
            }
        }
    }
}

/** A first run's page heading, with where the run stands when it is known. */
internal fun Modifier.firstRunHeading(where: String?): Modifier = semantics {
    heading()
    if (where != null) stateDescription = where
}

/** Tags for the tests. */
object HomeOfferTags {
    const val PAGE = "home_offer"
    const val PRIMARY = "home_offer_primary"
    const val PLACED = "home_offer_placed"

    fun choice(card: HomeCard) = "home_offer_choice_${card.name.lowercase()}"

    fun preview(card: HomeCard) = "home_offer_preview_${card.name.lowercase()}"

    fun pin(card: HomeCard) = "home_offer_pin_${card.name.lowercase()}"
}
