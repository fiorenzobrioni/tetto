// Pure Kotlin/JVM, and it must stay that way: the cap engine (periods, the rollover and the
// catch-up, what a cap says, the numpad's arithmetic, the sentence). All business logic lives
// here and is tested on the JVM without Android (PLANNING.md §2, §12).
plugins {
    alias(libs.plugins.tetto.jvm.library)
}

dependencies {
    api(project(":core:model"))
}
