# OpenWCIII

**A faithful open-source Warcraft III — with quality-of-life upgrades.**

OpenWCIII is a community fork of [Warsmash](https://github.com/Retera/WarsmashModEngine):
a free reimplementation of the Warcraft III engine. The mission is simple:

1. **Faithful first** — classic RoC / TFT gameplay, campaigns, maps, and mods
   should behave like retail Warcraft III wherever the engine claims support.
2. **Open forever** — engine code you can study, fix, and extend; no black box.
3. **QoL on top** — launcher profiles, diagnostics, modern OS/GPU support, and
   ergonomics that do not rewrite the game’s identity.

OpenWCIII ships **engine code only**. You must own Warcraft III and point the
engine at your assets via `warsmash.ini`.

---

## Why this exists

Warcraft III defined a generation of RTS and custom-map culture. Official
clients and patches have splintered that ecosystem. OpenWCIII aims to keep
classic WC3 playable and preservable: an open engine under community control,
compatible with owned game data, and improved where fidelity is not at risk.

Upstream Warsmash framed this as “OpenMW for Warcraft III.” OpenWCIII keeps
that compass and sharpens the product goal: **faithful WC3 first, then QoL**.

---

## Quick Start

### 1) Clone and test

```bash
git clone https://github.com/awest813/OpenWCIII.git
cd OpenWCIII
./gradlew :core:test
```

### 2) Configure assets

Edit `core/assets/warsmash.ini` and set `[DataSources]` to your Warcraft III
installation. Patch-specific notes are in [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)
and the INI section below.

### 3) Launch

```bash
./gradlew :desktop:runGame
```

Useful launcher flags:

- `-help` — show all options and exit
- `-profile safe|balanced|high` — preset launch profile
- `-window [width height]` — force windowed mode
- `-fps <value>` — cap FPS (`0` uncapped)
- `-vsync` / `-novsync`
- `-msaa <samples>` (including `-msaa 0` to disable)
- `-validate` — validate `warsmash.ini` data-source paths and exit
- `-ini <path>` — use a custom config
- `-loadfile <path>` — auto-load map or TOC
- `-nolog` — keep logs on console

---

## Mission pillars

| Pillar | Meaning |
|--------|---------|
| **Fidelity** | Prefer retail WC3 behavior for maps, campaigns, JASS, and UI flows we claim to support. |
| **Preservation** | Keep classic patches (especially 1.22–1.29 and well-tested 1.32.10) running on modern hardware. |
| **QoL** | Better launch/debug UX, stability, and docs — without turning WC3 into a different game. |
| **Openness** | Readable engine, documented tradeoffs, contributions welcome. |

Detailed product/engineering roadmap: [docs/MISSION.md](docs/MISSION.md),
[docs/ENGINE_MODERNIZATION_ANALYSIS.md](docs/ENGINE_MODERNIZATION_ANALYSIS.md).

Campaign parity tracking (RoC + TFT): work continues toward full single-player
campaign support; see `CHANGELOG.md` and open PRs for current spine progress.

---

## Project Status & Roadmap

| Phase | Focus | Status |
|-------|-------|--------|
| **A** | Diagnostics, launcher QoL, CI, docs | **Complete** |
| **B** | Light-system leak fix, GLSL normalization, parser consolidation design | **Complete** |
| **C** | Render hot-path allocation/frame-time reductions | **Complete** |
| **D** | Parser unification, server hardening, async asset pipeline | **Complete** |
| **E** | JASS/Lua coverage, campaign/map fidelity, multiplayer hardening | **In progress** |
| **F** | Community modding layer (asset overrides, mod APIs/tooling) | Planned |

Also see [`CHANGELOG.md`](CHANGELOG.md) and [`docs/COMPATIBILITY.md`](docs/COMPATIBILITY.md).

### Current focus

Phase E: deepen JASS/campaign fidelity and map-format coverage so RoC/TFT and
classic custom maps feel like Warcraft III — then layer QoL without breaking
that contract.

---

## Relationship to Warsmash

OpenWCIII is based on Warsmash by Retera and contributors. Upstream history,
engine architecture, and most setup instructions still apply. Prefer this
repository for OpenWCIII-specific goals (faithful WC3 + QoL); credit and
link back to [WarsmashModEngine](https://github.com/Retera/WarsmashModEngine)
when discussing shared engine foundations.

---

### In the News (Warsmash)

Some social posts in 2022 claimed Warsmash was taken down by Activision
Blizzard. That did **not** happen; confusion originated from a parody video
takedown. Upstream Discord: https://discord.com/invite/ucjftZ7x7H

## Gameplay Example (Warsmash)

[![GAMEPLAY VIDEO](http://img.youtube.com/vi/EO-FDeQhFWc/0.jpg)](https://www.youtube.com/watch?v=EO-FDeQhFWc)

---

## Before you Begin: INI File

Regardless of whether you edit from an IDE, run from the command line, or build
a binary release, you need a correct `warsmash.ini`. Warsmash (and OpenWCIII)
do not auto-detect a single “true” Warcraft III install: Activision’s patch
history moved registry keys and archive layouts repeatedly (1.27 → Reforged).

Put the user in control. The `[DataSources]` block describes a virtual file
system of layered “places to look.” Once configured, the engine can play from
any of the layouts below (all tested at some point upstream):

- Warcraft III: Frozen Throne: Patch 1.22 - 1.28
  - `[DataSources]` set to using MPQ files + the "resources" folder from this repo
  - `[Emulator]` block required to have `MaxPlayers=16`
- Warcraft III: Frozen Throne: Patch 1.29
  - MPQ files + resources; remove `War3Patch.mpq` from the list
  - `[Emulator]` `MaxPlayers=28`
- Warcraft III: Frozen Throne: Patch 1.30
  - Manually extracted CASC folders (folders named `.mpq` that are not MPQ archives)
  - Still include the repo `resources` folder
  - CASC parser targets 1.32.10+; 1.30 folder names are probed if present
  - `MaxPlayers=28` — less recently tested
- Warcraft III: Frozen Throne: Patch 1.31
  - Extracted `.w3mod` folders + resources; `MaxPlayers=28` — less recently tested
- Warcraft III: Frozen Throne: Patch 1.32
  - CASC directly with INI “Prefixes” for `.w3mod`s; see `./core/assets/warsmashRF.ini` for HD
  - Still include resources; best-tested modern layout is often 1.32.10
  - Sound tables / FLAC / DDS caveats: see [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)
- **Not** Patch 1.33+ model format — not parsed yet. Prefer an older asset set.

---

## How to Build/Run the Code

### From IntelliJ IDE
1. Download IntelliJ
2. Open this repo (`https://github.com/awest813/OpenWCIII.git`) via “Open from VCS”
3. Edit `core/assets/warsmash.ini` so data sources locate your WC3 assets
4. Run the Gradle target `runGame`
5. Optional: `runGame -Pargs="-loadfile WorldEditTestMap.w3x -window"`
6. Extra flags via `-Pargs`: `-windowed [width height]`, `-fps`, `-vsync`/`-novsync`, `-msaa`, `-ini`, `-help`

### From the Eclipse IDE
1. Install Eclipse + Marketplace plugin `ANTLR 4 IDE`
2. `git clone https://github.com/awest813/OpenWCIII.git` outside Eclipse
3. Use a separate Eclipse workspace folder
4. `File > Import` → `Gradle > Existing Gradle Project` (prefer `Next` over early `Finish`)
5. If Gradle import crashes on system Java, install Eclipse Temurin JDK 17 and retry
6. Configure ANTLR preferences: disable listener, enable visitor, set Directory to `./build/generated-src`
7. Gradle refresh all warsmash-* projects; trigger ANTLR regen in `fdfparser` and `jassparser` `antlr-src`
8. Run `DesktopLauncher` with working directory `desktop/assets` (or equivalent)
9. Point `warsmash.ini` at your WC3 install (Hive setup threads may help for older patches)

### From GNU/Linux Command Line
1. `git clone https://github.com/awest813/OpenWCIII.git`
2. Prefer Eclipse Temurin JDK 17 over some distro OpenJDK 17 packages (LibGDX native issues)
3. Edit `./core/assets/warsmash.ini` with forward-slash paths
4. `JAVA_HOME=…/temurin-17 ./gradlew desktop:runGame`

## How to Build Release Binary Version
1. Clone the repo
2. `./gradlew desktop:runtime`
3. Use `./desktop/build/image` — add WC3 assets + a valid `warsmash.ini`
4. Run `./bin/warsmash.bat` (Windows) or `./bin/warsmash.sh`
5. Optional Windows EXE wrapper: [Warsmash Windows Wrapper](https://github.com/Retera/WarsmashWindowsWrapper/tree/experimental)

## Background and History

The engine runs on Java 17 (Java 8 syntax) with LibGDX and a ported MDX/W3X
viewer stack. Major lineages include:

- Relevant portions of [mdx-m3-viewer](https://github.com/flowtsohg/mdx-m3-viewer) for MDX/W3X display
- Terrain systems descended from [HiveWE](https://github.com/stijnherfst/HiveWE), adapted for MDX rendering
- Graphical enhancements from [wc3data](https://github.com/d07RiV/wc3data) (waves, shadows, UberSplats, etc.)
- BLP/MPQ tooling from DrSuperGood; TGA pathing from OgerLord / Retera Model Studio lineage
- SLK/INI and object-editor parsers consolidated over Phases B–D

See upstream Warsmash history for deeper attribution. OpenWCIII continues that
work toward faithful WC3 + QoL.

## Legal Stuff
_NOTE: The following is not legal advice and is only back-of-the-hand speculation. In addition, this project may contain repackaged code from other projects where indicated, and these other projects may be subject to the terms of other license agreements such as the GPL. The licenses for those projects should be clearly indicated when you review their code._

Earlier versions of Warsmash included a footnote suggesting that the official Warcraft 3 game might some day be able to copy components from Warsmash as a means to improve itself, because Warsmash was MIT licensed. It was brought to attention that at least one of the dependencies was GPL licensed and more specifically that the exact terms of the GPL suggest that any project that uses GPL code as a dependency must itself be GPL licensed in order to comply with the GPL terms. As such, had Activision actually copied code from Warsmash and placed that code into Reforged, they would have been at risk of legally creating a situation that required the whole of Reforged itself to become free software, perhaps, because Warsmash may have this obligation to be free software likewise despite an incorrect documentation/understanding of the matter in previous versions.

**You must own Warcraft III to use OpenWCIII.** This repository does not redistribute Blizzard Entertainment game assets.
