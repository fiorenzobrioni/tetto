#!/usr/bin/env python3
"""Draws Tetto's launcher mark: the adaptive icon's three layers in app/src/main/res/drawable, and
the status-bar icon (core/reminder/src/main/res/drawable/ic_stat_tetto.xml).

    python3 tools/draw_launcher_icon.py

The mark is THE ROOF OVER THE COIN (owner, 9 Nov 2026, when the family's marks were redrawn as
one series): a white roof, a solid slab with overhanging eaves and a chimney on its right-hand
slope, over an amber coin with its rim struck in a deeper amber. A cap over the money. The
chimney is what makes the slab a roof: without it a roof reads as a caret (owner, 6 Nov 2026,
when the roof alone was set aside for the house with a window).

The series' rules, the same in every app of the family:
- the badge's ground is the app's own colour, here the indigo, from its light tone at the upper
  left to its deep tone at the lower right; the symbol is white with one amber accent;
- every shape is lit from the upper left: a linear ramp from its light tone at the upper left of
  its box to a darker one at the lower right (the white to its deep tone, which is still nearly
  white, the amber to its base), over the box's larger side; no shadow (the store and the
  launchers add their own);
- the symbol stays inside the keyline, a circle of KEYLINE around the centre: 56 dp of the 72
  that show, so a round mask leaves 8 dp of air all round and no mask of any launcher reaches it.

The status-bar icon is the same roof and coin in one ink on the 24-unit grid, the rim cut out of
the coin, inside the 2 units of air Android's system icons keep.

Re-running this script IS the drawing: ic_launcher_background.xml, ic_launcher_foreground.xml,
ic_launcher_monochrome.xml and ic_stat_tetto.xml are its output and are not edited by hand. Only
the standard library is used, so any Python 3 runs it.
"""

from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/drawable"
STAT = ROOT / "core/reminder/src/main/res/drawable/ic_stat_tetto.xml"

CENTRE = 54.0
KEYLINE = 28.0  # the symbol's reach from the centre: measured 27.6

# The family's palette, the tones this mark uses: (light, base, deep).
INDIGO = ("717BE0", "4750BC", "2F3796")
SUN = ("FFD36A", "FDB528", "F28C17")
WHITE = ("FFFFFF", "F4F7FB", "E3EAF4")
RIM_ALPHA = "BF"  # the coin's rim: the amber's deep tone at 75%

# The drawing, on the 108 grid, then fitted: scaled about the centre, and moved by FIT_SHIFT.
EAVES_LEFT, APEX, EAVES_RIGHT = (26.5, 51.5), (54.0, 26.0), (81.5, 51.5)
SLAB = 9.5  # the roof's thickness, measured down from its ridge
EAVE_CUT = 3.2  # how far in from the eaves' tips the slab's underside starts
ROOF_ROUND = 3.0  # a stroke of the roof's own colour round its outline: its corners round
CHIMNEY = (65.5, 72.5, 29.0, 48.0, 1.5)  # left, right, top, foot (inside the slab), corner
ROOF_LIGHT_BOX = (24.0, 24.0, 84.0, 60.0)  # the roof and its chimney lit as one shape
COIN = (54.0, 64.0, 15.5)  # middle and radius
RIM = (10.2, 2.6)  # the rim's radius and width
FIT_SCALE = 0.93
FIT_SHIFT = (0.0, 1.0)

# The status-bar icon: the same drawing, the roof a little heavier and the coin a unit lower so
# the two keep apart at 24 units, fitted to the grid.
STAT_SCALE = 0.33
STAT_SHIFT = (0.0, 2.0 * 0.33)
STAT_ROOF_ROUND = 5.0
STAT_COIN_DROP = 1.0
STAT_RIM = (10.4, 3.2)

HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- Written by tools/draw_launcher_icon.py: change the script and run it, never this file.
     {what} -->
"""


def f(v):
    return f"{v:.2f}"


def lit(box):
    """A shape's light: from the upper-left corner of its box to the lower-right one, along the
    diagonal of a square on the box's larger side, so every shape is lit at the same angle."""
    x0, y0, x1, y1 = box
    cx, cy, r = (x0 + x1) / 2, (y0 + y1) / 2, max(x1 - x0, y1 - y0) / 2
    return (cx - r, cy - r), (cx + r, cy + r)


def ramp(attr, box, light, deep, indent="    "):
    (sx, sy), (ex, ey) = lit(box)
    return (
        f'{indent}<aapt:attr name="android:{attr}">\n'
        f'{indent}    <gradient android:type="linear" android:startX="{f(sx)}" android:startY="{f(sy)}" '
        f'android:endX="{f(ex)}" android:endY="{f(ey)}">\n'
        f'{indent}        <item android:offset="0" android:color="#FF{light}"/>\n'
        f'{indent}        <item android:offset="1" android:color="#FF{deep}"/>\n'
        f"{indent}    </gradient>\n{indent}</aapt:attr>\n"
    )


def shaded(d, box, light, deep, stroke=None):
    """A shape lit from the upper left; with a stroke, the stroke wears the same light."""
    if stroke is None:
        return f'<path android:pathData="{d}">\n{ramp("fillColor", box, light, deep)}</path>\n'
    return (
        f'<path android:pathData="{d}" android:strokeWidth="{f(stroke)}" android:strokeLineJoin="round">\n'
        f'{ramp("fillColor", box, light, deep)}{ramp("strokeColor", box, light, deep)}</path>\n'
    )


def ink(d, stroke=None, even_odd=False):
    extra = f' android:strokeColor="#FFFFFFFF" android:strokeWidth="{f(stroke)}" android:strokeLineJoin="round"' if stroke else ""
    rule = ' android:fillType="evenOdd"' if even_odd else ""
    return f'<path android:pathData="{d}" android:fillColor="#FFFFFFFF"{rule}{extra}/>\n'


def vector(body, size=108, aapt=True):
    ns = '\n    xmlns:aapt="http://schemas.android.com/aapt"' if aapt else ""
    return (
        f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{ns}\n'
        f'    android:width="{size}dp"\n    android:height="{size}dp"\n'
        f'    android:viewportWidth="{size}"\n    android:viewportHeight="{size}">\n'
        f"{body}</vector>\n"
    )


def fitted(body, scale, shift):
    return (
        f'<group android:pivotX="{f(CENTRE)}" android:pivotY="{f(CENTRE)}" android:scaleX="{scale:g}" '
        f'android:scaleY="{scale:g}" android:translateX="{f(shift[0])}" android:translateY="{f(shift[1])}">\n'
        f"{body}</group>\n"
    )


def circle(cx, cy, r):
    return f"M {f(cx - r)},{f(cy)} a {f(r)},{f(r)} 0 1,0 {f(2 * r)},0 a {f(r)},{f(r)} 0 1,0 {f(-2 * r)},0 z"


def roof():
    """The slab: the outer chevron and, under it, the same chevron lowered by its thickness,
    its underside starting EAVE_CUT in from the eaves' tips."""
    (lx, ly), (ax, ay), (rx, ry) = EAVES_LEFT, APEX, EAVES_RIGHT
    slope = (ly - ay) / (ax - lx)
    inner_left = (lx + EAVE_CUT, ay + SLAB + (ax - lx - EAVE_CUT) * slope)
    inner_right = (rx - EAVE_CUT, ay + SLAB + (rx - EAVE_CUT - ax) * slope)
    return (
        f"M {f(lx)},{f(ly)} L {f(ax)},{f(ay)} L {f(rx)},{f(ry)} L {f(inner_right[0])},{f(inner_right[1])} "
        f"L {f(ax)},{f(ay + SLAB)} L {f(inner_left[0])},{f(inner_left[1])} Z"
    )


def chimney():
    x0, x1, top, foot, r = CHIMNEY
    return (
        f"M {f(x0)},{f(top + r)} Q {f(x0)},{f(top)} {f(x0 + r)},{f(top)} L {f(x1 - r)},{f(top)} "
        f"Q {f(x1)},{f(top)} {f(x1)},{f(top + r)} L {f(x1)},{f(foot)} L {f(x0)},{f(foot)} Z"
    )


def struck_coin(cx, cy, r, rim_radius, rim_width):
    """The coin in one ink with its rim cut out: the band outside the rim, and the disc inside."""
    out, inside = rim_radius + rim_width / 2, rim_radius - rim_width / 2
    return ink(f"{circle(cx, cy, r)} {circle(cx, cy, out)}", even_odd=True) + ink(circle(cx, cy, inside))


def background():
    body = shaded("M 0,0 H 108 V 108 H 0 Z", (18, 18, 90, 90), INDIGO[0], INDIGO[2])
    return HEADER.format(what="The badge's ground: the app's indigo, lit from the upper left.") + vector(body)


def foreground():
    cx, cy, r = COIN
    rim_radius, rim_width = RIM
    body = (
        shaded(chimney(), ROOF_LIGHT_BOX, WHITE[0], WHITE[2])
        + shaded(roof(), ROOF_LIGHT_BOX, WHITE[0], WHITE[2], stroke=ROOF_ROUND)
        + shaded(circle(cx, cy, r), (cx - r, cy - r, cx + r, cy + r), SUN[0], SUN[1])
        + f'<path android:pathData="{circle(cx, cy, rim_radius)}" android:fillColor="#00000000" '
        f'android:strokeColor="#{RIM_ALPHA}{SUN[2]}" android:strokeWidth="{f(rim_width)}"/>\n'
    )
    what = (
        "The mark: a white roof with its chimney over an amber coin, its rim struck in a deeper\n"
        f"     amber; inside {KEYLINE:g} units of the centre, the keyline."
    )
    return HEADER.format(what=what) + vector(fitted(body, FIT_SCALE, FIT_SHIFT))


def monochrome():
    cx, cy, r = COIN
    body = ink(chimney()) + ink(roof(), stroke=ROOF_ROUND) + struck_coin(cx, cy, r, *RIM)
    what = (
        "The themed icon (Android 13+): the system reads only the alpha, so the coin's rim is cut\n"
        "     out of it, and the rest is the mark's shapes."
    )
    return HEADER.format(what=what) + vector(fitted(body, FIT_SCALE, FIT_SHIFT), aapt=False)


def status_bar():
    cx, cy, r = COIN
    body = ink(chimney()) + ink(roof(), stroke=STAT_ROOF_ROUND) + struck_coin(cx, cy + STAT_COIN_DROP, r, *STAT_RIM)
    shift = (12 - CENTRE + STAT_SHIFT[0], 12 - CENTRE + STAT_SHIFT[1])
    what = (
        "The status-bar icon of the evening reminder: the mark's roof and coin in one ink, the rim\n"
        "     cut out, inside 2 units of air on the 24-unit grid. White on transparent, as status-bar\n"
        "     icons must be: the system tints it."
    )
    return HEADER.format(what=what) + vector(fitted(body, STAT_SCALE, shift), size=24, aapt=False)


def main():
    for path, text in (
        (RES / "ic_launcher_background.xml", background()),
        (RES / "ic_launcher_foreground.xml", foreground()),
        (RES / "ic_launcher_monochrome.xml", monochrome()),
        (STAT, status_bar()),
    ):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8", newline="\n")
        print("wrote", path)


if __name__ == "__main__":
    main()
