"""Checks the layout of src/main/resources/sanctumcraft/guide.txt like the Minecraft book screen:
114 px wide, 14 lines per page, default font widths (bold = +1 px). Also checks links and page count."""
import re, sys, pathlib

W, LINES, MAX_PAGES = 114, 14, 100
NARROW = {**{c: 2 for c in "!',.:;i|"}, **{c: 3 for c in "`l"}, **{c: 4 for c in " I[]t"},
          **{c: 5 for c in "\"()*<>fk{}"}, **{c: 7 for c in "@~"}}

def width(ch, bold):
    return NARROW.get(ch, 6) + (1 if bold else 0)

def parse(path):
    pages = []
    for raw in pathlib.Path(path).read_text(encoding="utf-8").splitlines():
        if raw.startswith("#"):
            continue
        if raw.startswith("==="):
            pages.append([raw[3:].strip(), []])
        elif pages:
            pages[-1][1].append(raw)
    return pages

def footer(anchor):
    if anchor.startswith("fr_") and anchor != "fr_toc":
        return "[[« Sommaire|fr_toc]]"
    if anchor.startswith("en_") and anchor != "en_toc":
        return "[[« Contents|en_toc]]"
    return None

def visual_lines(line):
    # strip link syntax and § codes, track bold
    text = re.sub(r"\[\[([^|\]]+)\|[^\]]+\]\]", r"\1", line)
    words, cur, n, bold, i = text.split(" "), 0, 1, False, 0
    out = 1
    x = 0
    tokens = re.split(r"( )", text)
    for tok in tokens:
        w = 0
        j = 0
        b = bold
        while j < len(tok):
            if tok[j] == "§" and j + 1 < len(tok):
                c = tok[j + 1]
                if c == "l": b = True
                elif c in "r0123456789abcdef": b = False
                j += 2
                continue
            w += width(tok[j], b)
            j += 1
        if tok == " ":
            x += width(" ", bold)
            continue
        if x + w > W and x > 0:
            out += 1
            x = 0
        while w > W:
            out += 1
            w -= W
        x += w
        bold = b
    return out

pages = parse(sys.argv[1] if len(sys.argv) > 1 else "src/main/resources/sanctumcraft/guide.txt")
anchors = {a for a, _ in pages if a}
ok = True
if len(pages) > MAX_PAGES:
    print(f"too many pages: {len(pages)}"); ok = False
for idx, (anchor, lines) in enumerate(pages, 1):
    while lines and lines[-1] == "":
        lines.pop()
    f = footer(anchor)
    all_lines = lines + ([f] if f else [])
    count = sum(visual_lines(l) if l else 1 for l in all_lines)
    for l in all_lines:
        for target in re.findall(r"\[\[[^|\]]+\|([^\]]+)\]\]", l):
            if target not in anchors:
                print(f"page {idx} ({anchor}): unknown link {target}"); ok = False
    flag = "OVER" if count > LINES else "ok"
    if count > LINES: ok = False
    print(f"{idx:3} {anchor:16} {count:2} lines {flag}")
print("pages:", len(pages), "OK" if ok else "PROBLEMS")
