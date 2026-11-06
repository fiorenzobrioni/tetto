# Tetto — Product Vision

> **Tetto** (Italian for "roof", and for "ceiling" as in a spending cap) is the fifth of a small family of focused, single-purpose apps, alongside **Chiaro** (weather), **Passo** (step counter), **Tempo** (clock and agenda) and **Saldo** (personal finance).
> Name: **Tetto**, in the launcher and on GitHub Releases (the only channel for now). Repository: `fiorenzobrioni/tetto`.
> Package name: `com.callbackdev.tetto`, the `callbackdev` namespace shared with the sibling apps.
>
> **Status: reviewed by the owner** (5 Nov 2026). Written from the owner's concept, the first
> `VISION.md` of this repository («TETTO - Product Vision Canvas (Expanded)», commit `0ea1b66`,
> in Italian). What this vision changes in it, and why, is in
> [From the concept to this vision](#from-the-concept-to-this-vision), accepted in full by the
> owner; the questions the review settled are in [The owner's answers](#the-owners-answers).
> Development goes on with PLANNING §11, Phase 0, then Phase 1.

## One-liner

How much you can still spend on each thing you put a cap on, and until when. Two touches and
the amount to add a spend, from the app or the home screen; what is left over is set aside, an
overspend is forgiven at the next period, and nothing else asks to be kept up.

## Why this app

Budgeting apps lose their readers in the first weeks, for three reasons:

- **Data entry is work.** A spend asks for a payee, a note, tags, an account, a date, a receipt.
  The more each one costs, the sooner the reader stops, and a budget with missing spends is
  worse than none: its numbers lie.
- **They answer the wrong question.** Charts, statistics and balances answer "where did my money
  go?". The question asked at the counter is "can I still afford this?".
- **One bad month ends the habit.** An overspend carried as a debt into the next month starts it
  behind; the reader feels they have failed and stops tracking altogether (the "what the hell"
  effect, first described in dieting research).
- **Every cap runs on the same calendar**, reset on the 1st, when coffee is a weekly matter and
  car maintenance a yearly one.

Tetto does one thing: the envelope method, made instant. Each cap has its own period. The
screen says what is left, not what was spent. What a period leaves over goes to a reserve that
absorbs the next surprise; an overspend eats that reserve first, and what it cannot cover is
forgiven when the period ends. Tetto keeps no ledger: no accounts, no balances, no income.

### Tetto and Saldo

The family has two money apps, and the owner sees them as **two alternatives, two separate
apps** (5 Nov 2026): a reader who wants the whole of their personal finances takes Saldo; a
reader who wants a budget and nothing else, at the greatest speed, takes Tetto. Nothing stops a
reader from keeping both, but most will choose one. They share the family's look and structure,
so they are recognisably siblings; they share nothing else. **Tetto's decisions are its own**:
what it does and how it does it are decided on Tetto's merits, never to match, follow or avoid
what Saldo does. The line between them, as the owner agreed it:

| | Saldo | Tetto |
|---|---|---|
| The question | "Where does my money go, and how much do I have?" | "How much can I still spend on this, until when?" |
| What it keeps | Every movement, for good: expenses, income, transfers, accounts, balances, recurring payments | Caps, and the spends of the periods they count in |
| A spend | Amount, category, account, date, description, tags | An amount, in one cap, now |
| Budgets | One part of the app, monthly, read against the ledger | The whole app, each cap on its own period, with a reserve and forgiveness |
| For | Readers who want the full picture | Readers who find any expense tracker too much |

Tetto is for the reader who will never keep a ledger. Each app stands alone, as the family's
do: no data passes between them. The guardrail that follows is Tetto's own: **Tetto never grows
a ledger** (history screens, statistics, accounts, income). Its speed comes from having none.

## Product principles

1. **What is left, first.** The screen leads with what can still be spent, and until when; what
   was spent is the secondary line.
2. **Two touches and the amount.** Touch a cap, type the amount, add. No category to look for,
   no date, no note, no account. From the home screen, too (the widgets, the launcher's
   shortcuts).
3. **Kind by design.** What a period leaves over is a reserve; an overspend uses the reserve
   first; what the reserve cannot cover is forgiven when the period ends. A new period never
   starts in debt.
4. **The numbers are right, and fixable.** Money is counted in whole cents, never in floating
   point. A spend typed wrong can be corrected while its period is open, not only in the three
   seconds after it was added. Every rule of the engine has its tests.
5. **No noise.** No charts, no statistics, no history to scroll, no alerts at 80%. A section with
   nothing to say is not drawn; a switch for something that has not shipped does not exist (the
   family's rules).
6. **Private by design.**
   - No `INTERNET` permission, no location, no contacts, no camera, no SMS, no account, no
     analytics, no ads SDKs.
   - Tetto sends nothing. Your data leaves the phone only through an export you make, or through
     Android's own backup if you turned it on.
7. **Battery first.** Nothing runs in the background. Periods are closed when Tetto is next read
   (the app, a widget, the reminder), never on a timer. The only scheduled work is the optional
   evening reminder (one inexact alarm a day) and the widgets' redraw at midnight.
8. **One sentence before any number** (the family's rule). "All under your caps; Restaurants is
   the closest: €12 until Sunday." "Shopping is €30 over; it starts again on Monday."
9. **The screen does not lie.** An overspend says so in words, not in colour alone. The reserve
   says where it came from. A change of currency relabels the amounts and says that it does not
   convert them.
10. **One family.** Chiaro's design language as Passo and Tempo carry it: the same palettes, type,
    shapes and motion in the app, the same card, inks and colours on the home screen.

## Target users

- People who want the envelope method without the envelopes: a few caps, each answered at a
  glance.
- People who tried an expense tracker and gave up on the data entry.
- People whose spending has different rhythms: a daily coffee, a weekly grocery run, a monthly
  night out, a yearly car service.
- Privacy-conscious readers who will not give a bank's credentials to anyone and want their
  numbers on their phone.
- Readers of the family's apps who want a matching set of widgets.

## Scope for v1.0

### Caps (the app's one main screen)

- **The sentence** on top, before any number: everything under its cap, the one closest to its
  cap and until when (none named while nothing has been spent), the ones over and when they start
  again. Computed from the caps alone. Two lines where a small step down of its size is enough.
- **One tile per cap**, in two columns on a phone (one at large text sizes; the family's centred
  column on an open foldable), so eight caps fit one screen: the cap's icon and name; **what is left**
  in the family's figure style (tabular, so the digits do not wobble as they change), with its
  word small beside it on the same line, "€12 left" or, past the cap, "€30 over" ("ancora 12 €"
  in Italian, the word first); the cap and, on a line of its own, when it renews ("of €200",
  "until Sunday"), so every tile has the same lines and the bars of a row stand level; the
  reserve as a small chip at the tile's foot, "+€30 reserve", only while there is one, in a
  colour that is never the attention ink (owner, 6 Nov 2026). Or, the reader's choice in
  Settings, **one row per cap** across the page, shorter, for many caps: the icon, the name and
  "€12 left" on one line, the cap and its day, the bar; the pace and the reserve in the cap's
  sheet (owner, 6 Nov 2026).
- **A bar of what was spent**, with a thin mark where today falls in the period: a reader sees at
  once whether spending runs ahead of the days. The bar is drawn in the accent; it takes the
  family's *attention* ink when spending runs ahead of the period, past a little room that is a
  step of the period on its first day (a day of a week, a week of a month) and nothing on its
  last, so a big shop early in a month is not a warning (owner, 6 Nov 2026); and an over state
  that is also a shape and words, never a colour alone. No traffic lights.
- **The pace**, where it helps: for a cap of a week or longer with days left, "about €4 a day
  until Monday".
- **Touch a cap**: the numpad opens on it. **The arrow by its name**, a long press, the numpad's
  header or TalkBack's action: the cap's own sheet (the arrow, owner, 6 Nov 2026).
- **A last tile, "New cap"**; the caps' order is the reader's, changed by dragging in an edit
  mode.
- **Live while visible**: a page open across midnight closes the periods that end and starts the
  new ones, without a restart.
- **States**: no cap yet (one line and "New cap"), every cap over, a cap just renewed.
- **No total of the caps.** Caps with different periods do not add up; a sum of a weekly and a
  yearly cap would mean nothing, and a sum of what is left would undo the envelopes.
- **Your month**, last on the page, under the caps (owner, 6 Nov 2026): what was added to the
  caps in the reader's month so far (the month from the day chosen in Settings, payday for a
  reader who wants it) and in the month before, and what the caps are worth in a month, about,
  to set against a salary. Two sums of what the reader added and one planning figure: no
  statistics, no month before the last, no chart, no choice of period, and only what is added in
  Tetto, which the guide says.

### The numpad (a bottom sheet)

- **Header**: the cap's icon and name, and what is left now.
- **The amount**, large and tabular, with a caret; under it **the preview**, in words, updated
  at each key: "€15 left" while it fits; "€30 over" in the over state once it does not; "Still
  €30 over" when the cap was over already.
- **The cap's habits** (owner, 6 Nov 2026): once a cap has spends, its most used amounts (of its
  last twenty spends, at most three, smallest first) wait between the preview and the keys, each
  added in one touch, with the same Undo; they fade, keeping their room, as soon as a key is
  pressed, and are not offered while a spend is corrected. Amounts only: no date, no count.
- **The keys**: 0 to 9, the locale's decimal separator (absent for a currency with no decimals),
  delete (a long press clears). Large, airy, borderless, each with a short tick of the phone's
  keyboard haptics; a hardware keyboard types too.
- **The button**, full width: "Add €5.00", disabled until there is an amount. The sheet closes by
  the system's gestures (swipe down, back, a touch outside), never by a button whose meaning
  changes under the thumb.
- **After adding**: the sheet closes, the cap's figure rolls to its new value, and a snackbar says
  "€5.00 added to Coffee" with **Undo**. Undo stays as long as Material's accessible timing
  requires (longer with TalkBack); the next change replaces it rather than waiting behind it.
- **Caps** on the amount: up to nine digits before the separator. No dates (a spend counts
  now), no notes, no category picker (the cap was chosen by the touch that opened the sheet).

### A cap's sheet

- **This period**: its dates ("27 Oct – 2 Nov") and its figures as a receipt: the cap, plus the
  reserve, less what was spent, and under a rule what is left (or how far over). The sheet's
  figures, the receipt's and the spends', share one rule: cents on every one as soon as one has
  them (owner, 6 Nov 2026).
- **This period's spends**, newest first (amount, day and time). A touch edits the amount, a
  swipe deletes it, with undo. Spends of closed periods cannot be changed: a closed period is
  frozen, so the reserve never changes behind the reader's back.
- **The last periods, one line each**, the reserve's story: "Week of 20 Oct: €18 of €20, +€2 to
  the reserve"; "October: €230 of €200, €30 forgiven". Not a history to browse: the few lines
  that explain the chip.
- **Clear the reserve**: one touch, with undo, for a reader who wants to start fresh.
- **Edit the cap** (name, amount, period, icon) and **delete it** (confirmed).

### New and edited caps

- Four fields: **name**, **amount**, **period** (day, week, month, year) and **icon**, from a
  curated palette of about two dozen Material Symbols, so the screen keeps one look.
- A week starts on the locale's first day of the week, a month on the day set in Settings (the
  1st by default; "from the 27th" for a reader paid on the 27th), a year on that day of January.
- A change of amount applies to the open period at once, and a reserve larger than the new amount
  comes down to it. A change of period replaces the open period by the new one that contains
  today: where it holds this period's days, this period's spends move into it; where it starts
  later, the days already past close as a line of their own, on their share of the cap, and only
  what is spent from its first day counts in it. The reserve is held to the new cap (owner,
  6 Nov 2026; PLANNING §4.5).

### First run

- **Welcome**, in the family's onboarding frame: the name, its sentence, three promises (what is
  left at a glance; kind to the bad weeks; private, on the phone), Start.
- **"What do you want to put a cap on?"**: a grid of common areas, each an icon and a name
  (groceries, eating out, coffee and bars, shopping, fun, transport, fuel, gifts, personal care,
  pets, travel, home). The reader picks only theirs. Bills are left out on purpose: a fixed bill
  is what it is, and a cap is for spending one can steer. Any area can be added later, bills
  included.
- **The amounts**: one row per picked area, amount and period (a month by default), in the
  currency of the phone's region, changeable from the same page.
- The caps screen opens already shaped to the reader. "Start from nothing" skips the grid.
- No permission is asked at the first run.

### Widgets (Jetpack Glance)

The family's pair, on the family's card, the same sizes and the same settings screen as the
sisters' «At a glance» and «In words»:

- **«At a glance»** («Colpo d'occhio»): the caps as many as the card's size allows, each with its
  icon, name, what is left and a slim bar; never a scrolling list, the rest counted ("3 more").
- **«In words»** («In parole»), **the family's ring card**: one cap, chosen per widget (or "the
  one closest to its cap"). The ring is what was spent, the cap's icon inside it; beside or
  under it the words: "€12 left", "Coffee · until Monday", and on a taller card the pace. The
  ring is **the family's circle**, the one Passo's and Tempo's cards draw since 4 Nov 2026: the
  same size at every size of card, set by the card and never by the day's words (owner, 5 Nov
  2026): as large as a one-cell card; at the leading edge of a one-row card, beside the words,
  at the row's size; in the top corner of a card two rows high and up, the words hanging from
  the bottom. The same type steps for the same kind of line, so the three cards side by side on
  a home screen line up and read as one set. As on the sisters' cards, the reader may keep a
  tall card to the amount and its cap, without the sentence, and the circle then grows as
  theirs do.
- **A touch on a cap adds a spend from the home screen**: the numpad opens over the home screen,
  and the reader is back on it after Add. Two touches and the amount, without opening the app.
- **Per widget**: the family's card (light, dark, following the phone, or one of the six colours,
  at any opacity, with a live preview), and which caps.
- **Updates without polling**: when a spend or a cap changes, at midnight (a period may end, the
  days left change), and when the clock, the time zone or the language changes.

### System surfaces

- **The launcher's shortcuts** (long press on the icon): the first four caps, each straight to
  its numpad.
- **The evening reminder**, opt-in, from Settings: one local notification at a time the reader
  picks (20:30 by default), "Anything to add today?", **not sent on a day a spend was already
  added**. An inexact alarm; the notification permission is asked when the reader turns it on,
  never before. Touching it opens the caps screen.
- No other notification: no alerts at 80%, no "you went over". The preview says it where it
  matters, before the spend is added.

### Settings and personalization

- **Money**: the currency (one for the whole app; any currency the phone knows, from a
  searchable list; the phone's region by default), the day a month starts, the first day of the
  week (the locale's by default).
- **The reminder**: off (default) or on, and its time.
- **Appearance**, the family's: theme (system, light, dark), palette (Vivid, Paper), typeface
  (Google Sans, Inter, the phone's), wallpaper colours on or off.
- **Language**: Italian or English, through Android's per-app language setting.
- **Your data**: what Tetto keeps and where (the privacy statement, in the family's words);
  **export a backup** and **restore from a backup**; **export the spends as CSV**; **erase
  everything**, confirmed.
- **The guide**, in the family's shape and style (owner, 5 Nov 2026), as Chiaro's, Passo's and
  Tempo's: a card at the top of Settings, and a first-day tip on the caps screen pointing to it
  (once dismissed or followed, never again). A few short chapters: what the caps screen answers,
  how a period, the reserve and forgiveness work, the numpad and correcting a spend, the home
  screen, your data, and what Tetto does not do (a reader who wants a ledger wants an expense
  tracker; the guide says so without naming one, as the family's rule keeps sister apps out of
  every string the app shows). It never teaches a control, only what a part of the screen is for; it shows the app's
  own components as examples, captioned as such, so no sample reads as the reader's money.
- **About**: the privacy statement, credits, the licence.
- No haptics switch: the numpad follows the phone's own touch-feedback setting.

### Data

- Tetto keeps the caps, their spends and one line per closed period, in a local database, and
  the reader's settings. Spends of closed periods are kept for the CSV and the backup, never
  listed on a screen.
- Android's backup carries the database and the settings, an allowlist, as in the family. The
  backup file (JSON, versioned) carries the same, and a restore replaces this phone's caps after a
  confirmation. The CSV is for a spreadsheet: one row per spend.

## Out of scope (non-goals)

- **A ledger**: accounts, balances, income, transfers, recurring payments, statistics, charts,
  search, a history screen. Tetto is fast because it has none; a reader who wants one has Saldo.
- **Moving money between caps** (covering one cap's overspend from another): the reserve and
  forgiveness are the whole mechanism.
- **A total budget** across caps: periods differ, sums would mislead. (What was added in the
  reader's month, and what the caps are worth in one, are said under the caps: «Your month».)
- **Spend details**: notes, tags, payees, photos, receipts, dates other than now.
- **Bank connections, SMS or notification reading, receipt scanning**: no network, no camera, no
  SMS, no notification access.
- **Several currencies at once** and conversion: one currency keeps the arithmetic and the
  screen simple.
- **Sync, accounts, sharing a budget with someone**, a cloud of Tetto's own.
- **Alerts at a threshold**: the preview is the alert.
- An app lock or PIN in v1 (a later idea); Wear OS; a tablet layout beyond the family's centred
  column; ads, analytics, crash reporting over the network.

### Possible post-1.0 ideas (not committed)

- A one-off cap that does not renew ("Lisbon trip: €800"), with an end date.
- A refund (a negative spend), from the cap's sheet.
- Pausing a cap (holidays) without its reserve growing.
- A Quick Settings tile that opens the numpad on a chosen cap.
- An app lock (PIN and biometrics) and hiding the amounts on the widgets.
- A period of two weeks, for a fortnightly pay.
- Publication on Google Play.

## From the concept to this vision

The concept is the starting point and most of it stands: the question it answers, the reserve
and forgiveness, the independent periods, the numpad and its preview, the curated icons, the
first run, the evening reminder that knows when to stay quiet, no background work. These are the
changes, the corrections and the additions, each with its reason.

| Concept | In this vision | Why |
|---|---|---|
| (not in the concept) | **Tetto and Saldo**, two alternatives, the line between them written down; Tetto never grows a ledger | Two money apps in one family both answer "how much can I still spend?" in part. Written down, the line says which reader each one is for, and keeps Tetto from drifting into a ledger. The owner's view, 5 Nov 2026: two separate apps, and Tetto's decisions are never shaped by Saldo's |
| One Room entity, `CategoriaBudget`, "for constant 120 fps" | Three small tables: caps, spends, closed periods | Correcting a spend, the CSV, the backup and "a spend was added today" (the reminder) all need the spends. A few thousand rows a year cost nothing to read; frame rate is decided by the UI, not by the number of tables |
| No history; mistakes fixed only by the 3-second undo | Undo kept, **and** this period's spends listed in the cap's sheet, editable; past periods as one line each; still no history screen | A wrong amount noticed an hour later would stay wrong for the whole period: the numbers would lie, and a budget whose numbers lie is abandoned, the very thing the concept fights. A 3-second snackbar also fails readers who need more time (TalkBack, motor impairments) |
| `max(0.0, riserva + tettoBase - spesaCorrente)` | The same rule, in **whole cents** (`Long`), never `Double` | Floating point cannot hold 0.10 exactly; sums of small spends drift by a cent and the screen shows €0.01 left on an exact budget |
| The reserve grows without limit | **Capped** at one period's cap (owner, 5 Nov 2026) | Unbounded, a €20 weekly coffee cap left half used becomes a €500 coffee budget within the year, and the cap stops meaning anything. Worse: after a few weeks without opening the app, every empty period would add a full cap to the reserve, as if money spent and not entered had been saved. A cap on the reserve bounds both |
| Rollover at launch, "if now > end of period" | The same lazy idea as an **idempotent catch-up**, run before anything reads the caps (the app, a widget, the reminder) and at midnight while a page is open; periods are **local dates**, and a spend keeps the period it was added in | A widget and the reminder read the caps without the app being launched. A timestamp comparison breaks on a daylight-saving night, a flight across time zones, a clock set back; local dates do not. A page left open across midnight must roll over by itself |
| Day, week, month, year | Kept, with a **week start** (the locale's) and a **month start day** (Settings; the 1st by default) | "From the 27th to the 27th", the salary month, is the most asked-for budgeting option: a reader paid on the 27th thinks in months that start then |
| The confirm button reads "Annulla" until a digit is typed | A disabled "Add", enabled with the amount; the sheet is dismissed by the system's gestures | A button whose meaning flips from cancel to confirm under the same thumb invites the wrong touch |
| The typed figure blinks | A caret; the figures roll to their new value (the family's motion), a fade under reduced motion | Blinking is noise, and a problem for some readers |
| A switch for the numpad's haptics | No switch: the phone's own touch-feedback setting decides | Android already has the switch; one fewer in Settings |
| Theme selector and a Material You switch | The family's appearance: theme, palette, typeface, wallpaper colours (off by default) | One design system for the family |
| Material 3 cards, `headlineLarge` on each | The family's components and type: one sentence on top, the figures tabular, the hero size kept for one number per screen | Six cards each shouting a hero number have no hierarchy; the family's screens lead with one sentence |
| A bar from green to orange to red | The accent; the family's *attention* ink when spending runs ahead of the days, with a mark for today in the period; over said in words and shape | Colour alone fails colour-blind readers and reads as alarm; a fixed 80% means nothing on the last day of a week, while "spending ahead of the days" does |
| A list of cards (`LazyColumn`) | A grid of tiles, two columns on a phone (owner, 5 Nov 2026) | With eight caps in a column of large cards, three are below the fold and the two-touch promise needs a scroll first; two columns show them all, and fall back to one at large text sizes |
| (not in the concept) | **Widgets**, the family's pair, adding a spend from the home screen, «In words» the family's ring card at the sisters' sizes; the launcher's shortcuts | "How much is left" is a glance, and every sister has its pair of cards. A spend added from the home screen in two touches is the one place Tetto can be faster than opening any app |
| (not in the concept) | **A cap's sheet**: this period, its spends, the reserve's story, clear the reserve | The chip "+€30 reserve" needs one place that says where the €30 came from |
| The currency follows the phone, or is forced to €, $, £ | One currency for the app, any the phone knows (a searchable list), the phone's region by default; decimals from the currency (none for the yen) | A reader in Switzerland counts in francs. A change relabels the amounts and says it does not convert them |
| A "push" notification at 20:30 | A **local** notification, opt-in, from an inexact alarm; its permission asked when turned on | Tetto has no network: nothing can be pushed. "Local" is also what makes the privacy promise true |
| "Your data stays only on this device" | "Tetto sends nothing", and what Android's backup does, said | With Android's backup on, the system copies the data: the family's wording rule (Passo's ADR 0007) |
| CSV export | CSV, plus a **backup file** to export and restore, plus Android's backup (an allowlist) | A CSV cannot bring the caps back on a new phone |
| Onboarding areas including Bills | Common areas of spending one can steer; bills can still be added as a cap later | A cap is a tool to steer spending; a fixed bill does not move with it |
| `minSdk 33` | `minSdk 34`, `targetSdk`/`compileSdk 37` (owner, 5 Nov 2026) | The family's floor since Passo and Tempo: one build-logic, one platform floor. Android 13 phones are left out |
| "120 fps", "100% reliability" | Success criteria that can be checked (below) | A promise the tests check is a promise kept |

## Key decisions

| Area | Decision | Rationale |
|---|---|---|
| Positioning | A cap per thing, its own period, a reserve, forgiveness; no ledger | The envelope method made instant. Saldo is the alternative for a reader who wants a ledger; Tetto's choices are its own |
| Money | Whole minor units of one currency (`Long`), formatted by the locale | Exact sums; one currency keeps the screen and the arithmetic simple |
| Periods | Local dates: a day, a week (from the locale's first day), a month (from the reader's day), a year | Correct through daylight saving, travel and clock changes |
| Rollover | `reserve' = min(limit, max(0, reserve + cap − spent))`, closed when next read, frozen once closed | The concept's formula, bounded and exact, with no background work |
| Storage | Room (caps, spends, closed periods) and DataStore (settings) | The family's stack; the schema exported and every migration tested |
| Widgets | Jetpack Glance, the family's pair, `SizeMode.Exact`, never a scrolling list | The family's widget technology and rules |
| Background | None, but the optional reminder's inexact alarm and the widgets' midnight redraw | Battery first |
| Platform | `minSdk 34` (Android 14), `targetSdk`/`compileSdk 37` | The family's levels (owner): one platform floor for the apps built from one build-logic. Android 13 phones are left out, knowingly |
| UI | Jetpack Compose + Material 3 in Chiaro's design language; Glance for the widgets | One design system for the family |
| Languages | Italian and English for the app; English for code and documentation | As the family |
| Name | **Tetto** | Consistent with Chiaro, Passo, Tempo and Saldo |
| License | GPL-3.0 | As the family. Every dependency must be GPL-3.0-compatible (Apache-2.0, MIT, BSD) |
| Distribution | Signed APKs on GitHub Releases; Google Play kept open as a later option | As the family. The release key exists (the four repository secrets) and is kept for life |

## Success criteria

- **Fast.** From the caps screen, a spend is two touches and the amount; from the home screen,
  the same. The numpad answers each key at once, and the caps screen is drawn without a visible
  wait on a cold start (measured in Phase 6; a Baseline Profile if it is slow).
- **One set on the home screen.** «In words» beside Passo's and Tempo's ring cards, at the same
  size, has its ring and its lines at the same sizes as theirs.
- **Exact.** Every rule of the engine has its unit tests (PLANNING §4.7): periods across
  daylight saving, month starts on the 29th to the 31st, leap years, a flight across time zones,
  a clock set back, a long absence, a cap or a period changed mid-period, the reserve's limit,
  forgiveness. Sums are exact to the cent.
- **Fixable.** Any spend of an open period can be corrected or deleted; an undo follows every
  change.
- **Battery.** With the screen off Tetto does nothing, except one inexact alarm a day for the
  reminder (if on) and the widgets' redraw at midnight (if placed). Checked with `dumpsys alarm`
  and `batterystats`.
- **Privacy is enforced.** The merged release manifest holds no `INTERNET`, location, camera,
  SMS, contacts or storage permission, and CI fails if one appears.
- **For every reader.** TalkBack reads each cap as one sentence ("Coffee, €12 left of €20,
  until Monday") and the numpad's preview as it changes; text works at twice its size; an open
  foldable shows a centred column.

## The owner's answers

The questions this vision left open, settled in the owner's review (5 Nov 2026; PLANNING §15).
Every change of «From the concept to this vision» and every suggestion of the draft was
accepted with them.

1. **Tetto and Saldo**: two alternatives, two separate apps; the border as written. Tetto's
   decisions are never shaped by what Saldo does: it shares the family's look and structure, so
   the apps are recognisable, and nothing else.
2. **The spends of the open period**: listed in the cap's sheet, editable and deletable; closed
   periods frozen; old spends kept for the CSV and the backup, never listed.
3. **The reserve**: up to one period's cap (a period can at most double).
4. **Widgets and shortcuts in v1**: yes, the family's pair, a spend added from the home screen,
   and the launcher's four shortcuts. «In words» is the family's ring card, at the same sizes as
   Passo's and Tempo's for every size of card and every line of text, so the cards line up on a
   home screen.
5. **The caps screen**: two columns of tiles on a phone; a row per cap as the reader's choice in
   Settings (owner, 6 Nov 2026).
6. **`minSdk 34`**, the family's.
7. **The guide**: in the family's shape and style, as the sisters' guides.
8. **The mark**: the family's ring, with Tetto's emblem at the lower right, where Saldo's
   coins stand (the owner's choice: the two money apps share the place, and a reader most often
   has one of them). Drawn in Phase 0 as a roof on a fuchsia ring; redrawn by the owner on
   6 Nov 2026 as a house with a window on a slate ring, clearly apart from Saldo's.

## Glossary

- **Cap** (*tetto*): the most a category may spend in one period. Also the name of a category
  with its cap, as the reader sees it ("the Coffee cap").
- **Period**: a cap's cycle: a day, a week, a month or a year, in local dates, start included
  and end excluded.
- **Spend** (*spesa*): an amount added to a cap; it counts in the period open when it was added.
- **Left** (*residuo*): cap + reserve − spent this period. Negative is **over** (*sotto*).
- **Reserve** (*riserva*): what past periods left over, carried, up to its limit.
- **Forgiveness** (*condono*): an overspend the reserve could not cover, dropped when the
  period ends. A new period never starts in debt.
- **Catch-up**: closing every period that has ended since the caps were last read, in order,
  before anything is shown.
- **Pace**: what is left divided by the days left in the period ("about €4 a day").
- **Preview**: the sentence under the numpad's amount, saying what the spend would leave.
- **Card**: one widget on the home screen. **Form**: the layout a card takes at a given size.
