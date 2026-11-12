package com.callbackdev.tetto.shell

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.callbackdev.tetto.R
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.theme.balanced
import com.callbackdev.tetto.core.designsystem.theme.padding
import com.callbackdev.tetto.core.designsystem.theme.pageGutter
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.core.reminder.EveningReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.callbackdev.tetto.core.reminder.R as ReminderR

/**
 * The first run's evening reminder (owner, 7 Nov 2026): Tetto's one notification, offered on the
 * run's last page, so a reader learns it exists without having to find it in Settings. Nothing is
 * asked unless they choose it: «Remind me at 8:30 PM» turns it on and, only then, lets Android ask
 * for the notifications; «Not now» leaves it off, and Settings keeps it for later.
 */
@HiltViewModel
class ReminderOfferViewModel
@Inject
constructor(
    private val settings: SettingsRepository,
    private val reminder: EveningReminder,
) : ViewModel() {
    /** The reminder's time, the default one at a first run; null until the settings are read. */
    val time: StateFlow<LocalTime?> = settings.settings.map { it.reminderTime }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Android is about to ask: from now on, a refusal may be for good (Settings tells them apart). */
    fun noteAsked() {
        viewModelScope.launch { settings.noteNotificationsAsked() }
    }

    /** The reminder on, its alarm armed now that the notifications can show. */
    fun turnOn() {
        viewModelScope.launch {
            settings.setReminder(true)
            reminder.accessMayHaveChanged()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * Whether the page has something to offer: the reminder off, and the notifications allowed or
 * still askable. Where Android would no longer ask (refused for good, after an erase), a touch
 * would do nothing: the page is not drawn, and Settings says how to allow them.
 */
fun reminderWorthOffering(context: Context, activity: Activity?, settings: UserSettings): Boolean {
    if (settings.reminderOn) return false
    return notificationsAllowed(context) ||
        !settings.askedNotifications ||
        activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
}

private fun notificationsAllowed(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun ReminderOfferRoute(
    onLeave: () -> Unit,
    where: String? = null,
    viewModel: ReminderOfferViewModel = hiltViewModel(),
) {
    val time by viewModel.time.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Android's own question; whatever the answer, the first run is over. Refused, the reminder
    // stays off: the reader said no to it, here, and Settings keeps it for another day.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.turnOn()
        onLeave()
    }
    ReminderOfferPage(
        time = time ?: UserSettings.DEFAULT_REMINDER_TIME,
        onRemind = {
            if (notificationsAllowed(context)) {
                viewModel.turnOn()
                onLeave()
            } else {
                viewModel.noteAsked()
                runCatching { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }.onFailure { onLeave() }
            }
        },
        onLeave = onLeave,
        where = where,
    )
}

/**
 * «A word in the evening», in the first run's frame (no icon of the app's, one sentence, one wide
 * button and the quiet way past under it): what the reminder says, drawn as an example of the
 * notification, and when; the button turns it on, at its time. [where] is the run's step, said on
 * the heading as the run's pages say it.
 */
@Composable
fun ReminderOfferPage(time: LocalTime, onRemind: () -> Unit, onLeave: () -> Unit, where: String? = null) {
    val gutter = pageGutter()
    val clock = clockTime(time)
    BackHandler(onBack = onLeave)
    Surface(Modifier.fillMaxSize().testTag(ReminderOfferTags.PAGE)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            BoxWithConstraints(Modifier.weight(1f)) {
                val room = maxHeight
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(ReminderOfferTags.SCROLL)
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = room)
                        .padding(gutter)
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.reminder_offer_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.firstRunHeading(where),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.reminder_offer_body),
                        style = MaterialTheme.typography.bodyLarge.balanced(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(32.dp))
                    NotificationExample(clock)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.reminder_offer_note),
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
                Button(
                    onClick = onRemind,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(ReminderOfferTags.REMIND),
                ) {
                    Text(stringResource(R.string.reminder_offer_on, clock))
                }
                TextButton(
                    onClick = onLeave,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(ReminderOfferTags.LATER),
                ) {
                    Text(stringResource(R.string.home_offer_later))
                }
            }
        }
    }
}

/**
 * The evening's notification, as an example: the bell, the time, its question and its line, in the
 * words the real one uses. A picture of a notification, not the system's own: read by TalkBack as
 * one sentence that says it is an example.
 */
@Composable
private fun NotificationExample(clock: String) {
    val title = stringResource(ReminderR.string.reminder_title)
    val text = stringResource(ReminderR.string.reminder_text)
    val description = stringResource(R.string.reminder_offer_example, clock, title)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .testTag(ReminderOfferTags.EXAMPLE)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    TettoIcons.Bell,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.reminder_offer_example_head, stringResource(R.string.app_name), clock),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The time in the phone's own clock, 12 or 24 hours, in the locale's pattern. */
@Composable
private fun clockTime(time: LocalTime): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
    return remember(time, locale, skeleton) {
        time.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
    }
}

/** Tags for the tests. */
object ReminderOfferTags {
    const val PAGE = "reminder_offer"

    /** The words and the example, which scroll over a short screen or at a large text size. */
    const val SCROLL = "reminder_offer_scroll"
    const val EXAMPLE = "reminder_offer_example"
    const val REMIND = "reminder_offer_remind"
    const val LATER = "reminder_offer_later"
}
