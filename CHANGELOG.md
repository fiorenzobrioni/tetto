# Changelog

All notable changes to Tetto are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed (`## [1.0.0]` for
`v1.0.0`) and uses it as the body of the GitHub Release, so a version's entry is written
**before** its tag, and kept to what somebody arriving at that page wants to read.

## [Unreleased]

### Added

- The rules behind every cap: a cap's own period (a day, a week, a month from
  the day you choose, or a year); what a period leaves over set aside as a reserve, up to one
  period's cap; an overspend the reserve cannot cover forgiven when the period ends; amounts
  counted in whole cents, and a spend correctable while its period is open.
- The caps screen: one sentence before any number, a tile per cap with what is left, until when,
  a bar with a mark for today, the pace and the reserve; over said in words.
- The numpad: two touches and the amount, the preview of what will be left, Undo after every
  change; a cap's sheet with this period's spends, to correct or delete, and the last periods.
- New caps and changes to them (name, amount, period, icon), and the caps' order.
- The first run: pick the areas you want a cap on, give each an amount and a period, in your
  currency, and the caps screen opens ready; or start from nothing.
- Settings: the currency, any your phone knows (changing it relabels your amounts and never
  converts them); the day a month starts and the first day of the week; the appearance (theme,
  palette, typeface, wallpaper colours); the app's language; the privacy statement, the licence
  and the credits.
- The evening reminder, if you turn it on in Settings: "Anything to add today?" around the time
  you choose, only on a day with caps and no spend added, gone by itself when the day ends.
  Android's permission is asked when you turn it on, never before; if notifications are off,
  Settings says so and opens the page that turns them back on.
- Your data: a backup of your caps, every spend and your settings, to a file you choose, and
  restored from one, after it is read whole and checked and you have seen what it holds; the
  spends for a spreadsheet, one row each, as CSV; and erasing everything, back to the first run.
  Tetto still sends nothing: no storage permission is asked, the file goes where you put it.
- Two home-screen cards, on the same card as the family's: «At a glance», your caps as many as
  the card's size allows, each with what is left and a slim bar; and «In words», one cap in a
  ring (the one you choose, or the closest to its cap) with what is left in words. Over is said
  in words on the cards too. Each card has its own settings: its background and colour, its
  opacity, which caps, and the sentence.
- A spend from the home screen: a touch on a cap opens the numpad over the home screen, and a
  moment after Add you are back where you were; Undo puts the amount back on the numpad.
- The launcher's shortcuts: a long press on Tetto's icon, and your first four caps go straight to
  the numpad.
- The cards stay fresh without asking anything of the battery: after every change, at midnight
  (one alarm that never wakes the phone, only while a card is placed), and when the clock, the
  time zone or the language changes.
- After the first run, and in Settings, Tetto offers to put a card on your home screen, where the
  launcher allows it.
