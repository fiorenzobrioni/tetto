# ADR 0003: The widgets, the numpad over the home screen and the launcher's shortcuts (Phase 5)

- Status: accepted
- Date: 2026-11-05

## Context

VISION.md promises the family's pair on the home screen, «At a glance» and «In words», on the
family's card; a spend added from the home screen in two touches and the amount; the launcher's
four shortcuts; and cards that are fresh without polling. PLANNING.md §7 sets the forms, the taps
and the refresh, and binds «In words» to **the family's circle**: the ring of the sisters' ring
cards, at the same size for every size of card (owner, 5 Nov 2026). The sisters' widget modules
are the family's working answer to most of it (the card, the inks, the arithmetic of what fits
measured in the launcher's face, the settings screen with the live card, the generated previews,
the refresh that keeps the process awake for exactly the redraw it asked for); what Tetto adds is
money, a ring per cap, and a numpad that opens over the home screen.

## Decisions

1. **The family's card, carried over.** The launcher's own corner
   (`system_app_widget_background_radius`), the insets (14 dp; 6 dp snug on a row; 8 dp on one
   cell), the four grounds (light, dark, the phone's, one of the six colours of `WidgetPalette.kt`),
   the opacity and the ink rule (`widgetInk`: the card is its own ground from half opacity, the
   wallpaper's hint below it), every ink resolved against the ground the card really has; the
   measuring in the launcher's face (`textEm`, `measureWidgetLines`, a 6% margin); the guarded
   load that never leaves Glance's spinner on screen; `WidgetRefresh`'s revision, so a live
   session reloads; the receivers' cleanup that never finishes a pending result twice.
   `WidgetCardColor` moved to `:core:model`, with the card's look (`WidgetLook`), as §5 names it;
   its hexes stay in the design system's `WidgetPalette.kt`.

2. **What fits is pure Kotlin** (`GlanceFit`, `:core:domain`): the widget measures, the domain
   decides, the tests pin the decisions at the reference grants (one cell 85 dp; rows 85, 189,
   293; two, three, four cells 159, 250, 340; the owner's two-by-two, 188 × 218).
   - **«In words»**: one cell, the cell's circle with the figure under it where it fits; one row,
     the row's circle, what is left over the cap and its last day, the pace at the far edge where
     those words stay whole; two rows and up, the tall circle in the top trailing corner, the
     name beside it where 56 dp are left, the hero ("€12 left", 34 sp), the cap's sentence, and
     the last words.
   - **The family's circle** is `cellCircle`, `rowCircle`, `tallCircle`, copied constant for
     constant: 69; 44, 56, 56; 52.75, 73.75, 73.75; 104, 104, 119.25; without the sentence 95
     two rows high; the owner's two-by-two 81.75 and 104. `GlanceFitTest` pins the arithmetic,
     `WidgetFitTest` the ring's side on the card as drawn, and that it does not change with the
     cap shown. The words are counted as the family's band counts them (1.32 em a line, nothing
     more): at the default text size, the circle and the band it budgets stand together exactly.
   - **«At a glance»**: one cell, one cap's figure as the hero, the word ("left", "over"), the
     icon and the bar where the height leaves them; one row, slots of 60 dp at least (two, three,
     four on two, three, four cells), each its icon and figure, its name and its bar, the last
     slot counting the rest ("+3"); two rows and up, a grid of one, two or three columns, each
     item its icon, name, figure and bar on one line, "3 more" where the caps do not all fit.
   - **Glance's eleventh child**: no container holds more than five groups (`GROUP_MAX`), a row
     of slots or of items at most nine children with its gaps; the tests count them.

3. **Over is said in words on the cards too.** "€30 over" in the over ink (Material's `error`
   of the dress, 4.5:1 or better on every solid ground, pinned by `WidgetInkTest`); on a slot,
   the word "over" in the name's place; on one cell with no room under the ring, the word inside
   it, in the icon's place. The ring and the bar are full past the cap; spending ahead of the days
   takes the *attention* ink; a thin mark, over a sliver of the card's own ground, says where
   today falls in the period. Glance cannot ask for tabular figures: the launcher's face draws
   them as it draws every card's.

4. **The ring is a bitmap**, painted at the size shown, capped at 416 px a side, one per card,
   with the cap's icon drawn in its middle from the same Material Symbols path data the app's
   badges use (`CapIconDrawing`, `:core:designsystem`): an arc with round caps, the mark for
   today at any day of the period, in any of the card's inks, is the one drawing boxes cannot
   build. The bars are boxes (track, fill, mark): no bitmap where boxes do.

5. **The last words of a tall card take the fullest form the card has the lines for** (§15,
   5 Nov 2026, Phase 5): "of €20 · until Sunday" on one line; broken at its separator ("of €20"
   over "until Sunday"); the day alone. With the name nowhere beside the circle, the name takes
   the cap's place ("Coffee · until Sunday", "Coffee" over "until Sunday", "Coffee"). The
   sentence says how much a day, never until when: the day is the last words'. A row's second
   line likewise ("Coffee · until Sunday", "until Sunday", "Coffee"), and a name beside the
   circle steps down to 14 and 13 sp before a word of it would break.

6. **Pushed, never polled** (PLANNING.md §9). A write: `WidgetUpdater` hears Room's invalidation
   of the caps' tables (after the transaction commits, whatever wrote it: the caps screen, the
   numpad over the home screen, the first run, a restore, an erase) and the settings, and redraws
   the cards while one is placed. Midnight: one `AlarmManager.set(RTC)`, inexact and non-wakeup,
   a second past the next local midnight, armed after every render while a card is placed,
   disarmed with the last card (`MidnightAlarm`). The exempt broadcasts (time set, zone,
   language, package replaced), from the manifest; a boot sends the cards their update. Every
   render runs the catch-up first (`CapsRepository.currentStates`): a card never shows a period
   that should have closed. Whatever asked stays until the cards are drawn. No service of
   Tetto's, no periodic work, no wakelock of its own; Glance's WorkManager brings its own.

7. **Touches** (§7, Taps). A cap opens `QuickAddActivity` on it; on «In words» the whole card is
   its cap. The rest of «At a glance» (its air, the count of the rest) opens the caps screen. Each
   cap's door carries its own data, so two caps' touches are two pending intents. The door is
   named in `:core:data` (`QuickAdd`), where the cards, the shortcuts and the numpad's ViewModel
   can see it; `:app`'s test holds the name to the activity.

8. **The numpad over the home screen** (`QuickAddActivity`, `:app`; `QuickAddViewModel` and the
   sheet, `:feature:caps`): a translucent activity in its own task, out of the recent apps, in
   the reader's appearance, holding the caps screen's numpad in a bottom sheet. After Add the
   sheet becomes one line, "€5.00 added to Coffee", what is left after it and Undo, for four
   seconds stretched by the reader's accessibility timeout, then slides away; a touch outside
   closes it at once, and so does leaving it (home, the screen off). **Undo puts the amount back
   on the numpad**: a spend undone at once is most often a key pressed wrong, and the reader
   fixes it in one touch more. A spend added here takes the evening reminder back from the shade.
   A cap deleted since the card was drawn is said to be gone, with the way into Tetto.

9. **The launcher's shortcuts** (`CapShortcuts`, `:app`): dynamic, the first four caps in the
   reader's order, each its name (short) and "Add a spend to Coffee" (long), its icon the cap's
   symbol on the dress's container as an adaptive bitmap, straight to the numpad. Published again
   when the caps, their order, names or icons, or the palette change (the repository's flow, for
   the life of the process), and when the app's language changes (`onConfigurationChanged`);
   never sent when the launcher already has them (its rate limit). A shortcut whose cap is gone is
   disabled, with the words that say why.

10. **The settings screen** is the family's: the real card, composed by Glance into `RemoteViews`
    and inflated as the launcher does, at the size it has and the reference sizes; the ground and
    its colour; the opacity. Then Tetto's content: «At a glance» every cap, or those ticked (the
    last one cannot be unticked: none chosen would read as all); «In words» the closest to its
    cap, or one cap, and «The sentence». Before any cap exists, the preview draws sample caps and
    says so. Every choice is saved as made and redraws that card.

11. **The pin request** (§8): Settings' «Home screen» group, where the launcher takes the
    request; and the first run's last page, after a run that made caps, where the launcher takes
    it and no card is there yet: «Your caps on the home screen», the real card drawn from the
    reader's new caps, «Add the card» and «Not now». The caps are written before the page shows,
    so a process ended on it loses nothing and makes nothing twice. The feature modules reach
    `:widget` through `HomeScreenCards` (`:core:data`), bound by Hilt.

12. **The previews**: static `previewLayout`s for the picker below Android 15 (sample values, the
    default blue card, only views `RemoteViews` inflates); generated ones from Android 15
    (`providePreview`, `WidgetSamples`: five caps on a fixed Wednesday, in the reader's language
    and the phone's currency), published once per app version.

13. **Not backed up**: the looks live in their own file (`widgets`), outside the backup's
    allowlist (ADR 0004): a restore renews the cards' ids, and a look kept under an old id would
    dress nothing, or the wrong card.

## Consequences

- The merged manifest holds the services Glance and WorkManager declare (as the sisters'):
  `GlanceRemoteViewsService`, `RemoteViewsCompatService`, WorkManager's three; Tetto's own code
  starts none and holds no wakelock. Room's multi-instance service stays removed.
- What only a phone can say waits for the owner's: the cards beside the sisters' ring cards,
  the launcher's grants and its pin dialog, the numpad over the home screen, the shortcuts, and
  `dumpsys alarm` with the screen off (§11 Phase 5, acceptance).
