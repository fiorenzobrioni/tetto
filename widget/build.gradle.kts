// The home-screen widgets (Jetpack Glance, PLANNING.md §7): «At a glance» and «In words», their
// receivers, the per-card settings screen with its live preview, and what keeps them fresh (a
// write in the app, the non-wakeup midnight alarm, the exempt broadcasts). The card, its inks and
// its colours: docs/adr/0003-widgets.md.
plugins {
    alias(libs.plugins.tetto.android.library)
    alias(libs.plugins.tetto.android.compose)
    alias(libs.plugins.tetto.android.hilt)
}

android {
    namespace = "com.callbackdev.tetto.widget"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    testImplementation(project(":core:testing"))

    implementation(libs.androidx.glance.appwidget)
    // Glance runs its sessions on WorkManager; named here so the version pinned in the catalog is
    // the one that resolves (PLANNING.md §1).
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.androidx.junit)
    // The loader's test reads a real database and settings file, as the app does.
    testImplementation(libs.androidx.room.runtime)
}
