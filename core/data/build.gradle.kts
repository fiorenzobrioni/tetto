// Room (the caps, their spends, the closed periods) and DataStore (the settings), behind
// repositories that expose Flow and run the catch-up before every read (PLANNING.md §4.4, §5).
plugins {
    alias(libs.plugins.tetto.android.library)
    alias(libs.plugins.tetto.android.hilt)
    alias(libs.plugins.tetto.android.room)
}

android {
    namespace = "com.callbackdev.tetto.core.data"

    // The exported schemas, for the schema and migration tests (MigrationTestHelper reads them
    // as assets).
    sourceSets {
        getByName("test") {
            assets.directories += "$projectDir/schemas"
        }
    }
}

dependencies {
    api(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
