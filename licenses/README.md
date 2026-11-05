# Third-party licenses

Tetto itself is GPL-3.0 (`LICENSE` at the repo root). What ships inside the APK and is not ours:

| What | License | Where |
|---|---|---|
| [Google Sans](https://fonts.google.com/specimen/Google+Sans), the app's default typeface, bundled as `core/designsystem/src/main/res/font/google_sans_variable.ttf`: the upstream `ofl/googlesans` variable font cut down by Chiaro's `tools/import_google_sans.py`, copied from Tempo, which copied it from Passo and Passo from Chiaro, so the family shares one drawing | SIL Open Font License 1.1 | `GoogleSans-OFL.txt` |
| [Inter](https://github.com/rsms/inter), the second typeface, bundled as `core/designsystem/src/main/res/font/inter_variable.ttf` | SIL Open Font License 1.1 | `Inter-OFL.txt` |

The typefaces are credited in Settings, About (Phase 3), with the one in use named, because a
licence that asks for credit is not satisfied by a file the reader never opens. The app's line
icons are drawn in the app (`TettoIcons`, the family's drawings). The caps' icons, a curated set
of [Material Symbols](https://github.com/google/material-design-icons) (Apache License 2.0),
join with Phase 2, and their licence with them.

The caps and the spends on screen are the reader's own: Tetto ships no data of anyone else's.
