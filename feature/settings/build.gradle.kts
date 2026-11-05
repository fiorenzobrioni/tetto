// Settings (PLANNING.md §11 Phase 3): the money (currency, month start, week start), the
// appearance, the language, the privacy statement and about. The reminder and your data join
// with Phase 4, the guide's card with Phase 6.
plugins {
    alias(libs.plugins.tetto.android.feature)
}

android {
    namespace = "com.callbackdev.tetto.feature.settings"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.androidx.junit)
    // The ViewModel's test runs on a real database and settings file, as the app does.
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.datastore.preferences)
}
