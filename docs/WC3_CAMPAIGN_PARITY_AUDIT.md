# Warcraft III campaign parity: status and implementation plan

**Reviewed September 24, 2026. Status: incomplete; no full-parity sign-off.**

Scope: RoC and TFT single-player campaigns, chapter transitions, interludes,
credits, and bonus-campaign branches. Multiplayer, replay, arbitrary custom
campaigns, and Warcraft II formats require separate plans.

A registered native can be a no-op; a parsed map can be unwinnable. This plan
separates implemented code, bounded verification, and remaining behavior.
No overall completion percentage is justified.

## Evidence baseline

Results describe the local working tree, not a tagged release or clean-checkout
CI guarantee. The environment used Windows, Java 17, Gradle 8.6, and layered
`war3.mpq`, `War3x.mpq`, and `War3xlocal.mpq` under `F:/WC3Data`.
The base archive was matched to the user's RoC disc image; the installed TFT
archives were not independently matched to their installer.

| Check | Recorded result | Scope and exclusions |
|---|---|---|
| Core suite, latest run | 222 tests; 0 reported failures/errors/skips | Focused assertions, including retail fixtures; not mission playthroughs |
| Strict idle audit, latest run | 85/85 maps; 300 ticks each; 25,500 total | Object loading, checked AI parsing/initialization, idle simulation; no mission objectives, rendering, or leak measurement |
| Progression audit, earlier review | 70/70 distinct next-map targets resolved | Static target discovery and map opening; not executed transitions |
| Backing-screen audit, earlier review | 47/47 checks passed | Selected assets and model data; not visual approval |
| Native audit, earlier review | 0 reachable names missing from registration | Static reachability/registration; not correct native behavior |
| Cinematic-reference audit, earlier review | 0 missing files among references checked | Literal references and model parsing; not playback or timing |

The inventory includes interludes, credits, and bonus submaps. Counts depend on
the archives and are not a count of completed missions.

Local evidence: `Logs/campaign-regressions-final.txt`,
`Logs/campaign-strict-final.txt`, and earlier
`Logs/campaign-presentation-review.txt`. These are local artifacts, not guaranteed
contents of a fresh checkout. The generated
[native report](CAMPAIGN_NATIVE_COVERAGE.md) uses “implementation” for registration
coverage; it cannot establish behavior.

Earlier smoke results were weaker: ability definitions were not found from the
repository working directory and some failures were swallowed. Correct discovery
exposed 32 legacy-aura loading failures, now fixed. Strict parsing exposed unary
plus in `u08x02.ai`, now supported. The latest strict run contains no reported AI
parse or path-cycle exceptions.

**Remaining warnings:** unsupported upgrade effects and skipped legacy
destructable modifications still occur. The audit can pass despite them.
Retail-dependent tests also use local paths and may skip or return early when
data is missing. Zero reported skips alone does not prove every fixture ran.

## Implementation and limits

| Area | Present / bounded evidence | Remaining gap |
|---|---|---|
| Campaign menus | Profile-specific persistent availability, default seeding, selection, guarded loading | Verify fresh profiles, unlock order, cinematic availability, and returns in real campaigns |
| Chapter transitions | ChangeLevel routing, score Continue flow, failure recovery, archive lifetime fixes | Execute victory/defeat/retry and every branch |
| Hero gamecache | Stats, skills, name, inventory, disk persistence; retail test covers equipped Paladin and Holy Light healing | Verify actual chapter scripts, other heroes/items, and bonus transitions |
| Saved games | v5 primitive globals/arrays, resources, clock/camera; older formats readable; safer writes; restore after startup | Full battlefield, handles, triggers, timers, and execution state missing from gameplay resume |
| Entity save scaffold | Collection/serialization helpers, including pending creation/removal handling | Not wired into complete gameplay save/load; restore creates entities without preserving script identity |
| Campaign AI | Script environment, native registrations, expansion/guard/assault code and focused tests | Verify economy, production, research, attacks, defense, targeting, and timing |
| Abilities/pathing | Human skill fallbacks, legacy aura defaults, corrected equipment bonuses, isolated search state | Missing ability and upgrade behavior; broader combat/movement verification |
| Mission UI | Quests, dialogs, leaderboard/multiboard, victory/defeat interfaces | Several basic overlays; verify layout, input, hotkeys, and timing |
| Cinematics/audio | External ffmpeg movies, sky/camera support, music state/fade-in, stacked sound registry | Model playback, camera roll, fade-out, non-music volume effects, rendered/audio review |
| Options/custom campaigns | Options persistence and custom-campaign format parser | Some options lack live effects; parsing does not establish a complete launch flow |

Focused tests include `StoredUnitDataSimulationTest`, `CampaignHeroCarryoverTest`,
`CGameSaveTest`, `CampaignProgressStoreTest`, `CPathfindingProcessorTest`, and
`JassFileFailureTest`. They establish their assertions, not feature-wide parity.

## Prioritized work and acceptance gates

All gates remain open. Completion requires behavioral evidence against a stated
retail asset/version baseline.

### P0 — Real mission verification

- [ ] Execute map configuration, mission startup, triggers, timers, and AI in the
  verification harness. Record runtime errors, not just parse failures.
- [ ] Inventory maps from the archives; distinguish chapters, interludes, credits,
  and bonus submaps.
- [ ] Make retail fixture paths configurable and replace silent early returns
  with explicit unavailable-data reporting.
- [ ] Record revision, archive fingerprints, settings, seed, scenario actions,
  ticks, errors, and warnings with every run.
- [ ] Complete one opening mission through its actual victory trigger and next
  chapter before expanding coverage.

**Exit evidence:** a reproducible fresh-profile run through objectives and the
next chapter with no unexplained script errors. Retain idle checks as smoke tests.

### P0 — Gameplay blockers

- [ ] Inventory unsupported upgrade effects; implement required effects and
  verify the resulting unit behavior.
- [ ] Handle the legacy destructable modifications currently discarded.
- [ ] Identify behaviorless ability/native fallbacks reached by missions and
  implement the required effects with outcome-based tests.
- [ ] Exercise AI economy, production, research, expansion, guard allocation,
  attacks, and victory-critical interactions in a live simulation.
- [ ] Verify terrain/pathing, transports, scripted units, and special mechanics
  where the mission inventory demonstrates their use.

**Exit evidence:** affected objectives complete, required AI behavior executes,
and required gameplay data is not silently discarded. Suppressing a warning
without implementing its behavior does not satisfy the gate.

### P0 — Complete mission save/resume

- [ ] Design identity-preserving restoration before wiring entity helpers into
  gameplay. Reconnect script references without duplicating map objects.
- [ ] Restore required unit/item/destructable state, orders, abilities, buffs,
  cooldowns, upgrades, and player state.
- [ ] Restore handles/handle arrays, triggers, timers, queued and sleeping scripts,
  objectives, gamecache relationships, and AI execution state.
- [ ] Verify versioning, corruption recovery, missing assets, and failed loads
  without damaging the last usable save.
- [ ] Compare uninterrupted play with save/quit/relaunch/resume during battle,
  a sleeping trigger, a timed objective, hero inventory changes, and a transition.

**Exit evidence:** resumed missions preserve identity and objectives and remain
completable. Primitive-state restoration does not satisfy this gate.

### P1 — Progression and presentation

- [ ] Verify victory, defeat, retry, Continue, menu return, and failure recovery.
- [ ] Verify fresh-profile gating, unlock persistence after restart, profile
  switching/deletion, difficulty selection, and all campaign branches.
- [ ] Check hero carryover through actual chapter scripts and bonus revisits.
- [ ] Review menus, loading screens, quests, dialogs, scores, input, and subtitles
  in rendered runs at documented resolutions.
- [ ] Finish model cinematic/audio behavior and review movies, transmissions,
  skipping, camera restoration, and music transitions.
- [ ] Make visible options effective or clearly identify unavailable controls.

**Exit evidence:** recorded scenario results plus visual/audio review, including
skip and error cases. Asset resolution alone is insufficient.

### P2 — Coverage and release verification

- [ ] Complete every playable mission and required branch at each difficulty
  claimed as supported; review interludes and endings.
- [ ] Measure memory, resource lifetime, frame time, and stability over repeated
  chapter transitions and save/load cycles.
- [ ] Recheck the declared OS/GPU and asset-version matrix on release builds.
- [ ] Publish a revision-specific report with open deviations and artifacts;
  narrow support claims when a gate remains open.

**Exit evidence:** no unexplained progression blockers or omitted required
behavior in the declared scope. Registration and smoke checks cannot approve
full campaign parity.

## Mission result record

No completed start-to-finish campaign matrix is established by this review.
Create a row per actual scenario; never prefill success from the idle audit.

| Map / scenario | Revision + assets | Difficulty / seed | Objectives + AI | Save/resume | Exit + next chapter | Visual/audio | Evidence / defects |
|---|---|---|---|---|---|---|---|
| Not yet recorded | — | — | Unverified | Unverified | Unverified | Unverified | Add reproducible actions and logs |

Use **passed**, **failed**, **blocked**, **not run**, or **not applicable with
reason**. Attach reproduction steps to failures. Distinguish automated assertions
from manual observation.

## Reproduce the bounded checks

Use JDK 17 and the Gradle 8.6 wrapper from the repository root. On Windows,
replace `./gradlew` with `.\gradlew.bat`.

```bash
./gradlew :core:test
./gradlew :desktop:campaignSoakAudit -Pargs="--mpq C:/WC3Data/war3.mpq --mpq C:/WC3Data/War3x.mpq --mpq C:/WC3Data/War3xlocal.mpq --ticks 300"
```

Replace example paths. Audits do not derive archives from the launcher INI;
pass `--mpq` explicitly. Defaults are developer-specific `F:/WC3Data` paths.
Gradle splits forwarded arguments on whitespace, so use paths without spaces.
The soak task supports `--filter` and `--limit`; label filtered results accordingly.

Run each additional task with the same three `--mpq` arguments:

| Gradle task | Purpose |
|---|---|
| `:desktop:campaignProgressionAudit` | Discover literal chapter targets and check map resolution |
| `:desktop:campaignNativeAudit` | Generate reachable native registration report |
| `:desktop:campaignBackingAudit` | Check selected backing/loading model assets |
| `:desktop:campaignCinematicRefs` | Inspect literal cinematic asset references |

Nonzero audit exits fail Gradle. Empty discovery fails progression and soak tasks.
Inspect warnings and discovered/tested counts even on success. The soak task's
3 GB heap limit is not a measured minimum system requirement.

Core reports are in `core/build/reports/tests/test/index.html` and
`core/build/test-results/test`. For retail tests, check fixture availability as
well as the reported test counts.

## Code map and maintenance

| Responsibility | Source |
|---|---|
| Native semantics / gamecache | [Jass2.java](../core/src/com/etheller/warsmash/parsers/jass/Jass2.java) |
| Campaign AI | [JassAIEnvironment.java](../core/src/com/etheller/warsmash/parsers/jass/JassAIEnvironment.java) |
| Save representation / entity scaffold | [CGameSave.java](../core/src/com/etheller/warsmash/viewer5/handlers/w3x/simulation/CGameSave.java) |
| Hero reconstruction | [StoredUnitData.java](../core/src/com/etheller/warsmash/viewer5/handlers/w3x/simulation/StoredUnitData.java) |
| Menus / chapter loading | [MenuUI.java](../core/src/com/etheller/warsmash/viewer5/handlers/w3x/ui/MenuUI.java) |
| Startup / pending save application | [WarsmashGdxMapScreen.java](../core/src/com/etheller/warsmash/WarsmashGdxMapScreen.java) |
| Audits | [desktop tools](../desktop/src/com/etheller/warsmash/desktop/tools) |
| Commands / working directories | [desktop/build.gradle](../desktop/build.gradle) |

Update this plan when evidence or scope changes. Older completion claims in the
changelog, modernization analysis, and compatibility notes are historical context;
they do not override the campaign status here. Preserve the distinction between
implemented code and verified player-visible behavior.
