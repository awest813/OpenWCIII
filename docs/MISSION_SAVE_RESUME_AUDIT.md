# Mission checkpoint reconstruction — September 29, 2026

Single-player mission saves now use a v7 checkpoint journal. Loading builds a
fresh mission and replays its startup and tick-stamped inputs to the saved tick.
This preserves the engine's original entity handles, script aliases, trigger
registrations, timers, execution stacks, sleeping locals, and AI threads rather
than replacing map entities underneath existing scripts.

The implementation is wired into the in-game Save button, JASS `SaveGame`,
`LoadGame` and `ReloadGame`, main-menu Load Saved, direct `.w3s` launch, and the
retail defeat menu's Load picker. Back restores the originating defeat dialog;
an empty picker explains that no complete saves are available.

## Checkpoint contract

- Record external orders, selection/deselection, chat, GUI player events,
  script-dialog clicks, trackables, pause changes, and save/load notifications.
  Script-generated changes during simulation updates run again from the scripts.
- Record camera, selection, sound and save-file observations that depend on
  host state, together with every disk gamecache read. Replay returns those
  observations and suppresses historical cache/save-file writes.
- Preserve startup campaign progress, player/difficulty settings, and static
  trigger/gamecache handle counters. Replay temporarily suppresses profile
  persistence and restores the current profile's progress afterward.
- Validate map/script/engine fingerprints before reconstruction. At the target
  tick, compare the gameplay/VM digest, including pending entities, orders,
  timers, sleeping frames, random state, fog, pathfinding jobs, pathing overlays
  and spatial indexes, plus AI captains, assault groups and assigned guard posts. Failure returns to the menu with an error.
- Save during a script update only after the completed tick. Flush pending local
  orders before capture. Keep atomic replacement and bounded format parsing.
- Restore camera position/angles/zoom, the Space-key bookmark, pending pan and
  independent target-offset/scripted-setup height interpolation,
  camera rates/noise, filter elapsed time, and remaining cinematic transmission
  duration/text after validation. Restore selected units without additional
  selection triggers. Send the game-loaded event once. Script-created camera
  setup/controller references are retained from reconstruction.
- Discard reconstruction time from the first live simulation/UI/camera frame.
  The rendered audit asserts that a near-expiry transmission and pan survive
  those first frames.
- Store slots under `.warsmash/saves/profiles/p-<profile-name SHA256>/`.
  Quick Save and named native saves use the same directory. Deleted profiles'
  files move into `saves/retired/`; recreating the name starts with empty slots.

Older v1–v5 files remain readable as legacy data, but gameplay loading rejects
their partial state. v6 journals remain readable but predate the presentation
checkpoint. v6/v7 files require matching engine binaries, map/scripts,
player profile, and game data. Rebuilding or changing engine code can invalidate
a checkpoint. Loading is available only in single-player sessions, and saving
after the mission has ended is rejected.

## Recorded verification

Windows 11, Java 17, Gradle 8.6, Intel Arc A750 OpenGL, combined classic RoC/TFT
MPQs configured through `core/assets/warsmash.ini`. The audit runs at 960×540 and
30 FPS with sound and music disabled, using isolated homes under `desktop/build`.
It does not use personal save files.

The Human01 fixture:

1. Skips the opening through its retail Escape event and starts an opt-in JASS
   function. Creates a unit, a handle alias/group, a hero inventory item, an
   initialization-time dialog, and a periodic timer; learns hero experience.
2. Clicks the real script-dialog button and issues a movement order plus a
   queued return order. Starts a sleeping script with local integer `731` and a
   local unit reference. The timer changes an array and resources and invokes
   native `SaveGame` while the interpreter is running.
3. Clicks the real in-game menu Save button and continues for 240 simulation
   ticks. Checks the sleeping local, live unit alias, dialog event and save/load
   event counts, and records the complete runtime digest. The reference branch
   receives the same game-loaded notification as the loaded branch.
4. Launches a fresh JVM from Quick Save. Validates the saved tick and compares
   the later digest with the reference. Both the default save offset and a
   second, earlier offset have passed.
5. Saves the already resumed mission again, executes the retail defeat action,
   and loads that new save from the picker. Checks that resumed script state and
   earlier load notifications survive another reconstruction, post-checkpoint
   defeat state is discarded, and the restored hero is alive. Executes the retail victory preparation and
   next-level actions, then clicks both Continue stages.
6. Checks Human02's carried hero experience, skill points, name, strength and
   inventory slots/charges, Human02 availability, Human03 remaining locked,
   and return to the menu.

An additional rendered failure audit creates copies with altered engine and
state fingerprints. It checks that both loads fail through the real menu error
path, then loads the untouched valid checkpoint successfully.

The strict retail core suite passes **355 tests, zero failures/errors/skips**.
Tests include checkpoint roundtrip and copy ownership, truncation and invalid
input rejection, sleeping-frame digest sensitivity, legacy save decoding,
profile slot isolation, recoverable retirement, camera pan/height continuation,
transmission deadlines, AI captain/guard digest sensitivity, and AI queries of
pending unit creations/removals, separate harvest jobs and builder protection,
queued production targets, disabled/full producers, unmet requirements, pending
research, and repeated destructable removal. The three-size outcome-menu
audit and five-process campaign profile persistence audit also pass after the
save-picker changes.

## Battle, AI and cinematic follow-up

The `broader` scenario runs all of these alongside the original fixture on
Human01, in the real desktop client and through the ordinary JASS AI loader:

- Two archers attack a footman while an AI captain owns their assault group.
  A separate footman fills a guard post. Save waits for an actual projectile
  in flight, after the native save and while the scripted pan is still active.
- The AI starts a periodic worker thread and sleeps with local integer `913`.
  It later returns the captain and guard to their home positions. The resumed
  process must preserve the two-unit assault group and continue both threads.
- A timed XY pan and height transition run with an interpolated cine filter,
  portrait and subtitles. Immediately after reconstruction, the complete saved
  camera/timer/text snapshot must match byte-for-byte. The live continuation
  must finish the cinematic, kill the combat target, and fire its death event
  exactly once, then match the uninterrupted gameplay/VM/AI digest.
- The existing resave, defeat-to-load, victory and Human02 carryover sequence
  follows that combined continuation.

This exposed and fixed four defects:

1. Loading cancelled an active camera pan.
2. Completing an XY pan cancelled its independent height transition.
3. AI queries missed units created in the current tick, leaving new assault and
   guard groups empty.
4. Reconstruction work consumed the first live frame, prematurely expiring a
   nearly finished cinematic.

AI queries now include pending creations and exclude queued removals.
Dynamically loaded AI preambles/scripts are also fingerprinted, and the runtime
digest includes the AI environment's captain/assault/guard state.

The default save offset passes. The later checkpoint with a 30 FPS writer and
15 FPS reader also passes, including the assertion that the pan and subtitles
survive the first live frames after reconstruction. The final strict run saved
at tick **225** and matched its uninterrupted continuation at tick **465** with
digest `0450f9ab2184eb1be539bc6053780b3f56f736016c367091a1765ffe7ac477a7`.
Its isolated home is `desktop/build/mission-resume-home-1790733463991`.
Engine and state mismatch copies were rejected before the valid save loaded;
the resumed resave/defeat/victory/Human02 sequence then completed. Rendered scene captures are
written as `scene-write.png` and `scene-read.png` inside the isolated profile's
save directory. These are visual evidence of the filter, portrait and subtitles,
not a pixel equality test of independently paced frames.

## AI economy and production follow-up — September 30

The `economy` scenario adds a controlled Human base to Human01. It clears nearby
destructables and creates a town hall, barracks, blacksmith, three peasants, a
gold mine and two trees. It uses unmodified retail production and harvest data,
the ordinary AI loader, and actual `HarvestGold`, `HarvestWood`, `SetProduce`
and `SetUpgrade` orders. An AI loop repeats its requests while sleeping with
local integer `927`.

Save waits until both harvest jobs are active, with carried lumber and a miner
carrying gold or inside the mine, while a farm is under construction and both
training and research queues have progressed. The fresh process must restore
that combined state. After another 1,500 simulation ticks, both branches must
have deposited gold and lumber, consumed actual mine resources, finished one
farm, trained exactly two footmen, completed one melee attack research level,
applied its attack bonus, drained both queues and preserved the AI's sleeping
local. Their complete gameplay/VM/AI digests must match. The resave,
defeat-to-load, victory and Human02 carryover checks follow.

The fixture and focused tests exposed and fixed:

- Gold and lumber allocation selected the same first workers and interrupted
  each other's jobs. Existing gatherers now count toward their resource's goal;
  additional allocation uses idle workers and preserves construction.
- Training goals ignored queued units and repeatedly bought extra units.
  Queued units and pending research now satisfy the corresponding requests.
- Production selected disabled/full buildings and reported success for unmet
  requirements. It now checks availability and can select another producer.
- Repeated destructable removal crashed the renderer and retained removed
  simulation handles and spatial entries. Removal is idempotent and clears
  simulation, pathing and render references.

The initial run saved at tick **227** and matched tick **1,727** after a fresh
30 FPS process launch. The final strict run used the later save offset `180`
and a **15 FPS reader**: it saved at tick **298** and matched tick **1,798**, with
gold **4,790**, lumber **4,990**, exactly two trained footmen, one completed farm
and one completed research level. The final writer and reader matched digest
`ec7678fbc826ffb3e2e53aee4c41b17b6fe3cf2440a4cdf7ad7e8d245353835b`.
Its isolated home is `desktop/build/mission-resume-home-1790766850569`.
Engine/state mismatch copies were rejected through the menu, followed by the
valid checkpoint's resave, defeat/load, victory and Human02 carryover sequence.

After these changes, the `broader` scenario was rerun with offset `95` and a
15 FPS reader. It passed at saved tick **223** and continuation tick **463**,
including the resave/chapter sequence, with digest
`2c638094acfbf987f55303e624803dcd128092201f144beba8586f29652880c2`.
Its home is `desktop/build/mission-resume-home-1790767125559`; both scene captures
were inspected and show the filter, portrait and subtitles after save/load.

## Other race economy coverage — September 30

The same economy fixture now accepts `economy-orc`, `economy-undead` and
`economy-nightelf`. Each uses its retail worker, supply building, producer,
trained unit and attack upgrade. Undead construction summons a Haunted Gold
Mine on its neutral parent. Night Elf setup entangles an in-range mine with an
ordinary order; its fixture disables fog and waits for the mine before assigning
wisps. Creation waits for tree removal to finish before placing the base.

The AI recognizes acolyte gold harvesting, ghoul lumber harvesting, wisp tree
income and entangled-mine boarding. Hidden wisps in a mine's actual cargo count
toward the gold-worker goal. Older retail tables without mine placement flags
fall back to the standard mine abilities; explicit flags take priority. AI mine
construction chooses the parent mine's exact center rather than sampling radial
points around the town.

The strict suite includes a boarded-wisp allocation regression, real resource
income for each race, exact parent placement, and ordinary entangling of a
visible mine. The recorded Orc, Undead and Night Elf desktop runs also passed the complete
resave, defeat/load and Human02 transition fixture:

| Scenario | Saved tick | Matching future tick | Isolated home |
| --- | ---: | ---: | --- |
| Orc | 257 | 1,757 | `mission-resume-home-1790774812462` |
| Undead | 1,225 | 2,725 | `mission-resume-home-1790774822613` |
| Night Elf | 227 | 1,727 | `mission-resume-home-1790773372553` |

Orc continuation digest:
`b9f28a893ec8015dacaa1b95c01ab6cab2e0bc54261fadee15a103225f6e21b1`.
Undead continuation digest:
`83c342bdbc82eeea030c39385341c5d287da7fcf1d86d687aa73131bfae2359d`.
Night Elf continuation digest:
`93f3b30d285b9d3db3e14f88d2931f73184399e72860d17b735b4f4d04261a90`.
The Night Elf checkpoint contains a boarded gold wisp, a harvesting lumber wisp,
a Moon Well under construction, and active training and research. Its future
contains two trained archers, one Moon Well, one applied attack upgrade and
actual income from both resources.
Orc and Undead were repeated after the ordinary-play driver was finalized; both
again passed the complete continuation and chapter fixture. Orc finished with
4,570 gold and 4,945 lumber; Undead finished with 4,390 gold and 4,780 lumber,
three ghouls including its initial lumber worker, one Ziggurat and one upgrade.
These are controlled single-base economies; normal campaign strategy, expansions
and dynamic reassignment after losses remain separate acceptance work.

## Ninety-six-unit battle coverage — September 30

The `battle` scenario creates two opposing 48-unit armies on Human01. Each army
contains twelve retail Riflemen, Headhunters, Crypt Fiends and Archers. Both use
the ordinary AI loader, assault groups and captain attack orders. The fixture
clears destructables and static pathing in its bounded arena and disables fog;
it does not establish combat behavior across the original terrain.

The checkpoint at tick **183** contains **95 living units, one combat death and
eleven missiles in flight**. The uninterrupted and fresh-process branches both
continue to tick **2,583**, with **57 living units and 39 combat deaths**. Both
sides suffer losses, registered death-event counts match the dead units, and
both AI worker threads retain their sleeping locals and continue running. The
complete gameplay/VM/AI digest matches:
`7e9418c9d8a498d154e243a9791255c87787e0209b947ff4e1a298d8f48fea79`.

The same run passes resave, defeat/load, victory menus and the Human02 carryover
fixture. Its isolated home is
`desktop/build/mission-resume-home-1790773661784`. Writer and reader scene
captures were inspected; they show the armies and active missiles before and
after loading. A headless 96-unit regression also checks natural combat deaths
after captain orders. These checks cover ranged combat at this size; larger
armies, combined melee/siege/spell combat, terrain navigation and performance
remain unverified.

## Scripted camera follow-up — September 30

The Human02 ordinary-order audit exposed a quick-position bookmark that moved
the live view and custom setup heights that put an interlude shot below terrain.
The fixes preserve the bookmark and authored height, including a pending
forced-duration height transition, in the presentation checkpoint. Four focused
regressions cover bookmark behavior/round-trip and camera height/reset/descent;
the descent's uninterrupted and resumed branches keep the eye above terrain
and reach the same target height. See the [rendered campaign evidence](CAMPAIGN_FLOW_AUDIT.md#camera-render-follow-up--september-30-2026).

After the final camera change, the strict retail-data suite passed **359 tests,
zero failures/errors/skips**. A fresh `:desktop:missionResumeRenderAudit` also
passed with home `desktop/build/mission-resume-home-1790810468208`. The saved
tick **186** matched digest
`a5d45c93e3cfee30b20d1f73eb5ecfe7c3e77b9e03e41b10f38ad269dab72c62`.
Both the uninterrupted and fresh-process continuation reached tick **426** with
`f927341ae2e563fdcfd7f3eeb73786c6f8edad5b3ca2443936f478dacc4ab55e`.
Quick Save, native SaveGame, the broader projectile/AI/cinematic fixture,
resave, defeat-to-load, victory and Human02 carryover all passed. This verifies
the checkpoint format with the final camera fields; the timed scripted-height
restoration itself is asserted by the focused camera regression.

## Reproduce

```sh
./gradlew :core:test -PrequireRetailData=true
./gradlew :desktop:missionResumeRenderAudit
./gradlew :desktop:missionResumeRenderAudit -PmissionResumeSaveOffset=50
./gradlew :desktop:missionResumeFailureAudit
./gradlew :desktop:missionResumeRenderAudit -PmissionResumeScenario=broader
./gradlew :desktop:missionResumeFailureAudit -PmissionResumeScenario=broader -PmissionResumeSaveOffset=95 -PmissionResumeReadFps=15
./gradlew :desktop:missionResumeRenderAudit -PmissionResumeScenario=economy
./gradlew :desktop:missionResumeFailureAudit -PmissionResumeScenario=economy -PmissionResumeSaveOffset=180 -PmissionResumeReadFps=15
./gradlew :desktop:missionResumeRenderAudit -PmissionResumeScenario=economy-orc
./gradlew :desktop:missionResumeRenderAudit -PmissionResumeScenario=economy-undead
./gradlew :desktop:missionResumeRenderAudit -PmissionResumeScenario=economy-nightelf
./gradlew :desktop:missionResumeRenderAudit -PmissionResumeScenario=battle
./gradlew :desktop:campaignPlaythroughAudit
./gradlew :desktop:campaignPlaythroughResumeAudit
./gradlew :desktop:outcomeMenuRenderAudit
./gradlew :desktop:campaignPersistenceRenderAudit
```

On Windows use `.\gradlew.bat`. Configure retail data in the launcher's INI;
`retailAssets` controls core fixtures rather than desktop launch configuration.
Logs are `desktop/build/mission-resume-<scenario>-write.log`,
`mission-resume-<scenario>-read.log`, and scenario-specific
`reject-engine`/`reject-state` logs.
Diagnostic state traces and temporary saves stay inside the isolated audit home.
Audits fail if their completion markers are absent.

## Limits of this evidence

The separate [ordinary-order Human01 playthrough](CAMPAIGN_FLOW_AUDIT.md#ordinary-order-playthrough--september-30-2026)
now also completes both optional quests and resumes a real mid-mission ledger
checkpoint in a fresh process before checking earned carryover into Human02.
That follow-up validates the saved runtime digest and compares later objective
outcomes and the hero snapshot. The scenarios described above use fixtures.

These are controlled retail-data/script regressions, not completed objective-driven
playthroughs. They establish active-projectile combat, captain/guard state,
sleeping AI workers, timed RTS-camera/filter/transmission continuation, and
mixed harvesting/construction/training/research for all four races across process relaunch.
They also establish a 96-unit ranged battle in a cleared arena. They do not
certify every mission, larger or combined-arms battles, custom maps, multi-town expansion, dynamic worker reallocation, all
buffs/cooldowns/upgrades, timed objectives, or repeated long-session save/load
cycles. The full campaign verification gate
remains open for those scenarios.

Reconstruction cost grows with elapsed simulation time. Historical script
presentation can run while seeking. MDX camera tracks/model cinematics, video playback,
normal-HUD talking-head timing, music/audio playback position and manual listening
remain unverified. The new presentation snapshot covers the RTS camera and
cinematic transmission/filter timers; it does not serialize media decoders or
GPU resources. Presentation resources and process-specific audio timestamps are
excluded from the gameplay digest. A matching digest verifies the inspected
runtime graph; it is not a claim that every host object is serialized or checked.

## Trigger execution follow-up — September 30, 2026

After fixing explicit nested trigger execution order, the strict core suite
passed **361 tests with zero failures/errors/skips**. A fresh
`:desktop:missionResumeRenderAudit --offline` passed in **1 minute 13 seconds**,
using `desktop/build/mission-resume-home-1790816944372`.
Saved tick **221** matched state
`3887d0a4570206a9956823c41d687efa226476725dec28fcf557c1cc41751f95`;
both continuations matched tick **461**, state
`3c7d562c2375a8e3eb9c38c8763523658a0475a750cf045abaa971f3df0696e1`.
Resave, defeat-to-load, victory and Human02 carryover also passed.

Human02's Searinox quest-item writer failed in ordinary dragon combat before
saving; its fresh-process reader did not run. That optional mission continuation
remains open. The [requested stopping point](CAMPAIGN_FLOW_AUDIT.md#requested-stopping-point)
records the failed run and the remaining campaign work.

## Pre-commit verification — September 30, 2026

After the audit/test cleanup, `assemble :core:test -PrequireRetailData=true
--offline` passed with **361 tests and zero failures/errors/skips**.
The combined `missionResumeFailureAudit outcomeMenuRenderAudit --offline` run
passed in **1 minute 53 seconds**, using
`desktop/build/mission-resume-home-1790817775585`.
Saved tick **191** had state
`f6b511807f6f97033c965720df2634d1e9f9d3e7d54ed0a4119be0e0c0d94998`.
Both continuations matched tick **431**, state
`f7f30b59015c13cd02018c12cefdfcd752cc36168db7be716a8bafd091ecf0dc`.
The engine mismatch and altered state fixtures returned to the menu as expected;
resave, defeat-to-load, victory, Human02 carryover and the outcome menus passed.
This verification does not close the remaining campaign playthrough gaps.
