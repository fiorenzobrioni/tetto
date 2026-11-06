#!/usr/bin/env python3
"""Draws Tetto's launcher mark: the two adaptive-icon layers in app/src/main/res/drawable, and
the status-bar icon (core/reminder/src/main/res/drawable/ic_stat_tetto.xml).

    python3 tools/draw_launcher_icon.py

The mark is the family's ring (radius 21, stroke 10) on a warm white, cut by an amber emblem at
the lower right (owner, 5 Nov 2026: the place the other money app's emblem takes, since a reader
most often installs one of the two). The emblem is a house (owner, 6 Nov 2026): walls, a gable at
42 degrees, a chimney on the right-hand slope, and a window of four panes cut out of its front,
as a clock's hands are cut out of its face. A house is as compact as the family's round emblems,
so it weighs what they weigh (213 square units against their 211 to 222) and still reads at
36 px; the window, where a door would be expected, keeps it from reading as a launcher's home
key. The roof alone, drawn first as three strokes and then as a solid slab, read as a caret
beside the family's filled emblems.

The ring is slate (owner, 6 Nov 2026), shading clockwise from just past the house round to it
again, light to dark: the blue grey of a slate roof, nearly opposite the amber on the colour
wheel, so the ring stays quiet and the house is the brightest thing in the mark. Its lightness is
the family's (Oklab L 0.57 on average against 0.56 to 0.66), so the marks weigh the same side by
side on a home screen.

Every shape is a convex core grown by a radius (a point grown is a circle, a segment a capsule, a
polygon a polygon with round corners), so the gap round each shape, where it cuts the ring, is the
same shape grown by GAP, and its outline is exact: straight edges and arcs.

The status-bar icon is the house alone, its window cut out, as large as the 24-unit grid allows
with 2 units clear on each side: a ring with a cut would read as a blot at that size, and the
house is the mark's.

Re-running this script IS the drawing: ic_launcher_foreground.xml, ic_launcher_monochrome.xml and
ic_stat_tetto.xml are its output and are not edited by hand. Only the standard library is used,
so any Python 3 runs it.
"""

import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/drawable"
STAT = ROOT / "core/reminder/src/main/res/drawable/ic_stat_tetto.xml"
STAT_SIZE = 24.0
STAT_MARGIN = 2.0

CENTRE = 54.0
RING_RADIUS = 21.0
RING_WIDTH = 10.0
SAFE_RADIUS = 33.0  # every launcher mask keeps this circle whole
HOUSE_ANGLE = 40.0  # degrees clockwise from three o'clock: the lower right
GAP = 3.0  # the clear band around the house, where it cuts the ring: the family's gap

# The house, in units of the 108 grid, around the emblem's point on the ring (x right, y down).
HALF_WIDTH = 8.3
EAVE = -1.4  # where the walls meet the gable
PITCH = 42.0  # the gable's slope, in degrees
BOTTOM = 8.2
CORNER = 1.5  # the round of the house's corners: the family's emblems have no sharp point
# The chimney's sides, and the round of its corners. It stands on the right-hand slope and
# rises to just under the ridge; its foot is buried in the roof.
CHIMNEY = (3.5, 6.9)
CHIMNEY_CORNER = 0.9
# The window: its middle (y), half the width of the cross between its panes, a pane's side and
# the round of a pane's corners. Large enough that the four panes stay four at 36 px.
WINDOW_MIDDLE = 3.0
MULLION = 0.7
PANE = 2.35
PANE_CORNER = 0.35
# The house sits a little above the ring's point: its foot is its widest part, and the lower
# right is where the safe circle is nearest.
LIFT = -0.6

# The ring, from just past the house round to it again: (position along the sweep, colour).
# Slate (owner, 6 Nov 2026), chosen from nine rings drawn beside the family's marks on light
# and dark grounds, under deuteranopia and in grey (PLANNING.md §15, 6 Nov 2026).
RING_STOPS = [(0.0, "A4C2DC"), (0.45, "58769D"), (1.0, "273857")]
AMBER = ("FFC658", "EF8618")  # the emblem's amber, the family's, light at the upper left

FULL = "M 0,0 H 108 V 108 H 0 Z"


def f(v):
    return f"{v:.2f}"


def point(deg, r=RING_RADIUS, c=CENTRE):
    a = math.radians(deg)
    return c + r * math.cos(a), c + r * math.sin(a)


def circle(cx, cy, r):
    """A circle walked counter-clockwise (sweep flag 0)."""
    return f"M {f(cx - r)},{f(cy)} a {f(r)},{f(r)} 0 1,0 {f(2 * r)},0 a {f(r)},{f(r)} 0 1,0 {f(-2 * r)},0 z"


def mix(a, b, t):
    ca = [int(a[i:i + 2], 16) for i in (0, 2, 4)]
    cb = [int(b[i:i + 2], 16) for i in (0, 2, 4)]
    return "".join(f"{round(x + (y - x) * t):02X}" for x, y in zip(ca, cb))


def shoelace(pts):
    return sum(a[0] * b[1] - b[0] * a[1] for a, b in zip(pts, pts[1:] + pts[:1]))


class Shape:
    """A convex core (one or more points) grown by [r]."""

    def __init__(self, core, r):
        self.core = [tuple(p) for p in core]
        self.r = r

    def moved(self, dx, dy, scale=1.0):
        return Shape([(dx + x * scale, dy + y * scale) for x, y in self.core], self.r * scale)

    def reach(self, centre):
        """How far the shape reaches from [centre]: exact, since the farthest point of a grown
        convex core is a core point plus the radius."""
        return max(math.dist(centre, p) for p in self.core) + self.r

    def bounds(self):
        xs = [x for x, _ in self.core]
        ys = [y for _, y in self.core]
        return min(xs) - self.r, min(ys) - self.r, max(xs) + self.r, max(ys) + self.r

    def outline(self, grow=0.0, hole=False):
        """The outline as path data, walked clockwise on screen; as a [hole] it is walked against
        the canvas, so it cuts a clip under the nonzero rule."""
        R = self.r + grow
        if len(self.core) == 1:
            (x, y) = self.core[0]
            sweep = 0 if hole else 1
            return (f"M {f(x - R)},{f(y)} A {f(R)},{f(R)} 0 1,{sweep} {f(x + R)},{f(y)} "
                    f"A {f(R)},{f(R)} 0 1,{sweep} {f(x - R)},{f(y)} Z")
        pts = self.core
        if len(pts) > 2 and shoelace(pts) < 0:
            pts = pts[::-1]
        n = len(pts)
        normals = []  # the outward normal of the edge from point i to point i + 1
        for i in range(n):
            (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % n]
            length = math.hypot(x1 - x0, y1 - y0)
            normals.append(((y1 - y0) / length, (x0 - x1) / length))
        # At each corner the outline comes in along the previous edge, offset by R, turns round
        # the corner on an arc and leaves along the next edge.
        turns = []
        for i in range(n):
            (vx, vy), (ax, ay), (bx, by) = pts[i], normals[i - 1], normals[i]
            turns.append(((vx + ax * R, vy + ay * R), (vx + bx * R, vy + by * R)))
        if not hole:
            d = f"M {f(turns[0][0][0])},{f(turns[0][0][1])}"
            for i, (_, out) in enumerate(turns):
                d += f" A {f(R)},{f(R)} 0 0,1 {f(out[0])},{f(out[1])}"
                nxt = turns[(i + 1) % n][0]
                d += f" L {f(nxt[0])},{f(nxt[1])}"
            return d + " Z"
        d = f"M {f(turns[0][1][0])},{f(turns[0][1][1])}"
        for i in range(n):
            into, _ = turns[-i % n]
            d += f" A {f(R)},{f(R)} 0 0,0 {f(into[0])},{f(into[1])}"
            prev = turns[(-i - 1) % n][1]
            d += f" L {f(prev[0])},{f(prev[1])}"
        return d + " Z"


def rounded(pts, r):
    """A convex polygon of the given final corners, rounded by [r]: its core is the polygon with
    each edge pulled in by [r]."""
    if shoelace(pts) < 0:
        pts = pts[::-1]
    n = len(pts)
    lines = []
    for i in range(n):
        (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % n]
        length = math.hypot(x1 - x0, y1 - y0)
        nx, ny = (y1 - y0) / length, (x0 - x1) / length
        lines.append(((x0 - nx * r, y0 - ny * r), (x1 - x0, y1 - y0)))
    core = []
    for i in range(n):
        (p, d), (q, e) = lines[i - 1], lines[i]
        s = ((q[0] - p[0]) * e[1] - (q[1] - p[1]) * e[0]) / (d[0] * e[1] - d[1] * e[0])
        core.append((p[0] + s * d[0], p[1] + s * d[1]))
    return Shape(core, r)


def box(x0, y0, x1, y1, r):
    return rounded([(x0, y0), (x1, y0), (x1, y1), (x0, y1)], r)


def house():
    """The house's filled shapes and the panes cut out of it, on the canvas."""
    top = EAVE - HALF_WIDTH * math.tan(math.radians(PITCH))
    walls = rounded([(0, top), (HALF_WIDTH, EAVE), (HALF_WIDTH, BOTTOM), (-HALF_WIDTH, BOTTOM), (-HALF_WIDTH, EAVE)], CORNER)
    chimney = box(CHIMNEY[0], top + 0.2, CHIMNEY[1], EAVE - 1.2, CHIMNEY_CORNER)
    c, g, p = WINDOW_MIDDLE, MULLION, PANE
    panes = [
        box(-g - p, c - g - p, -g, c - g, PANE_CORNER),
        box(g, c - g - p, g + p, c - g, PANE_CORNER),
        box(-g - p, c + g, -g, c + g + p, PANE_CORNER),
        box(g, c + g, g + p, c + g + p, PANE_CORNER),
    ]
    ox, oy = point(HOUSE_ANGLE)
    oy += LIFT
    return [s.moved(ox, oy) for s in (walls, chimney)], [s.moved(ox, oy) for s in panes]


def check_safe(solids):
    """The whole mark inside the launcher's safe circle, with room to spare."""
    reach = max(s.reach((CENTRE, CENTRE)) for s in solids)
    assert reach <= SAFE_RADIUS - 1.0, f"the house reaches {reach:.2f}, past the safe circle"
    return reach


def ring_stops():
    """The ring as one sweep (which starts at three o'clock): its seam hides under the house."""

    def colour_at(t):
        for (t0, c0), (t1, c1) in zip(RING_STOPS, RING_STOPS[1:]):
            if t0 <= t <= t1:
                return mix(c0, c1, (t - t0) / (t1 - t0))
        raise ValueError(t)

    def colour(deg):
        return colour_at(((deg - HOUSE_ANGLE) % 360) / 360)

    stops = [(0.0, colour(0.0))]
    stops += [((HOUSE_ANGLE + t * 360) % 360, c) for t, c in RING_STOPS[1:-1]]
    stops += [(HOUSE_ANGLE - 0.2, RING_STOPS[-1][1]), (HOUSE_ANGLE + 0.2, RING_STOPS[0][1]), (360.0, colour(360.0))]
    stops.sort()
    return "\n".join(
        f'                <item android:offset="{d / 360:.4f}" android:color="#FF{c}"/>' for d, c in stops
    )


def ring(solids, stroke):
    """The ring less the band round each of the house's shapes. One group per shape: nested
    clips intersect, so the ring keeps what lies outside every band."""
    clips = [
        f'<group>\n    <clip-path android:pathData="{FULL} {s.outline(GAP, hole=True)}"/>' for s in solids
    ]
    body = f'    <path android:pathData="{circle(CENTRE, CENTRE, RING_RADIUS)}" android:strokeWidth="{f(RING_WIDTH)}" android:fillColor="#00000000"{stroke}'
    return "\n".join(clips) + "\n" + body + "\n" + "</group>\n" * len(clips)


def emblem(solids, panes, fill, full=FULL):
    """The house in one path, inside one group per pane whose clip leaves that pane out."""
    clips = [f'<group>\n    <clip-path android:pathData="{full} {p.outline(hole=True)}"/>' for p in panes]
    path = f'<path android:pathData="{" ".join(s.outline() for s in solids)}"{fill}'
    return "\n".join(clips) + "\n" + path + "\n" + "</group>\n" * len(panes)


def amber_fill():
    cx, cy = point(HOUSE_ANGLE)
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


def status_bar(solids, panes):
    """The house moved to the 24-unit grid, as large as it allows, centred, in white."""
    bounds = [s.bounds() for s in solids]
    left, top = min(b[0] for b in bounds), min(b[1] for b in bounds)
    right, bottom = max(b[2] for b in bounds), max(b[3] for b in bounds)
    scale = (STAT_SIZE - 2 * STAT_MARGIN) / max(right - left, bottom - top)
    dx = STAT_SIZE / 2 - (left + right) / 2 * scale
    dy = STAT_SIZE / 2 - (top + bottom) / 2 * scale
    body = emblem(
        [s.moved(dx, dy, scale) for s in solids],
        [p.moved(dx, dy, scale) for p in panes],
        ' android:fillColor="#FFFFFFFF"/>',
        full="M 0,0 H 24 V 24 H 0 Z",
    )
    return HEADER.format(
        what="The status-bar icon of the evening reminder: the mark's house alone, its window cut out,\n"
        "     on the 24-unit grid. White on transparent, as status-bar icons must be: the system tints it."
    ) + f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
{body}</vector>
"""


def main():
    solids, panes = house()
    reach = check_safe(solids)
    RES.mkdir(parents=True, exist_ok=True)
    (RES / "ic_launcher_foreground.xml").write_text(
        HEADER.format(
            what="The mark: the family's ring, shading light to dark, cut by an amber house at the\n"
            "     lower right. Everything sits inside the 33-unit safe circle of every launcher\n"
            f"     mask (ring edge 26, house's edge {reach:.1f})."
        )
        + vector(ring(solids, sweep_stroke()) + "\n" + emblem(solids, panes, amber_fill())),
        encoding="utf-8",
        newline="\n",
    )
    (RES / "ic_launcher_monochrome.xml").write_text(
        HEADER.format(
            what="The themed icon (Android 13+): the foreground's shapes in one ink, which the system\n"
            "     tints: the ring with its cut, and the house with its window."
        )
        + vector(ring(solids, ' android:strokeColor="#FF000000"/>') + "\n" + emblem(solids, panes, ' android:fillColor="#FF000000"/>'), aapt=False),
        encoding="utf-8",
        newline="\n",
    )
    STAT.parent.mkdir(parents=True, exist_ok=True)
    STAT.write_text(status_bar(solids, panes), encoding="utf-8", newline="\n")
    print("wrote", RES / "ic_launcher_foreground.xml", "and", RES / "ic_launcher_monochrome.xml", f"(reach {reach:.2f})")
    print("wrote", STAT)


if __name__ == "__main__":
    main()
