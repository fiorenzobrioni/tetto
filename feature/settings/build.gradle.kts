// Settings (PLANNING.md §11 Phases 3 and 4): the money (currency, month start, week start), the
// evening reminder, the appearance, the language, your data (the privacy statement, the backup,
// the restore, the spends for a spreadsheet, erasing everything) and about. The guide's card
// joins with Phase 6.
plugins {
    alias(libs.plugins.tetto.android.feature)
}

android {
    namespace = "com.callbackdev.tetto.feature.settings"
}

dependencies {
    // The reminder's alarm and whether its notifications can show.
    implementation(project(":core:reminder"))

    implementation(libs.androidx.core.ktx)
    // The system's file picker and the permission's question.
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.androidx.junit)
    // The ViewModel's test runs on a real database and settings file, as the app does.
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.datastore.preferences)
}
