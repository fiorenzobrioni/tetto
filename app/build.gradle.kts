plugins {
    alias(libs.plugins.tetto.android.application)
    alias(libs.plugins.tetto.android.compose)
    alias(libs.plugins.tetto.android.hilt)
    // The shell's keys are serializable: the back stack outlives the process.
    alias(libs.plugins.kotlin.serialization)
}

// The version lives in gradle.properties; versionCode follows from it (PLANNING.md §11 Phase 0),
// so the two can never disagree. A pre-release suffix (0.2.0-rc1) shares its final's code.
val tettoVersionName = providers.gradleProperty("tetto.versionName").get()
val tettoVersionCode =
    tettoVersionName.substringBefore('-').split('.').map(String::toInt).let { (major, minor, patch) ->
        major * 10_000 + minor * 100 + patch
    }

android {
    namespace = "com.callbackdev.tetto"

    defaultConfig {
        applicationId = "com.callbackdev.tetto"
        versionCode = tettoVersionCode
        versionName = tettoVersionName
    }

    // Tetto speaks English and Italian (VISION.md). Without this the APK would also carry every
    // language the AndroidX libraries ship, and their system strings would mix into a screen
    // that is otherwise in one of the two.
    androidResources {
        localeFilters += listOf("en", "it")
    }

    signingConfigs {
        // Shared debug keystore, committed on purpose (see CLAUDE.md): debug builds from CI and
        // from any machine carry one signature and can update each other's installs.
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "tetto-debug"
            keyPassword = "android"
        }

        // Release key. The keystore lives OUTSIDE the repo; the four properties come from
        // ~/.gradle/gradle.properties locally and from ORG_GRADLE_PROJECT_* env vars in the
        // release workflow. Created only when fully configured, so a clean checkout still builds.
        val releaseStore = findProperty("TETTO_KEYSTORE") as String?
        val releaseStorePassword = findProperty("TETTO_KEYSTORE_PASSWORD") as String?
        val releaseKeyAlias = findProperty("TETTO_KEY_ALIAS") as String?
        val releaseKeyPassword = findProperty("TETTO_KEY_PASSWORD") as String?
        if (!releaseStore.isNullOrBlank() && !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() && !releaseKeyPassword.isNullOrBlank()
        ) {
            create("release") {
                storeFile = rootProject.file(releaseStore)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    // The crash page (src/crashpage, CrashCatcher): a crash on a test device shows its stack trace,
    // to copy or share, with no adb at hand. In debug builds, and in the debug-signed release built
    // for testing (-PsignReleaseWithDebugKey), where R8's breakage shows and nowhere else; never in
    // a release signed with the release key.
    val crashPage = "src/crashpage"
    val testingRelease = signingConfigs.findByName("release") == null && project.hasProperty("signReleaseWithDebugKey")
    sourceSets {
        listOfNotNull("debug", "release".takeIf { testingRelease }).forEach { name ->
            getByName(name) {
                kotlin.directories += "$crashPage/kotlin"
                res.directories += "$crashPage/res"
                manifest.srcFile("$crashPage/AndroidManifest.xml")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            // Different app id, so the dev build installs side by side with the release one.
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // The release key wins whenever configured. Otherwise -PsignReleaseWithDebugKey signs
            // the minified build with the debug key so it can be smoke-tested (R8 breakage shows
            // up nowhere else). Opt-in, so an unconfigured checkout can never produce an
            // installable release by accident.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
                    .takeIf { project.hasProperty("signReleaseWithDebugKey") }
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:reminder"))
    implementation(project(":feature:caps"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:settings"))
    implementation(project(":widget"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // The first run's last page has a ViewModel of its own (the shell's HomeOffer).
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":core:testing"))
    // The first run's last page is drawn from caps' states, as the engine makes them.
    testImplementation(project(":core:domain"))
    testImplementation(libs.androidx.junit)
}
