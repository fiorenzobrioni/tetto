// What the UI tests share (PLANNING.md §12): the accessibility checks every screen's tests run,
// and Main on a real thread for the ViewModels' tests on real threads (`RealMainThread`).
// Test code only: modules take it as `testImplementation`, and nothing here reaches the app.
plugins {
    alias(libs.plugins.tetto.android.library)
    alias(libs.plugins.tetto.android.compose)
}

android {
    namespace = "com.callbackdev.tetto.core.testing"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.truth)
    api(libs.androidx.lifecycle.viewmodel)
    api(libs.kotlinx.coroutines.test)
    implementation(project(":core:designsystem"))

    testImplementation(libs.androidx.junit)
}
