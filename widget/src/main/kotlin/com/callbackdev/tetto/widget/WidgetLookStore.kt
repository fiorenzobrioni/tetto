package com.callbackdev.tetto.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.callbackdev.tetto.core.model.WidgetCardColor
import com.callbackdev.tetto.core.model.WidgetGround
import com.callbackdev.tetto.core.model.WidgetLook
import com.callbackdev.tetto.core.model.WordsBelow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The file the cards' looks live in (PLANNING.md §5, `widgets`): outside the backup's allowlist
 * on purpose, because a restore renews the cards' ids and a look kept under an old id would dress
 * nothing, or the wrong card.
 */
internal val Context.widgetLookDataStore by preferencesDataStore(name = WidgetLookStore.FILE_NAME)

/**
 * The looks, keyed by appWidgetId. Presentation only, so it lives with the widgets and not in
 * `:core:data`; a removed card takes its look with it ([forget]).
 */
@Singleton
class WidgetLookStore internal constructor(private val dataStore: DataStore<Preferences>) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.widgetLookDataStore)

    suspend fun lookFor(appWidgetId: Int): WidgetLook {
        val prefs = dataStore.data.first()
        val default = WidgetLook()
        return WidgetLook(
            ground = prefs[groundKey(appWidgetId)]
                ?.let { name -> WidgetGround.entries.firstOrNull { it.name == name } }
                ?: default.ground,
            opacityPct = (prefs[opacityKey(appWidgetId)] ?: default.opacityPct).coerceIn(0, 100),
            cardColor = prefs[colorKey(appWidgetId)]
                ?.let { name -> WidgetCardColor.entries.firstOrNull { it.name == name } }
                ?: default.cardColor,
            glanceCaps = prefs[glanceCapsKey(appWidgetId)]?.let(::decodeIds),
            wordsCap = prefs[wordsCapKey(appWidgetId)],
            showSentence = prefs[sentenceKey(appWidgetId)] ?: default.showSentence,
            wordsBelow = prefs[belowKey(appWidgetId)]
                ?.let { name -> WordsBelow.entries.firstOrNull { it.name == name } }
                ?: default.wordsBelow,
            wordsBelowCaps = prefs[belowCapsKey(appWidgetId)]?.let(::decodeIds) ?: default.wordsBelowCaps,
            hideAmounts = prefs[hiddenKey(appWidgetId)] ?: default.hideAmounts,
        )
    }

    suspend fun set(appWidgetId: Int, look: WidgetLook) {
        dataStore.edit { prefs ->
            prefs[groundKey(appWidgetId)] = look.ground.name
            prefs[opacityKey(appWidgetId)] = look.opacityPct.coerceIn(0, 100)
            prefs[colorKey(appWidgetId)] = look.cardColor.name
            val chosen = look.glanceCaps
            if (chosen ==
                null
            ) {
                prefs.remove(glanceCapsKey(appWidgetId))
            } else {
                prefs[glanceCapsKey(appWidgetId)] =
                    encodeIds(chosen)
            }
            val cap = look.wordsCap
            if (cap == null) prefs.remove(wordsCapKey(appWidgetId)) else prefs[wordsCapKey(appWidgetId)] = cap
            prefs[sentenceKey(appWidgetId)] = look.showSentence
            prefs[belowKey(appWidgetId)] = look.wordsBelow.name
            prefs[belowCapsKey(appWidgetId)] = encodeIds(look.wordsBelowCaps)
            prefs[hiddenKey(appWidgetId)] = look.hideAmounts
        }
    }

    /** From the receivers' `onDeleted`: a removed card leaves nothing behind. */
    suspend fun forget(appWidgetIds: IntArray) {
        dataStore.edit { prefs ->
            appWidgetIds.forEach { id ->
                listOf(
                    groundKey(id),
                    opacityKey(id),
                    colorKey(id),
                    glanceCapsKey(id),
                    wordsCapKey(id),
                    sentenceKey(id),
                    belowKey(id),
                    belowCapsKey(id),
                    hiddenKey(id),
                ).forEach { prefs.remove(it) }
            }
        }
    }

    private fun groundKey(id: Int) = stringPreferencesKey("ground_$id")
    private fun opacityKey(id: Int) = intPreferencesKey("opacity_$id")
    private fun colorKey(id: Int) = stringPreferencesKey("card_color_$id")
    private fun glanceCapsKey(id: Int) = stringPreferencesKey("glance_caps_$id")
    private fun wordsCapKey(id: Int) = longPreferencesKey("words_cap_$id")
    private fun sentenceKey(id: Int) = booleanPreferencesKey("sentence_$id")
    private fun hiddenKey(id: Int) = booleanPreferencesKey("hide_amounts_$id")
    private fun belowKey(id: Int) = stringPreferencesKey("words_below_$id")
    private fun belowCapsKey(id: Int) = stringPreferencesKey("words_below_caps_$id")

    companion object {
        const val FILE_NAME: String = "widgets"

        /** A set of ids as one string ("3,7,12"); an empty choice is kept as an empty set. */
        internal fun encodeIds(ids: Set<Long>): String = ids.sorted().joinToString(",")

        internal fun decodeIds(text: String): Set<Long> = text.split(',').mapNotNull {
            it.trim().toLongOrNull()
        }.toSet()
    }
}
