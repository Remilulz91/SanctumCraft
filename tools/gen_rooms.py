#!/usr/bin/env python3
"""
Generates the placeholder room templates of SanctumCraft (structure .nbt files).

Each room is 25 x 10 x 25 blocks:
  - floor at y=0, ceiling at y=9, walls on the border
  - a 3-wide, 4-tall doorway in the middle of each side (x/z 11..13, y 1..4);
    the mod seals the doorways that don't lead anywhere.
Air is not stored: rooms are placed in an empty void.

Usage:  pip install nbtlib  &&  python tools/gen_rooms.py
Output: src/main/resources/data/sanctumcraft/structure/room/*.nbt

These rooms are meant to be retouched in game: load one with a structure block
(name "sanctumcraft:room/hall_pillars"), edit it, save it with the same name/size,
and copy the file from the world's generated/ folder back into the mod.
"""
import os, random, math
from nbtlib import File, Compound, List, Int, String

DATA_VERSION = 3955  # Minecraft 1.21.1
W, H = 25, 10
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "data", "sanctumcraft", "structure", "room")


class Room:
    def __init__(self, seed):
        self.blocks = {}
        self.rnd = random.Random(seed)

    def set(self, x, y, z, name, **props):
        if 0 <= x < W and 0 <= y < H and 0 <= z < W:
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
            "size": List[Int]([Int(W), Int(H), Int(W)]),
            "palette": List[Compound](palette),
            "blocks": List[Compound](blocks),
            "entities": List[Compound]([]),
        })
        os.makedirs(OUT, exist_ok=True)
        path = os.path.join(OUT, name + ".nbt")
        File(root).save(path, gzipped=True)
        print(f"{path}: {len(blocks)} blocks, {len(palette)} states")


def is_door(x, y, z):
    on_ns = (z == 0 or z == W - 1) and 11 <= x <= 13
    on_we = (x == 0 or x == W - 1) and 11 <= z <= 13
    return (on_ns or on_we) and 1 <= y <= 4


def shell(r):
    """Floor, walls, ceiling, doorway frames and lighting shared by every room."""
    rnd = r.rnd
    for x in range(W):
        for z in range(W):
            border = x in (1, W - 2) or z in (1, W - 2)
            if border:
                r.set(x, 0, z, "minecraft:polished_deepslate")
            elif (x + z) % 6 == 0 and (x - z) % 6 == 0:
                r.set(x, 0, z, "minecraft:chiseled_deepslate")
            else:
                r.set(x, 0, z, "minecraft:cracked_deepslate_tiles" if rnd.random() < 0.12 else "minecraft:deepslate_tiles")
            r.set(x, H - 1, z, "minecraft:deepslate_tiles")
    for y in range(1, H - 1):
        for i in range(W):
            for (x, z) in ((i, 0), (i, W - 1), (0, i), (W - 1, i)):
                if is_door(x, y, z):
                    continue
                if y == 1:
                    name = "minecraft:polished_deepslate"
                elif y == H - 2:
                    name = "minecraft:chiseled_deepslate"
                else:
                    name = "minecraft:cracked_deepslate_bricks" if rnd.random() < 0.15 else "minecraft:deepslate_bricks"
                r.set(x, y, z, name)
    # Rune stone frames around doorways
    for i in (10, 14):
        for y in range(1, 6):
            for (x, z) in ((i, 0), (i, W - 1), (0, i), (W - 1, i)):
                r.set(x, y, z, "sanctumcraft:rune_stone")
    for i in range(10, 15):
        for (x, z) in ((i, 0), (i, W - 1), (0, i), (W - 1, i)):
            r.set(x, 5, z, "sanctumcraft:rune_stone")
    # Corner pillars
    for (cx, cz) in ((1, 1), (1, W - 2), (W - 2, 1), (W - 2, W - 2)):
        r.fill(cx, 1, cz, cx, H - 2, cz, "minecraft:polished_deepslate")
    # Hanging soul lanterns
    for (x, z) in ((6, 6), (18, 6), (6, 18), (18, 18)):
        r.set(x, H - 2, z, "minecraft:chain", axis="y")
        r.set(x, H - 3, z, "minecraft:soul_lantern", hanging="true")


def pillar(r, cx, cz, half=1):
    r.fill(cx - half, 1, cz - half, cx + half, H - 2, cz + half, "minecraft:polished_deepslate")
    r.fill(cx - half, 1, cz - half, cx + half, 1, cz + half, "minecraft:chiseled_deepslate")
    r.fill(cx - half, H - 2, cz - half, cx + half, H - 2, cz + half, "minecraft:chiseled_deepslate")
    for dx, dz, f in ((-half - 1, 0, "west"), (half + 1, 0, "east"), (0, -half - 1, "north"), (0, half + 1, "south")):
        r.set(cx + dx, 3, cz + dz, "minecraft:amethyst_cluster", facing=f)


def entrance():
    r = Room(1)
    shell(r)
    # Exit portal on the north side, facing the room.
    r.fill(10, 1, 3, 14, 6, 3, "sanctumcraft:rune_stone")
    r.fill(11, 2, 3, 13, 5, 3, "sanctumcraft:sanctum_portal", axis="x")
    r.fill(9, 0, 2, 15, 0, 5, "minecraft:polished_deepslate")
    for x in (9, 15):
        r.set(x, 1, 3, "minecraft:soul_lantern")
    # Spawn spot marker in the floor (12, 0, 9)
    r.set(12, 0, 9, "minecraft:chiseled_deepslate")
    # Two braziers
    r.set(7, 1, 12, "minecraft:soul_campfire", lit="true")
    r.set(17, 1, 12, "minecraft:soul_campfire", lit="true")
    r.save("entrance")


def nest():
    r = Room(2)
    shell(r)
    c = 12
    for x in range(W):
        for z in range(W):
            d = math.hypot(x - c, z - c)
            if d <= 4.6:
                r.set(x, 1, z, r.rnd.choice(["minecraft:rooted_dirt", "minecraft:coarse_dirt", "minecraft:mud", "minecraft:packed_mud"]))
            if d <= 2.6:
                r.set(x, 2, z, r.rnd.choice(["minecraft:moss_block", "minecraft:mud", "minecraft:rooted_dirt"]))
            if 4.6 < d <= 6.5 and r.rnd.random() < 0.35:
                r.set(x, 1, z, "minecraft:moss_carpet")
    # Placeholder egg on top of the nest (real egg gameplay comes in 0.2)
    r.set(c, 3, c, "minecraft:sniffer_egg")
    for (x, z) in ((c - 1, c), (c + 1, c), (c, c - 1), (c, c + 1)):
        r.set(x, 3, z, "minecraft:moss_carpet")
    r.save("nest")


def destination():
    r = Room(3)
    shell(r)
    c = 12
    r.fill(c - 3, 1, c - 3, c + 3, 1, c + 3, "minecraft:polished_deepslate")
    r.fill(c - 2, 2, c - 2, c + 2, 2, c + 2, "minecraft:chiseled_deepslate")
    r.set(c, 3, c, "minecraft:lodestone")
    for (x, z) in ((c - 2, c - 2), (c + 2, c - 2), (c - 2, c + 2), (c + 2, c + 2)):
        r.set(x, 3, z, "minecraft:crying_obsidian")
        r.set(x, 4, z, "minecraft:end_rod", facing="up")
    for (x, z, f) in ((c, c - 3, "north"), (c, c + 3, "south"), (c - 3, c, "west"), (c + 3, c, "east")):
        r.set(x, 2, z, "minecraft:amethyst_cluster", facing="up")
    r.save("destination")


def hall_pillars():
    r = Room(4)
    shell(r)
    for (x, z) in ((7, 7), (17, 7), (7, 17), (17, 17)):
        pillar(r, x, z)
    r.save("hall_pillars")


def hall_crystals():
    r = Room(5)
    shell(r)
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
    shell(r)
    rnd = r.rnd
    # Broken pillar
    r.fill(7, 1, 16, 8, 3, 17, "minecraft:cracked_deepslate_bricks")
    r.fill(7, 4, 16, 7, 4, 17, "minecraft:cobbled_deepslate")
    # Rubble piles
    for (cx, cz) in ((17, 7), (6, 6), (18, 18)):
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                if rnd.random() < 0.55:
                    r.set(x, 1, z, rnd.choice(["minecraft:cobbled_deepslate", "minecraft:cobbled_deepslate_slab",
                                               "minecraft:deepslate_tile_slab", "minecraft:gravel"]))
    # Fallen ceiling: a few holes and cobwebs
    for (x, z) in ((10, 8), (11, 8), (15, 15), (4, 13)):
        r.set(x, H - 2, z, "minecraft:cobweb")
    for (x, y, z) in ((2, 7, 2), (22, 7, 22), (2, 7, 22), (22, 7, 2), (3, 6, 2)):
        r.set(x, y, z, "minecraft:cobweb")
    r.save("hall_ruins")


if __name__ == "__main__":
    entrance()
    nest()
    destination()
    hall_pillars()
    hall_crystals()
    hall_ruins()
