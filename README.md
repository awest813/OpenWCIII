# OpenWCIII

OpenWCIII is a community fork of Warsmash that reimplements the Warcraft III
engine in Java. The goal is faithful Reign of Chaos (RoC) and The Frozen Throne
(TFT) gameplay, followed by quality-of-life improvements.

**Development status: incomplete. Full single-player campaign parity is not
verified.** Mission completion, enemy AI behavior, presentation, and saved-game
resume still need substantial verification and implementation.

You need your own Warcraft III game data. This repository does not supply a
Warcraft III installation.

## Verified status

Latest local verification, September 24, 2026, used Windows, Java 17, Gradle 8.6,
and a combined RoC/TFT MPQ asset set:

| Check | Result | What it establishes |
|---|---|---|
| Core tests | 222 passed; 0 reported failures, errors, or skips | The assertions exercised by the local suite passed |
| Campaign loading and idle simulation | 85/85 discovered maps; 300 ticks each | Object loading, checked AI script loading, and short simulation runs |
| Focused hero carryover test | Passed with retail data | The tested hero retained stats, learned Holy Light, and equipment through a disk gamecache round trip |

The inventory includes interludes, credits, and bonus maps. This is **not 85
completed missions**. The idle audit does not execute mission objectives, render
the game, verify AI strategy, or measure memory leaks. Retail-dependent tests
may skip or return early when local game data is unavailable.

The [campaign parity plan](docs/WC3_CAMPAIGN_PARITY_AUDIT.md) records evidence,
known gaps, reproduction commands, and completion criteria. No overall
“percent complete” figure is justified.

## Current limitations

- **Save/load is partial.** Gameplay restores primitive script globals/arrays,
  resources, clock, and camera, but not a complete battlefield, script handles,
  triggers, timers, or AI execution state.
- **Progression is not verified end to end.** Menu availability and transition
  code exist; resolving a next-map path does not prove victory, unlocks,
  carryover, retry, and the next chapter work together.
- **Gameplay coverage is incomplete.** The latest audit reports unsupported
  upgrade effects and skipped legacy destructable modifications. Unimplemented
  abilities can fall back to behaviorless placeholders.
- **Presentation is partial.** Some panels are basic overlays; model cinematics
  are not fully played. Movies require external ffmpeg. Some options persist
  without affecting gameplay yet.
- Custom campaign parsing exists, but a complete launch flow is not established.
  Replay, LAN, multiplayer, and arbitrary custom-map parity are not certified by
  the campaign checks.

## Build and run

Use **JDK 17** with `JAVA_HOME` set. The wrapper specifies **Gradle 8.6** and
needs network access to download Gradle and dependencies on a fresh checkout.

```bash
git clone https://github.com/awest813/OpenWCIII.git
cd OpenWCIII
./gradlew :core:test
```

In Windows PowerShell, replace `./gradlew` with `.\gradlew.bat`.

### Configure assets

Edit [core/assets/warsmash.ini](core/assets/warsmash.ini). Its checked-in paths
are developer-specific and must be replaced. A combined classic RoC/TFT example:

```ini
[DataSources]
Count=4
Type00=MPQ
Path00="C:/WC3Data/war3.mpq"
Type01=MPQ
Path01="C:/WC3Data/War3x.mpq"
Type02=MPQ
Path02="C:/WC3Data/War3xlocal.mpq"
Type03=Folder
Path03="C:/src/OpenWCIII/resources"

[Emulator]
MaxPlayers=16
GameVersion=1
```

Use your actual paths. Update the existing sections and retain the other INI
settings. Later sources take precedence. Add numbered Folder sources for maps
or movies stored outside the archives, and update `Count`.

The local checks used all three archives together. They do not establish
RoC-only compatibility or certify every patch. [COMPATIBILITY.md](docs/COMPATIBILITY.md)
contains layout notes and historical support claims; its hardware and patch
matrix was not revalidated by this campaign audit.

### Validate and launch

```bash
./gradlew :desktop:runGame -Pargs="-validate"
./gradlew :desktop:runGame -Pargs="-window 1280 720"
```

The task runs from `core/assets`. For IDE launches, use that working directory
and the `com.etheller.warsmash.desktop.DesktopLauncher` main class.

Use `-Pargs="-help"` for options. These include `-ini`, `-loadfile`,
`-profile safe|balanced|high`, `-fps`, `-vsync` / `-novsync`, `-msaa`, and `-nolog`.
Gradle forwards `-Pargs` by splitting on whitespace, so paths containing spaces
are not reliably preserved by that mechanism.

Validation checks data-source paths, not campaign completeness. The launch task
currently ignores application exit codes; inspect its output after a failure.

Movie decoding requires ffmpeg on `PATH`, through `WARSMASH_FFMPEG`, or via the
`warsmash.ffmpeg` Java system property. Missing movies or decoder support use a
fallback overlay, which does not count as movie parity.

## Development priorities

1. Run actual mission scripts and track objectives and branches.
2. Resolve missing upgrade, object-data, ability, and AI behavior.
3. Implement complete, identity-preserving mission save/resume.
4. Verify progression, difficulty, defeat/retry, and chapter carryover.
5. Review rendered UI, cinematics, sound, and long-session stability.

The [parity plan](docs/WC3_CAMPAIGN_PARITY_AUDIT.md) is the campaign status source.
[MISSION.md](docs/MISSION.md) describes the goal;
[ENGINE_MODERNIZATION_ANALYSIS.md](docs/ENGINE_MODERNIZATION_ANALYSIS.md) and
[CHANGELOG.md](CHANGELOG.md) provide design and historical context. Historical
phase labels are not current campaign certification.

Report revision, OS/Java details, asset version, map, difficulty, reproduction
steps, and logs with issues. Include behavioral evidence with fixes and identify
what remains untested. Do not contribute proprietary game archives.

## Attribution and license

OpenWCIII builds on Warsmash by Retera and contributors, with components derived
from projects including mdx-m3-viewer, HiveWE, and wc3data. Preserve source and
dependency attribution notices.

The repository's [LICENSE](LICENSE) contains the GNU Affero General Public
License, version 3. Consult it and component-specific notices for applicable
terms. Warcraft III game data is separate from the engine source.
