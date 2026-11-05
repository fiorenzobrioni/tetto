# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in
this repository.

## What this repository is

**Tetto** ("roof", and "ceiling" as in a spending cap) is a private, instant Android budgeting
app (Kotlin / Jetpack Compose Material 3, Glance widgets): the envelope method without the
envelopes. Each cap has its own period (a day, a week, a month, a year); the screen says what is
left and until when; a spend is two touches and the amount; what a period leaves over goes to a
reserve, and an overspend the reserve cannot cover is forgiven when the period ends. No ledger,
no account, no ads, no internet permission at all. It belongs to a small family of
single-purpose apps by callbackdev, with **Chiaro** (weather), **Passo** (step counter),
**Tempo** (clock and agenda) and **Saldo** (finance): same developer identity, same build and
release setup, and the same visual language. Its build and structure are Tempo's (itself
Passo's), with Passo's Room back.

**State (5 Nov 2026): Phases 0 to 4 are built.** The build, the design system, CI, the
keystores, the cap engine with its storage, the caps screen with its numpad, its sheets, the
editor and the order, the first run and Settings, your data (the backup file, the restore, the
CSV, erasing everything) and the evening reminder; the modules in the build are `:app`,
`:core:model`, `:core:domain`, `:core:data`, `:core:reminder`, `:core:designsystem`,
`:core:testing`, `:feature:caps`, `:feature:onboarding` and `:feature:settings`. The rest of
the table below is the target: each module joins with the phase that brings its first code
(PLANNING.md §11), so check before citing a file.

**Tetto and Saldo are two separate apps, alternatives to each other** (owner, 5 Nov 2026).
Tetto takes the family's look and structure from its sisters, so the apps are recognisable;
its product decisions are its own. Never argue a choice from what Saldo does, has or plans, to
copy it or to avoid it: argue it on Tetto's merits.

Source of truth:

- `VISION.md`: the product. Principles, v1.0 scope, non-goals, the border with Saldo, the
  corrections made to the owner's concept, key decisions, success criteria, **the open
  questions**, glossary. The owner's original concept, in Italian, is the repository's first
  commit (`0ea1b66`).
- `PLANNING.md`: the architecture, the cap engine (§4), the data model (§5), the derived views
  (§6), the widgets (§7), notifications and shortcuts (§8), battery rules (§9), permissions
  (§10), and the phased plan with checkable steps (§11). **Keep it updated as work progresses**:
  tick the boxes, and record every decision and deviation with its reason in §15 (and as an ADR
  in `docs/adr/` when it is architectural). It is kept as the family's:
  - **Write only in `PLANNING.md`**: a new `###` section at the end of §15's log, dated, with
    who asked. A box ticked or opened goes in §11 (a phase's step) or in §15's Open list.
    §1-§10 and §12-§14 are the living specification: they change in place, with the code.
  - **`docs/planning/NN-<period>.md` is the archive**, the closed periods of the log with the
    phases they closed, **frozen**: never edit it. A decision that changes an archived one is
    written in `PLANNING.md`, naming the archived file and the section or entry it changes.
  - **To find a section** cited by name or date in the code ("PLANNING.md §11 Phase 3",
    "§15, 5 Nov 2026"), grep `PLANNING.md` first, then `docs/planning/README.md`, the index of
    every archived heading and dated entry with its file and line, and read from that line.
    Never load an archive file whole.
  - **Archive** when the log passes ~80 KB or at a release, whichever first, as `PLANNING.md`
    "How it is made" says, then run `python3 tools/planning_index.py` (`--check` tells if the
    index is behind).

Work one phase at a time, starting from the phase's section in PLANNING.md §11. The owner
answered VISION.md's questions on 5 Nov 2026 (VISION, «The owner's answers»; PLANNING §15); a
new question is written in PLANNING §15's Open list and asked before it is built. Never invent
an answer the owner has not given.

## Build and commands

Stack: Kotlin 2.4 (compiled by AGP 9's built-in Kotlin), Compose Material 3, Hilt, Room (KSP),
DataStore, kotlinx.serialization, Glance, WorkManager (Glance's, pinned); Gradle 9.8 / AGP 9.4,
version catalog in `gradle/libs.versions.toml`, convention plugins in `build-logic/`.
Package/applicationId: `com.callbackdev.tetto`. minSdk 34, compile/targetSdk 37 (the family's;
owner, 5 Nov 2026). Java 21.

- Build debug APK: `./gradlew :app:assembleDebug` (output: `app/build/outputs/apk/debug/app-debug.apk`)
- All unit tests: `./gradlew test`
- One module: `./gradlew :core:domain:test`; one class: `--tests "com.callbackdev.tetto.core.domain.period.PeriodCalendarTest"`
- Lint (every module, via `checkDependencies`): `./gradlew :app:lintDebug`
- Format: `./gradlew spotlessApply` (CI runs `spotlessCheck`; rules in `.editorconfig`)
- Forbidden-permission check on the merged manifests: `./gradlew :app:checkForbiddenPermissions`
- Installable minified build: `./gradlew :app:assembleRelease -PsignReleaseWithDebugKey`
- Launcher icon: `python3 tools/draw_launcher_icon.py`
- On a machine with no system JDK, prepend `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
- **On Windows, Room's `MigrationTestHelper` cannot open its database** under Robolectric: its
  driver looks for the file name after a `/`, and the path there has `\` (Room 2.8.5). It works on
  CI (Linux). `SchemaTest` builds version 1 from the exported JSON instead; a migration test that
  takes the helper runs on CI only, and says so.
- **Maven Central answers HTTP 429** (Too Many Requests; it happens in the Claude Code cloud
  sandbox): add `--init-script gradle/google-maven-mirror.init.gradle.kts` to any command. It
  puts Google's mirror of Maven Central first, and points Robolectric's own `android-all`
  download at it, for that run only; nothing in the build changes. In the sandbox the SDK is at
  `/opt/android-sdk` (`local.properties`, not committed).

**Convention plugins** (`build-logic/convention`): `tetto.android.application`,
`tetto.android.library`, `tetto.android.compose`, `tetto.android.feature`, `tetto.android.hilt`,
`tetto.android.room`, `tetto.jvm.library`. SDK levels and the Java version live in `TettoSdk` /
`Tetto.kt`; a module's own build file only says what is specific to it. The forbidden-permission
list is `ForbiddenPermissionPatterns` in `ForbiddenPermissions.kt`.

**Version**: `tetto.versionName` in `gradle.properties` is the only place it is written;
`versionCode` is derived (major * 10000 + minor * 100 + patch). `release.yml` refuses a tag
that does not match it.

## Modules (PLANNING.md §2)

| Module | Kind | Holds |
|---|---|---|
| `:core:model` | pure Kotlin/JVM | data classes shared by everything: `Money` (`Long` minor units), `AppCurrency`, `PeriodKind`, `PeriodRule`, `DateRange`, `Cap`, `Spend`, `ClosedPeriod`, `CapState`, `CapIcon`, `CapDraft`, the settings enums and `UserSettings`, `WidgetLook` |
| `:core:domain` | pure Kotlin/JVM | the cap engine: `PeriodCalendar` (a period from a date and a rule), `Days` (where dates meet instants: midnights, a day's instants, how near a day is), `Rollover` and `CatchUp` (closing periods, the reserve's limit, forgiveness, a run of empty periods folded), `CapChanges` (the open period a new cap or a new rule gets), `CapStates` (left, over, the bar, the mark for today, ahead of the days, the pace, the renewal), `MoneyInput` (the numpad's buffer), `Rescale` (a change of currency's decimals), `SpendPreview`, `CapsSentence`; `ReminderSchedule`; `BackupCodec`, `CsvExport`; the widgets' arithmetic (`GlanceFit`) |
| `:core:data` | Android library | Room (`TettoDatabase`: `caps`, `spends`, `closed_periods`; schema exported to `core/data/schemas/`), DataStore (`settings`; `widgets`, not backed up); `CapsRepository` (the catch-up before every read and write, in one transaction, one at a time), `SettingsRepository`, `BackupRepository` (the backup's snapshot, the restore, the CSV, erasing everything, under the caps' lock), `TodaySource`, `PhoneDefaults` (the region's currency, the locale's week) |
| `:core:reminder` | Android library | the evening reminder's Android half (`EveningReminder`, started by the application): the inexact alarm and its re-arming, the receiver (alarm, boot, package replaced, time and zone, the notifications' block states), the `reminder` channel and its notification (`ic_stat_tetto`, drawn by the icon script), `ReminderAccess` (whether it can show, and the page that fixes it) |
| `:core:designsystem` | Android library | M3 theme (Chiaro's dresses through Passo and Tempo), typography, shapes, motion, page gutter (`PageWidth.kt`), the widget card colours, `TettoIcons` (the curated cap icons, Material Symbols drawn in code); the settings rows and `StatusCard`; the numpad's keys, a cap's tile, the bar with its mark, the ring; the amount field (`MoneyField`, through `:core:domain`'s `MoneyInput`, its one use of the domain) and the currency picker (`CurrencyPickerDialog`, `Currencies`), shared by the editor, the first run and Settings; money formatting (`MoneyFormat`, from minor units and the locale) |
| `:core:testing` | Android library, test-only | `assertAccessible()` (labels, 48dp touch targets) and `walkPage()`, shared by the UI tests as `testImplementation` |
| `:feature:*` | Android library | `caps` (the caps screen, the numpad sheet and its ViewModel, a cap's sheet, the cap editor), `onboarding` (welcome, the areas, the amounts), `settings` (money, the reminder, appearance, your data with its `DataViewModel`, about), `guide` (in Chiaro's shape, Phase 6) |
| `:widget` | Android library | the two Glance cards («At a glance», «In words»), their settings screen, their looks, the midnight alarm and the exempt broadcasts |
| `:app` | application | `Application`, `MainActivity` (the reader's appearance), the shell (onboarding, then Navigation 3), `QuickAddActivity` (the numpad over the home screen, from a card or a shortcut), the dynamic launcher shortcuts, DI entry points; wires everything |

Rules: `:core:model` and `:core:domain` stay pure Kotlin/JVM; if a class there needs a `Context`
or a `Resources`, it is in the wrong module. Feature modules depend on `core:*`, never on each
other. All business logic lives in `:core:domain`, with unit tests. A module joins the build in
the phase that brings its first code.

## Invariants (non-negotiable)

- **Never add a forbidden permission** (PLANNING.md §10): `INTERNET`, any `ACCESS_*LOCATION`,
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `CAMERA`,
  `RECORD_AUDIO`, `READ_SMS`, `RECEIVE_SMS`, `READ_CONTACTS`, `WRITE_CONTACTS`, `READ_CALENDAR`,
  `WRITE_CALENDAR`, the storage permissions (`READ_`/`WRITE_`/`MANAGE_EXTERNAL_STORAGE`,
  `READ_MEDIA_*`), `QUERY_ALL_PACKAGES`, `ACTIVITY_RECOGNITION`, `HIGH_SAMPLING_RATE_SENSORS`,
  `BODY_SENSORS*`. The build fails on the merged manifest; a library that adds one is removed or
  patched with `tools:node="remove"`. Files go through the Storage Access Framework.
- **Money is `Long` minor units.** Never `Double`, `Float` or a floating-point parse anywhere in
  `:core:model`, `:core:domain` or `:core:data`; the numpad's input is parsed digit by digit
  (`MoneyInput`). Only `:core:designsystem` formats an amount, from minor units and the locale.
- **Periods are local dates** (start included, end excluded), from an injected clock and zone.
  Never decide a period by comparing an instant with a stored end (PLANNING.md §4.2).
- **The rollover is the engine's, and only the engine's**: `reserveAfter = min(limit, max(0,
  reserve + cap − spent))`, forgiveness for what the reserve cannot cover (PLANNING.md §4.4).
  A period is closed by `CatchUp`, idempotently, in one transaction, **before every read**: the
  caps screen, a widget's render, the reminder's receiver, `QuickAddActivity`, the export. A
  period is never reopened; a closed period is never changed; only the open period's spends can
  be edited or deleted.
- **Don't change the cap engine without its tests** for every edge case in PLANNING.md §4.7.
- **The database is a contract.** The Room schema is exported and committed; every migration has
  a test (`MigrationTestHelper`) and the upgrade from the first release's schema is tested.
  Never `fallbackToDestructiveMigration`.
- **No ledger.** No history screen, statistics, charts, accounts, income, notes, tags or dates on
  a spend (VISION, non-goals and «Tetto and Saldo»). A feature that needs one is out of scope;
  ask before building anything that looks like it.
- **Battery first** (PLANNING.md §9): no service of any kind, no polling, no wakelocks of the
  app's own, no exact alarms, no periodic workers. Two alarms at most, each only while needed:
  the reminder's (inexact, once a day, while it is on and allowed) and the cards' midnight
  redraw (inexact, non-wakeup, while a card is placed). The midnight ticker lives in a visible
  page's lifecycle.
- **The widgets never scroll**: a Glance `Column`/`Row`, never `LazyColumn`; what does not fit is
  counted. Glance drops the eleventh child of a container silently: count them.
- **Over is said in words**, never by a colour alone, in the app and on the cards.
- **Backup is an allowlist** (`app/src/main/res/xml/data_extraction_rules.xml`): the database and
  the settings file, nothing else. Android sends the copy, so the no-`INTERNET` rule stands; say
  "Tetto sends nothing", never "nothing leaves the phone". A restore from a file is read whole
  and checked before anything is written, and replaces only after the reader's confirmation.
- **Every user-facing string is a resource, in English and Italian** (`values/`,
  `values-it/`), with plurals where counts appear. The app language follows the system
  per-app language picker (`locales_config.xml`). Dates are formatted with the locale's own
  patterns (`DateFormat.getBestDateTimePattern`), amounts with the locale's currency format,
  never a hard-coded order or symbol.
- **No new third-party dependency without approval**, and its license must be
  GPL-3.0-compatible (Apache-2.0, MIT, BSD).

## Code style

Kotlin official style (ktlint `intellij_idea` via Spotless), no `!!`, `Flow` over `LiveData`,
immutable UI state, MVVM with unidirectional data flow. Code and documentation in English.
Comments explain *why*, not what. In the code a cap is a `Cap`, a spend a `Spend`, the reserve
`reserve`, what is left `left` (VISION's glossary); the Italian words are the UI's.

## Design

Tetto keeps the same visual language as Chiaro, Passo and Tempo, for the app screens and for the
widgets, so the family reads as one (`docs/adr/0002-design-language.md`, Phase 0).
`:core:designsystem` holds it as roles (color, type, shape), never as hexes inside a composable:
Chiaro's two generated dresses (`theme/Scheme.kt`, copied through Passo and Tempo, never
hand-edited), Google Sans / Inter / system type, Chiaro's shapes and springs, and every animation
collapses to a fade under reduced motion. Its principles hold here too: one sentence before any
number, every number with the line that says what it means, no dead tab and no switch for a
feature that has not shipped, a section with nothing to say is not drawn. Amounts are tabular
figures; the hero size is kept for one number per screen. The semantic inks are `attention`
(spending ahead of the days) and, only if a phase needs it, one for over, each with its contrast
test. Icons are `TettoIcons`, drawn in code: the cap palette is a curated set of Material
Symbols (Apache-2.0, credited in `licenses/`), never the whole library. The launcher icon (the
family's ring in fuchsia to wine, cut by Tetto's amber roof at the lower right, Saldo's place:
owner, 5 Nov 2026) and the reminder's status-bar icon (the roof alone, `ic_stat_tetto`) are
written by `tools/draw_launcher_icon.py`: change the script and re-run it, never the XML. The
first run shows no icon (the family's onboarding frame).

**The guide** (Phase 6) is in the family's shape and style (owner, 5 Nov 2026), from Tempo's
`GuideScreen`: its card first in Settings, a first-day tip on the caps screen, short chapters.
It never teaches a control, only what a part of the screen is for; it shows the app's own
components as examples, each captioned as one, so no sample reads as the reader's money. The Compose UI tests write
screenshots to each module's `build/screenshots`: look at them after changing a screen.

**The numpad** is the app's most used surface: keys at least the family's touch target, the
locale's decimal separator, the phone's own keyboard haptics through `LocalHapticFeedback`
(no in-app switch), a hardware keyboard typing too, the preview as a polite live region for
TalkBack, and an Add button that is disabled, never relabelled, until there is an amount.

**Accessibility and foldables** (the family's, from the start): a screen's tests call
`assertAccessible()` on what they draw, and walk the page at twice the text size and on an open
foldable (`walkPage()`); text inks are pinned at 4.5:1 by `ContrastTest`. A page leaves
`pageGutter()` on each side (in a list's content padding, never by narrowing the list), so an
open foldable shows a centred column and a phone upright is untouched. TalkBack reads a cap as
one sentence ("Coffee, €12 left of €20, until Monday").

**Widgets** (PLANNING.md §7, `docs/adr/0003-widgets.md` from Phase 5): the family's pair,
«At a glance» and «In words», on the family's card (Passo's ADR 0005, from Chiaro): its corner,
insets, grounds, Chiaro's six colours (`WidgetPalette.kt`), opacity and ink rule. The forms'
arithmetic is pure (`GlanceFit`) and pinned by tests at the family's reference grants; a gallery
test draws every form to `widget/build/screenshots`: look at them after changing a card. A card
says dates ("until Monday"), never hours from now: it is redrawn at midnight, not each minute.
**«In words» is the family's ring card** (owner, 5 Nov 2026): its ring is **the family's
circle** (PLANNING.md §7), the three functions both sisters draw their circle with since
4 Nov 2026 (Tempo's `WordsFit.cellCircle`, `rowCircle`, `tallCircle`; Passo's `dotRingSize`,
`rowRingSize`, `tallCircle`), and its type steps the family's widget scale (`WidgetType`),
copied constant for constant, so it lines up with Passo's and Tempo's ring cards on a home
screen. The circle is sized by the card, never by the day's words. Never retune them for Tetto
alone: a change to them is the family's, decided with the owner; when the sisters change
theirs, Tetto's follows, recomputed and pinned at the reference grants.

**README screenshots** (`docs/screenshots/`, shown in the root `README.md`): drawn by
`ReadmeScreenshots` test classes from realistic sample caps, in English, and only on request:
`./gradlew test -PupdateScreenshots` (plus the mirror init script in the sandbox). The family's
standing rules: **regenerate them** whenever a change alters what an existing one shows, and look
at them before committing; **add one** when a phase brings something worth showing, with its
caption in the README's table; keep the set small.

## The family in the code

The sister apps (Chiaro, Passo, Tempo, Saldo) are named **only** in the root `README.md`
(«The family») and in the project's documents (VISION, PLANNING, the ADRs, this file): never in
a string the app shows, its guide and onboarding included, and never in the code, comments and
tests included (the family's rule). A comment says why a value is what it is, not which sister
has it too; where a value's provenance matters (the generated schemes), it is written in its
ADR. Tetto reads nothing from Saldo and writes nothing for it: each app stands alone.

## Signing and CI

- **Debug signing**: `keystore/debug.keystore` is intentionally committed (alias `tetto-debug`,
  passwords `android`; Tetto's own, made in Phase 0, not a copy of a sister's) so debug APKs from
  CI and any machine share one signature. Do not regenerate it once committed. Debug builds
  carry `applicationIdSuffix ".debug"`. Debug builds and the debug-signed release carry a crash
  page (`app/src/crashpage`): an uncaught exception shows its stack trace, to copy or share, so a
  test device can report a crash without adb (the minified one's trace is retraced with that
  run's `mapping.txt`). Never in a release signed with the release key.
- **Release signing**: the real keystore lives OUTSIDE the repo; the `release` signingConfig is
  created only when the four `TETTO_KEYSTORE*` / `TETTO_KEY_*` properties are all set (from
  `~/.gradle/gradle.properties` locally, from `ORG_GRADLE_PROJECT_*` env vars in CI). Without
  them the release build is unsigned; `-PsignReleaseWithDebugKey` signs it with the debug key
  for testing only. The release key (`tetto-release.jks`, alias `tetto`, PKCS12) was made by
  the owner and put in the repository's secrets on 5 Nov 2026; it lives with the family's keys
  outside every repository. Its SHA-256 fingerprint (PLANNING §15, 5 Nov 2026) is
  `28:A0:E8:60:45:9D:3C:24:06:BC:A7:6F:18:FA:1D:91:42:37:9F:A2:03:EA:85:1D:DE:4B:66:B2:55:19:9D:F1`:
  published in `keystore/README.md` and the root README, and pinned in `release.yml`, which
  refuses an APK signed by any other key. Never regenerate it: a future Google Play listing
  reuses it (§11 Phase 8), and a new key would mean no reader could update without
  uninstalling.
- **Release key in CI**: the four repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, `KEY_PASSWORD` (the same names as the sister apps'); `release.yml` decodes the
  keystore into the runner's temp folder and fails by name if one is missing. Never commit
  the real key.
- **CI** (`.github/workflows/android-ci.yml`, every push and PR): formatting, forbidden
  permissions, unit tests and lint run *before* the APKs; a red suite must never produce an
  installable artifact. **Release** (`release.yml`, on `v*` tags): the same gates, then the
  signed APK, its SHA-256 and the R8 mapping on a GitHub Release, with the body taken from
  the tag's `CHANGELOG.md` section (write it before tagging).

## Writing `README.md` (root file only)

**No em dashes (`—`) or en dashes (`–`) in the root `README.md`.** Rewrite the sentence rather
than swapping in a hyphen: use a colon when the clause explains, a full stop when the thoughts
are separate, parentheses for an aside. Same house style as the family, deliberately scoped to
that one file: every other file keeps normal punctuation.
