# ADR 0004: Your data: the backup file, the restore, the CSV, erasing (Phase 4)

- Status: accepted
- Date: 2026-11-05

ADR 0003 is the widgets' (PLANNING.md §7, Phase 5), named before this one was written; the
numbers follow the plan, not the order of writing.

## Context

A reader's caps, their reserves and the story behind each reserve exist nowhere but on their
phone. VISION («Data») asks for three ways out and one way back: Android's own backup, a backup
file to export and restore, the spends as CSV for a spreadsheet; and for erasing everything. Tetto
has no `INTERNET` permission and no storage permission (PLANNING.md §10), and must keep neither.
A backup file is also a contract: a file written by this version must be read by every later
one, and a file this version cannot read must never be half-written into the database.

## Decisions

1. **Android's backup is an allowlist** (`data_extraction_rules.xml`, written in Phase 0): the
   database (`tetto.db` and its write-ahead log) and the settings file
   (`files/datastore/settings.preferences_pb`), for the cloud copy and the phone-to-phone
   transfer. The names are the code's own constants (`TettoDatabase.NAME`,
   `UserPreferencesDataSource.FILE_NAME`), which say so where they are declared. The widgets'
   looks (Phase 5) stay out: they are keyed by widget ids, which a restore renews. Checked with
   `bmgr` on the owner's phone (§11 Phase 4).
2. **The backup file is JSON, versioned, readable without the app** (`BackupCodec`,
   `:core:domain`): `format: "tetto-backup"`, `version: 1`; the app's version, the zone and the
   instant it was written; `currency` (ISO 4217) and its `decimals` where it was written; the
   reader's settings (the month and week start, the appearance, the reminder); the caps in the
   reader's order, each holding its spends and its closed lines. Amounts are whole minor units,
   dates ISO, instants epoch milliseconds, a time of day `20:30`. No ids: a restore gives new
   ones, and the order of the array is the caps' order. Not carried: what belongs to one phone
   (whether its first run is done, whether it ever asked for the notification permission).
3. **Read whole and checked before anything is written.** A file is decoded to the end and every
   part of it checked before the reader is even asked: not JSON, or not Tetto's, is *not a Tetto
   backup*; a later `version` asks for an *update*; anything cut short, or that no Tetto could
   have written, is *damaged*. Strict where a value decides a figure or a period: an amount of
   nothing or below zero, a date that is not one, a period kind this version does not know (a new
   kind raises the version), a month start outside 1 to 31, a spend in a period after the open
   one, a closed line reaching into it or written twice, figures whose sum would not fit a
   `Long`. Lenient where a default cannot mislead: an icon this version does not know is
   "other", an unknown palette or font is the default, an unreadable reminder time is 20:30.
   Where the file's decimals for its currency are not this phone's, the amounts are rescaled by
   the rule of a change of currency (§4.1).
4. **A restore replaces, after a confirmation that says so.** The dialog says when and by which
   Tetto the file was written, its caps by name, its spends and their currency, and what it
   replaces here. On a new phone there is nothing to merge with, and a merge of two sets of caps
   would double them. The settings come with the caps: the open periods were made by the
   backup's week and month starts, and judged by another phone's they could close by mistake.
   Under the caps' one lock: the settings first, then, in one Room transaction, the old caps
   out, the backup's in, and the catch-up, so a backup from last month closes what has ended
   since. A transaction that fails puts the settings back: the phone is as it was.
5. **The CSV is the spends** (`CsvExport`): one row per spend of every period, oldest first;
   `date`, `time` (the phone's zone now), `cap`, `amount` (the whole unit, a dot), `currency`,
   `period_start`, `period_end` (both days included, as the cap's sheet says them). RFC 4180,
   UTF-8 with a byte-order mark, fixed English headers in every language, and a name that starts
   like a formula written as text. Made from the same snapshot as a backup, after the catch-up.
6. **Files go through the Storage Access Framework** (`CreateDocument`, `OpenDocument`): the
   reader picks where a file goes and what to open; Tetto asks for no storage permission and
   sends nothing. A file larger than 32 MB is not read: no reader's years of spends come near it.
7. **Erasing everything** is confirmed in a dialog that says what goes, that a saved backup is not
   touched, and that it cannot be undone. The caps first, then the settings to their defaults:
   should the second fail, the reader sees an empty caps screen, never a first run that would add
   its caps to the old ones. The first run then takes over by itself. Whether Tetto ever asked
   for the notification permission stays: it is a fact about the phone, not the reader's data.

## Consequences

- A file written by `version: 1` is read by every later Tetto, or that Tetto raises its version
  and says why in its own ADR.
- A backup file is not encrypted. The card that says it was saved says so too, and where it went.
- Android's Auto Backup takes its copy roughly once a day, while the phone charges on Wi-Fi; the
  file is the dependable way to move a reader's caps, and Android's backup the convenience.
