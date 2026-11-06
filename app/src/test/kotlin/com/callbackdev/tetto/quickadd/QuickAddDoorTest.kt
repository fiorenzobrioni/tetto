package com.callbackdev.tetto.quickadd

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.data.widget.QuickAdd
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The door the cards and the shortcuts open: named by a string where `:app` cannot be seen, so this
 * holds the string to the activity, and the activity to the manifest.
 */
@RunWith(AndroidJUnit4::class)
class QuickAddDoorTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `the door names the activity`() {
        assertThat(QuickAdd.ACTIVITY).isEqualTo(QuickAddActivity::class.java.name)
    }

    @Test
    fun `each cap's door is its own, in its own task, to the activity in the manifest`() {
        val coffee = QuickAdd.intent(app, 3)
        val groceries = QuickAdd.intent(app, 4)
        assertThat(coffee.filterEquals(groceries)).isFalse()
        assertThat(coffee.getLongExtra(QuickAdd.EXTRA_CAP_ID, -1)).isEqualTo(3)
        assertThat(coffee.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0)
        assertThat(
            app.packageManager.resolveActivity(coffee, 0)?.activityInfo?.name,
        ).isEqualTo(QuickAddActivity::class.java.name)
    }
}
