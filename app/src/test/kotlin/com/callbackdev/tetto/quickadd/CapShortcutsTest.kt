package com.callbackdev.tetto.quickadd

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.data.widget.QuickAdd
import com.callbackdev.tetto.core.model.CapIcon
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** The launcher's shortcuts (PLANNING.md §8): the first four caps, in the reader's order and words. */
@RunWith(AndroidJUnit4::class)
class CapShortcutsTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    private val caps = listOf(
        ShortcutCap(3, "Coffee", CapIcon.COFFEE),
        ShortcutCap(4, "Groceries", CapIcon.GROCERIES),
        ShortcutCap(7, "Eating out", CapIcon.EATING_OUT),
        ShortcutCap(9, "Transport", CapIcon.TRANSPORT),
        ShortcutCap(12, "Fun", CapIcon.FUN),
    )

    private fun published() = ShortcutManagerCompat.getDynamicShortcuts(app).sortedBy { it.rank }

    @Test
    fun `the first four caps, each straight to its numpad`() {
        CapShortcuts.publish(app, caps)
        val shortcuts = published()
        assertThat(
            shortcuts.map {
                it.shortLabel.toString()
            },
        ).containsExactly("Coffee", "Groceries", "Eating out", "Transport").inOrder()
        assertThat(shortcuts.first().longLabel.toString()).isEqualTo("Add a spend to Coffee")
        assertThat(shortcuts.first().intent.getLongExtra(QuickAdd.EXTRA_CAP_ID, -1)).isEqualTo(3)
    }

    @Test
    fun `a new order, a new name, or fewer caps reach the launcher`() {
        CapShortcuts.publish(app, caps)
        CapShortcuts.publish(app, listOf(caps[1].copy(name = "Food"), caps[0]))
        assertThat(published().map { it.shortLabel.toString() }).containsExactly("Food", "Coffee").inOrder()
    }

    @Test
    fun `a change of the app's language reaches the launcher`() {
        CapShortcuts.publish(app, caps)
        CapShortcuts.publish(app.inLanguage(Locale.ITALIAN), caps)
        assertThat(published().first().longLabel.toString()).isEqualTo("Aggiungi una spesa a Coffee")
    }

    private fun Context.inLanguage(locale: Locale): Context =
        createConfigurationContext(Configuration(resources.configuration).apply { setLocale(locale) })
}
