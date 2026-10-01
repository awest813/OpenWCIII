# Campaign transition audit

September 28, 2026. Windows, Java 17, Gradle 8.6, classic RoC/TFT MPQs.

## Executed flow

`./gradlew :desktop:campaignFlowRenderAudit` opens the real desktop client with
an isolated, fresh preferences/gamecache directory. It loads Human01 normally,
waits for map initialization, and executes these steps:

1. Verify the local campaign player and a locked Human02 on the fresh profile.
2. Invoke the retail `Trig_Defeat_Cheat_Actions` function and select its Reduce
   Difficulty choice. Confirm the restart using its Restart hotkey.
3. Verify a new Human01 viewer loads at Normal difficulty while retaining Hard
   as the originally selected difficulty.
4. Give Arthas a distinctive name, level, learned Holy Light, and a three-charge
   potion in slot 4. Invoke the retail `Trig_Next_Level_Prep_Actions` and
   `Trig_Next_Level_Run_Actions` functions.
5. Verify the next mission unlock, click the retail victory dialog's Continue,
   and use the engine score dialog's Continue hotkey.
6. Wait for Human02's real initialization and hero restoration. Verify carried
   experience, skill points, name, base strength, learned skill, inventory slot
   and charges, plus restoration of the selected Hard difficulty.
7. Reload the profile and verify the Human02 unlock persists while the next
   unfinished chapter remains locked. Return to the menu.

All eight stage markers and the completion marker were reached. Output is saved
to `desktop/build/campaign-flow.log`. The task rejects a missing completion
marker or GL error and has a three-minute process limit. Each run gets a fresh
`desktop/build/campaign-flow-home-*` directory; personal saves are not used.
Configure `core/assets/warsmash.ini` before running, as for the menu/map audits.

## Objective-event regression — September 29, 2026

Run `./gradlew :desktop:campaignFlowRenderAudit -PcampaignObjectiveEvents=true`
to replace direct completion-action invocation with world-event fixtures:

1. Skip the opening cinematic through Escape, then kill Arthas through the
   simulation. The registered unit-death trigger must enter the retail defeat
   flow; defeat must leave Human02 locked.
2. Reduce difficulty and retry through the same dialogs as the original flow.
   The new viewer must contain a living Arthas, an unfinished travel objective,
   and cleared game-over state, while preserving current and selected difficulty.
3. Create the distinctive hero/inventory fixture, then move the Warlord into
   his retail return region. The region trigger must queue and complete the
   village travel objective and create the slay objective.
4. Kill all but one member of the retail Warlord-and-crew group. After eight
   simulation ticks, game-over must remain false and Human02 must remain locked.
5. Kill the final member. The player-unit-death event must satisfy the retail
   victory condition, run hero storage/unlock actions, and complete both the
   slay and surviving-Arthas objectives. Skip the victory cinematic through Escape.
6. Continue through both victory dialogs and verify Human02 restoration,
   difficulty, profile reload, unfinished-chapter gating, and menu return.

This mode passed all eight stage markers, both objective markers, and the
`objective events complete` marker. The task writes
`desktop/build/campaign-objective-events.log`; the original mode writes
`desktop/build/campaign-flow.log`. Both reject JASS exceptions, GL errors, and
missing completion markers. The core suite was rerun with retail fixtures
required: **322 tests, zero failures/errors/skips**. Local command evidence is
`Logs/campaign-objective-events-verification.txt`.

Environment: Windows, Java 17.0.20, Gradle 8.6, local changes over
`6af407c56f860cd22a127e07d781b4fceed148f4`; safe profile, 960×540, 30 FPS,
audio disabled, fresh isolated preferences for each run. Archive SHA-256:

| Archive | SHA-256 |
|---|---|
| `war3.mpq` | `00DC676FB5CA3E93BA85839D60155EA8877F2F32A2BFF1612440D8065F5B5626` |
| `War3x.mpq` | `53CF10384AB9C40B52156EF5145D3397222E3158BE3A8E816D0246FE17535538` |
| `War3xlocal.mpq` | `80D7918CBFE713E6614716C4A6875ABBB048C5757F4351885236DA241AA9E816` |

The scenario deliberately uses direct simulation positioning and kills. It
verifies event dispatch, objective conditions, and progression together, but
does not verify travel pathfinding, combat balance, optional quests, or a normal
start-to-finish mission or campaign playthrough. Difficulty is set by the probe
after initial startup, so this run does not establish Hard-mode startup behavior.
Profile reload in this scenario is within the running process. The separate
process-relaunch/profile-isolation follow-up below adds that bounded check.
Existing missing-asset warnings remain in the local log.

## Fixes

- Retry previously reloaded the menu-selected difficulty, discarding the retail
  defeat dialog's reduction. Transitions now snapshot both current and selected
  difficulty and apply them around map configuration.
- `GetDefaultDifficulty` previously returned the mutable current difficulty.
  It now returns the original selection, allowing the retail victory routine to
  restore it before entering the next chapter.
- Known campaign entries previously inherited the permissive unknown-mission
  fallback. The menu now seeds the first mission/opening as available and later
  missions/ending as locked without overriding saved script values.
- The in-game Restart button was disabled. It now uses the reload path with
  restart presentation instead of a Victory heading.
- Dialog hotkeys previously invoked JASS event dispatch directly, bypassing the
  native score Continue callback. They now activate the same guarded button as
  mouse clicks.
- Configuration-only map archives now close after the configuration is read.

Two focused tests cover difficulty snapshots and fresh-profile gating/unlock
persistence. The live flow exercises the real JASS, menus, map reload, and disk
gamecache path together. Existing single-use continuation tests remain in place.

## Limits

The original mode invokes retail completion actions; the objective-event mode
reaches victory through world-event fixtures. Those modes do not prove that
Human01's victory conditions can be reached through normal play. The September
30 ordinary-order mode below adds that evidence for Human01's main objectives.
The campaign-wide audit
does not certify other campaigns, interludes, bonus branches, full save/resume,
other profile/campaign combinations, long-session resource use, or audiovisual
parity. Existing missing-asset warnings are still present in the local log.

The older `campaignProgressionAudit` remains a static map-reference and native
call-site inventory. Its description now states that scope explicitly.

The September 29 [victory/defeat menu follow-up](OUTCOME_MENUS_AUDIT.md) adds
proper outcome routing, result choices, modal input, and restart cancellation.
The flow probe now issues each cinematic skip once and waits for the actual
engine result screen before continuing.

## Process relaunch and profile isolation — September 29, 2026

`./gradlew :desktop:campaignPersistenceRenderAudit` uses five separate desktop
JVMs sharing a fresh isolated home. It drives the real profile manager through
an opt-in probe and uses retail chapter scripts to store and restore heroes:

1. WorldEdit completes the objective-event Human01 flow, storing a named level-3
   hero with learned Holy Light and a three-charge potion in inventory slot 4.
2. A second profile runs the completion-action flow with a differently named
   hero; it must start with Human02 locked and its own empty gamecache.
3. Relaunch into Human02 as WorldEdit. Verify its original hero name, level,
   skill, item slot/charges, Human02 unlock, and unfinished Human03 lock.
4. Relaunch into Human02 as the second profile. Verify its distinct hero and
   the same independent unlock/lock conditions.
5. Delete and recreate the second profile, then launch Human02 directly as a
   fixture. It must retain locked Human02/Human03 and restore the map's default
   Arthas, without either saved fixture hero or potion.

The direct launches deliberately bypass chapter selection to exercise the cache
fallback even for a locked mission. This is not a profile-picker mouse walkthrough
or normal mission playthrough. The original objective-event scenario's direct
positioning/kills and other limitations still apply. Difficulty persistence
across process relaunch, arbitrary profiles/campaigns, and full mission save/resume
are outside this check.

Previously, JASS gamecaches used one shared `.warsmash/gamecache` directory.
They now use `.warsmash/gamecache/profiles/p-<SHA-256 of profile name>`; an
environment captures its selected profile directory when the map is initialized.
Fixed-length lowercase identifiers avoid invalid path characters and case-only
aliases on Windows. Existing shared files are assigned once to the profile
selected at startup, before profile switching is possible, and copied without
overwriting newer scoped caches. Original files remain intact. Completed-copy
installation prevents an interrupted copy from becoming the destination cache.
A damaged ownership record stops migration rather than silently assigning data
to another profile.

Deleting a profile retires its scoped directory under `gamecache/retired` and
removes its availability preferences. Recreating the name starts empty;
legacy files cannot be reimported into that recreated profile. The retired
directory and original shared files remain available for manual recovery.

The task records `desktop/build/campaign-persistence-{phase}.log`; each client
has a three-minute process limit and must emit its completion marker without
JASS exceptions or GL errors. Initial local evidence is
`Logs/campaign-profile-persistence.txt`; final evidence is
`Logs/campaign-profile-persistence-final.txt`. Six focused filesystem regressions
cover independent cache reads, legacy migration/retry, preservation of newer data,
deletion/recreation, bounded identifiers, and damaged ownership records.
The strict retail-data core suite passed **329 tests, zero failures/errors/skips**.
The outcome-menu render audit also passed after the profile-cache change.

## Ordinary-order playthrough — September 30, 2026

`./gradlew :desktop:campaignPlaythroughAudit` now completes Human01's main
objectives in the real desktop client at the default Normal difficulty. Its
opt-in driver uses ordinary validated move, attack, hero-skill and spell orders,
plus Escape to skip the opening, Orc and victory cinematics. It does not create
units/items, move units directly, alter health/experience, or invoke completion
actions. The route recruits the retail rescue footmen and fights the existing
bandits and Orcs, using Holy Light and Divine Shield through player orders.

The passed run verifies:

1. Human02 starts locked in the fresh profile.
2. Walking and the retail Warlord sequence complete the travel objective.
3. Combat kills every member of the three-unit Warlord-and-crew group while
   Arthas survives. The actual game-over/victory sequence completes the slay
   and survival objectives. Arthas has **389 earned XP** before the transition.
4. Victory unlocks Human02, and both the retail dialog Continue and score-screen
   Continue load the next chapter.
5. Human02 restores the earned XP, skill points, proper name, base strength,
   learned ability IDs/levels and inventory slots/charges. This route does not
   collect items; nonempty inventory carryover remains covered by the separate
   transition and persistence fixtures.
6. Reloading the profile retains the Human02 unlock while Human03 stays locked.

The run completed in **4 minutes 50 seconds** at 960×540 with a 30 FPS cap and
audio disabled. Its log is `desktop/build/campaign-playthrough.log`; its fresh
home is `desktop/build/campaign-playthrough-home-1790774430849`. The task requires
the completion marker and rejects JASS exceptions and GL errors. Missing-asset
warnings remain in the log, as in the earlier desktop audits.

This initial run verified one chapter's main-objective path and transition.
Optional quests and saving during ordinary play were added in the follow-up below.
Human02's objectives, other difficulties/campaigns, uninterrupted cinematic and
audio playback remain unverified.
The separate [checkpoint audit](MISSION_SAVE_RESUME_AUDIT.md) covers controlled
save/resume scenarios. Full campaign playthrough acceptance remains open.

The extended ordinary-order route and fresh-process checkpoint can be run with:

```sh
./gradlew :desktop:campaignPlaythroughResumeAudit
```

This task runs an uninterrupted reference through Human01's main and optional
objectives and Human02, then starts another desktop JVM from a Quick Save taken
while Arthas carries the quest ledger. Both processes use the ordinary order
driver. A sidecar restores the driver's route, input timing and verification baseline;
the save journal reconstructs the mission's units, inventory, quests and scripts.
The driver respects disabled user control during scripted scenes.

## Optional quests and ordinary-play save/resume — September 30, 2026

The extended task passed both branches with a fresh isolated home at
`desktop/build/campaign-playthrough-resume-home-1790793413189`. The complete
two-process audit took **13 minutes 52 seconds** with the same retail MPQs,
default Normal difficulty, 960×540, 30 FPS cap and disabled audio.

1. Walking to Gerard and Alicia discovers the ledger and Timmy quests.
2. Combat defeats the existing gnolls. The retail rescue script frees Timmy,
   completes his quest and awards the **Ring of Protection (`rde0`)**.
3. Road waypoints lead to the bandit camp. Combat defeats Menag; the ordinary
   inventory smart order picks up his dropped ledger.
4. The real menu Save button creates Quick Save at tick **5,601**, with Timmy's
   quest complete, the ledger quest unfinished, and the ring and ledger in
   inventory slots 0 and 1. Checkpoint XP is **328**; learned skills are Holy
   Light level 1 and Divine Shield level 1. A fresh JVM reconstructs that exact
   tick and validates the full saved gameplay/VM/AI digest:
   `3f1547418b6a1aff3784106d9bb5dd8f36711d8096eae1b07f44111645080202`.
5. Both branches walk back to Gerard. The quest removes the ledger and consumes
   the strength tome exactly once: stored permanent base strength changes from
   **22 to 23**, and the tome is absent from inventory.
6. The main-objective fights reach the actual victory trigger. Both branches
   finish with **572 XP**, Holy Light level 2, Divine Shield level 1, matching
   skill points, name, all stored base/bonus attributes, and inventory slots and
   charges. The earned ring remains in slot 0; the ledger is absent.
7. Both Continue screens load Human02. The full recorded hero snapshot matches
   there, and profile reload retains Human02's unlock while Human03 stays locked.

Logs are `desktop/build/campaign-playthrough-write.log` and
`desktop/build/campaign-playthrough-read.log`. Checkpoint/driver/outcome evidence
is under the isolated home's `playthrough/` directory. Missing-asset warnings
remain; neither branch reported a JASS exception or GL error. The strict retail
core suite was rerun: **355 passed, zero failures/errors/skips**.

This establishes Human01's main and optional objective paths, an inventory/quest
checkpoint during ordinary play, and the earned Human02 transition. The saved
runtime digest is exact; later comparison checks the quest outcomes and carried
hero snapshot rather than a full future-world digest. Human02's main objectives
are covered by the following audit. Other difficulties and campaigns,
long-session repeated saves, and full rendered cinematic/audio parity remain open.

## Human02 main objectives and the Human03 transition — September 30, 2026

Run the two-chapter route with:

```sh
./gradlew :desktop:human02PlaythroughAudit -PcampaignHuman02Chain=true
```

Without the property, the task directly launches Human02 with its retail fallback
hero in a fresh isolated profile. The chain option starts at Human01 and includes
its main and optional objectives before handing control to the Human02 driver.
Both drivers use validated player orders and real menu/cinematic input. They do
not create units, grant resources, change health/XP, execute quest triggers, or
teleport units. Injured Arthas retreats during Divine Shield's cooldown, and
higher-level Holy Light waits for larger injuries to conserve mana.

The completed chain used default Normal difficulty, the configured retail MPQs,
960×540, a 30 FPS cap and disabled audio. Evidence is in
`desktop/build/human01-human03-playthrough.log` and the isolated home
`desktop/build/human02-playthrough-home-1790807935043`.

1. Human01 completes both optional quests and the actual main victory. Human02
   receives **572 XP**, Holy Light level 2, Divine Shield level 1, the earned
   ring, and the permanent strength increase from 22 to 23.
2. Human02's peasants harvest actual resources. Valid construction orders build
   one barracks and two farms on the existing terrain; training orders produce
   six footmen. Native construction/training events complete the base quest.
   The retail Orc AI and Uther's defense scripts remain enabled.
3. The driver skips the native Blademaster introduction, respects disabled
   control during scripted scenes, and walks the army through real combat to
   the existing Blademaster. His death completes the second main quest. Both
   main quests, two completed farms and six trained footmen are asserted before
   victory input advances the chapter.
4. The native victory dialog and score Continue lead to
   **Human02Interlude — Jaina's Meeting**. The story script runs to its own end
   without Escape input, then loads Human03.
5. Human03 restores **753 XP**, zero unspent skill points, the same learned
   ability levels, name, all stored base/bonus attributes, and inventory
   slots/charges. The Ring of Protection (`rde0:0`) remains in slot 0; the ledger
   and consumed strength tome are absent. The two hero properties files under
   `human02/` match semantically.
6. Profile reload retains the interlude and Human03 unlocks while Human04 stays
   locked. Retail `UI/CampaignStrings.txt` includes interludes among mission
   entries: indices 2, 3 and 4 are Human02Interlude, Human03 and Human04. The Human
   campaign has 12 entries, including nine playable chapters and three interludes.

The chain produced its completion marker and all three captures in **18 minutes
30 seconds**. A capture-filename interpolation error in Gradle's post-run check
was corrected; `:desktop:human02PlaythroughReportAudit` then passed against the
retained log, PNGs and matching hero properties. No JASS exception or GL error
was reported; missing-asset warnings remain.

Visual inspection exposed two camera defects: a quick-position bookmark moved
the live camera into fogged terrain, and custom camera setups ignored their
height field, putting the opening interlude shot below terrain. These prompted
the camera fixes and fresh render verification recorded below.

This proves the two main-objective chapter paths, Human01's optional rewards,
and the actual interlude/Human03 transition. Human02's Searinox optional quest,
Human02 save/resume, later objectives, other difficulties/campaigns and full
cinematic/audio acceptance remain open.

## Camera render follow-up — September 30, 2026

`SetCameraQuickPosition` now stores the script's Space-key bookmark without
moving the live camera or cancelling an active pan. Space jumps to the bookmark
when ordinary camera input is enabled. Camera setups now apply their authored
height and interpolate it over forced-duration transitions, including reset to
the game camera. The bookmark and the current/pending setup-height transition
are included in the presentation checkpoint.

A fresh direct Human02 run passed in **7 minutes 54 seconds**, with evidence in
`desktop/build/human02-playthrough.log` and
`desktop/build/human02-playthrough-home-1790809453956`. Its inspected
`human02/base-objectives.png` shows the completed barracks, two farms and army
in the visible base. Both main objectives, Blademaster combat, victory menus,
unskipped interlude, Human03 carryover and persisted unlocks passed. This run
uses the retail fallback hero; its final **397 XP** is separate from the full
Human01 chain's 753 XP.

The final height-interpolation fix was verified with:

```sh
./gradlew :desktop:human02InterludeRenderAudit :core:test :desktop:missionResumeRenderAudit -PrequireRetailData=true
```

This combined command passed in **2 minutes 47 seconds**. The standalone
interlude runs without Escape and advances to Human03 through its native
script. Evidence is in `desktop/build/human02-interlude.log` and
`desktop/build/human02-interlude-home-1790810468173`. Both captures were inspected:
`human02/interlude.png` shows Antonidas, Medivh and the Dalaran floor above
terrain during the low-angle shot; `human02/human03.png` shows Arthas and the
footmen at the opening campfire. The standalone task checks rendering and the
transition; hero carryover and unlock assertions belong to the full mission
audit above.

The strict core suite passed **359 tests, zero failures/errors/skips**. Four new
camera regressions cover a bookmark during an active pan, bookmark restoration,
the authored low-angle height/reset, and uninterrupted versus resumed height
descent. The last test checks the camera eye stays above terrain throughout the
transition and both continuations reach the same height.

The final fresh-process checkpoint audit used
`desktop/build/mission-resume-home-1790810468208`. The saved tick **186** matched
`a5d45c93e3cfee30b20d1f73eb5ecfe7c3e77b9e03e41b10f38ad269dab72c62`;
both branches reached tick **426** with
`f927341ae2e563fdcfd7f3eeb73786c6f8edad5b3ca2443936f478dacc4ab55e`.
Quick Save, native SaveGame, resave, defeat-to-load, victory and Human02 carryover
also passed. These fingerprints describe the final compiled camera state;
earlier checkpoints require their matching binaries.

This is bounded visual verification of the base and captured story shots.
Full cinematic model/audio playback, other camera sequences and the campaign
gaps listed above remain open.

## Human03 objectives and Human04 initialization — September 30, 2026

The opt-in `HumanCampaignPlaythroughProbe` completed Human03's village
investigation, granary destruction through combat, and optional Fountain of
Health through validated ordinary player orders. It used the retail fallback
heroes for a direct Human03 start. Arthas finished with **809 XP**, Holy Light,
Divine Shield and Devotion Aura at level 1, and a greater healing potion in
slot 0. Both Arthas and Jaina's saved properties exactly matched their restored
properties in Human04. The victory capture was inspected and shows the
destroyed warehouse and native main-quest completion message.

Evidence:

- `desktop/build/human-campaign-human03-human05.log`
- `desktop/build/human-campaign-home-1790814909933/human-campaign/`
  (`human03-victory.png`, both `human03-*-victory.properties`, and both
  `human04-*-restored.properties`).

This is a verified Human03 segment, **not a successful Human03–Human05 Gradle
run**. The original run reached Human04 and then stalled; it was stopped during
diagnosis. Human04's opening read a null Jaina because explicit `TriggerExecute`
calls queued their nested hero loaders after the opening. Explicit trigger
actions now execute before the caller continues, until their first sleep;
sleeping actions remain scheduled for later continuation. Script target orders
with null targets return false rather than dereferencing a missing widget.

JASS exceptions previously went only to separate `core/assets/Logs/*.jass.log`
files. They now also reach stderr, where desktop audits can reject them.
Long mission audits retain a private runtime copy; the optional writer and
reader share that copy. A prior live run failed when a concurrent rebuild
replaced its jar. That failed run supplied no save/resume completion evidence.

The strict command `:core:test -PrequireRetailData=true --offline` passed
**361 tests, zero failures/errors/skips**. The two new regressions verify nested
initialization order and continuation after an action sleeps without repeating
its prefix. A fresh `:desktop:missionResumeRenderAudit --offline` also passed in
**1 minute 13 seconds**, using
`desktop/build/mission-resume-home-1790816944372`: saved tick **221**, state
`3887d0a4570206a9956823c41d687efa226476725dec28fcf557c1cc41751f95`;
both continuations matched tick **461**, state
`3c7d562c2375a8e3eb9c38c8763523658a0475a750cf045abaa971f3df0696e1`.
Resave, defeat-to-load, victory and Human02 carryover also passed in that bounded
fixture.

Human04's corrected opening, base creation, harvested-income construction and
18-unit army were observed in the next run. Complete Human04–Human09 objectives,
the later story interludes, final campaign victory/unlocks and a single complete
Human01–Human09 chain remain unverified. Later chapter drivers are opt-in audit
work in progress, not acceptance evidence.

### Requested stopping point

The user requested a stopping point before the remaining campaign was complete.
All live audit processes were stopped or allowed to finish; no later full run
was started after that request. Retained incomplete attempts are:

- Human04: `desktop/build/human-campaign-human04-human06.log`, home
  `desktop/build/human-campaign-home-1790816696196`. The corrected opening,
  economy and 18-unit army were observed without JASS exceptions. The assault
  reached waypoint 4 with seven surviving troops; the run was stopped before
  Andorhal/Kel'Thuzad victory. `human-campaign/human04-progress.png` is retained.
- Human02 optional: `desktop/build/human02-optional-write.log`, home
  `desktop/build/human02-resume-home-1790816741326`. The main base objective,
  riflemen recruitment and ground approach to the dragons succeeded. Arthas
  died in ordinary dragon combat before Searinox was defeated and before a
  quest-item Quick Save. The Gradle task failed; its reader did not run. Improve
  air-combat positioning and protection before retrying `human02OptionalResumeAudit`.
- Human06: `desktop/build/human-campaign-human06-complete.log`, home
  `desktop/build/human-campaign-home-1790816803712`. The city approach, house
  destruction and three native denial-counter increments were observed. This
  exploratory run was stopped; 100 denials, Mal'Ganis competition, victory and
  the later chapters are unverified.

Resume with Human02 optional combat and Human04's complete objectives, then
verify Human05–Human09 and their interludes/carryover. The final acceptance gate
is one uninterrupted Human01–Human09 campaign with native final victory and
persisted Undead unlock. None of these incomplete attempts satisfies that gate.
