// The caps screen, the numpad sheet and its ViewModel, a cap's sheet, the cap editor and the
// order (PLANNING.md §11 Phase 2). The numpad sheet is also what QuickAddActivity shows over the
// home screen (Phase 5).
plugins {
    alias(libs.plugins.tetto.android.feature)
}

android {
    namespace = "com.callbackdev.tetto.feature.caps"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.androidx.junit)
    // The ViewModel's test runs on a real database and settings file, as the app does.
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.datastore.preferences)
}
