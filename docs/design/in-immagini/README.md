# «In pictures» («In immagini»): the design's sources

The boards of the canvas the owner approved on 15 Nov 2026
(https://claude.ai/artifact/VFwVjbxyAyWcWRLcRW9uhY, page «Tetto»), **frozen on that day**. They are
the reference of Phase 6c (`PLANNING.md` §11, «Phase 6c — «In pictures», the caps in a row»): where
the plan writes no measure, the measure is here.

| File | Board |
|---|---|
| `TettoCard.dc.html` | The card itself, every form: the one component every board draws |
| `TettoForms.dc.html` | The eight forms, 2×1 to 4×4, at their real sizes in dp, four grounds and the hidden amounts |
| `TettoConfig.dc.html` | The settings screen (interactive on the canvas) |

**How to read them.** They are pages in the canvas's Design Component format: without its runtime
they do not open in a browser, they are read as source. The inline styles are the measures (`px` =
dp, `font-size` = sp, the strings are the Italian UI); the columns' geometry (the slots, the roof,
the body, the fill, the two ticks, the labels) is computed in the `Component` class at the end of
`TettoCard.dc.html`, by `renderVals()` and its `columns` function. The five caps (Sunday 15 Nov)
are a sample, and the sample's «ahead» test (2% over the ticks) is a stand-in: the card uses the
engine's own (`CapStates`).

**Not to be edited.** A measure the implementation changes is written in `PLANNING.md` with its
reason (Phase 6c's fidelity rule), not here.
