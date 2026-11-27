"""Generates the Patchouli book "sanctumcraft:guide" (French + English).
Run from the project root: python tools/gen_guide.py
Writes data/sanctumcraft/patchouli_books/guide/book.json and
assets/sanctumcraft/patchouli_books/guide/<lang>/{categories,entries}/..."""
import json, pathlib, shutil

ROOT = pathlib.Path("src/main/resources")
BOOK = "guide"
NS = "sanctumcraft"

book = {
    "name": "book.sanctumcraft.guide.name",
    "landing_text": "book.sanctumcraft.guide.landing",
    "subtitle": "book.sanctumcraft.guide.subtitle",
    "use_resource_pack": True,
    "i18n": True,
    "book_texture": "patchouli:textures/gui/book_purple.png",
    "model": "sanctumcraft:sanctum_guide",
    "creative_tab": "sanctumcraft:main",
    "show_progress": False,
    "show_toasts": False,
    "text_overflow_mode": "resize",
    "nameplate_color": "D9A3FF",
    "link_color": "6A1B9A",
    "link_hover_color": "B23CFF",
    "macros": {
        "$(item)": "$(#6a1b9a)",
        "$(thing)": "$(#8a5a00)",
        "$(bad)": "$(#b00020)",
        "$(good)": "$(#1b7a1b)",
    },
}

# category id -> (icon, sortnum, {lang: (name, description)})
CATEGORIES = {
    "basics": ("sanctumcraft:rune_stone", 0, {
        "fr_fr": ("Démarrer", "Le principe du mod, le portail runique et ses recettes."),
        "en_us": ("Getting started", "What the mod is about, the rune portal and its recipes.")}),
    "raid": ("sanctumcraft:sanctum_egg_rare", 1, {
        "fr_fr": ("Le raid", "Ce qui se passe dans un Sanctum : chrono, difficultés, œufs, monstres."),
        "en_us": ("The raid", "What happens inside a Sanctum: timer, difficulties, eggs, monsters.")}),
    "loot": ("minecraft:chest", 2, {
        "fr_fr": ("Butin", "Coffres, clés, salles fortes et reliques."),
        "en_us": ("Loot", "Chests, keys, vaults and relics.")}),
    "progress": ("sanctumcraft:sanctum_altar", 3, {
        "fr_fr": ("Progression", "L'Autel du Sanctum, les niveaux de chasseur et les talents."),
        "en_us": ("Progression", "The Sanctum Altar, Hunter levels and talents.")}),
    "companions": ("sanctumcraft:incubator", 4, {
        "fr_fr": ("Compagnons", "Faire éclore tes œufs et partir à l'aventure avec tes compagnons."),
        "en_us": ("Companions", "Hatch your eggs and adventure with your companions.")}),
    "extras": ("sanctumcraft:sanctum_shard", 5, {
        "fr_fr": ("Interface et commandes", "Œuf roulant, HUD, touches, commandes et options."),
        "en_us": ("Interface and commands", "Rolling egg, HUD, keys, commands and options.")}),
}

PORTAL_MB = {
    "mapping": {"R": "sanctumcraft:rune_stone", "0": "sanctumcraft:rune_stone",
                "P": "sanctumcraft:sanctum_portal[axis=x]"},
    "pattern": [["RRRR"], ["RPPR"], ["RPPR"], ["RPPR"], ["RR0R"]],
    "symmetrical": True,
}

def t(fr, en):
    return {"fr_fr": fr, "en_us": en}

def text(fr, en, title=None):
    p = {"type": "patchouli:text", "text": t(fr, en)}
    if title: p["title"] = title
    return p

def spot(item, fr, en, title=None):
    p = {"type": "patchouli:spotlight", "item": item, "text": t(fr, en)}
    if title: p["title"] = title
    return p

def craft(recipe, fr, en):
    return {"type": "patchouli:crafting", "recipe": recipe, "text": t(fr, en)}

# entry id -> (category, icon, sortnum, names, pages)
ENTRIES = {
"basics/welcome": ("basics", "sanctumcraft:sanctum_egg_legendary", 0, t("Le principe", "The idea"), [
    text("Construis un $(l:basics/portal)portail runique$() et entre dans un $(item)Sanctum$() : un donjon flottant dans le vide, généré rien que pour toi et jusqu'à 2 amis.$(br2)Ton but : déterrer un $(l:raid/eggs)œuf rare$(), piller les coffres et ressortir vivant.",
         "Build a $(l:basics/portal)rune portal$() and step into a $(item)Sanctum$(): a dungeon floating in the void, generated just for you and up to 2 friends.$(br2)Your goal: dig up a $(l:raid/eggs)rare egg$(), loot the chests and get back out alive."),
    text("Le Sanctum a un $(l:raid/raid)chrono$(). Quand il tombe à zéro, le Sanctum s'effondre : $(bad)tous ceux qui sont encore dedans meurent$() et perdent ce qu'ils portaient.$(br2)Gère ton temps, et pense à rentrer.",
         "The Sanctum has a $(l:raid/raid)timer$(). When it hits zero, the Sanctum collapses: $(bad)everyone still inside dies$() and loses what they carried.$(br2)Manage your time, and remember to head back.", "Attention ! / Watch out!"),
]),
"basics/portal": ("basics", "sanctumcraft:sanctum_shard", 1, t("Le portail runique", "The rune portal"), [
    text("Fais un cadre en $(l:basics/recipes)Pierres runiques$() comme un portail du Nether : intérieur de $(thing)2×3$() à $(thing)21×21$(), les coins sont facultatifs.$(br2)Fais un clic droit sur le cadre avec un $(item)Éclat de Sanctum$() (il est consommé) pour l'allumer.",
         "Build a $(l:basics/recipes)Rune Stone$() frame like a Nether portal: inside from $(thing)2×3$() to $(thing)21×21$(), corners are optional.$(br2)Right-click the frame with a $(item)Sanctum Shard$() (used up) to light it."),
    {"type": "patchouli:multiblock", "name": t("Portail minimal", "Smallest portal"), "multiblock": PORTAL_MB,
     "text": t("Un cadre de 4×5 avec un intérieur de 2×3.", "A 4×5 frame with a 2×3 inside.")},
    text("Entre dans le portail et choisis la $(l:raid/difficulties)difficulté$() : le Sanctum se génère pendant quelques secondes.$(br2)Après l'effondrement, ce portail doit se recharger ($(thing)30 min$()). Chaque portail est indépendant : construis-en un autre pour repartir tout de suite.$(br2)Des ruines de portail existent aussi dans le monde.",
         "Step into the portal and pick a $(l:raid/difficulties)difficulty$(): the Sanctum is generated over a few seconds.$(br2)After the collapse, that portal has to recharge ($(thing)30 min$()). Every portal is independent: build another one to go again right away.$(br2)Portal ruins also exist in the world.", "Recharge"),
]),
"basics/recipes": ("basics", "sanctumcraft:rune_stone", 2, t("Recettes du portail", "Portal recipes"), [
    craft("sanctumcraft:rune_stone", "Pour le cadre du portail, l'Autel et l'incubateur.", "For the portal frame, the Altar and the incubator."),
    craft("sanctumcraft:sanctum_shard", "Allume le portail. Aussi vendu à l'$(l:progress/altar)Autel$().", "Lights the portal. Also sold at the $(l:progress/altar)Altar$()."),
]),
"raid/raid": ("raid", "minecraft:clock", 0, t("Déroulement du raid", "How a raid works"), [
    text("Chrono : $(thing)30 min$() en Normal, $(thing)25$() en Cauchemar, $(thing)20$() en Chaos. Il s'affiche en haut de l'écran et dans le $(l:extras/hud)HUD$().$(br2)Pour sortir : repasse par le $(item)portail d'arrivée$(), ou active le $(item)mécanisme de destination$() (reste 5 s à côté) pour ouvrir une sortie plus loin.",
         "Timer: $(thing)30 min$() in Normal, $(thing)25$() in Nightmare, $(thing)20$() in Chaos. It shows at the top of the screen and in the $(l:extras/hud)HUD$().$(br2)To get out: go back through the $(item)arrival portal$(), or activate the $(item)destination mechanism$() (stay 5 s next to it) to open an exit deeper in."),
    text("Reste à côté 5 secondes pour ouvrir le portail de sortie de la salle de destination.",
         "Stay next to it for 5 seconds to open the destination room's exit portal.", "Destination"),
    text("Dans le Sanctum, le mode $(thing)Survie$() est forcé. Tu peux $(good)poser des blocs$() mais pas en casser.$(br2)$(bad)Interdits$() : seaux, pistons, TNT, lits, coffres de l'Ender, outils sur les blocs du donjon.$(br2)Personne ne peut entrer pendant la dernière minute.",
         "Inside the Sanctum, $(thing)Survival$() is forced. You can $(good)place blocks$() but not break them.$(br2)$(bad)Forbidden$(): buckets, pistons, TNT, beds, ender chests, tools on dungeon blocks.$(br2)Nobody can enter during the last minute.", "Règles / Rules"),
]),
"raid/difficulties": ("raid", "sanctumcraft:chaos_ticket", 1, t("Difficultés", "Difficulties"), [
    text("$(good)Normal$() : le Sanctum tel quel.$(br2)$(#c62828)Cauchemar$() : monstres plus forts, butin $(thing)×1,5$(), meilleurs œufs, chrono plus court.$(br2)$(#6a1b9a)Chaos$() : très dur, butin $(thing)×2$(), chrono le plus court.",
         "$(good)Normal$(): the Sanctum as it is.$(br2)$(#c62828)Nightmare$(): stronger monsters, loot $(thing)×1.5$(), better eggs, shorter timer.$(br2)$(#6a1b9a)Chaos$(): very hard, loot $(thing)×2$(), shortest timer."),
    spot("sanctumcraft:chaos_ticket", "Consommé pour entrer en Chaos. Tu en gagnes un à chaque $(l:progress/levels)niveau de chasseur$(), ou à l'$(l:progress/altar)Autel$() (le prix monte à chaque achat et revient à la normale chaque jour).",
         "Used up to enter Chaos. You get one each $(l:progress/levels)Hunter level$(), or at the $(l:progress/altar)Altar$() (the price rises with each purchase and resets every day)."),
]),
"raid/eggs": ("raid", "sanctumcraft:sanctum_egg_epic", 2, t("Les œufs", "Eggs"), [
    text("Le Sanctum cache $(thing)1 à 3 nids actifs$() parmi 5 salles de nid. Frappe un nid ($(thing)10 coups$()) pour déterrer son œuf.$(br2)Quatre raretés : commun, rare, épique et légendaire.",
         "The Sanctum hides $(thing)1 to 3 active nests$() among 5 nest rooms. Hit a nest ($(thing)10 hits$()) to dig up its egg.$(br2)Four rarities: common, rare, epic and legendary."),
    spot("sanctumcraft:sanctum_egg_legendary", "Un œuf est $(bad)lourd$() : il te ralentit, encore plus s'il est épique ou légendaire. Si tu meurs, tu le lâches.",
         "An egg is $(bad)heavy$(): it slows you down, even more if it's epic or legendary. If you die, you drop it."),
    text("Ressors vivant avec l'œuf : il devient $(good)sécurisé$(), à ton nom, et te rapporte de l'XP de chasseur et des Coquilles.$(br2)Les œufs ne se vendent pas : fais-les éclore dans un $(l:companions/incubator)incubateur$().",
         "Get out alive with the egg: it becomes $(good)secured$(), in your name, and gives you Hunter XP and Shells.$(br2)Eggs can't be sold: hatch them in an $(l:companions/incubator)incubator$().", "Extraction"),
]),
"raid/monsters": ("raid", "minecraft:iron_sword", 3, t("Monstres et pièges", "Monsters and traps"), [
    text("Des gardiens protègent les salles. Des renforts arrivent pendant le raid : en Cauchemar et Chaos dès $(thing)30 %$() du temps, puis à $(thing)5 min$() et $(thing)2 min$() de la fin, avec des dégâts $(bad)+25 %$() puis $(bad)+50 %$().",
         "Guardians protect the rooms. Reinforcements arrive during the raid: in Nightmare and Chaos from $(thing)30%$() of the time, then $(thing)5 min$() and $(thing)2 min$() before the end, with damage $(bad)+25%$() then $(bad)+50%$()."),
    text("Une faille libère des vagues de monstres. Referme-la : un coffre apparaît.",
         "A rift releases waves of monsters. Close it: a chest appears.", "Failles / Rifts"),
    text("Une plaque au sol dans un couloir ? C'est sans doute un $(bad)piège à flèches$(). Les flèches des pièges ne se ramassent pas.",
         "A plate on a corridor floor? It's probably an $(bad)arrow trap$(). Trap arrows can't be picked up.", "Pièges / Traps"),
]),
"loot/chests": ("loot", "minecraft:chest", 0, t("Coffres", "Chests"), [
    text("La couleur d'un coffre donne sa rareté : $(#777777)basique$(), $(#1b7a1b)raffiné$(), $(#1565c0)rare$(), $(#6a1b9a)épique$(), $(#b8860b)légendaire$().$(br2)La fouille prend du temps : les objets apparaissent un à un, d'autant plus lentement qu'ils sont précieux.",
         "A chest's color is its rarity: $(#777777)basic$(), $(#1b7a1b)fine$(), $(#1565c0)rare$(), $(#6a1b9a)epic$(), $(#b8860b)legendary$().$(br2)Searching takes time: items appear one by one, slower when they're valuable."),
    text("Un coffre peut contenir une $(l:loot/keys)clé$() ou une $(l:loot/relics)relique$().",
         "A chest may hold a $(l:loot/keys)key$() or a $(l:loot/relics)relic$().", "Contenu / Contents"),
]),
"loot/keys": ("loot", "sanctumcraft:gilded_key", 1, t("Clés et salles fortes", "Keys and vaults"), [
    text("Chaque Sanctum a des $(item)salles fortes$() fermées par une porte verrouillée. La clé dépend de la difficulté du raid :$(li)$(#8b2500)rouillée$() en Normal$(li)$(#555555)argent$() en Cauchemar$(li)$(#b8860b)dorée$() en Chaos$(br2)Clic droit sur la porte avec la bonne clé : derrière, de meilleurs coffres.",
         "Every Sanctum has $(item)vaults$() behind a locked door. The key depends on the raid's difficulty:$(li)$(#8b2500)rusted$() in Normal$(li)$(#555555)silver$() in Nightmare$(li)$(#b8860b)gilded$() in Chaos$(br2)Right-click the door with the right key: better chests wait behind it."),
    spot("sanctumcraft:rusted_key", "Trouvée dans les coffres, ou achetée à l'$(l:progress/altar)Autel$().", "Found in chests, or bought at the $(l:progress/altar)Altar$()."),
]),
"loot/relics": ("loot", "sanctumcraft:relic_sanctum_crown", 2, t("Reliques et vitrine", "Relics and showcase"), [
    text("$(thing)12 reliques$() en 3 collections de 4 : Ruines, Profondeurs et Nid. Elles tombent de tous les coffres, plus souvent dans les coffres rares. Rarement, c'est la version $(#b8860b)collector$().",
         "$(thing)12 relics$() in 3 collections of 4: Ruins, Depths and Nest. They drop from every chest, more often from rare chests. Rarely, you get the $(#b8860b)collector$() version."),
    spot("sanctumcraft:relic_golden_nest", "Expose une relique classique dans ta vitrine à l'$(l:progress/altar)Autel$() : $(good)+1 %$() de Coquilles. Sublime-la ensuite avec la collector : $(good)+3 %$().",
         "Display a classic relic in your showcase at the $(l:progress/altar)Altar$(): $(good)+1%$() Shells. Then upgrade it with the collector: $(good)+3%$()."),
    text("Une rangée complète de la vitrine donne un bonus en plus. L'exposition est définitive : la relique ne ressort plus.$(br2)Les reliques classiques se vendent, pas les collector.",
         "A complete showcase row gives an extra bonus. Displaying is permanent: the relic doesn't come back out.$(br2)Classic relics can be sold, collector ones can't.", "Vitrine / Showcase"),
]),
"progress/altar": ("progress", "sanctumcraft:sanctum_altar", 0, t("L'Autel du Sanctum", "The Sanctum Altar"), [
    text("Clic droit sur l'Autel :$(li)$(item)Boutique$() : clés, éclats, pierres runiques, Ticket Chaos...$(li)$(item)Vente$() : ton butin contre des Coquilles$(li)$(item)Profil$(), $(l:progress/talents)talents$(), $(l:loot/relics)vitrine$() et $(l:companions/companions)compagnons$()",
         "Right-click the Altar:$(li)$(item)Shop$(): keys, shards, rune stones, Chaos Ticket...$(li)$(item)Sell$(): your loot for Shells$(li)$(item)Profile$(), $(l:progress/talents)talents$(), $(l:loot/relics)showcase$() and $(l:companions/companions)companions$()"),
    craft("sanctumcraft:sanctum_altar", "Ton profil te suit d'un Autel à l'autre.", "Your profile follows you from one Altar to another."),
]),
"progress/levels": ("progress", "minecraft:experience_bottle", 1, t("Niveaux de chasseur", "Hunter levels"), [
    text("Extraire des œufs te donne de l'$(item)XP de chasseur$(). À chaque niveau, tu gagnes :$(li)un $(l:raid/difficulties)Ticket Chaos$()$(li)des Coquilles$(li)$(thing)1 point$() de $(l:progress/talents)talent$()$(br2)$(thing)/sanctum profile$() affiche ton niveau et tes statistiques.",
         "Extracting eggs gives you $(item)Hunter XP$(). Each level gives you:$(li)a $(l:raid/difficulties)Chaos Ticket$()$(li)Shells$(li)$(thing)1$() $(l:progress/talents)talent$() point$(br2)$(thing)/sanctum profile$() shows your level and stats."),
]),
"progress/talents": ("progress", "minecraft:enchanted_book", 2, t("Talents", "Talents"), [
    text("Trois branches, chaque talent jusqu'au $(thing)rang 3$() :$(li)$(#c62828)Survie$() : vie, armure, vitesse, second souffle$(li)$(good)Exploration$() : fouille, nids, poids des œufs, vision$(li)$(#b8860b)Butin$() : Coquilles, clés, trésors, collector$(br2)Les points se dépensent à l'$(l:progress/altar)Autel$().",
         "Three branches, each talent up to $(thing)rank 3$():$(li)$(#c62828)Survival$(): health, armor, speed, second wind$(li)$(good)Exploration$(): searching, nests, egg weight, vision$(li)$(#b8860b)Loot$(): Shells, keys, treasure, collector$(br2)Points are spent at the $(l:progress/altar)Altar$()."),
]),
"companions/incubator": ("companions", "sanctumcraft:incubator", 0, t("L'incubateur", "The incubator"), [
    text("Pose un œuf $(good)sécurisé$() dans l'incubateur. Durées : $(thing)10 min$() (commun), $(thing)20$() (rare), $(thing)40$() (épique), $(thing)60$() (légendaire).$(br2)Quand il est prêt, un clic le fait éclore. Un ami peut couver ton œuf : le compagnon reste à toi.$(br2)Les œufs non sécurisés sont refusés.",
         "Put a $(good)secured$() egg in the incubator. Times: $(thing)10 min$() (common), $(thing)20$() (rare), $(thing)40$() (epic), $(thing)60$() (legendary).$(br2)When it's ready, one click hatches it. A friend can hatch your egg: the companion stays yours.$(br2)Unsecured eggs are refused."),
    craft("sanctumcraft:incubator", "Fonctionne n'importe où.", "Works anywhere."),
]),
"companions/companions": ("companions", "minecraft:bone", 1, t("Compagnons", "Companions"), [
    text("Chaque éclosion ajoute un compagnon à ta collection ($(thing)10 places$()) : loup, renard, allay, ou golem de fer pour les œufs épiques et légendaires.$(br2)Chacun a des stats aléatoires et $(thing)1 à 3 traits$() : féroce, robuste, vif, guérisseur, pisteur, gardien.",
         "Each hatch adds a companion to your collection ($(thing)10 slots$()): wolf, fox, allay, or iron golem for epic and legendary eggs.$(br2)Each one has random stats and $(thing)1 to 3 traits$(): ferocious, sturdy, swift, healer, tracker, guardian."),
    {"type": "patchouli:entity", "entity": "minecraft:wolf", "name": t("Ton compagnon", "Your companion"),
     "text": t("Choisis le compagnon actif ou libère-en un à l'$(l:progress/altar)Autel$().", "Pick the active companion or release one at the $(l:progress/altar)Altar$().")},
    text("Ton compagnon te suit partout, même à travers les portails. Il se soigne quand il n'est plus en combat.$(br2)$(bad)K.O.$() ? Il se repose quelques minutes avant de pouvoir revenir.",
         "Your companion follows you everywhere, even through portals. It heals when out of combat.$(br2)$(bad)Knocked out$()? It rests a few minutes before it can come back.", "Au combat / In combat"),
]),
"companions/whistle": ("companions", "sanctumcraft:companion_whistle", 2, t("Le sifflet", "The whistle"), [
    spot("sanctumcraft:companion_whistle", "Clic droit : appelle ou rappelle ton compagnon actif. Ton premier compagnon t'en donne un.",
         "Right-click: call or recall your active companion. Your first companion gives you one."),
    craft("sanctumcraft:companion_whistle", "Sans forme : lingot d'or, éclat d'améthyste et ficelle.", "Shapeless: gold ingot, amethyst shard and string."),
]),
"extras/rolling": ("extras", "sanctumcraft:sanctum_egg_common", 0, t("Œuf roulant", "Rolling egg"), [
    text("Dans un Sanctum, appuie sur $(k:sanctumcraft.roll)$() : tu te roules en œuf et tu vas $(good)plus vite$(). Accroupis-toi pour un $(item)dash$() (recharge de 3 s).$(br2)En roulant, $(bad)impossible$() d'attaquer, creuser, ouvrir ou utiliser quoi que ce soit. Un coup te relève. Pas possible en portant un œuf.",
         "Inside a Sanctum, press $(k:sanctumcraft.roll)$(): you curl up into an egg and go $(good)faster$(). Sneak to $(item)dash$() (3 s cooldown).$(br2)While rolling, you $(bad)can't$() attack, dig, open or use anything. A hit stands you up. Not possible while carrying an egg."),
]),
"extras/hud": ("extras", "minecraft:compass", 1, t("Le HUD", "The HUD"), [
    text("En haut à gauche dans le Sanctum : difficulté et chrono, prochains renforts, œufs portés, équipe, compagnon, Coquilles et niveau.$(br2)$(k:sanctumcraft.hud)$() l'affiche ou le masque.$(br2)Toutes les options se règlent dans $(thing)Mod Menu$() si $(thing)Cloth Config$() est installé.",
         "Top left inside the Sanctum: difficulty and timer, next reinforcements, eggs carried, team, companion, Shells and level.$(br2)$(k:sanctumcraft.hud)$() shows or hides it.$(br2)Every option can be set in $(thing)Mod Menu$() when $(thing)Cloth Config$() is installed."),
]),
"extras/commands": ("extras", "minecraft:command_block", 2, t("Commandes", "Commands"), [
    text("$(thing)/sanctum status$() : temps restant$(br)$(thing)/sanctum profile$() : ton profil$(br)$(thing)/sanctum guide$() : un nouveau guide si tu as perdu celui-ci$(br2)Pour les opérateurs :$(br)$(thing)/sanctum list$() : Sanctums ouverts$(br)$(thing)/sanctum egg <rareté>$() : un œuf sécurisé",
         "$(thing)/sanctum status$(): time left$(br)$(thing)/sanctum profile$(): your profile$(br)$(thing)/sanctum guide$(): a new guide if you lost this one$(br2)For operators:$(br)$(thing)/sanctum list$(): open Sanctums$(br)$(thing)/sanctum egg <rarity>$(): a secured egg"),
    text("$(li)Garde un œil sur le chrono.$(li)Un portail en recharge ? Construis-en un autre.$(li)Sors avec l'œuf avant les derniers renforts.$(li)Passe à l'Autel après chaque raid.$(br2)$(#6a1b9a)Bonne chasse !$()",
         "$(li)Keep an eye on the timer.$(li)Portal recharging? Build another one.$(li)Get out with the egg before the last reinforcements.$(li)Visit the Altar after each raid.$(br2)$(#6a1b9a)Happy hunting!$()", "Astuces / Tips"),
]),
}

def pick(v, lang):
    if isinstance(v, dict) and set(v) == {"fr_fr", "en_us"}:
        return v[lang]
    if isinstance(v, dict):
        return {k: pick(x, lang) for k, x in v.items()}
    if isinstance(v, list):
        return [pick(x, lang) for x in v]
    if isinstance(v, str) and " / " in v and lang in ("fr_fr", "en_us") and len(v) < 40:
        a, b = v.split(" / ", 1)
        return a if lang == "fr_fr" else b
    return v

data_dir = ROOT / "data" / NS / "patchouli_books" / BOOK
data_dir.mkdir(parents=True, exist_ok=True)
(data_dir / "book.json").write_text(json.dumps(book, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

assets = ROOT / "assets" / NS / "patchouli_books" / BOOK
if assets.exists():
    shutil.rmtree(assets)
for lang in ("fr_fr", "en_us"):
    for cid, (icon, sort, names) in CATEGORIES.items():
        f = assets / lang / "categories" / f"{cid}.json"
        f.parent.mkdir(parents=True, exist_ok=True)
        name, desc = names[lang]
        f.write_text(json.dumps({"name": name, "description": desc, "icon": icon, "sortnum": sort},
                                ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    for eid, (cat, icon, sort, names, pages) in ENTRIES.items():
        f = assets / lang / "entries" / f"{eid}.json"
        f.parent.mkdir(parents=True, exist_ok=True)
        entry = {"name": names[lang], "icon": icon, "category": f"{NS}:{cat}", "sortnum": sort,
                 "pages": pick(pages, lang)}
        f.write_text(json.dumps(entry, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("categories:", len(CATEGORIES), "entries:", len(ENTRIES))
