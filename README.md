# OpenWCIII

OpenWCIII is an open-source desktop engine for classic Warcraft III. It uses your
copy of **Reign of Chaos** and **The Frozen Throne** to load maps, run their JASS
scripts, and render the game with LibGDX and OpenGL. It builds on
[Warsmash](https://github.com/Retera/WarsmashModEngine).

The priority is faithful single-player campaigns, including mission objectives,
cinematics, hero carryover, and the desktop client. Complete campaign and client
parity is still in progress. The repository ships engine code and project
resources. Bring your own Warcraft III installation or extracted classic archives.

## Play

### Requirements

- **JDK 17** and a working OpenGL desktop environment. The repository includes
  the Gradle 8.6 wrapper.
- Classic `war3.mpq`, `War3x.mpq`, and `War3xlocal.mpq` for the verified
  combined RoC/TFT setup.
- **FFmpeg** for movie playback. Put it on `PATH`, set `WARSMASH_FFMPEG`,
  or set `FfmpegPath` in the INI.

### Clone and configure

```sh
git clone https://github.com/awest813/OpenWCIII.git
cd OpenWCIII
```

Copy [core/assets/warsmash.ini](core/assets/warsmash.ini) to
`core/assets/warsmash.local.ini`. Keep the other sections and point
`[DataSources]` at your archives and this checkout's `resources` directory.

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
```

In `[Emulator]`, use `MaxPlayers=16` and `GameVersion=1` for this setup.
Replace any example `FfmpegPath` with your path or remove that entry. Later
data sources take precedence. If your maps or movies are in separate folders,
add numbered `Folder` entries and update `Count`. See the
[compatibility guide](docs/COMPATIBILITY.md) for other layouts.

### Validate and launch

```powershell
.\gradlew.bat :desktop:runGame '-Pargs=-ini warsmash.local.ini -validate'
.\gradlew.bat :desktop:runGame '-Pargs=-ini warsmash.local.ini -profile safe -nolog'
```

On Linux or macOS, use `./gradlew` in place of `.\gradlew.bat`.
Gradle runs the client from `core/assets`, where it resolves
`warsmash.local.ini`. The `safe` profile starts a 1280×720 window with VSync,
a 60 FPS cap, and MSAA disabled. Launch without `-profile safe` to restore
saved display preferences.

| Launch option | What it does |
| --- | --- |
| `-loadfile Maps/Campaign/Human01.w3m` | Open a map directly |
| `-window 1280 720` or `-fullscreen` | Set the display mode for this launch |
| `-fps 60`, `-vsync`, `-msaa 4` | Override frame-rate and display settings |
| `-validate` | Validate configuration and asset paths |
| `-nolog` | Keep diagnostic output in the console |
| `-help` | Print the launcher options |

Gradle splits `-Pargs` on whitespace. Keep paths with spaces in the INI.
For an IDE launch, use
`com.etheller.warsmash.desktop.DesktopLauncher` with `core/assets` as the
working directory.

## Campaign and client status

| Area | Verified behavior |
| --- | --- |
| Reign of Chaos | Human campaign main objectives completed across linked runs, through the Human finale and Undead campaign unlock. Selected optional quests, earned hero carryover, and Orc and Night Elf routes have separate coverage. |
| The Frozen Throne | The Night Elf campaign has verified earned progression through the Tomb of Sargeras and the next chapter's courier boarding. The Blood Elf opening has verified two Observatory repairs. Durotar has verified travel between original zones, hero restoration, and the Shimmerweed turn-in. |
| Desktop client | Render checks cover campaign profiles, victory and defeat actions, retry, restart cancellation, modal input, and multiple window sizes. The installed expansion intro movie reached its natural ending and returned control to the player. |
| Save and resume | Original-map fixtures exercise named saves, fresh-process checkpoint replay, active simulation state, and transition to the next chapter. |

Several campaign routes still need native victories verified from earned
saves, including the Orc finale retry, the Night Elf courier's sea escape, and
the final Blood Elf Observatory repair. Long-running checkpoints, further
campaign routes, gameplay edge cases, and comparisons with the original
client remain open. See the [campaign playthrough audit](docs/CAMPAIGN_FLOW_AUDIT.md),
[client presentation audit](docs/CAMPAIGN_PRESENTATION_AUDIT.md), and
[save and resume audit](docs/MISSION_SAVE_RESUME_AUDIT.md).

## Build and verify

```powershell
.\gradlew.bat assemble
.\gradlew.bat :core:test -PretailAssets=C:/WC3Data -PrequireRetailData=true
.\gradlew.bat :desktop:campaignSoakAudit '-Pargs=--mpq C:/WC3Data/war3.mpq --mpq C:/WC3Data/War3x.mpq --mpq C:/WC3Data/War3xlocal.mpq --ticks 300 --ability-gaps --item-ability-gaps'
```

The core suite passed **573 tests** with the classic archives required. The
full campaign smoke audit passed **85 maps over 25,500 simulation ticks**.
The inventory includes missions, interludes, credits, and bonus maps. This
check exercises loading and early simulation. The playthrough audit records
objective completion separately.

The render tasks open a desktop window, use the asset configuration in
`core/assets/warsmash.ini`, and write isolated audit output under
`desktop/build`.

```powershell
.\gradlew.bat :desktop:outcomeMenuRenderAudit
.\gradlew.bat :desktop:campaignPersistenceRenderAudit
.\gradlew.bat :desktop:missionResumeRenderAudit
```

## Source and contributions

- `core` contains the simulation, client UI, rendering, audio, and tests.
- `desktop` contains the launcher and audit tools.
- `jassparser` and `fdfparser` parse game scripts and UI definitions.
- `shared`, `server`, and `resources` provide shared code, server code, and
  engine resources.

See [CONTRIBUTING.md](CONTRIBUTING.md) for the development workflow and
[CHANGELOG.md](CHANGELOG.md) for history. When reporting a bug, include the
revision, asset version, map, difficulty, reproduction steps, and relevant
logs. A screenshot and graphics settings help with rendering reports.
Keep proprietary archives and extracted game assets out of contributions.

OpenWCIII builds on Warsmash by Retera and contributors, with components from
projects including mdx-m3-viewer, HiveWE, and wc3data. The engine is distributed
under the [GNU Affero General Public License v3](LICENSE). See component
notices for their terms.
