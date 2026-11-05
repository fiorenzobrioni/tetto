// The first run (PLANNING.md §11 Phase 3): the welcome, the areas a reader puts a cap on, their
// amounts and the currency; then the caps screen, already shaped by the reader's picks.
plugins {
    alias(libs.plugins.tetto.android.feature)
}

android {
    namespace = "com.callbackdev.tetto.feature.onboarding"
}

dependencies {
    // The system's back between the pages (BackHandler).
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.androidx.junit)
    // The ViewModel's test runs on a real database and settings file, as the app does.
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.datastore.preferences)
}
