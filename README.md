# OpenWCIII

An open-source Warcraft III engine in Java, built on [Warsmash](https://github.com/Retera/WarsmashModEngine).

OpenWCIII aims to recreate **Reign of Chaos** and **The Frozen Throne** faithfully,
then add carefully scoped quality-of-life improvements. It loads Warcraft III
assets and maps, runs JASS scripts and game simulation, and renders the world
through LibGDX and OpenGL. Internal packages and configuration files retain the
Warsmash name.

**In active development. Full campaign playthroughs and save/resume parity are
not yet verified.** You need your own Warcraft III game data; the repository
does not include a game installation.

[Getting started](#getting-started) · [Current work](#current-work) ·
[Verification](#verification) · [Known gaps](#known-gaps) ·
[Contributing](#contributing)

## Getting started

### 1. Build the engine

Use **JDK 17** and the included **Gradle 8.6 wrapper**. Set `JAVA_HOME` to your
JDK. A fresh checkout needs network access to download build dependencies.
Running the desktop client also requires a working OpenGL graphics environment.

```bash
git clone https://github.com/awest813/OpenWCIII.git
cd OpenWCIII
./gradlew :desktop:classes :core:test
```

In Windows PowerShell, use `.\gradlew.bat` in place of `./gradlew`.
Tests that depend on retail data can report skips until assets are configured.

### 2. Point the engine at your game data

Copy [core/assets/warsmash.ini](core/assets/warsmash.ini) to
`core/assets/warsmash.local.ini`, which is ignored by Git. Replace its
`[DataSources]` section with your paths. For a combined classic RoC/TFT setup:

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

Keep the other sections from the original file. Under `[Emulator]`, the tested
TFT configuration uses `MaxPlayers=16` and `GameVersion=1`. Replace or remove
the copied machine-specific `FfmpegPath` as appropriate.

Later data sources take precedence. Add numbered `Folder` sources and increase
`Count` if your maps or movies live outside the archives. Use the actual path
to this checkout's `resources` directory for `Path03`.

The verified local setup uses all three MPQs together. This does not establish
RoC-only compatibility or compatibility with every retail patch. See the
[compatibility notes](docs/COMPATIBILITY.md) for other data layouts; historical
platform and patch claims there have not all been revalidated.

### 3. Validate and launch

```bash
./gradlew :desktop:runGame -Pargs="-ini warsmash.local.ini -validate"
./gradlew :desktop:runGame -Pargs="-ini warsmash.local.ini -profile safe -nolog"
```

The task runs from `core/assets`, so `-ini warsmash.local.ini` resolves there.
Validation checks asset paths. The `safe` profile starts a 1280×720 window with
VSync, a 60 FPS cap, and no MSAA. Later launches without a profile or display
flags can restore saved display preferences:

```bash
./gradlew :desktop:runGame -Pargs="-ini warsmash.local.ini -nolog"
```

For an IDE, use `com.etheller.warsmash.desktop.DesktopLauncher` as the main class
and `core/assets` as the working directory.

### Launch controls

| Option | Purpose |
| --- | --- |
| `-profile safe`, `balanced`, or `high` | Choose window/fullscreen, VSync, FPS, and MSAA defaults |
| `-window 1280 720` / `-fullscreen` | Override the display mode |
| `-fps 60` / `-fps 0` | Set a frame cap / run uncapped |
| `-vsync` / `-novsync` | Enable / disable VSync |
| `-msaa 4` | Request multisample antialiasing at startup |
| `-loadfile Maps/Campaign/Human01.w3m` | Load a map directly through the configured data sources |
| `-ini warsmash.local.ini` | Select a configuration file |
| `-nolog` | Keep diagnostic output in the console |
| `-help` | Print the full option list |

Individual launch flags override profile values. Double-dash forms, including
`--fps=60`, are supported. Gradle's `-Pargs` forwarding splits on whitespace;
use paths without spaces in these examples. The launch task ignores application
exit codes, so inspect its output when diagnosing a failure.

Movies require ffmpeg, found through `PATH`, `WARSMASH_FFMPEG`, the INI's
`[Emulator] FfmpegPath`, or the `warsmash.ffmpeg` Java property. Unavailable
movies use a fallback overlay.

## Current work

### Graphics and menus

The retail Video controls now drive resolution, gamma, model detail, animation
sampling, texture quality, particle density, local lights, shadows, and tree
occlusion. Display and preference failures are reported with rollback attempts.
Texture quality changes retain the original data for reversible switching;
they do not promise lower video-memory use.

The main menu has corrected backdrop framing, centered portrait layouts,
fractional border tiles, and smoother magnified glow textures around the TFT
ice. The FPS overlay is hidden by default. Model detail depends on authored
LODs, and gamma support depends on the display driver.

Details: [graphics settings](docs/GRAPHICS_SETTINGS_AUDIT.md),
[menu graphics and audio](docs/MENU_GRAPHICS_AUDIO_AUDIT.md), and
[menu input and dialogs](docs/MENU_AUDIT.md).

### Unit behavior

Recent fixes cover Hold Position target selection, autocast eligibility and
range, automatic orders preserving queued commands, follow/combat transitions,
patrol resumption, attack-move route exhaustion, and safe queue cancellation.
Sixteen focused simulation scenarios exercise these behaviors using retail
Human01 object data. Campaign strategy and large-army pathfinding need separate
coverage. See the [unit AI audit](docs/UNIT_AI_AUDIT.md).

### Audio

Sound-effects volume and mute now reach shared playback and active script
sounds. Unit responses and menu ambience honor their toggles; script-created
sounds own independent playback handles and support live volume, pitch,
position, and positional-audio changes. Audio remains an initial pass: some
world loops, category controls, EAX, fades, and listening tests remain open.
See the [audio audit and limitations](docs/MENU_GRAPHICS_AUDIO_AUDIT.md#initial-audio-work).

## Verification

Latest local verification: **September 30, 2026**, on Windows with Java 17,
Gradle 8.6, and a combined classic RoC/TFT asset set.

| Check | Recorded result | Scope |
| --- | --- | --- |
| Core suite | **361 passed**, no failures, errors, or skips | Includes graphics settings, unit orders, audio playback, nested trigger execution, campaign and cinematic-camera regressions |
| Graphics GPU audit | Passed | Texture-quality switching, model LOD identity, foliage fading/cutouts, border tiles, and glow sampling |
| Main-menu render audit | Passed at 800×600, 1280×720, and 600×800 | Real rendering, backdrop coverage, captures, and same-frame glow comparisons |
| Retail backing-screen audit | 47/47 passed | Selected assets, cameras, sequences, and skins |
| Retail campaign flow | Human01 main/optional and save/resume; Human02 main/interlude/carryover; Human03 main/optional objectives and Human04 hero restoration verified through ordinary orders | Separate fixtures cover defeat/retry, difficulty and menu return; Human02 optional quest/save-resume, Human04–09 objectives and full campaigns remain unverified |
| Campaign profile persistence | Passed across five client launches | Two distinct heroes/unlocks, profile switching, deletion/recreation, and preserved legacy cache migration; [scope](docs/CAMPAIGN_FLOW_AUDIT.md#process-relaunch-and-profile-isolation--september-29-2026) |
| Victory/defeat menus | Passed at three window sizes | Chapter and end-state summaries, outcome routing, retry, Quit, restart cancellation, modal camera/input, overlay suppression, and Load/Back recovery; [scope](docs/OUTCOME_MENUS_AUDIT.md) |
| Mission checkpoint replay | Human01 → save → relaunch → defeat/load → victory → Human02 | VM locals, aliases, timers, arrays, queued orders, inventory and carryover; [scope](docs/MISSION_SAVE_RESUME_AUDIT.md) |
| Earlier campaign idle audit | 85/85 discovered maps, 300 ticks each | Loading, checked AI initialization, and short headless simulation |

The campaign inventory includes interludes, credits, and bonus maps. It is not
a count of completed missions. Earlier campaign results, research/combat
regressions, hero carryover evidence, and their exclusions are recorded in the
[campaign parity plan](docs/WC3_CAMPAIGN_PARITY_AUDIT.md).

### Reproduce checks

```bash
# Require local retail fixtures; fail instead of skipping missing data.
./gradlew :core:test -PretailAssets=C:/WC3Data -PrequireRetailData=true

# Real GPU checks; requires a desktop graphics session.
./gradlew :desktop:graphicsSettingsRenderAudit

# Retail backing assets and real main-menu rendering.
./gradlew :desktop:campaignBackingAudit -Pargs="--mpq C:/WC3Data/war3.mpq --mpq C:/WC3Data/War3x.mpq --mpq C:/WC3Data/War3xlocal.mpq"
./gradlew :desktop:menuGraphicsAudit

# Render Human01 and switch graphics settings during the run.
./gradlew :desktop:graphicsSettingsMapAudit

# Execute retail defeat/retry and completion actions through to Human02.
./gradlew :desktop:campaignFlowRenderAudit

# Exercise the mission's registered death/region events and victory condition.
./gradlew :desktop:campaignFlowRenderAudit -PcampaignObjectiveEvents=true

# Verify profile-specific heroes/unlocks after quitting and relaunching the client.
./gradlew :desktop:campaignPersistenceRenderAudit

# Verify victory/defeat choices and capture dialogs at three window sizes.
./gradlew :desktop:outcomeMenuRenderAudit

# Play Human02's main objectives, story interlude and earned carryover into Human03.
./gradlew :desktop:human02PlaythroughAudit
# Include Human01's main and optional objectives before Human02.
./gradlew :desktop:human02PlaythroughAudit -PcampaignHuman02Chain=true
# Capture the unskipped Human02 interlude and its transition on their own.
./gradlew :desktop:human02InterludeRenderAudit

# Save a running mission, relaunch, compare continuation, and exercise chapter carryover.
./gradlew :desktop:missionResumeRenderAudit
./gradlew :desktop:missionResumeRenderAudit -PmissionResumeScenario=broader
```

The menu/map render tasks use `core/assets/warsmash.ini`; configure that file
for those tasks. They isolate player preferences under `desktop/build`, write
logs/captures there, and open short-lived windows. The `retailAssets` property
configures test fixtures, not the launcher's INI. Generated reports and local
asset files are not included in the repository.

## Known gaps

- **Save/resume:** v7 checkpoints reconstruct the mission and interpreter by
  replaying recorded inputs, then validate state before resuming. The retail
  Human01 fixture passes across process relaunch and chapter transition, including
  active projectiles, AI captain/guard/worker state, timed camera/filter/subtitle
  continuation, and mixed economies for all four races with construction, training and research.
  A 96-unit ranged battle also passes in a cleared arena. Larger or combined-arms
  battles, full campaign playthroughs, long missions, model/video
  cinematics, audio timing and custom maps remain unverified; see the [checkpoint audit](docs/MISSION_SAVE_RESUME_AUDIT.md).
- **Campaign progression:** the Human01 → Human02 chain now has a live
  [regression](docs/CAMPAIGN_FLOW_AUDIT.md) covering defeat/retry, objective
  events, victory, unlocks, and carryover together using simulation fixtures.
  An ordinary-order playthrough also completes Human01's main and optional
  objectives, resumes a real quest-item checkpoint, and verifies earned carryover
  into Human02. A two-chapter route also completes Human02's main objectives,
  its story interlude and earned carryover into Human03. A direct Human03 run
  completed its main/optional objectives and verified both heroes in Human04.
  Human02's optional quest/save-resume, Human04–09 completion, full campaigns
  and other branches remain unverified.
- **Gameplay:** unsupported upgrade effects and legacy destructable changes
  remain; some unimplemented abilities use behaviorless placeholders.
- **Presentation:** some panels and cinematics remain partial. Passing asset or
  rendering checks does not establish full retail visual or audio parity.
- **Other modes:** custom campaign launch, replay, LAN, multiplayer, and arbitrary
  custom-map compatibility are not certified by the campaign audits.

Priorities are mission objectives and strategy behavior, missing gameplay
implementations, complete save/resume, chapter progression, and rendered and
audible playthrough checks. The [mission](docs/MISSION.md) and
[campaign parity plan](docs/WC3_CAMPAIGN_PARITY_AUDIT.md) describe the intended
scope and completion criteria.

## Contributing

Start with [CONTRIBUTING.md](CONTRIBUTING.md). For a bug report, include the
revision, OS and Java version, asset version, map and difficulty, reproduction
steps, and logs. For visual issues, include a screenshot and display settings.
Fixes should identify the behavior tested and any remaining uncertainty.
Do not contribute proprietary game archives.

| Location | Contents |
| --- | --- |
| `core` | Rendering, simulation, UI, audio, data sources, and tests |
| `desktop` | Launcher, desktop backend, editors, and audit tools |
| `jassparser` / `fdfparser` | Script and UI-definition parsers |
| `shared` / `server` | Shared protocol and server code |
| `resources` | Project-provided runtime resources |
| `docs` | Audit evidence, compatibility notes, and design documents |

See [CHANGELOG.md](CHANGELOG.md) for change history and the
[modernization analysis](docs/ENGINE_MODERNIZATION_ANALYSIS.md) for architectural
context. Historical milestone labels do not certify current campaign parity.

## Attribution and license

OpenWCIII builds on Warsmash by Retera and contributors, with components derived
from projects including mdx-m3-viewer, HiveWE, and wc3data. Preserve source and
dependency attribution notices.

The repository's [LICENSE](LICENSE) contains the GNU Affero General Public
License, version 3. Consult it and component-specific notices for applicable
terms. Warcraft III game data is separate from the engine source.
