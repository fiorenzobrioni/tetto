// Pure Kotlin/JVM, and it must stay that way: the cap engine (periods, the rollover and the
// catch-up, what a cap says, the numpad's arithmetic, the sentence), the reminder's schedule, the
// backup file and the CSV. All business logic lives here and is tested on the JVM without Android
// (PLANNING.md §2, §12).
plugins {
    alias(libs.plugins.tetto.jvm.library)
    // The backup file (Phase 4): kotlinx.serialization, already the shell's for its routes, pure
    // Kotlin like the rest of this module (PLANNING.md §1).
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.serialization.json)
}
