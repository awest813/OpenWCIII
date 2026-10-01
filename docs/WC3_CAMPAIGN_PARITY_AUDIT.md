# Warcraft III campaign parity: status and implementation plan

**Reviewed September 27, 2026. Status: incomplete; no full-parity sign-off.**

September 28 follow-up: the [campaign flow audit](CAMPAIGN_FLOW_AUDIT.md) now
executes retail Human01 defeat/retry and victory actions through Human02's hero
restoration, unlock persistence, and menu return in the desktop client. This is
a script-driven transition regression, not an objective-driven playthrough;
the earlier evidence and remaining campaign-wide criteria below still apply.

September 29 follow-up: the same audit also passes with objective-event
fixtures: Arthas death causes defeat, a region event updates the village
objective, partial enemy-group deaths leave victory/unlocks blocked, and the
final death drives victory and Human02 carryover. The core suite was rerun:
322 passed, zero failures/errors/skips, retail fixtures required. See the
[scenario and archive fingerprints](CAMPAIGN_FLOW_AUDIT.md#objective-event-regression--september-29-2026).
Direct positioning and kills are fixtures; complete normal playthroughs,
process-relaunch persistence, and other branches remained unverified at that stage. These
bounded results leave the campaign-wide acceptance gates below open.

September 29 persistence follow-up: hero gamecaches are now profile-scoped.
The [process-relaunch audit](CAMPAIGN_FLOW_AUDIT.md#process-relaunch-and-profile-isolation--september-29-2026)
uses five separate client launches to check two profiles' distinct carried heroes
and unlocks, then deletion/recreation with a fresh fallback hero. Legacy shared
caches are preserved and assigned once to the startup profile. This adds bounded
relaunch/isolation evidence; it does not verify normal campaign playthroughs or
complete save/resume. Victory/defeat presentation and input evidence is recorded
in the [outcome menu audit](OUTCOME_MENUS_AUDIT.md).
The strict retail-data core suite at that stage passed 329 tests with zero failures,
errors, or skips (`Logs/campaign-profile-persistence-final.txt`).

September 29 mission-save follow-up: v7 checkpoints now reconstruct the running
mission and interpreter from recorded startup and tick inputs. The
[save/resume audit](MISSION_SAVE_RESUME_AUDIT.md) covers a separate-process
Human01 continuation, resaving the loaded mission, defeat-to-load, victory and
Human02 carryover, plus engine/state mismatch rejection and menu recovery.
The strict suite at that stage passed 355 tests with zero failures/errors/skips. The combined
active-projectile/AI captain/guard/worker/RTS-camera/filter/subtitle fixture also
passes across process relaunch. The September 30 follow-up also verifies
simultaneous harvesting, construction, training and research for all four races
through fresh-process continuation. A 96-unit ranged battle in a cleared arena
also matches its uninterrupted continuation after 39 combat deaths. Larger or
combined-arms battles, normal campaign strategy, multi-town
expansion, long sessions and model/video/audio cinematic coverage remain open.

September 30 ordinary-play follow-up: the
[Human01 playthrough](CAMPAIGN_FLOW_AUDIT.md#ordinary-order-playthrough--september-30-2026)
completes its main objectives through ordinary player orders, then verifies
actual victory, both Continue screens, earned hero carryover into Human02 and
persistent unlocks. Its extended route also passes Timmy's rescue and ledger
return, then repeats the remaining objectives and transition from a fresh-process
mid-mission quest-item checkpoint. Both branches retain the earned ring,
strength tome gain, 572 XP and learned skills. Subsequent chapters, other
difficulties and full campaigns remain open.

September 30 two-chapter follow-up: the
[Human02 audit](CAMPAIGN_FLOW_AUDIT.md#human02-main-objectives-and-the-human03-transition--september-30-2026)
extends the ordinary-order chain through harvested-income construction, six
trained footmen, the Blademaster's combat death, native victory menus and the
unskipped story interlude into Human03. The carried hero has 753 XP, matching
skills/stats/name and the earned ring; profile reload preserves the interlude
and Human03 unlocks while Human04 stays locked. Captures exposed quick-position
and scripted-camera-height defects. Fresh captures verify the corrected base
view and interpolated interlude descent; broader cinematic/audio acceptance
remains open. Human02's optional quest/save-resume and later objectives remain open.
The final camera changes passed **359 strict retail-data core tests**, with zero
failures/errors/skips, and a fresh-process checkpoint continuation at matching
saved and future ticks. See the [camera render follow-up](CAMPAIGN_FLOW_AUDIT.md#camera-render-follow-up--september-30-2026)
for captures, isolated homes and the saved-state fingerprints.

Scope: RoC and TFT single-player campaigns, chapter transitions, interludes,
credits, and bonus-campaign branches. Multiplayer, replay, arbitrary custom
campaigns, and Warcraft II formats require separate plans.

September 30 Human03 follow-up: ordinary player orders completed its village
investigation, granary combat and optional fountain. Earned Arthas/Jaina state
matched restoration in Human04. The subsequent run exposed and fixed nested
trigger initialization ordering and null script order targets; it was not a
full-campaign pass. The strict core suite now passes **361 tests with zero
failures/errors/skips**, and the fresh-process checkpoint regression passes
after these changes. See the [Human03 evidence and limitations](CAMPAIGN_FLOW_AUDIT.md#human03-objectives-and-human04-initialization--september-30-2026).
Human02 optional/save-resume, Human04–Human09 completion and full campaigns
remain open.

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
| Core suite, September 27 baseline | 271 tests; 0 failures/errors/skips (retail fixtures required) | Includes latest origin updates, incremental script loading, and fog regressions; not mission playthroughs |
| Strict idle audit, latest run | 85/85 maps; 300 ticks each; 25,500 total | Object loading, checked AI parsing/initialization, idle simulation; no mission objectives, rendering, or leak measurement |
| Progression audit, earlier review | 70/70 distinct next-map targets resolved | Static target discovery and map opening; not executed transitions |
| Backing-screen audit, earlier review | 47/47 checks passed | Selected assets and model data; not visual approval |
| Native audit, earlier review | 0 reachable names missing from registration | Static reachability/registration; not correct native behavior |
| Cinematic-reference audit, earlier review | 0 missing files among references checked | Literal references and model parsing; not playback or timing |

The inventory includes interludes, credits, and bonus submaps. Counts depend on
the archives and are not a count of completed missions.

September 27 core-suite evidence: `Logs/latest-merge-regression.txt` (271 passed).
Integrated origin/main through `8472ce72`, preserving the local campaign and
rendering changes. Ownership transfer retains food counts and refreshes ability
requirements. The AI cache includes retail common.j declarations as well as
common.ai; the incoming minimal preamble omitted constants needed by campaigns.
The separate `:desktop:skyBackgroundRenderAudit` also passed: real GPU pixel
checks verify terrain covers a nearer sky mesh and sky is excluded from later
world passes. Full cinematic visual parity remains unverified.
Latest campaign smoke evidence: `Logs/campaign-ownership-vision.txt`.
Earlier combat research evidence: `Logs/campaign-combat-research.txt`.
Previous research follow-up evidence: `Logs/campaign-skirmish-research.txt`.
Earlier evidence: `Logs/campaign-regressions-final.txt`,
`Logs/campaign-strict-final.txt`, and
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
| Chapter transitions | ChangeLevel routing, score Continue flow, failure recovery, archive lifetime fixes; ordinary-order Human01 main/optional objectives and a resumed branch, plus Human02 main objectives, interlude and Human03 carryover | Execute Human02's optional quest, later objectives, other victory/defeat/retry branches and full campaigns |
| Hero gamecache | Stats, skills, name, inventory, profile-scoped disk persistence; Human01/Human02 scripts restore two profiles' distinct heroes after process relaunch and preserve the ordinary-play hero's earned XP/skills | Verify other heroes/items/campaigns, bonus transitions, and subsequent normal playthroughs |
| Saved games | v7 deterministic reconstruction with handle/VM identity, gamecache/progress baselines, input/host observations, state validation and atomic writes; Human01 separate-process continuation and chapter transition fixture, active projectiles, AI captain/guard/worker state, timed RTS camera/filter/subtitles, four-race economies and a 96-unit ranged battle in a cleared arena | Larger or combined-arms battles, multi-town expansion, long missions and model/video/audio coverage unverified; older partial saves cannot resume |
| Entity save scaffold | Legacy collection/serialization helpers remain readable and unit-tested | Gameplay resume uses replay, avoiding entity replacement and disconnected script references |
| Campaign AI | Script environment, expansion/guard/assault code and focused tests; controlled four-race harvest/build/train/research continuation, separate worker jobs, boarded wisps, mine placement, queued targets and producer validation; two 48-unit captain assault groups across save/resume | Verify normal campaign AI, expansion/reallocation, combined-arms attacks, terrain navigation, defense, targeting and timing |
| Abilities/pathing | Human skill fallbacks, legacy aura defaults, corrected equipment bonuses, isolated search state | Missing ability and upgrade behavior; broader combat/movement verification |
| Mission UI | Quests, dialogs, leaderboard/multiboard, victory/defeat interfaces | Several basic overlays; verify layout, input, hotkeys, and timing |
| Cinematics/audio | External ffmpeg movies, sky/camera support, music state/fade-in, stacked sound registry | Model playback, camera roll, fade-out, non-music volume effects, rendered/audio review |
| Options/custom campaigns | Options persistence and custom-campaign format parser | Some options lack live effects; parsing does not establish a complete launch flow |

Focused tests include `StoredUnitDataSimulationTest`, `CampaignHeroCarryoverTest`,
`CGameSaveTest`, `CampaignProgressStoreTest`, `CPathfindingProcessorTest`, and
`JassFileFailureTest`. They establish their assertions, not feature-wide parity.

Research follow-up: `HarvestUpgradeSimulationTest` exercises Human01 overrides
and standard melee object data on a small synthetic pathing grid. It verifies
Improved Lumber Harvesting, actual harvest-hit accumulation, player isolation,
newly created/trained workers, research reversal, and configured spell-level
increments. This is not a standalone skirmish map or match playthrough. The
`rlum` upgrade effect is implemented; other unsupported upgrade effects remain.
The full follow-up run passed 223 tests and 85/85 maps at 300 idle ticks each.
At that point, eight unsupported effect types still appeared: `rart`, `rasd`, `ratc`, `raud`,
`rauv`, `renw`, `rrai`, and `rspi`.

Combat research follow-up: `CombatUpgradeSimulationTest` exercises Moon Glaives
and Reinforced Defenses using Human01 overrides and standard melee object data.
It checks damaging bounce chains, research removal during an in-flight bounce,
fortified damage reduction, newly created buildings, and independent armor
overrides. The fixture launches missiles and advances their real projectile updates on a
flat headless map, without rendering, the simulation scheduler, or mission-script
execution. The `ratc` and `rart` handlers are now implemented;
this does not establish full Night Elf or Orc combat parity.
The combat follow-up passed 225 tests and 85/85 campaign smoke checks at 300
ticks each. Six unsupported upgrade effects remain in the audit output:
`rasd`, `raud`, `rauv`, `renw`, `rrai`, and `rspi`.

### Test-quality audit follow-up

The strict retail run passed 231 tests (zero failures/errors/skips), recorded in
`Logs/retail-test-audit-full.txt` and `Logs/retail-test-audit-counts.txt`.
Campaign and standard melee object-data cases now appear as separate test cases.
The shared fixture captures typed missiles and rejects unsupported nonvisual
renderer calls. It does not substitute for a played skirmish.

The projectile tests check recipients, travel completion, one-time impacts,
damage falloff, overlapping chains, and research removal while an extra bounce
is in flight. This exposed and fixed a glaive returning to its initial target.
The expected two baseline hits and 50% falloff agree with the supplied archives
and [Blizzard's classic Huntress guide](https://classic.battle.net/war3/nightelf/units/huntress.shtml),
which also excludes the initial target from later bounces. An independent
zero-distance projectile regression covers finite impact coordinates.
Armor tests compare normal, piercing, siege, and magic damage with the loaded
retail armor table and verify player isolation and unchanged armor points.

Missing archives produce explicit JUnit skips; `-PrequireRetailData=true`
turns them into failures. A selected missing-data run recorded 13 skips and five
asset-independent passes; a strict missing-data run failed all four selected
combat cases as expected (before the two overlapping-flight cases were added).
These checks prevent absent fixtures from being reported as retail coverage.

Balance expectations are version-specific: this archive set uses a +5 initial
lumber-capacity upgrade. Do not replace those data-driven expectations with
values from a different patch's guide. The synthetic Holy Light level-reversal
check exercises upgrade arithmetic, not a retail priest training scenario.
Full mission completion, AI strategy, rendered combat, and complete save/resume
remain unverified.

### Ultravision follow-up

`UltravisionSimulationTest` verifies actual fog visibility with Human01 and
standard melee object data. The initial test failed because the researched archer
could not reveal a point beyond its normal night sight; the same test now passes.
The current full suite passes 233 tests with no failures/errors/skips and retail
fixtures required (`Logs/campaign-ultravision.txt`). The same run passed all
85 campaign maps at 300 ticks each (25,500 ticks total, zero failures).

Hunter's Hall research `Reuv` has no direct upgrade effect in these archives.
Instead, the unit's `Ault` passive requires that research. The registered ability
now checks its data-defined requirements and current owner; effective sight is
used by normal and shared-unit fog. Tests cover baseline/day/night visibility,
research removal/reapplication, new units, another player, ownership transfer,
unchanged building sight, and ability disabling/removal. This follows the
[official Night Elf guide](https://classic.battle.net/war3/nightelf/) for researched
night sight. It is a headless behavior test, not a rendered match or mission.

The six unsupported upgrade-effect IDs remain unchanged: `rauv` belongs to the
separate `Rguv` campaign glyph. The `AIuv` item alias also inherits a `Reuv`
requirement in the loaded data; its intended retail item behavior needs separate
verification and is excluded from this pass's acceptance claims.

### Ownership and ability lifecycle follow-up

The previous ownership check moved a researched archer to an unresearched player
and back. A new reverse-direction check exposed a stale requirement-disable flag:
an archer originally created without research remained disabled when received by
a player who already had Ultravision. Both retail fixtures reproduced this before
the fix (`Logs/ownership-vision-before.txt`).

`CUnit.setPlayerIndex` now reconciles disabled abilities against the new owner
when ownership actually changes. The expanded test verifies actual fog in both
transfer directions, passive activation/removal callbacks exactly once, unchanged
behavior when assigning the same owner, preservation of explicit script disables,
and the new owner's player-level ability restrictions. All 233 core tests pass
with retail fixtures required and zero failures/errors/skips. Latest run evidence:
`Logs/campaign-ownership-vision.txt`. The same run passed 85/85 campaign smoke
checks at 300 ticks each (25,500 total; zero failures).

This validates ability eligibility and lifecycle on ownership transfer. It does
not establish full ownership-transfer parity for research stat bonuses, upkeep,
food accounting, or AI control.

### Campaign presentation audit

The [screens, messages, and events audit](CAMPAIGN_PRESENTATION_AUDIT.md) records
September 25 fixes for local-player outcome routing, stale/repeated Continue
actions, transmission cleanup/timing, locked chapter controls, and accurate movie
fallback messages. Nine new tests bring the strict retail-data core suite to
242 passing tests with zero failures/errors/skips. Rendered screen appearance,
localization, full score screens, and mission event sequences remain unverified.

### Menu input and dialog follow-up

The [menu audit](MENU_AUDIT.md) covers dialog callback ordering, stale responses,
wrapping Tab/Shift+Tab focus, hidden-field input, modal keyboard capture, scoped
Escape navigation, and shared disabled/hidden button guards. Six additional
regressions bring the strict retail-fixture suite to 248 passing tests with no
failures/errors/skips. Live menu layout and input walkthroughs remain required.

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
- [x] Complete one opening mission through its actual victory trigger and next
  chapter before expanding coverage.

**Exit evidence:** a reproducible fresh-profile run through objectives and the
next chapter with no unexplained script errors. Retain idle checks as smoke tests.

### P0 — Gameplay blockers

- [ ] Inventory unsupported upgrade effects; implement required effects and
  verify the resulting unit behavior.
- [ ] Handle the legacy destructable modifications currently discarded.
- [ ] Identify behaviorless ability/native fallbacks reached by missions and
  implement the required effects with outcome-based tests.
- [x] Verify controlled harvesting, construction, training and research for all
  four races, plus captain/guard state and a 96-unit ranged battle, across
  fresh-process mission continuation.
- [ ] Exercise normal campaign AI, expansion, dynamic worker/guard allocation,
  combined-arms attacks, terrain navigation and other victory-critical interactions.
- [ ] Verify terrain/pathing, transports, scripted units, and special mechanics
  where the mission inventory demonstrates their use.

**Exit evidence:** affected objectives complete, required AI behavior executes,
and required gameplay data is not silently discarded. Suppressing a warning
without implementing its behavior does not satisfy the gate.

### P0 — Complete mission save/resume

- [x] Implement identity-preserving reconstruction through recorded startup and
  simulation inputs, avoiding duplicate entities and disconnected script handles.
- [ ] Restore required unit/item/destructable state, orders, abilities, buffs,
  cooldowns, upgrades, and player state.
- [ ] Restore handles/handle arrays, triggers, timers, queued and sleeping scripts,
  objectives, gamecache relationships, and AI execution state.
- [ ] Verify versioning, corruption recovery, missing assets, and failed loads
  without damaging the last usable save.
- [ ] Compare uninterrupted play with save/quit/relaunch/resume during battle,
  a sleeping trigger, a timed objective, hero inventory changes, and a transition.

The bounded retail checkpoint evidence and remaining limitations are recorded in
[MISSION_SAVE_RESUME_AUDIT.md](MISSION_SAVE_RESUME_AUDIT.md). The Human01 fixture
exercises a sleeping trigger, timer, dialog input, inventory, orders and transition;
broad campaign save/resume parity remains an open verification gate.

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
./gradlew :core:test -PretailAssets=F:/WC3Data -PrequireRetailData=true
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
`core/build/test-results/test`. Retail validation should use the strict fixture flag above and verify zero
skips as well as the reported test counts.

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
