#!/usr/bin/env python3
"""Draws Tetto's launcher mark: the two adaptive-icon layers in app/src/main/res/drawable.

    python3 tools/draw_launcher_icon.py

The mark is the family's ring (radius 21, stroke 10) on a warm white, cut by an amber emblem:
Tetto's roof, at the lower right (owner, 5 Nov 2026: the place the other money app's emblem
takes, since a reader most often installs one of the two). The roof stands upright, as a roof
does, whatever the ring's angle: two slopes and a chimney, each a stroke with round ends, so it
reads as a drawn roof and not as a caret or an arrow. The ring shades clockwise from just past
the roof round to it again, light to dark; its colours are the owner's choice (PLANNING.md §15,
Open), kept clearly apart from every sister's ring, under deuteranopia too.

Re-running this script IS the drawing: ic_launcher_foreground.xml and ic_launcher_monochrome.xml
are its output and are not edited by hand. Only the standard library is used, so any Python 3
runs it.
"""

import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/drawable"

CENTRE = 54.0
RING_RADIUS = 21.0
RING_WIDTH = 10.0
SAFE_RADIUS = 33.0  # every launcher mask keeps this circle whole
ROOF_ANGLE = 40.0  # degrees clockwise from three o'clock: the lower right

# The roof, around the emblem's point on the ring, as strokes with round ends: (from, to), in
# units of the 108 grid. Two slopes meeting at the ridge, at 42 degrees, and a chimney standing
# on the right-hand slope, its top level with the ridge. The stroke is heavy (6.2) so the roof
# weighs on the ring what the sisters' filled emblems do: at 4.6 it read as a thin tool.
SLOPE = math.radians(42.0)
RIDGE = (0.0, -4.8)
EAVE = 7.6  # half the roof's span, ridge to eave, along x
STROKE = 6.2
CHIMNEY_X = 4.4
CHIMNEY_TOP = -6.4
GAP = 3.0  # the clear band around the roof, where it cuts the ring: the sisters' gap

# The ring, from just past the roof round to it again: (position along the sweep, colour). The
# owner chooses it (PLANNING.md §15, Open): this is the proposal drawn for that choice.
RING_STOPS = [(0.0, "F27DA6"), (0.45, "C2286E"), (1.0, "5B1848")]
AMBER = ("FFC658", "EF8618")  # the emblem's amber, every sister's


def f(v):
    return f"{v:.2f}"


def point(deg, r=RING_RADIUS, c=CENTRE):
    a = math.radians(deg)
    return c + r * math.cos(a), c + r * math.sin(a)


def circle(cx, cy, r):
    """A circle walked counter-clockwise (sweep flag 0): filled on its own, a hole in a clip."""
    return f"M {f(cx - r)},{f(cy)} a {f(r)},{f(r)} 0 1,0 {f(2 * r)},0 a {f(r)},{f(r)} 0 1,0 {f(-2 * r)},0 z"


def mix(a, b, t):
    ca = [int(a[i:i + 2], 16) for i in (0, 2, 4)]
    cb = [int(b[i:i + 2], 16) for i in (0, 2, 4)]
    return "".join(f"{round(x + (y - x) * t):02X}" for x, y in zip(ca, cb))


def signed_area(poly):
    return sum(a[0] * b[1] - b[0] * a[1] for a, b in zip(poly, poly[1:] + poly[:1])) / 2


def strokes():
    """The roof's three strokes, on the canvas: the two slopes from the ridge, and the chimney
    from the right-hand slope up."""
    ox, oy = point(ROOF_ANGLE)
    eave_y = RIDGE[1] + EAVE * math.tan(SLOPE)
    chimney_foot = RIDGE[1] + CHIMNEY_X * math.tan(SLOPE)
    local = [
        (RIDGE, (-EAVE, eave_y)),
        (RIDGE, (EAVE, eave_y)),
        ((CHIMNEY_X, chimney_foot), (CHIMNEY_X, CHIMNEY_TOP)),
    ]
    return [((ox + ax, oy + ay), (ox + bx, oy + by)) for (ax, ay), (bx, by) in local]


def capsule(a, b, r, hole):
    """A stroke from [a] to [b] with round ends of radius [r], as a polygon. As a [hole] it is
    walked against the canvas (negative area), so it cuts a clip under the nonzero rule."""
    angle = math.atan2(b[1] - a[1], b[0] - a[0])
    corners = []
    for (ex, ey), start in ((b, angle - math.pi / 2), (a, angle + math.pi / 2)):
        for i in range(13):
            t = start + math.pi * i / 12
            corners.append((ex + r * math.cos(t), ey + r * math.sin(t)))
    if (signed_area(corners) > 0) == hole:
        corners.reverse()
    return "M " + " L ".join(f"{f(x)},{f(y)}" for x, y in corners) + " Z"


def check_safe():
    """The whole mark inside the launcher's safe circle, with room to spare."""
    farthest = max(
        math.dist((CENTRE, CENTRE), end) + STROKE / 2 for stroke in strokes() for end in stroke
    )
    assert farthest <= SAFE_RADIUS - 1.0, f"the roof reaches {farthest:.2f}, past the safe circle"
    return farthest


def ring_stops():
    """The ring as one sweep (which starts at three o'clock): its seam hides under the roof."""

    def colour_at(t):
        for (t0, c0), (t1, c1) in zip(RING_STOPS, RING_STOPS[1:]):
            if t0 <= t <= t1:
                return mix(c0, c1, (t - t0) / (t1 - t0))
        raise ValueError(t)

    def colour(deg):
        return colour_at(((deg - ROOF_ANGLE) % 360) / 360)

    stops = [(0.0, colour(0.0))]
    stops += [((ROOF_ANGLE + t * 360) % 360, c) for t, c in RING_STOPS[1:-1]]
    stops += [(ROOF_ANGLE - 0.2, RING_STOPS[-1][1]), (ROOF_ANGLE + 0.2, RING_STOPS[0][1]), (360.0, colour(360.0))]
    stops.sort()
    return "\n".join(
        f'                <item android:offset="{d / 360:.4f}" android:color="#FF{c}"/>' for d, c in stops
    )


def ring(stroke):
    """The ring less the band round each of the roof's strokes. One group per stroke: nested
    clips intersect, so the ring keeps what lies outside all three bands."""
    clips = [
        f'<group>\n    <clip-path android:pathData="M 0,0 H 108 V 108 H 0 Z {capsule(a, b, STROKE / 2 + GAP, hole=True)}"/>'
        for a, b in strokes()
    ]
    body = f'    <path android:pathData="{circle(CENTRE, CENTRE, RING_RADIUS)}" android:strokeWidth="{f(RING_WIDTH)}" android:fillColor="#00000000"{stroke}'
    return "\n".join(clips) + "\n" + body + "\n" + "</group>\n" * len(clips)


def roof(fill):
    return "\n".join(f'<path android:pathData="{capsule(a, b, STROKE / 2, hole=False)}"{fill}' for a, b in strokes())


def amber_fill():
    cx, cy = point(ROOF_ANGLE)
    return f""">
    <aapt:attr name="android:fillColor">
        <gradient android:type="linear" android:startX="{f(cx - 7)}" android:startY="{f(cy - 7)}" android:endX="{f(cx + 7)}" android:endY="{f(cy + 7)}">
            <item android:offset="0" android:color="#FF{AMBER[0]}"/>
            <item android:offset="1" android:color="#FF{AMBER[1]}"/>
        </gradient>
    </aapt:attr>
</path>"""


def sweep_stroke():
    return f""">
        <aapt:attr name="android:strokeColor">
            <gradient android:type="sweep" android:centerX="{f(CENTRE)}" android:centerY="{f(CENTRE)}">
{ring_stops()}
            </gradient>
        </aapt:attr>
    </path>"""


HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- Written by tools/draw_launcher_icon.py: change the script and run it, never this file.
     {what} -->
"""


def vector(body, aapt=True):
    ns = '\n    xmlns:aapt="http://schemas.android.com/aapt"' if aapt else ""
    return f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"{ns}
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
{body}
</vector>
"""


def main():
    reach = check_safe()
    RES.mkdir(parents=True, exist_ok=True)
    (RES / "ic_launcher_foreground.xml").write_text(
        HEADER.format(
            what="The mark: the family's ring, shading light to dark, cut by an amber roof at the\n"
            "     lower right. Everything sits inside the 33-unit safe circle of every launcher\n"
            f"     mask (ring edge 26, roof's edge {reach:.1f})."
        )
        + vector(ring(sweep_stroke()) + "\n" + roof(amber_fill())),
        encoding="utf-8",
        newline="\n",
    )
    (RES / "ic_launcher_monochrome.xml").write_text(
        HEADER.format(
            what="The themed icon (Android 13+): the foreground's shapes in one ink, which the system\n"
            "     tints: the ring with its cut, and the roof."
        )
        + vector(ring(' android:strokeColor="#FF000000"/>') + "\n" + roof(' android:fillColor="#FF000000"/>'), aapt=False),
        encoding="utf-8",
        newline="\n",
    )
    print("wrote", RES / "ic_launcher_foreground.xml", "and", RES / "ic_launcher_monochrome.xml", f"(reach {reach:.2f})")


if __name__ == "__main__":
    main()
