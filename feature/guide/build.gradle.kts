// The guide (PLANNING.md §11 Phase 6): what the caps screen answers, how a period, the reserve and
// forgiveness work, the numpad and fixing a spend, the home screen, your data, and what Tetto does
// not do. From the guide's card, first in Settings, and from the caps screen's first-day tip.
plugins {
    alias(libs.plugins.tetto.android.feature)
}

android {
    namespace = "com.callbackdev.tetto.feature.guide"
}

dependencies {
    testImplementation(libs.androidx.junit)
}
