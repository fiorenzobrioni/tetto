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
