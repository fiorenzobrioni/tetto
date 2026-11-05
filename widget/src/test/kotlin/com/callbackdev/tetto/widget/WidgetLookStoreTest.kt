package com.callbackdev.tetto.widget

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.model.WidgetCardColor
import com.callbackdev.tetto.core.model.WidgetGround
import com.callbackdev.tetto.core.model.WidgetLook
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

/** One look per card, kept apart, and gone with the card (PLANNING.md §5, the `widgets` file). */
@RunWith(AndroidJUnit4::class)
class WidgetLookStoreTest {
    @get:Rule val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store by lazy {
        WidgetLookStore(
            PreferenceDataStoreFactory.create(scope = scope) {
                File(folder.root, "widgets.preferences_pb")
            },
        )
    }

    @After
    fun close() = scope.cancel()

    @Test
    fun `a card nobody configured wears the family's default`() = runTest {
        val look = store.lookFor(7)
        assertThat(look).isEqualTo(WidgetLook())
        assertThat(look.ground).isEqualTo(WidgetGround.COLOR)
        assertThat(look.cardColor).isEqualTo(WidgetCardColor.BLUE)
        assertThat(look.opacityPct).isEqualTo(100)
        assertThat(look.glanceCaps).isNull()
        assertThat(look.wordsCap).isNull()
        assertThat(look.showSentence).isTrue()
    }

    @Test
    fun `every choice is kept, per card`() = runTest {
        val chosen = WidgetLook(
            ground = WidgetGround.DARK,
            opacityPct = 60,
            cardColor = WidgetCardColor.CLAY,
            glanceCaps = setOf(3L, 12L),
            wordsCap = 12L,
            showSentence = false,
        )
        store.set(7, chosen)
        assertThat(store.lookFor(7)).isEqualTo(chosen)
        assertThat(store.lookFor(8)).isEqualTo(WidgetLook())
    }

    @Test
    fun `back to every cap and the closest, the choice is dropped`() = runTest {
        store.set(7, WidgetLook(glanceCaps = setOf(3L), wordsCap = 3L))
        store.set(7, WidgetLook())
        assertThat(store.lookFor(7)).isEqualTo(WidgetLook())
    }

    @Test
    fun `a removed card takes its look with it`() = runTest {
        store.set(7, WidgetLook(ground = WidgetGround.LIGHT))
        store.set(8, WidgetLook(ground = WidgetGround.DARK))
        store.forget(intArrayOf(7))
        assertThat(store.lookFor(7)).isEqualTo(WidgetLook())
        assertThat(store.lookFor(8).ground).isEqualTo(WidgetGround.DARK)
    }

    @Test
    fun `ids are written and read back as a set`() {
        assertThat(
            WidgetLookStore.decodeIds(WidgetLookStore.encodeIds(setOf(12L, 3L, 40L))),
        ).containsExactly(3L, 12L, 40L)
        assertThat(WidgetLookStore.decodeIds("3,,x,7")).containsExactly(3L, 7L)
    }
}
