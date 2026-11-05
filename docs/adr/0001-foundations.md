# ADR 0001: Foundations (Phase 0)

- Status: accepted
- Date: 2026-11-05

## Context

Tetto starts from an empty repository and the owner's concept (the repository's first commit,
`0ea1b66`), reviewed into `VISION.md` and answered by the owner on 5 Nov 2026. The owner asked
for the family's structure: the same stack, the same repository layout, the same workflows and
releases, the same design, the same licence, so the five apps (Chiaro, Passo, Tempo, Saldo,
Tetto) are built and maintained one way. Tempo's build is Passo's without Room; Tetto keeps
tables, so its build is Passo's with Room, as Tempo carries it.

## Decisions

1. **Identity.** Developer namespace `callbackdev`, as the family: package and applicationId
   `com.callbackdev.tetto` (debug: `com.callbackdev.tetto.debug`), modules under
   `com.callbackdev.tetto.*`. Author Fiorenzo Brioni, licence GPL-3.0 (full text in `LICENSE`).
   Repository `fiorenzobrioni/tetto`, the bare app name, as the sisters'.
2. **The family's build, carried over** from Passo (`passo@e2b7923`) and Tempo (`tempo@6ce51eb`),
   which share it: Gradle 9.8.0, AGP 9.4.1 with its built-in Kotlin, Kotlin 2.4.20, KSP 2.3.12,
   Compose BOM 2026.09.00, Hilt 2.60.1, Room 2.8.5, Glance 1.2.0, WorkManager 2.10.5 (pinned, as
   the sisters'), Navigation 3 1.2.0, DataStore 1.2.1; the version catalog and the convention
   plugins in `build-logic/` (renamed `tetto.*`); Java 21 without a toolchain; ktlint through
   Spotless; JUnit 4, Truth, Turbine, Robolectric. The versions are Passo's and Tempo's of
   2 Nov 2026, their latest stable then, and stay so (owner, 5 Nov 2026): Tetto builds as they
   do. Chiaro and Saldo are on an older build (AGP 8.13); the family is not one build today.
3. **Room, back.** Passo's `AndroidRoomConventionPlugin` (`tetto.android.room`): KSP, the schema
   exported to `<module>/schemas` and committed. The caps, the open periods' spends and the
   closed periods are tables (PLANNING.md §5); the settings and the widgets' looks are DataStore.
   The plugin joins the build with `:core:data`, in Phase 1.
4. **minSdk 34**, the family's, target and compile SDK 37 (owner, 5 Nov 2026).
5. **The permission gate, widened for a money app.** The family's `checkForbiddenPermissions`
   on every variant's merged manifest, with Tetto's additions: the camera and the microphone,
   SMS, contacts, calendar, shared storage and media, the list of installed apps, activity
   recognition (PLANNING.md §10). "Tetto reads nothing of yours" is then something the build
   checks, not something the README promises.
6. **A module joins the build with its first code** (Tempo's rule). Phase 0 has `:app`,
   `:core:model` (the appearance's enums, which the theme reads), `:core:designsystem` and
   `:core:testing`; `:core:domain`, `:core:data` and the rest join with the phase that fills
   them. Until the caps screen exists (Phase 2), its empty state is drawn by the shell in `:app`:
   a feature module brings `:core:domain` and `:core:data` with it, and those have no code yet.
7. **Signing, as the family.** Tetto's own debug keystore, committed for good (`tetto-debug` /
   `android`, PKCS12, RSA 2048, created 5 Nov 2026, SHA-256 in `keystore/README.md`): each app's
   debug builds are signed apart. The release key (`tetto-release.jks`, alias `tetto`) was made
   by the owner and put in the four secrets before the first line of code; its SHA-256
   fingerprint is pinned in `release.yml`, which refuses an APK signed by any other key.
8. **Versioning from the start.** `tetto.versionName` in `gradle.properties` (0.1.0 for the
   foundations), `versionCode = major * 10000 + minor * 100 + patch`; the release workflow
   refuses a tag that does not match.
9. **Only English and Italian resources ship** (`localeFilters`), as the family's.
10. **Backup is an allowlist from the first build** (`data_extraction_rules.xml`): the database
    and the settings file, named before they exist, so no build ever carries more.
11. **Phase 0's screen is the caps screen with no cap**: its sentence and the line that says what
    a cap is. No button is drawn before it does something; "New cap" comes with the screen.

## Consequences

- AGP 9's DSL, as the sisters' that share this build: snippets from Chiaro (AGP 8.13) or Saldo
  need translating.
- A change in the sisters' build-logic is not inherited: it is copied here by hand when it
  matters, as they copy each other's.
- Android 13 phones cannot install Tetto. Lowering the floor later is one constant
  (`TettoSdk.MIN`) and this ADR's fourth point; nothing in the code depends on 34.
