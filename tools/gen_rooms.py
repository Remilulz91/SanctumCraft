#!/usr/bin/env python3
"""
Generates the placeholder room templates of SanctumCraft (structure .nbt files).

Grid cell = 25 x 25 blocks. Rooms:
  - 1x1 rooms: 25 x 10 x 25 (floor y=0, ceiling y=9)
  - small rooms: 25 x 7 x 25 (13x13 room in the middle + 4 corridor arms)
  - great halls: 50 x 14 x 50 (2x2 cells, ceiling y=13)
Every cell side has a doorway in its middle: offset 11..13, y 1..4. The mod seals the
doorways that don't lead anywhere. Corridors are built by the mod itself (no template).
Air is not stored: rooms are placed in an empty void.

Fixed positions used by the mod (do not move them when editing):
  - entrance: spawn at (12, 1, 9), exit portal interior x 11..13, y 2..5, z 3
  - nest: nest block at (12, 3, 12)
  - destination: mechanism at (12, 3, 12), exit portal frame interior x 11..13, y 2..5, z 20

Usage:  pip install nbtlib  &&  python tools/gen_rooms.py
Output: src/main/resources/data/sanctumcraft/structure/room/*.nbt

To retouch a room in game: structure block in Load mode, name "sanctumcraft:room/hall_pillars",
load, edit, switch to Save mode (same name and size), save, then copy
saves/<world>/generated/sanctumcraft/structures/room/<name>.nbt back into the mod.
"""
import os, random, math
from nbtlib import File, Compound, List, Int, String

DATA_VERSION = 3955  # Minecraft 1.21.1
CELL = 25
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "data", "sanctumcraft", "structure", "room")


class Room:
    def __init__(self, seed, sx=CELL, sy=10, sz=CELL):
        self.blocks = {}
        self.rnd = random.Random(seed)
        self.sx, self.sy, self.sz = sx, sy, sz

    def set(self, x, y, z, name, **props):
        if 0 <= x < self.sx and 0 <= y < self.sy and 0 <= z < self.sz:
            self.blocks[(x, y, z)] = (name, tuple(sorted(props.items())))

    def clear(self, x, y, z):
        self.blocks.pop((x, y, z), None)

    def fill(self, x1, y1, z1, x2, y2, z2, name, **props):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, name, **props)

    def save(self, name):
        palette, index, blocks = [], {}, []
        for pos, state in sorted(self.blocks.items()):
            if state not in index:
                index[state] = len(palette)
                n, props = state
                entry = Compound({"Name": String(n)})
                if props:
                    entry["Properties"] = Compound({k: String(v) for k, v in props})
                palette.append(entry)
            blocks.append(Compound({"pos": List[Int]([Int(pos[0]), Int(pos[1]), Int(pos[2])]),
                                    "state": Int(index[state])}))
        root = Compound({
            "DataVersion": Int(DATA_VERSION),
            "size": List[Int]([Int(self.sx), Int(self.sy), Int(self.sz)]),
            "palette": List[Compound](palette),
            "blocks": List[Compound](blocks),
            "entities": List[Compound]([]),
        })
        os.makedirs(OUT, exist_ok=True)
        path = os.path.join(OUT, name + ".nbt")
        File(root).save(path, gzipped=True)
        print(f"{name}: {self.sx}x{self.sy}x{self.sz}, {len(blocks)} blocks, {len(palette)} states")


def door_ranges(size):
    """Doorway offsets along a side of `size` blocks (one per cell)."""
    return [(c * CELL + 11, c * CELL + 13) for c in range(size // CELL)]


def is_door(r, x, y, z):
    if not (1 <= y <= 4):
        return False
    if z in (0, r.sz - 1) and any(a <= x <= b for a, b in door_ranges(r.sx)):
        return True
    if x in (0, r.sx - 1) and any(a <= z <= b for a, b in door_ranges(r.sz)):
        return True
    return False


def shell(r):
    """Floor, walls, ceiling, doorway frames, corner pillars and lanterns."""
    rnd, top = r.rnd, r.sy - 1
    for x in range(r.sx):
        for z in range(r.sz):
            border = x in (1, r.sx - 2) or z in (1, r.sz - 2)
            if border:
                r.set(x, 0, z, "minecraft:polished_deepslate")
            elif (x + z) % 6 == 0 and (x - z) % 6 == 0:
                r.set(x, 0, z, "minecraft:chiseled_deepslate")
            else:
                r.set(x, 0, z, "minecraft:cracked_deepslate_tiles" if rnd.random() < 0.12 else "minecraft:deepslate_tiles")
            r.set(x, top, z, "minecraft:deepslate_tiles")
    for y in range(1, top):
        walls = [(i, 0) for i in range(r.sx)] + [(i, r.sz - 1) for i in range(r.sx)] \
              + [(0, i) for i in range(r.sz)] + [(r.sx - 1, i) for i in range(r.sz)]
        for (x, z) in walls:
            if is_door(r, x, y, z):
                continue
            if y == 1:
                name = "minecraft:polished_deepslate"
            elif y == top - 1:
                name = "minecraft:chiseled_deepslate"
            else:
                name = "minecraft:cracked_deepslate_bricks" if rnd.random() < 0.15 else "minecraft:deepslate_bricks"
            r.set(x, y, z, name)
    # Rune stone frames around doorways
    for a, b in door_ranges(r.sx):
        for z in (0, r.sz - 1):
            for y in range(1, 6):
                r.set(a - 1, y, z, "sanctumcraft:rune_stone")
                r.set(b + 1, y, z, "sanctumcraft:rune_stone")
            for x in range(a - 1, b + 2):
                r.set(x, 5, z, "sanctumcraft:rune_stone")
    for a, b in door_ranges(r.sz):
        for x in (0, r.sx - 1):
            for y in range(1, 6):
                r.set(x, y, a - 1, "sanctumcraft:rune_stone")
                r.set(x, y, b + 1, "sanctumcraft:rune_stone")
            for z in range(a - 1, b + 2):
                r.set(x, 5, z, "sanctumcraft:rune_stone")
    for (cx, cz) in ((1, 1), (1, r.sz - 2), (r.sx - 2, 1), (r.sx - 2, r.sz - 2)):
        r.fill(cx, 1, cz, cx, top - 1, cz, "minecraft:polished_deepslate")


def hang_lantern(r, x, z, chain=1):
    top = r.sy - 1
    for i in range(chain):
        r.set(x, top - 1 - i, z, "minecraft:chain", axis="y")
    r.set(x, top - 1 - chain, z, "minecraft:soul_lantern", hanging="true")


def cell_lanterns(r):
    for cx in range(r.sx // CELL):
        for cz in range(r.sz // CELL):
            for (x, z) in ((6, 6), (18, 6), (6, 18), (18, 18)):
                hang_lantern(r, cx * CELL + x, cz * CELL + z, chain=1 if r.sy <= 10 else 3)


def pillar(r, cx, cz, half=1):
    top = r.sy - 1
    r.fill(cx - half, 1, cz - half, cx + half, top - 1, cz + half, "minecraft:polished_deepslate")
    r.fill(cx - half, 1, cz - half, cx + half, 1, cz + half, "minecraft:chiseled_deepslate")
    r.fill(cx - half, top - 1, cz - half, cx + half, top - 1, cz + half, "minecraft:chiseled_deepslate")
    for dx, dz, f in ((-half - 1, 0, "west"), (half + 1, 0, "east"), (0, -half - 1, "north"), (0, half + 1, "south")):
        r.set(cx + dx, 3, cz + dz, "minecraft:amethyst_cluster", facing=f)


def exit_frame(r, z):
    """Rune stone frame, interior x 11..13, y 2..5 (left empty)."""
    r.fill(10, 1, z, 14, 6, z, "sanctumcraft:rune_stone")
    for x in range(11, 14):
        for y in range(2, 6):
            r.clear(x, y, z)


# ------------------------------------------------------------------ 1x1 rooms

def entrance():
    r = Room(1)
    shell(r); cell_lanterns(r)
    r.fill(10, 1, 3, 14, 6, 3, "sanctumcraft:rune_stone")
    r.fill(11, 2, 3, 13, 5, 3, "sanctumcraft:sanctum_portal", axis="x")
    r.fill(9, 0, 2, 15, 0, 5, "minecraft:polished_deepslate")
    for x in (9, 15):
        r.set(x, 1, 3, "minecraft:soul_lantern")
    r.set(12, 0, 9, "minecraft:chiseled_deepslate")
    r.set(7, 1, 12, "minecraft:soul_campfire", lit="true")
    r.set(17, 1, 12, "minecraft:soul_campfire", lit="true")
    r.save("entrance")


def nest():
    r = Room(2)
    shell(r); cell_lanterns(r)
    c = 12
    for x in range(CELL):
        for z in range(CELL):
            d = math.hypot(x - c, z - c)
            if d <= 4.6:
                r.set(x, 1, z, r.rnd.choice(["minecraft:rooted_dirt", "minecraft:coarse_dirt", "minecraft:mud", "minecraft:packed_mud"]))
            if d <= 2.6:
                r.set(x, 2, z, r.rnd.choice(["minecraft:moss_block", "minecraft:mud", "minecraft:rooted_dirt"]))
            if 4.6 < d <= 6.5 and r.rnd.random() < 0.35:
                r.set(x, 1, z, "minecraft:moss_carpet")
    # Nest block (the mod sets it active or not)
    r.set(c, 3, c, "sanctumcraft:egg_nest", active="false")
    for (x, z) in ((c - 1, c), (c + 1, c), (c, c - 1), (c, c + 1)):
        r.set(x, 3, z, "minecraft:moss_carpet")
    r.save("nest")


def destination():
    r = Room(3)
    shell(r); cell_lanterns(r)
    c = 12
    r.fill(c - 3, 1, c - 3, c + 3, 1, c + 3, "minecraft:polished_deepslate")
    r.fill(c - 2, 2, c - 2, c + 2, 2, c + 2, "minecraft:chiseled_deepslate")
    r.set(c, 3, c, "sanctumcraft:destination_mechanism", active="false")
    for (x, z) in ((c - 2, c - 2), (c + 2, c - 2), (c - 2, c + 2), (c + 2, c + 2)):
        r.set(x, 3, z, "minecraft:crying_obsidian")
        r.set(x, 4, z, "minecraft:end_rod", facing="up")
    for (x, z) in ((c, c - 3), (c, c + 3), (c - 3, c), (c + 3, c)):
        r.set(x, 2, z, "minecraft:amethyst_cluster", facing="up")
    # Exit portal frame (opened by the mechanism)
    exit_frame(r, 20)
    r.fill(9, 0, 19, 15, 0, 21, "minecraft:polished_deepslate")
    r.save("destination")


def hall_pillars():
    r = Room(4)
    shell(r); cell_lanterns(r)
    for (x, z) in ((7, 7), (17, 7), (7, 17), (17, 17)):
        pillar(r, x, z)
    r.save("hall_pillars")


def hall_crystals():
    r = Room(5)
    shell(r); cell_lanterns(r)
    rnd = r.rnd
    for (cx, cz) in ((5, 5), (19, 5), (5, 19), (19, 19), (12, 12)):
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                d = math.hypot(x - cx, z - cz)
                if d <= 1.6:
                    r.set(x, 1, z, "minecraft:amethyst_block")
                    if d < 0.5:
                        r.set(x, 2, z, "minecraft:budding_amethyst")
                        r.set(x, 3, z, "minecraft:amethyst_cluster", facing="up")
                    elif rnd.random() < 0.6:
                        r.set(x, 2, z, rnd.choice(["minecraft:large_amethyst_bud", "minecraft:medium_amethyst_bud"]), facing="up")
                elif d <= 2.3 and rnd.random() < 0.4:
                    r.set(x, 1, z, "minecraft:small_amethyst_bud", facing="up")
    r.fill(12, 5, 12, 12, 7, 12, "minecraft:tinted_glass")
    r.save("hall_crystals")


def hall_ruins():
    r = Room(6)
    shell(r); cell_lanterns(r)
    rnd = r.rnd
    r.fill(7, 1, 16, 8, 3, 17, "minecraft:cracked_deepslate_bricks")
    r.fill(7, 4, 16, 7, 4, 17, "minecraft:cobbled_deepslate")
    for (cx, cz) in ((17, 7), (6, 6), (18, 18)):
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                if rnd.random() < 0.55:
                    r.set(x, 1, z, rnd.choice(["minecraft:cobbled_deepslate", "minecraft:cobbled_deepslate_slab",
                                               "minecraft:deepslate_tile_slab", "minecraft:gravel"]))
    for (x, z) in ((10, 8), (11, 8), (15, 15), (4, 13)):
        r.set(x, 8, z, "minecraft:cobweb")
    for (x, y, z) in ((2, 7, 2), (22, 7, 22), (2, 7, 22), (22, 7, 2), (3, 6, 2)):
        r.set(x, y, z, "minecraft:cobweb")
    r.save("hall_ruins")


# ------------------------------------------------------------------ small rooms (dead ends)

def small_base(seed):
    """13x13 room (6..18) with 4 corridor arms (3 wide, 4 tall) to the cell sides. Height 7."""
    r = Room(seed, sy=7)
    rnd = r.rnd
    def tube_floor(x, z):
        r.set(x, 0, z, "minecraft:deepslate_tiles" if rnd.random() > 0.12 else "minecraft:cracked_deepslate_tiles")
    # room
    for x in range(6, 19):
        for z in range(6, 19):
            tube_floor(x, z)
            r.set(x, 6, z, "minecraft:polished_deepslate")
            if x in (6, 18) or z in (6, 18):
                for y in range(1, 6):
                    r.set(x, y, z, "minecraft:cracked_deepslate_bricks" if rnd.random() < 0.15 else "minecraft:deepslate_bricks")
    # arms
    for i in range(0, 7):
        for (x, z, along_z) in ((None, i, True), (None, 24 - i, True), (i, None, False), (24 - i, None, False)):
            for w in range(10, 15):
                px, pz = (w, z) if along_z else (x, w)
                tube_floor(px, pz)
                r.set(px, 5, pz, "minecraft:polished_deepslate")
                if w in (10, 14):
                    for y in range(1, 5):
                        r.set(px, y, pz, "minecraft:deepslate_bricks")
    # open the room walls toward the arms (x/z 11..13, y 1..4)
    for w in range(11, 14):
        for y in range(1, 5):
            for (x, z) in ((w, 6), (w, 18), (6, w), (18, w)):
                r.clear(x, y, z)
    r.set(12, 5, 12, "minecraft:soul_lantern", hanging="true")
    return r


def small_shrine():
    r = small_base(7)
    r.fill(11, 1, 11, 13, 1, 13, "minecraft:polished_deepslate")
    r.set(12, 2, 12, "minecraft:chiseled_deepslate")
    r.set(12, 3, 12, "minecraft:amethyst_cluster", facing="up")
    for (x, z) in ((8, 8), (16, 8), (8, 16), (16, 16)):
        r.set(x, 1, z, "minecraft:candle", candles="3", lit="true")
    r.save("small_shrine")


def small_storage():
    r = small_base(8)
    rnd = r.rnd
    for (x, z) in ((7, 7), (7, 8), (8, 7), (17, 7), (17, 8), (16, 7), (7, 17), (8, 17), (7, 16), (17, 17)):
        r.set(x, 1, z, rnd.choice(["minecraft:bookshelf", "minecraft:decorated_pot", "minecraft:cobbled_deepslate"]))
    r.fill(17, 2, 17, 17, 2, 17, "minecraft:bookshelf")
    for (x, y, z) in ((7, 5, 7), (17, 5, 17), (7, 5, 17)):
        r.set(x, y, z, "minecraft:cobweb")
    r.save("small_storage")


# ------------------------------------------------------------------ great halls (2x2 cells)

def great_hall_columns():
    r = Room(9, sx=2 * CELL, sy=14, sz=2 * CELL)
    shell(r); cell_lanterns(r)
    for x in (8, 20, 29, 41):
        for z in (8, 20, 29, 41):
            if (x in (20, 29)) and (z in (20, 29)):
                continue
            pillar(r, x, z)
    # Sunken centre with a crystal
    r.fill(21, 0, 21, 28, 0, 28, "minecraft:polished_deepslate")
    r.fill(23, 1, 23, 26, 1, 26, "minecraft:chiseled_deepslate")
    r.fill(24, 2, 24, 25, 3, 25, "minecraft:amethyst_block")
    for (x, z) in ((24, 24), (25, 24), (24, 25), (25, 25)):
        r.set(x, 4, z, "minecraft:amethyst_cluster", facing="up")
    r.save("great_hall_columns")


def great_hall_crypt():
    r = Room(10, sx=2 * CELL, sy=14, sz=2 * CELL)
    shell(r); cell_lanterns(r)
    rnd = r.rnd
    # Rows of sarcophagi
    for x0 in (6, 16, 30, 40):
        for z0 in (6, 17, 30, 40):
            r.fill(x0, 1, z0, x0 + 3, 1, z0 + 1, "minecraft:polished_blackstone_bricks")
            r.fill(x0, 2, z0, x0 + 3, 2, z0 + 1, "minecraft:polished_blackstone_slab")
            if rnd.random() < 0.5:
                r.set(x0 + rnd.randint(0, 3), 1, z0 + 2, "minecraft:cobweb")
    # Raised central platform
    r.fill(20, 1, 20, 29, 1, 29, "minecraft:polished_deepslate")
    r.fill(22, 2, 22, 27, 2, 27, "minecraft:deepslate_tiles")
    for (x, z) in ((22, 22), (27, 22), (22, 27), (27, 27)):
        r.fill(x, 3, z, x, 6, z, "minecraft:polished_deepslate_wall")
        r.set(x, 7, z, "minecraft:soul_lantern")
    r.save("great_hall_crypt")


if __name__ == "__main__":
    entrance(); nest(); destination()
    hall_pillars(); hall_crystals(); hall_ruins()
    small_shrine(); small_storage()
    great_hall_columns(); great_hall_crypt()
