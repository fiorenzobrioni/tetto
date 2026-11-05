package com.callbackdev.tetto.core.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The database is a contract (PLANNING.md §5): a database made from the committed schema of
 * version 1, the first release's, opens with today's code (Room checks the schema's identity) and
 * reads back what it holds.
 *
 * Made here from the exported JSON rather than by Room's `MigrationTestHelper`: on Windows its
 * driver rejects the database's own path (it looks for the file name after a `/`, and the path
 * there has `\`), so the helper's tests would run on CI only. The migration tests of version 2 on
 * may take the helper, and that limit with it.
 */
@RunWith(AndroidJUnit4::class)
class SchemaTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `a database made from the exported schema opens with the code's`() = runTest {
        createFromSchema(version = 1).apply {
            execSQL(
                "INSERT INTO caps (id, name, icon, amount, kind, reserve, open_start, position, created_at) " +
                    "VALUES (1, 'Coffee', 'coffee', 2000, 'WEEK', 300, 20394, 0, 0)",
            )
            execSQL("INSERT INTO spends (id, cap_id, amount, added_at, period_start) VALUES (1, 1, 350, 0, 20394)")
            close()
        }

        val database = Room.databaseBuilder(context, TettoDatabase::class.java, NAME).build()
        try {
            val row = database.capsDao().capRows().single()
            assertThat(row.cap.name).isEqualTo("Coffee")
            assertThat(row.cap.reserve).isEqualTo(300)
            assertThat(row.spent).isEqualTo(350)
            assertThat(row.lastClosedEnd).isNull()
        } finally {
            database.close()
        }
    }

    /** The tables, indices and Room's identity row of the exported [version], as Room would write them. */
    private fun createFromSchema(version: Int): SQLiteDatabase {
        val path = "${TettoDatabase::class.java.name}/$version.json"
        val schema = JSONObject(
            context.assets.open(path).bufferedReader().use {
                it.readText()
            },
        ).getJSONObject("database")
        val file = context.getDatabasePath(NAME).apply { parentFile?.mkdirs() }
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = entity.optJSONArray("indices") ?: continue
            for (j in 0 until indices.length()) {
                db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        db.version = schema.getInt("version")
        return db
    }

    private companion object {
        const val NAME = "schema-test.db"
    }
}
