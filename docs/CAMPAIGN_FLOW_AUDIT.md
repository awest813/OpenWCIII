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

### Human02 quest checkpoint and fresh-process continuation — November 23, 2026

The audit kept Arthas and the footmen outside the dragon's attack range while
the riflemen fought through ordinary attack orders. Arthas picked up the heart
and the in-game menu wrote a Quick Save at tick 6,573. Both the uninterrupted
run and the fresh-process continuation returned the heart, earned the Orb of
Fire, completed the main objectives, and passed through the story interlude
into Human03. The saved, resumed, and carried hero checks covered the quest
item state and matched the final hero properties, including 513 XP.

The report checker requires the construction screenshot from the uninterrupted
run. For the resumed run it requires the checkpoint restoration marker and
compares the resumed hero with the retained uninterrupted result. The
interlude and Human03 screenshots are required in both runs. Both retained
reports passed. Reproduce the two runs with
`./gradlew :desktop:human02OptionalResumeAudit`.

### Undead01 opening and unit rescue — November 23, 2026

The Normal difficulty setup uses rectangles to remove extra starting units.
The spatial index's collision bounds were enough to return nearby ghouls
outside those rectangles, so the intro later tried to rescue removed units.
Scripted rectangle enumeration now checks the unit position. The pause and
facing natives also accept the empty handles for units omitted by difficulty.

A 50-second desktop run let the intro finish without skipping. The log showed
two live, visible ghouls transferred to the local player and no JASS or OpenGL
errors. This verifies the opening rescue on Normal difficulty. The remaining
mission objectives and other difficulty paths stay open.

### Human04 reinforcement and retreat — November 23, 2026

The desktop audit completed the Blacksmith and Arcane Sanctum through normal
build orders, then advanced with six riflemen, two priests, and a mortar team.
Arthas survived the first assault and retreated to the rally point. He returned
at full health and reached waypoint 4 before an audit-only null behavior access
ended the run at tick 25,325. The driver now handles units between behaviors.
The retry passed that tick and reached waypoint 4 with 23 live army units.

The retail Priest already had an Ability Builder Heal definition, but the
classic spell row leaves its target flags blank. Heal now uses the documented
friendly, organic, non-Ancient defaults for blank rows. The audit also issues
ordinary Heal orders to support its force. A retail-data regression test checks
manual healing, mana use, enemy and mechanical target rejection, and the
autocast toggle and Hold Position behavior. An attack-move test checks that
the Priest heals an ally and continues to the ordered destination. The Human04
victory and Human05 carryover checks remain pending.
[Blizzard's Priest guide](https://classic.battle.net/war3/human/units/priest.shtml)

### Chapter-specific AI preambles — November 23, 2026

Campaign AI now reuses parsed `common.j` and `common.ai` definitions only while
the same mission data source is active. A test starts AI against two distinct
chapter sources in the same process, then returns to the first. Each instance
executes its own preamble and produces the chapter-specific result.

### Campaign AI commands — November 23, 2026

The retail script inventory found 302 `CommandAI` call sites, including signals
in Human04 and coordinate and action commands in Human06. `CommandAI` now sends
the integer command and data to the addressed AI. AI scripts can count, peek,
and pop their pending commands, newest first. Checkpoint validation includes
the pending commands. The regression test checks the values after delivering
the same commands to two fresh simulations and consuming them in order.

### Human04 investigation and accelerated verification

`humanCampaignPlaythroughAudit` accepts `-PhumanCampaignTicksPerFrame=4` for
an isolated run with four fixed simulation ticks per rendered frame. The
default uses real-time updates. Use the default for presentation timing
and use the accelerated run to check objectives, score screens, and transitions.
To continue an isolated run from an earned chapter, pass
`-PhumanCampaignCache=<previous-audit-home>/.warsmash/gamecache` with the
next chapter as `humanCampaignStart`. The audit copies the cache into its
new profile before launching.

The first accelerated run fired the native Kel'Thuzad death trigger before
the Andorhal investigation had completed. The audit now guides the army
into the map's investigation region and keeps Kel'Thuzad out of its attack
orders until that quest completes. The ending replaces the slain unit with
a cinematic actor. The audit checks the replacement and the investigation
before proceeding to the Human05 carryover checks.

Both the real-time run and the corrected accelerated run completed
Investigate Andorhal, defeated Kel'Thuzad, and loaded Human05. The retained
victory screenshots and hero snapshots accompany both runs. The original
carryover comparison failed on Jaina because Human05 raises her to level six
and grants Mass Teleport after restoring her from the Human04 game cache.
The verifier now compares her data at `RestoreUnit` and checks the scripted
upgrade separately.

The corrected audit passed Human04 through Human05. The two Jaina snapshots
matched on every stored property before Human05's hero setup. After setup,
Jaina had 3,000 experience and Mass Teleport level one. Arthas's carried
properties matched his Human04 victory snapshot. The run retained a Human05
arrival screenshot and its earned-hero game cache for the next mission audit.

The first Human05 objective run used a copy of that cache. Its startup
aborted before the defense timer when the map tried to position an empty
Jaina inventory slot. `SetItemPosition` now accepts empty handles and uses
the normal inventory drop path for held items, preserving both the drop and
the following quest setup. The next run reached Defend Hearthglen with no JASS
exceptions. Arthas later died during the defense. The next audit reserves
resources for towers and uses ordinary repair orders on the Keep.

The Human05 timer dialog now uses the archive's `TimerDialogValue` frame and
updates from the mission timer while visible. The 960×540 desktop audit
captured **Uther's Return** at **29:40**, below the resource bar. Destroying
the dialog removes it from the UI update list. A later perimeter run reached
the end of the 30-minute timer with Arthas at full health. Its army was down
to one unit when the grain caravan objective appeared, and the audit stopped
at the optional objective check before the victory sequence. The next run
prioritized the first barracks and trained infantry while fortifying. Its
interception force stalled before reaching the active wagon, and long path
searches repeatedly hit the 20,000-iteration cap. A pathfinding regression
test reproduces the failure with a reachable detour on a 256×256-cell grid.
The revised search finds that route in 34,842 iterations while retaining the
per-update work cap. With the revised search, the retail Human05 audit
destroyed the caravan through combat, completed the defense, and loaded
Human06. The Human06 cache restore matched Arthas's Human05 victory
snapshot. The run retained the earned game cache and chapter arrival image.

In The Culling, the native mission counter advanced while the visible
leaderboard stayed at zero. The classic `LeaderboardGetPlayerIndex` takes
a player handle and returns that player's row. The engine now resolves that
row before updating the score. A rendered run from the earned Hearthglen
cache showed Arthas's first four points on the leaderboard.

The earned-cache Culling run reached the native 100-point objective and
displayed Victory. The audit had been waiting on the defeat-side game-over
flag. It now checks the victory cinematic and uses the score dialog's
Continue action. The corrected run completed the native victory, played
the interlude, and loaded Human07 with an exact Arthas carryover match.
The opponent's conversion count remained at zero in that run. The engine
now reads the spell's target race and replacement unit from the installed
map data, and computer-controlled casters can issue the normal order.
Regression tests cover scripted and automatic orders, the sleeping state,
and the resulting summon event. A desktop replay recorded 10 native
opponent conversions during ordinary play. The focused test also verifies
the five-second casting delay for the opponent and the zero-delay
scripted variant.

In a contested run, two nearby destinations in The Culling exhausted the
old short-distance path budget because the road detoured around the city.
The updated limit is based on grid size. Both targets resolve on the
installed pathing grid, and a regression test covers a long detour to a
nearby destination. The next contested replay passed the stalled district
and reached 91 denials and 99 opponent conversions. The audit now prioritizes
an ordinary attack on the opposing hero when he is within reach. With the
casting delay applied, the complete replay won 100–35 and again verified
Arthas's carryover into Human07.

The Chapter Seven audit followed a route around the ridge that the installed
pathing grid confirmed. It established the town hall, rescued the dwarves,
and destroyed the undead citadel. The verifier withdrew Arthas when wounded
while the army kept attacking. The game loaded Human08 and matched Arthas
and Muradin against their Human07 victory snapshots.
The same handoff passed again with the cache earned by the Human01–Human06
run, after correcting the audit's recruitment budget and retreat orders.

The first Chapter Eight run targeted a ship's position offshore and stalled
before reaching the coast. The audit now moves to reachable land within
range of each ship, in the order the paths connect. It can also hire nearby
mercenaries through the camps' normal purchase orders.
The food display went negative when hired mercenaries died. Hiring now
updates the player's used-food total through the same path as training.
A regression test buys a unit from the installed chapter's camp, checks the
food increase, and checks that its death restores the previous total.

The chapter's hint calls for Goblin Sappers to clear the trees in the canyon.
Kaboom now applies its configured blast damage to eligible destructibles.
Blank target flags use the usual ground, structure, and tree targets.
[Blizzard's Goblin Sapper guide](https://classic.battle.net/war3/neutral/goblins.shtml)
A regression test issues the normal order on an installed tree, checks
damage to nearby trees, and checks that a tree outside the radius survives.
A desktop replay destroyed a tree from the laboratory ledge. The cliff
still blocked passage. The map's pathing grid and destructible footprints
showed a route through the lower grove after three blasts. Staging the
idle Sappers outside each blast radius cleared the route. The replay
crossed the canyon, hired the northern camp's mercenaries, and destroyed
the third ship. Arthas fell while approaching the fourth. The installed
Troll Priest and High Priest now use their shared original Heal behavior
and their own amounts. The next replay bought the available northern
reinforcements, destroyed all five ships, and continued through the native
victory screen. Human09 restored Arthas and Muradin with exact matches for
their stored attributes, skills, experience, and inventories. The strict
retail-data suite passed 383 tests with zero failures, errors, or skips.

The Ogre Magi's Bloodlust now reads the installed ability's attack speed,
movement speed, size, duration, and cooldown. The chapter's blank target
field defaults to friendly organic units that are vulnerable to magic.
The test casts, recasts, expires, and autocasts the ability on an ally
fighting an enemy. [Blizzard's Shaman guide](https://classic.battle.net/war3/ORC/units/shaman.shtml)

The first finale pilot sent base troops through the valley with Arthas and
lost the base. The next pilot uses the map's designated valley party and
leaves the other units at the base. The engine now handles the scripted
attack orders used by one of the finale's AI players. The pilot claimed
Frostmourne and defeated Mal'Ganis, then lost Arthas before clearing the
base. A later pilot held a garrison at the Castle, issued repairs, and
destroyed several Undead production structures. It retreated with
14 combat units and a 46/24 food count after losing farms. The next replay
allows the builder to replace farms while other structures upgrade.
These pilots started on Human09 without an earned Human08 game cache.
The earned-cache replay exposed a second AI issue. The wave launch
native ordered every combat unit to attack even after the script had
formed a specific assault group. It now orders only the formed wave
and keeps defenders in their own group. Tests cover both structure and
unit targets, wave readiness, and empty groups. An earned-cache replay
picked up equipment dropped by the valley encounters, kept both heroes
alive through the Guardian fight, and completed Frostmourne. It lost
Arthas after the first base assault. A later replay used attack-move
orders to bring the force to Mal'Ganis's inner production buildings.
The successful assault destroyed every required structure and defeated
Mal'Ganis. That exposed an argument-count error in the ending cinematic
native. The client now accepts the original two-argument opening and
ending calls. A new replay from the earned Human08 cache repeated the
combat, started the Human ending movie, and displayed the native Victory
screen. The replay verified the recorded unlocks for the ending, the
Undead campaign, and its opening interlude.

The uninterrupted Human campaign runs completed both Human01 optional
quests and reached the Warlord fight. One run died and another stalled
while repeatedly issuing a long-range attack on the remaining Warlord.
The audit now preserves a pending order on the same target. The same
change applies to Human02's long approach to pick up Searinox's heart.
The subsequent Human02 run saved at tick 6,341 with the heart in
inventory. The writer and a fresh-process reader both earned the Orb of
Fire, won the chapter, and loaded Human03 with matching hero properties,
including 542 experience.

A subsequent Chapter One run used attack-move to reveal the remaining
crew after Arthas regrouped. It won the chapter, completed both optional
quests, and entered Human02 with 548 experience and the earned hero
state. Chapter Two reached Searinox's heart again. Its waiting escort
was staged along the approach. Pulling the escort back opened the route
in a replay from the earned Chapter One cache. That replay returned the
heart, earned the Orb of Fire, won Chapter Two, and reached Chapter Four.
It entered Chapter Three with 846 experience and the matching hero state
from its Chapter Two victory.
An isolated continuation from the earned Human03 cache built a second
town hall and sent a summoned scout toward the investigation region. It won
Human04 with 1,772 experience on Arthas and verified both hero restores
into Human05 before the receiving chapter's own hero setup.
The uninterrupted replay reached Human05 with those earned heroes. It
held Hearthglen and missed the grain caravan. The follow-up driver
prioritized the active wagon even after sustaining losses. An isolated
replay seeded with that run's cache completed the caravan and keep
objectives and entered Human06 with matching Arthas properties.

A later replay started from the earned Human08 cache and completed the
Human09 finale again. A replay after the movement corrections repeated
the result. Arthas acquired Frostmourne, Mal'Ganis fell, and the original
victory unlocked the Undead campaign and Human ending.

The Undead opening verifier uses proximity to rescue Acolytes and checks
the optional graveyard and Arthas carryover into Undead02. The replays
rescued Acolytes through the map's own events and completed Find the
Graveyard. The authored healing crate exposed a widget handle-type error
in the installed Blizzard.j wrapper for `GetDyingDestructable`. The native
now returns the concrete widget type. A rendered replay broke the crate
and picked up its healing potion through ordinary orders. Arthas still
fell before the remaining rescues.
The player-unit enumeration natives now include units created during the
same simulation tick. Undead01's original initialization consequently
found its 19 ordinary rescue candidates and kept the bridge rescue and
other scripted scenes separate. The rendered approach recruited the Shade
and verified that the Human guards could not see it. Breaking the
original gate opened the northern route, and the replay reached 16 of
the 20 required rescues. Victory and carryover into Undead02 remain open.

The Landfall replay verified the original ship rescues and followed
Cairne to the Tauren village. The original centaur wave triggers
completed Protect Cairne with both heroes alive. The replay let the
ending cinematic finish, confirmed the original victory prompt, and
entered Orc02 with Thrall's experience, skill points, abilities, stats,
and inventory intact. A second complete replay also recovered the
healing potion from the second ship's crate and verified that Thrall
still held it in Orc02.
The Long March started from that earned game cache. A prior run passed a
crash that occurred when a summoned wolf died without optional explosion
art. The completed replay received the merchant's Raider reinforcements
and delivered all four wagons to the third oasis. The original story
interlude continued into Orc03 and restored Thrall's experience, learned
abilities, skill points, stats and inventory. Run
`./gradlew :desktop:orcMarchPlaythroughAudit -PorcMarchCache=<earned Orc01 cache>`
to replay the objectives and handoff.
Raider Ensnare now follows its research requirement and uses its authored
projectile and durations. A retail-data fixture verifies that a trapped
air unit can be targeted by ground weapons until the net expires. A
retail-data regression also confirms that the fountain's aura heals unarmed
units. In the rendered mission, all four wagons left both oases at full health.
[Blizzard’s Raider guide](https://classic.battle.net/war3/orc/units/raider.shtml)

The standalone Orc03 settlement replay used the starting Peons to build
a Great Hall and deliver 100 gold from the original mine. The original
script completed the town quest and opened the Zeppelin quest. Run
`./gradlew :desktop:orcSettlementPlaythroughAudit` to repeat that milestone.
An earned-cache replay then defeated the Harpies, sent Peons to collect
enough lumber for two Zeppelins, bought both at the original laboratory,
and returned them to trigger native victory. The end scene continued into
Orc04. Use `-PorcSettlementZeppelin=true` with the earned Orc02 cache to
reproduce that run.
The next earned replay completed the original Orc04 Pillage research
event, cut a path through the western grove, and damaged the first Tree of
Life through normal attacks. Its original death event awarded 3,000 lumber.
The full 15,000-lumber objective and Grom's Orc05 carryover remain open.
The installed Orc03 overrides give its laboratory two Zeppelins in stock
and replenish one every five seconds. The stock regression bought both
through the normal neutral-shop order with Thrall nearby, confirmed a
second laboratory retained its stock, and verified the first refill.
The command card reads the shop's current count and refill timer.

The movement tests verify that units use the full simulation step
when crossing a waypoint and that a replacement order discards the
previous pending route. They also verify that a proposed shortcut keeps
the detour if a unit's collision footprint would cross blocked terrain.

Campaign AI now reads the unit's invisibility state and the observing
player's detection state. A regression test checks ordinary vision, an
invisible unit revealed by detection, and an observer without detection.
The original NightElf01 tutorial completed through normal orders. Wisps
mined the entangled gold mine, gathered lumber, and built an Ancient of
War and Moon Wells. The Ancient trained the required Archers, and the
army defeated the original Paladin. The first victory exposed two
authored triggers that stop fog modifiers before they exist. The fog
modifier natives now accept null handles, and a repeat completed without
script errors. The original victory and Continue restored Tyrande's
experience, abilities, stats and inventory in NightElf02.
An optional-quest run also reached the original Furbolg elder, freed
the captives, and earned the Furbolg reinforcements before the Paladin
battle. The same run verified Chapter Two carryover.
NightElf02 has 13 map conditions that query `IsUnitInvisible` for Tyrande.
The map native uses that same player-specific visibility check.
NightElf02's startup also looks up Hydralisk by its legacy name. Unit lookup
now skips entries without legacy names and treats case consistently. The
desktop opening completed initialization and changed graphics quality without
a JASS error. The replacement Shadow Meld implementation uses the map's
shorter fade on Tyrande and makes Hide prevent automatic attacks. The
rendered audit observed the opening's native Hide, then issued Hide near
an active patrol, broke the authored barricade, and used the waygate. The
Huntress Sentinel now uses a charge and reveals invisible units from
the occupied tree. The installed-data tests verify a Shade becomes
detectable, that two owls cannot occupy the same tree, and that damaging
the tree removes its sight. The rendered replay observed the original
Huntress spend its charge and reveal the Shade. In the completed replay,
Ballistas cleared the southern siege lane and opened the gate. The
transition restored Tyrande in NightElf03 with her earned stats, spells
and inventory.
The game now reads Tyrande's Searing Arrows damage, mana cost, missile
and manual cast range from the original ability data. Tests cover
manual orders, autocast, and ordinary attacks when mana runs out.
Starfall and Tranquility use the installed cooldown, radius, duration,
target masks, pulse intervals, and effects. The original-data tests
verify the first pulse, Starfall's reduced damage to buildings,
Tranquility's ally healing, and cancellation by the Stop order.

The earned NightElf03 replay built an economy on the mine entangled by
the original opening script, trained its army, and reached Moonglade
Isle. It defeated the Keepers, awakened Furion with the Horn of Cenarius,
and matched Tyrande's skills, stats, and inventory in NightElf04 after
native victory. The optional dragon objective remains unverified.
The next earned replay rooted the Tree of Ages, registered the original
mine construction event, trained an army, broke the Dryad gate, crossed
the forest, and defeated the Satyr base. Furion woke the Druids at the
Barrow Dens. The mission awarded victory, and both heroes matched their
earned snapshots in NightElf05.
[Blizzard’s Priestess of the Moon guide](https://classic.battle.net/war3/nightelf/units/priestessmoon.shtml)
[Blizzard’s Keeper of the Grove guide](https://classic.battle.net/war3/nightelf/units/keepergrove.shtml)
[Blizzard’s Night Elf guide](https://classic.battle.net/war3/nightelf/)
[Blizzard’s Huntress guide](https://classic.battle.net/war3/nightelf/units/huntress.shtml)

### Frozen Throne opening and neutral shops

The installed Night Elf expansion opening teaches Maiev Fan of Knives
through its original script. The ability uses the campaign's 350-point
damage cap, and the test observes five 70-point hits across ground and
air targets. Shadow Strike uses the existing timed poison and slow with
the original enemy and organic target flags. The regression verifies the
initial hit, five poison ticks, and movement recovery.

The rendered pursuit follows the opening's scripted landing, breaks the
original village gate, and opens the harbor quest. A matching-build
process restored an earned harbor save. In the completed replay, Maiev
crossed the islands with Blink, drew the last defender into range of her
escort, and triggered the original ship-defense victory. Units already
removed by the script no longer count toward the harbor group. The
transition matched Maiev's skills, stats, and inventory in NightElfX02.

The original Orc Chapter Three laboratory starts with two Zeppelins
and replenishes one after five seconds. The Goblin Merchant stocks one
pair of Boots of Speed per shop and replenishes the pair after sixty
seconds. Its Scroll of Town Portal stock starts after 440 seconds.
Both shop tests make purchases through the normal order executor.

The movement scheduler now rejects out-of-bounds searches from fleeing
units and accepts subsequent orders. The strict verification passes
443 core tests with zero skips, 85 campaign smoke runs, and three rendered
menu sizes.

Far Sight uses the original Orc mission's reveal radius, duration and
detection flags. The test observes a distant Shade from the caster's
player, verifies the reveal remains private, and checks expiration.
Chain Lightning uses its own cast order and the original target flags.
The initial strike and first jump hit eligible enemies for 100 and 85
raw damage.
The original Hearthglen Frost Nova fixture verifies its 150-point
primary hit, 50-point splash, attack and movement penalties, and expiry.
The Ashenvale Entangling Roots test verifies that the original three-second
hero duration blocks movement and attacks and deals three damage ticks.
The expansion Forked Lightning test verifies the selected target and two
additional targets inside the authored cone.
The Hearthglen Frost Armor fixture verifies the original manual hero-skill
slot, temporary armor, melee-only chill and expiry. The expansion variant
exposes autocast independently.
The Rexxar Healing Wave fixture verifies three consecutive friendly heals
with the authored 25 percent reduction at each bounce. Earthquake damages
structures and trees, slows ground units to the original map data's 150
minimum speed, and removes its effects when interrupted.
A Peon delivered lumber from the original Barrens tree position into the
Great Hall in a headless fixture using the mission's terrain and object data.
The Hearthglen Death and Decay fixture verifies two pulses at four percent
of maximum life each, including friendly units, flying units, structures
and trees, and checks that interruption stops subsequent pulses.
The original Dark Ritual target rules allow an owned living nonhero and
exclude allied owners. The Blood Mage fixture verifies 18 mana per pulse
and that the original leash can extend beyond cast range.
The Orc05 Mirror Image fixture verifies the half-second split, a single
copy with the Blademaster's identity, zero outgoing damage, double incoming
damage, replacement on recast, and expiration. The copy stays outside the
real hero roster and uses no food.

The original Frozen Throne Snapdragon test verifies poison on weapon impact,
one damage stream per attacker, shorter duration on heroes, and the two-hit-
point floor. A separate Orc02 fixture verifies that identical scripted move
orders allow the current formation path to finish.
The installed Undead finale data also exercises Animate Dead. The spell
selects the six strongest nearby eligible corpses, including enemies,
and the Death Knight's copies remain invulnerable for their authored
40-second lifetime. The summoned units use no food.

An early Human08 retry from the earned Human07 cache cleared the canyon
and destroyed all five transports. Ranged mercenaries took the southern
shore route and both heroes' snapshots matched on entering Human09.
The continuation then triggered Arthas's defeat in the Frostmourne valley.
In a subsequent finale replay, both heroes reached the native Frostmourne
scene and Arthas acquired the sword. A scripted loot drop beyond the map's
collision bounds exposed a quadtree crash. The index now keeps those objects
enumerable through splits, movement and removal. The rerun passed that point
and rebuilt the army, then lost the final assault.
With the later earned Human08 cache, a Human09 replay kept Muradin at the
mountain pass, used Divine Shield through the Guardian fight, and obtained
Frostmourne. The expedition advanced from its forward town, defeated
Mal'Ganis, and verified native victory, the Human ending, and the Undead
campaign unlock. This run used the exact heroes earned by the five-ship
victory. A subsequent fresh-profile run played all nine missions and their
interludes in one process on Normal. It verified earned hero carryover at
each transition, the native final victory, and the Undead unlock. The
Hearthglen caravan was verified in a separate run.
An Orc caravan retry reached the merchant and exposed an additional collision
crash when a moving object retained an old registration. The quadtree records
one position per object identity and a regression covers re-registration
after a caller has moved its own rectangle.

The next complete Orc02 replay used the earned Orc01 cache, brought all
four wagons to the final oasis, played the story interlude, and matched
Thrall's snapshot in Orc03. The interlude had attempted to reposition a
removed unit. The position natives now ignore those stale handles.

The optional `--ability-gaps` flag on `:desktop:campaignSoakAudit` reports
fallback abilities on preplaced units in player slots and on preplaced
heroes. Before the Inferno change it reported 434 map, unit type and ability
combinations across the 85 loaded maps, including original campaign heroes
whose Inferno was missing. This inventory covers preplaced units after
300 idle simulation ticks. It does not execute later mission scripts
or identify abilities with registered but incomplete behavior. Some reported
abilities, such as Spiked Barricades, already have effects supplied by
other systems.

The installed Undead data exercises Inferno's one-second impact delay,
50 damage, different stun lengths for heroes and regular units, and the
180-second Infernal lifetime. The Infernal's original ability list supplies
its spell immunity and Permanent Immolation. The regression checks that
Immolation deals 10 damage after one second and that enemies outside the
eligible target categories are unaffected by the impact.

The installed Orc03 fixture verifies that Pillage enables after research,
awards gold and lumber for weapon damage to enemy structures, and caps
the payout at the structure's remaining health when an attack overkills.

The installed Undead04 fixtures verify that Crypt Fiends can autocast Web
after research. A webbed flyer can be hit by ground weapons and still counts
as airborne when selected for spells. Its normal flight height and movement
return when the effect expires. The same map data verifies that Acolytes
recover resources as they unsummon owned Undead buildings and automatically
unsummon a Haunted Gold Mine after taking its last gold.

The installed NightElf07 fixture verifies that Finger of Death rejects
heroes and destroys the corpse of a regular unit it kills. In that map,
Rain of Chaos uses the authored Inferno ability to produce two separate
impacts and summons.

NightElf03's startup script uses the instant entangle order before the first
simulation tick. The installed-data test verifies that a paused Tree of Life
can complete that order and preserve the original gold deposit. The rendered
expedition from the earned NightElf02 cache mined gold, defeated the Keepers,
and used the Horn of Cenarius to awaken Furion at Moonglade Isle. Tyrande's
snapshot matched on entering NightElf04. The optional dragon remains open.
NightElf04 verified the original Entangled Gold Mine construction event,
reopened income at a second mine, and cleared the Dryad gate. The completed
replay destroyed the Satyr base and woke the Druids at the Barrow Dens.
Tyrande and Furion carried into NightElf05 after native victory.
The NightElf05 replay restored those heroes, played the original introduction,
and verified that the dungeon gate split the party and replaced the quests.
The original spider-egg sequence also broke an egg. If the authored selection
finds no remaining destructable, `KillDestructable` leaves the script running.
Furion then reached, raised and crossed the original lava bridge.
At the Druid of the Claw encounter, the map's native Cyclone order lifted one
of the Druids. The cast now recognizes the map's neutral alliance, and script
waits use the authored dialogue length even with audio muted. The Cyclone
fixture also verifies Master Training, the shorter hero duration, and Priest
Dispel. [Blizzard's Druid of the Talon guide](https://classic.battle.net/war3/nightelf/units/druidofthetalon.shtml)
Furion moved past the Druid patrols with Cyclone, opened both den gates, and
reached the Circle of Power. The native quest completed and returned control
to Tyrande for the second branch. The party reached the northern pressure
plate on foot, used the southern passage opened by the map, and activated
the second plate. The native trigger opened Tyrande's prison gate. The
expedition crossed it, cleared the crates in the western passage, and used
the original waygate into Illidan's prison.

Moon Glaive's passive command-card icon now follows the installed research
requirements. The installed-data test verifies the icon and the additional
bounce on both campaign and melee Huntresses, including research removal.
[Blizzard's Huntress guide](https://classic.battle.net/war3/nightelf/units/huntress.shtml)
The Faerie Fire fixture uses NightElf05 terrain to verify autocast toggles,
the installed five-point armor reduction, moving shared sight, and continued
visibility when the affected unit turns invisible. Dispel removes both the
armor penalty and sight. [Blizzard's Druid of the Talon guide](https://classic.battle.net/war3/nightelf/units/druidofthetalon.shtml)

The Orc Chapter Three replay used the earned Orc02 cache, defeated the Harpies,
and bought both Zeppelins after Peons delivered the required lumber. Returning
both transports to the starting settlement triggered the original victory
and loaded Orc04. The Orc04 replay verified the Pillage research event,
completed the western Great Hall, felled the five original groves, and reached
15,000 lumber. A separate run from that earned cache played the original Orc04
story interlude and matched Grom's snapshot on entering Orc05.
The Orc05 opening restored that Grom, mined gold at the original base, and
placed the starting Witch Doctor's Sentry Ward through the normal cast order.
The ward test verifies concealment from an ordinary scout and detection of
an enemy Shade. The expedition also cut through the eastern forest, opened
the Satyr waygate, and reached the Fountain of Blood. The full native
cinematic transformed Grom. Chaos Grunts and Warlocks flanked the southern
towers, defeated Cenarius, and triggered the original victory. The replay
matched the Chaos Grom snapshot stored for Orc08 and entered Orc06.
[Blizzard's Witch Doctor guide](https://classic.battle.net/war3/orc/units/witchdoctor.shtml)

The Orc06 replay used the earned Orc05 cache. It matched Thrall's items and
learned skills and verified the chapter's scripted 600 XP award, recruited
Cairne, defeated the Harpy guards with Headhunters, and freed all six original
Wyverns. Run `:desktop:orcWyvernPlaythroughAudit` with `-PorcWyvernCache` set to
the earned cache to repeat the rescue.
The summit replay also boarded both heroes, a Catapult and the ranged escort
on a Goblin Zeppelin. It landed on the summit and fought through the eastern
base. The starting town was lost before the summit quest completed.

The original Orc07 replay observed the cave-in separate the two parties.
Thrall fought through the dungeon, recovered the Blade Key and opened the
King's Door. The original trigger consumed the key. Run
`:desktop:orcOraclePlaythroughAudit` to repeat this section.

The Frozen Throne opening reached Illidan's harbor and completed the original
ship defense. The harbor audit identified two removed Naga that remained in
the scripted group. JASS group operations now distinguish removed units from
live hidden units. A fresh desktop process restored the earned harbor checkpoint
at the original objectives. The completed playthrough also matched Maiev's
snapshot on entering NightElfX02. A separate Chapter Two run restored her from
that earned cache and took gold from the original settlement's Entangled Gold
Mine. The voyage replay bought another transport from the original shipyard,
loaded Maiev and Naisha through normal orders, and sailed the channel with both
aboard. The northern landing replay brought six Archers and unloaded the
expedition on the northern shore. Maiev used Blink to reach the approach, and
the original Tomb Guardians quest appeared. The assault opened the Ruins Gate
and defeated the first four guards. The transport then returned to port and
landed a second Archer escort. The expedition defeated the remaining guardians,
earned native victory, and verified Maiev's skills, stats, and inventory on
entering NightElfX03. A separate run with that earned cache played the original
Tomb of Sargeras introduction. Maiev followed the authored Blink hint and
claimed the Rune of Mana on the isolated ledge. The cinematic's cleanup can now
enumerate hidden ground items and make them visible again. The party then
destroyed the southern Demonic Gate, cleared the first rock barrier, and
freed the trapped Watchers. It activated the original floor switch, then
cleared the second rock barrier and rescued the Dryads. Maiev blinked across
the river to activate the bridge plates. Her escort crossed the raised bridge,
destroyed the third barrier, and freed the remaining Watchers. The original
barrier quest marked itself complete. The expedition reached Illidan's
cinematic and started the native escape countdown. Maiev used Blink and
the placed escape runes, crossed the river after the bridge collapsed, and
reached the exit with over a minute remaining. The next chapter restored
the earned Maiev snapshot in NightElfX04. In that chapter, Maiev used Blink
to cross the ravines and reached the original boat rescue. The quest advanced
and transferred a controllable transport to her side. Maiev's escort secured
the causeway and western ridge, and the original courier boarded the rescued
transport by the northwest shore.
The sea escape replay took the original courier around the occupied pier
using a normal move order. A second transport carried Maiev and the rescued
Glaive Thrower to the first sea gate and landed the party. The escort ship
was destroyed by the sea patrols after unloading, before the first gate fell.

The installed UndeadX05 Anti-magic Shell fixture verifies Adept Training,
blocking spells and spell damage, physical attacks still dealing damage,
and Priest Dispel removing the shell without harming the protected summon.
[Blizzard's original Warcraft III manual](https://ftp.blizzard.com/pub/misc/Warcraft%20III%20Manual.pdf)

The installed NightElfX04 Tornado fixture verifies the original summoned unit,
steering, movement slow, both building damage ranges, a ground-unit toss, idle
wander, and death of the Tornado when the caster stops channeling. The pulse
interval and lift durations come from the map data. The per-target repeat
interval follows Blizzard's classic guide. The exact wander trajectory and
airborne visual still need a comparison against the classic executable.
[Blizzard's Naga Sea Witch guide](https://classic.battle.net/war3/neutral/nagaseawitch.shtml)
[Blizzard's later Tornado patch notes](https://news.blizzard.com/en-us/article/23445051/warcraft-iii-reforged-patch-notes-version-1-32-6)

The neutral creep fixture verifies that the installed 1,000-unit hard
limit interrupts autonomous pursuit and sends the creep back to its guard
position. A unit with guarding disabled and a non-neutral unit both continue
the chase. The engine also reads the installed 600-unit soft distance and
five-second return delay, and resets the delay when the creep takes damage.
[Blizzard's classic creep guide](https://classic.battle.net/war3/neutral/creepbasics.shtml)

The installed Orc05 Kodo Beast fixture verifies that Devour rejects allies
and heroes, holds one victim at a time, and digests it at five hit points per
second. Killing or removing the Kodo frees a living victim. A fully digested
victim dies and clears the cargo slot. Devour shares the Kodo's sight with
the victim's owner while digestion runs.
[Blizzard's Kodo Beast guide](https://classic.battle.net/war3/orc/units/kodobeast.shtml)

The Healing Ward regression exercises both the Witch Doctor ability and the
item order. The Witch Doctor needs Master Training. The item consumes one
charge and places a ward for a hero without any Witch Doctor research.

The Orc05 Fel Orc Grunt fixture verifies that Berserk preserves an existing
movement order. In the installed campaign it adds 20% movement speed, 50%
attack speed and 50% damage taken for twelve seconds, with a thirty-second
cooldown.
[Blizzard's Berserk guide](https://classic.battle.net/war3/orc/units/trollberserker.shtml)

The Human04 Mortar Team fixture verifies that Flare requires its research,
spends the original single charge, and reveals an invisible Shade only
after impact. The vision expires after fifteen seconds. The installed map
fields determine the detection, delay, radius and charge count.
[Blizzard's Mortar Team guide](https://classic.battle.net/war3/human/units/mortarteam.shtml)

The Orc03 Zeppelin fixture verifies the installed `cargoSize` field. Four
two-slot Catapults fill its eight-slot hold. Unloading one restores two slots.
The cargo state identifies the current carrier for the JASS queries
`IsUnitInTransport` and `IsUnitLoaded`. The fixture also checks that a passenger
frees its slot when it dies or the map removes it.
The NightElfX02 transport fixture verifies that an unload attempt in deep
water keeps Maiev aboard. The same transport can unload her at a legal shore.
[Blizzard's Goblin Zeppelin guide](https://classic.battle.net/war3/neutral/goblins.shtml)

The original NightElfX03 Druid of the Claw fixture verifies Adept Training,
Rejuvenation's 400 healing over twelve seconds, and a full-health,
magic-immune Dryad as a valid target. The map data can omit the buff
identifier. The stock Rejuvenation fallback retains the buff icon and art and
allows dispel to remove the effect.

The installed Orc04 Shaman tests verify that Purge removes removable buffs
from allies, deals 400 damage to eligible hostile summons and gradually restores
enemy movement. Lightning Shield requires Adept Training and damages nearby
friendly and hostile ground units for 20 per pulse while leaving its bearer
unharmed. After the Witch Doctor changes, the smoke inventory reported 234
unimplemented preplaced map, unit type and ability combinations across 86 raw IDs.
After Moon Glaive, Disease Cloud, Cyclone, Faerie Fire, and Anti-magic Shell,
the same inventory reported 202 combinations across 77 raw IDs. After
Tornado, Devour, and the Reinforced Defenses research indicator, the same
85-map scan reported 175 combinations across 72 raw IDs. The next run
reported 172 combinations across 71 raw IDs.
[Blizzard's Shaman guide](https://classic.battle.net/war3/orc/units/shaman.shtml)

The item inventory separately checks preplaced items and rawcodes passed
literally to supported item creation, granting, and loot natives. After the
restoration and armor item changes, it reported 97 missing map, item and
ability combinations across 42 raw IDs, down from 201 combinations. The
follow-up scan after the dispel, mine, and Town Portal changes reported 77
combinations across 37 raw IDs. The Crystal Ball and lumber changes brought
that to 72 combinations across 34 raw IDs. The next 85-map smoke run also
passed with Hardened Skin, Rain of Fire, and the stock Sapper target data.
The installed Mountain Giant and campaign Hydra reduced eligible attacks by
15 and 12 respectively after research, both with a three-point damage floor.
Rain of Fire respected its five-target damage budget, hit friendly units,
reduced building damage by half, and kept burning for three seconds after an
interrupted channel. [Blizzard's Rain of Fire guide](https://classic.battle.net/war3/neutral/pitlord.shtml)
The stock Hex and Polymorph tests verified the original targeting rules,
attack suppression, and later restoration. Hex retains the hero and its
inventory while temporarily preventing item use. Polymorph requires the
Sorceress's master training, gives the transformed unit medium armor, and
restores the original armor when dispelled. [Blizzard's Shadow Hunter guide](https://classic.battle.net/war3/orc/units/shadowhunter.shtml) and [Blizzard's Sorceress guide](https://classic.battle.net/war3/human/units/sorceress.shtml)
Separate retail fixtures verified the Stone Token's summon, the Potion of
Anti-Magic's protection and expiration, and the Scroll of Resurrection's
six strongest eligible corpses, original ownership, and food restoration.
[Blizzard's campaign item guide](https://classic.battle.net/war3/basics/heroitemsconsumed.shtml)
The installed Orb of Fire and Orb of Kil'jaeden add their authored damage,
splash and projectile effects, enable the carrier's authored air attack,
and preserve the equipment bonus through a unit type change.
[Blizzard's Orb of Fire guide](https://classic.battle.net/war3/basics/heroitemspermanent.shtml)
The Frost and Venom fixture verified their original attack effects, Frost
expiration, and inventory priority when the hero carries both orbs.
The original Orb of Slow applied its distinct hero and unit durations and
could be dispelled. The later Orb of Lightning dealt the installed Purge's
150 damage to summons. The Orb of Darkness created its 80-second Dark Minion
when the carrier killed an eligible unit.
The classic Orb of Lightning gave the equipped hero its six damage and
applied its separate three-second Purge. The Sacrificial Skull consumed one
charge and expanded blight around the selected point to the installed radius.
The Mount Hyjal Dark Portal test verified the installed demon roster and
spawn count, original ownership and cast cooldown.
The Orc06 Battle Stations fixture verified that the selected Burrow calls
the four closest eligible Peons and gains its ranged attack. Stand Down
released the workers and disabled the attack. The UndeadX01 Wand of
Neutralization removed buffs from the selected unit and nearby units on
its side without damaging summons or affecting units outside its radius.
The follow-up smoke scan passed 85 maps over 25,500 ticks and recorded 143
unit-ability and 11 item-ability entries. The core suite passed 516 tests
with zero failures or skips.
Separate Frozen Throne fixtures verified that the Demonic Figurine summons
a Doom Guard, the Spiked Collar summons a Fel Stalker, the Ancestral Staff
summons two Troll Berserkers, the Scepter of the Sea summons two Murlocs,
and the Spider Silk Broach temporarily grounds an eligible air unit.
The Wand of Mana Stealing fixture verified that the caster receives only
the mana available on the target. The Diamond of Summoning recalled twelve
owned units after the authored delay. Enemy towers inside the Horn of the
Clouds stopped firing during the channel and recovered on interruption.
The Rune of Rebirth fixture verified that pickup grants a living creature
of the type carried by the rune's drop, with no timed life. The original
blood elf and Durotar scripts drop the rune from a unit death event.
A further full run passed 525 tests and all 85 maps. The scan found zero
missing item-ability entries across preplaced items and literal script
references, with 141 unit-ability entries still reported.
[Blizzard's charged item guide](https://classic.battle.net/war3/basics/heroitemscharged.shtml)
The Meat Wagon fixtures verified corpse pickup, release, storage between
casts, the five-corpse production limit after Exhume Corpses research, and
Raise Dead directly from an owned wagon. UndeadX05's original Sleep Always
variant woke once on damage and stopped sleeping when the owning player's
script disabled it. The Vengeance fixture verified the summoned Avatar and
its own Vengeance ability. The Tree of Life now uses the authored effect
attachments during its upgrade.
The full run after these changes passed 529 tests and all 85 maps. The scan
reported 93 unit-ability entries and zero item-ability entries. A subsequent
focused run verified Doom's 40 damage per second, immunity to dispel and
120-second summoned Doom Guard, plus missile delivery for Wand of Neutralization.
[Blizzard's Meat Wagon guide](https://classic.battle.net/war3/undead/units/meatwagon.shtml)
[Blizzard's Pit Lord guide](https://classic.battle.net/war3/neutral/pitlord.shtml)
The Possession fixture verified master training, an interrupted channel
that frees the target, and a completed channel that transfers the same
unit handle and consumes the caster. The full run through Possession
passed 531 core tests and all 85 maps. It reported 85 unit-ability entries
and zero item-ability entries in the audited map data.
[Blizzard's Banshee guide](https://classic.battle.net/war3/undead/units/banshee.shtml)
The Spell Breaker fixture verified Feedback's original mana burn against
ordinary units and heroes. The Attribute Bonus fixture verified stat
changes at ranks one and two and removal of those changes with the ability.
The full run through those changes passed 533 core tests and all 85 maps
over 25,500 simulation ticks. It reported 79 unit-ability entries and zero
item-ability entries in the audited map data.

The Blood Elf opening audit followed Kael's introduction and used the
chapter's starting lumber to repair the first Observatory. The native
unit-state event fired as life crossed 2,499, advancing the quest and
triggering the Observatory reveal. The regression fixture verified the
event's unit and state, repeat crossings, and removal. The next replay
reached the original shore, watched Vashj's cinematic, and verified both
of the usable transports transferred to Kael's side. A later replay boarded
Kael and the original Peasant, crossed the lake, and unloaded both on the
shore below the second Observatory. The route from the first Observatory also
broke the gate to the original hidden cache and picked up its Tiny Castle.
The updated replay collected the Tiny Barracks and gold from a second cache,
established a base beside the island's original mine, and sent an escorted
Peasant to repair the second Observatory while the base gathered lumber.
The Observatory reached full life and the original trigger marked it repaired.
With town defense orders active, the three-Observatory replay stocked the
required lumber. It boarded Kael, the Peasant, and two escorts and unloaded
the party on the shore below the third Observatory. The mission ended when
the last player-owned structure fell. Kael and the repair worker were still
alive, and the third Observatory was still awaiting repair.

The Mountain Giant fixture verified Taunt's ten-target cap and that a new attack order can
override it. Sylvanas's Black Arrow fixture verified the mana charge, an
impact mark, and the resulting Dark Minion. The full run through these
changes passed 536 tests and all 85 maps over 25,500 simulation ticks. It
reported 74 unit-ability entries and zero item-ability entries.
[Blizzard's Mountain Giant guide](https://classic.battle.net/war3/nightelf/units/mountaingiant.shtml)
[Blizzard's Dark Ranger guide](https://classic.battle.net/war3/neutral/darkranger.shtml)

The extended Oracle replay broke the gates in the east passage and lower
hall, used the original waygate, and triggered the Dragon encounter. Thrall
defeated the Dragon and picked up the Heart of Aszune from the native drop.
He broke the gate on the return route and delivered the Heart to the statue.
The original trigger consumed it, completed the quest, and opened the next
passage. Explicit JASS trigger evaluation and execution now work when the
trigger is disabled for event dispatch. Cairne's next route activated the
original floor switch to open the Quilboar gate, then triggered the hall trap
and recovered the Enchanted Gemstone. She used the Ghost Key dropped by the
animated statue to open the ghost door and return the Gemstone. The original
return trigger consumes the Gemstone and completes the quest. The client
restores the map's spectral bridge at full health when that call receives
zero life from the unset last-created destructible. A desktop replay verified
the Ghost Key, Gemstone handoff, and raised bridge. With
`-PorcOracleVictory=true`, Thrall crossed the bridge and triggered the ending.
The replay continued through both victory dialogs and verified Thrall and
Cairne's levels, skills, and inventory when Orc08 restored both heroes.
The Rokhan fixture verified that
Serpent Ward uses the campaign alias and
creates the original immobile, magic-immune ward. The creep Slam fixture
verified the original order and its ground-only damage. The full run
passed 538 core tests and all 85 maps over 25,500 simulation ticks. It
reported 72 unit-ability entries and zero item-ability entries.
[Blizzard's Serpent Ward guide](https://classic.battle.net/war3/orc/units/serpentward.shtml)

The Magtheridon fixture verified Frenzy's autocast and the Howl of Terror
fixture verified that dispel removes its attack penalty. The Destroyer
fixtures verified Absorb Mana's owned-unit restriction and Devour Magic's
hostile dispel reward and summon damage. The full run through these abilities
passed 541 core tests and all 85 maps over 25,500 simulation ticks. It reported
67 unit-ability entries and zero item-ability entries.
[Blizzard's Pit Lord guide](https://classic.battle.net/war3/neutral/pitlord.shtml)
[Blizzard's Quilbeast guide](https://classic.battle.net/war3/neutral/quilbeast.shtml)
[Blizzard's Destroyer guide](https://classic.battle.net/war3/undead/units/destroyer.shtml)

The Nerubian Tower and Frost Wyrm fixture verified the original five- and
ten-second chill durations and the Frost Wyrm's chill on enemy splash victims.
The full run passed 543 core tests and all 85 maps over 25,500 simulation ticks.
It reported 64 unit-ability entries and zero item-ability entries.
The later Freezing Breath fixture verified the research requirement and the
five-second production suspension. Expiration restored production at the same
progress, even when a separate scripted pause overlapped the freeze.
[Blizzard's Nerubian Tower guide](https://classic.battle.net/war3/undead/buildings/nerubiantower.shtml)
[Blizzard's Frost Wyrm guide](https://classic.battle.net/war3/undead/units/frostwyrm.shtml)

The Naga Siren fixture verified that Parasite autocast targets an eligible
enemy, applies periodic damage, and summons the original Watery Minion on
death. The full run through this change passed 545 core tests and all 85 maps
over 25,500 simulation ticks. It reported 60 unit-ability entries and zero
item-ability entries.

The Crypt Fiend fixture verified that a normal hit damages its target and
sends a harmless returning spiderling one second later. The transport fixture
verified that a selected shore is used when passengers can land there, and
deep water keeps passengers aboard. The Blood Elf desktop replay placed the
original Tiny Castle after unloading Kael and the Peasant on the selected
shore. The full run passed 546 core tests and all 85 maps over 25,500
simulation ticks, with 48 unit-ability entries and zero item-ability entries.

The Mortar Team fixture verified that Fragmentation Shards requires the
original research and adds 25 damage before armor at the center of impact and
18 in the tested middle radius. The heavy-armored and allied fixtures received
their ordinary splash damage. The full run passed 547 core tests and all 85
maps over 25,500 simulation ticks, with 45 unit-ability entries and zero
item-ability entries. [Blizzard's Mortar Team guide](https://classic.battle.net/war3/human/units/mortarteam.shtml)

The original NightElfX04 fixtures verified Submerge for the Naga Myrmidon,
Royal Guard, and Snap Dragon. All three reject shallow shore and can submerge
in deep water. The Myrmidon and Snap Dragon require the original research.
Surfacing restores the same unit handle and its original attacks and vision.
The full run passed 550 core tests and all 85 maps over 25,500 simulation
ticks, with 39 unit-ability entries and zero item-ability entries.

The Mountain Giant fixture uprooted an original tree through a right-click
order, used the authored siege weapon for 15 strikes, and restored the normal
attack afterward. The Chimaera fixture verified that researching Corrosive
Breath displays its passive icon and enables the authored siege attack. The
full run passed 551 core tests and all 85 maps over 25,500 simulation ticks,
with 36 unit-ability entries and zero item-ability entries.
[Blizzard's Mountain Giant guide](https://classic.battle.net/war3/nightelf/units/mountaingiant.shtml)
[Blizzard's Chimaera guide](https://classic.battle.net/war3/nightelf/units/chimaera.shtml)

The original Dryad fixture verified the Abolish Magic research gate, removing
a harmful buff from an ally and a beneficial buff from an enemy. A plain
summon was ignored by autocast and took 300 damage from a manual cast. The
Gryphon Rider fixture verified that Storm Hammers appears when the research
enables the upgraded attack. The full run passed 552 core tests and all 85
maps over 25,500 simulation ticks, with 33 unit-ability entries and zero
item-ability entries.
[Blizzard's Dryad guide](https://classic.battle.net/war3/nightelf/units/dryad.shtml)

The original Spell Breaker fixture verified that Control Magic requires
research and charges the installed base cost plus a fraction of the summon’s
maximum life. It transferred the same Water Elemental handle and preserved
its original timed-life expiry. The full run passed 553 core tests and all
85 maps over 25,500 simulation ticks, with 31 unit-ability entries and zero
item-ability entries. [Blizzard’s Warcraft III 1.15 patch notes](https://warcraft.wiki.gg/wiki/Warcraft_III/Patch_1.15)

The original Hippogryph fixture used the stock mount and dismount commands.
Both commands require the original research. Mounting and separating
the units preserve health percentage and total food usage. An empty item
slot now yields no item for scripted removal. The rendered Orc08 replay
verified that Jaina awards the Soul Gem, Thrall captures Grom with it, and
the original quest advances when he returns to the Circle of Power. The
full run passed 554 core tests and all 85 maps over 25,500 simulation ticks,
with 30 unit-ability entries and zero item-ability entries.
[Blizzard’s Hippogryph Rider guide](https://classic.battle.net/war3/nightelf/units/hippogryphrider.shtml)

The original Faerie Dragon fixture verified its Phase Shift autocast orders,
the triggering hit, the installed mana cost, protection from later hits, and
return to combat at the end of the authored duration. The cooldown prevents
another shift immediately after the unit returns.
The full run passed 555 core tests and all 85 maps over 25,500 simulation
ticks, with 29 unit-ability entries and zero item-ability entries.
[Blizzard’s Faerie Dragon guide](https://classic.battle.net/war3/nightelf/units/faeriedragon.shtml)

The original Tauren and Flying Machine fixtures verified that each research
unlock displays its command-card icon and enables its combat effect. The
Tauren hit nearby ground enemies for 60 damage in the inner ring and 30 in
the outer ring. The Flying Machine acquired its ground attack. Together with
the Shimmerweed fix, the full run passed 559 core tests and all 85 maps over
25,500 simulation ticks. It reported 26 unit-ability entries and zero
item-ability entries.
[Blizzard’s Tauren guide](https://classic.battle.net/war3/orc/units/tauren.shtml)
[Blizzard’s Flying Machine guide](https://classic.battle.net/war3/human/units/flyingmachine.shtml)

The Spell Breaker fixture verified a manual Bloodlust transfer from an enemy
to an ally, and an automatic Slow transfer from an ally to an enemy. Both
effects kept their remaining duration. The classic Slow data can omit its
buff identifier, so the loader now restores the stock identifier for that
case. The transfer selection also recognizes Faerie Fire, Cripple, and
Polymorph. The classic manual-only cases still need implementation.
[Blizzard’s Spell Breaker guide](https://classic.battle.net/war3/human/units/spellbreaker.shtml)

The Flying Machine Flak Cannons fixture verified three damage rings against
enemy air units, with allies and ground units unaffected. The full run through
Flak Cannons and Spell Steal passed 561 core tests and all 85 maps over 25,500
simulation ticks, with 21 unit-ability entries and zero item-ability entries.
[Blizzard’s Flying Machine guide](https://classic.battle.net/war3/human/units/flyingmachine.shtml)

The Brewmaster fixture verified that Drunken Haze slows nearby enemy units
and can cause their attacks to miss. Breath of Fire dealt its authored initial
damage to units in the cone and applied burning damage only to the hazed
units. The full run passed 562 core tests and all 85 maps over 25,500
simulation ticks, with 17 unit-ability entries and zero item-ability entries.
[Blizzard’s Pandaren Brewmaster guide](https://classic.battle.net/war3/neutral/pandarenbrewmaster.shtml)

The original Human06 fixture verified that Soul Preservation accepts a
converted zombie and removes it without a death event. The implementation
uses the installed three-second duration for the transfer. The full run
passed 563 core tests and all 85 maps over 25,500 simulation ticks, with 15
unit-ability entries and zero item-ability entries.

The original Chen fixture verified that Storm, Earth, and Fire creates all
three elemental spirits. Chen reunited at a surviving spirit with the same
inventory, and died when the spirits were all destroyed. The full run passed
564 core tests and all 85 maps over 25,500 simulation ticks, with 13
unit-ability entries and zero item-ability entries.

The original Batrider fixture verified that Liquid Fire requires research,
slows a tower's attack, interrupts an active repair, and blocks further repair
orders while the fire burns. Repairs resumed after the damage expired. The
full run passed 565 core tests and all 85 maps over 25,500 simulation ticks,
with 12 unit-ability entries and zero item-ability entries.
[Blizzard's Troll Batrider guide](https://classic.battle.net/war3/orc/units/trollbatrider.shtml)

The Siege Engine fixture verified the original research transformation and
the resulting air attack. Its first volley homed in on eight additional
enemy air units. Ground units, allies, and flyers outside the radius were
unaffected. [Blizzard's Siege Engine guide](https://classic.battle.net/war3/human/units/siegeengine.shtml)

The Orc05 fixture verified that Chaos research lets Fel Orc Peons board the
original Burrow through Battle Stations. The standard Burrow still accepted
four own Peons and rejected other units in its separate fixture.

The Zeppelin fixture verified that a land crash releases surviving passengers
with dizziness for 30 seconds on heroes and 60 seconds on ordinary units. Its
deep-water case verified that cargo is lost when there is no landing available.
[Blizzard's Goblin Zeppelin guide](https://classic.battle.net/war3/neutral/goblins.shtml)

The original Orc interlude fixture verified idle critter movement and an
explicit move order. The original Wander entry has no distance or timing
fields, so the engine uses a 96-unit move within 256 units of the starting
point, with a three-second interval. The Tornado fixture still uses its
authored movement fields. The full run passed 570 core tests and all 85 maps
over 25,500 simulation ticks, with four unit-ability entries and zero
item-ability entries.

The Mana Flare fixture verified the armor bonus while channeling, capped
retaliation against an enemy spellcaster and a nearby unit with mana, and
cleanup when the dragon receives a move order. Damage during the channel
does not trigger Phase Shift. The full run passed 556 core tests and all 85
maps over 25,500 simulation ticks, with 28 unit-ability entries and zero
item-ability entries.
[Blizzard’s Faerie Dragon guide](https://classic.battle.net/war3/nightelf/units/faeriedragon.shtml)

A Human replay from the earned Chapter Six cache rescued the dwarves and
destroyed the undead citadel in Chapter Seven, then destroyed all five ships
in Chapter Eight. A separate replay from the Chapter Eight cache regrouped
the original force, acquired Frostmourne, and defeated Mal'Ganis. The final
victory unlocked the Undead campaign.

The Tiny Great Hall fixture verified placement cancellation, the 20-second
construction, and the food-cap change at completion and destruction. The
Orc08 fixture verified that Soul Gem capture preserves the enemy hero's handle
and delivers the consumed gem to the item-use event. Removing the possession
ability releases the hero, as required by the native Orc08 script.
The speed potion and rune fixture verified the installed bonuses, the speed
cap, and expiry after the authored durations. The Tome of Retraining fixture
verified that hero ranks become available skill points while XP, level,
equipment, name, and derived defense are preserved.
[Blizzard's item reference](https://classic.battle.net/war3/basics/heroitemsconsumed.shtml)
The Human09 fixture verified that Amulet of Recall consumes its charge and
brings twelve eligible owned units from the targeted area to the hero.
Runtime-computed item IDs are outside that scan. The Barrow Deeps fixture
verified the Scroll of Healing on organic allies, including Dryads, and the
Talisman of the Wild's original Furbolg summon event. The Goblin Land Mine
fixture verified charge consumption, the ten-second arm delay, the two
authored damage radii, and allied units remaining unharmed.
The Hearthglen fixture verified the Scroll of Town Portal's five-second
channel, caster protection, targeted landing, and default selection of the
highest owned town hall. It also checked that allied troops, distant troops,
and a working Peasant stay behind.
[Blizzard's consumed item guide](https://classic.battle.net/war3/basics/heroitemsconsumed.shtml)
[Blizzard's charged item guide](https://classic.battle.net/war3/basics/heroitemscharged.shtml)
[Blizzard's Town Portal guide](https://classic.battle.net/war3/basics/townportalscrolls.shtml)
The Spirit Forest fixture verified that the original Crystal Ball reveals a
distant invisible Shade for ten seconds and remains in inventory. The
Blood Elf opening fixture verified that the original Bundle of Lumber grants
250 lumber immediately on pickup.

The Barrow Deeps replay took Tyrande through the prison, equipped the
authored Gauntlets of Ogre Strength, defeated the last prison guard, and
freed Illidan. It verified the stored Tyrande and Furion snapshots and the
native handoff to NightElf06. The receiving map assigns Tichondrius after
the difficulty trigger first calls `SelectHeroSkill` on his unset reference.
The native now safely ignores that early call. A separate desktop process
also rendered NightElf06 from the earned campaign cache.
The NightElf06 replay with `-PnightElfSkullQuest=true` watched the original
introduction, mined gold with the starting Wisps, trained Archers and Dryads,
destroyed the original Demon Gate, and verified the Skull cinematic's
transformation and new Tichondrius quest. With `-PnightElfDefeatTichondrius=true`,
Illidan and the expedition defeated Tichondrius through combat and the game
loaded the original NightElf06 interlude. With `-PnightElfFinaleOpening=true`,
the interlude played through to Mount Hyjal. NightElf07 restored Tyrande and
Furion with their earned skills and inventory and granted each the authored
700 XP. The original 45-minute countdown started, the Wisps mined gold, and
the Ancient of War trained an Archer. With `-PnightElfFinaleVictory=true`,
the expedition survived the entire authored timer and the original ending
loaded the Night Elf campaign credits. The first defence run uncovered a
unit-only ability callback receiving a destructible from splash damage. The
callback now ignores non-unit targets and the installed-map fixture covers
the exact case.

The simulation timer regression verified that a paused countdown freezes its
elapsed and remaining time, resumes with the same remaining ticks, and can
restart while scheduled. It also covers the separate initial delay for a
repeating timer.

The Founding of Durotar replay watched the original introduction, verified that
Rexxar's Feral Spirit cast fired the summon event and left Misha alive, and
confirmed that the original Mogrin's Report was in Rexxar's inventory. It then
followed the road to Orgrimmar, turned the report in to Thrall, and recruited
Rokhan. Run `:desktop:rexxarOpeningPlaythroughAudit` to repeat the opening.
The same replay with `-PrexxarAcceptOpeningQuests=true` follows the original
roads and discovers the three assignments from Gazlowe, Drek'Thar, and
Nazgrel after their cinematics.
With `-PrexxarEnterGoblinTunnels=true`, the original zone transition saves a
Durotar archive under the active profile's `Blizzard/Zones` directory and
restores Rexxar and Rokhan in the Goblin Tunnels with their inventories,
experience, and learned abilities. The save-store test checks that a manual
save's zone copy and the working zone archive use distinct paths and that
the original flattened slots remain readable.
With `-PrexxarReturnFromGoblinTunnels=true`, the replay returns through the
original exit, reloads the Durotar archive, and restores the party and quest
state. The engine uses the map flag values in the installed `common.j` and
suppresses historical map-change requests during checkpoint reconstruction.
With `-PrexxarBackUpZoneSaves=true`, Quick Save runs the original save trigger
and verifies the Durotar archive in `Blizzard/QuickSave` before returning.
With `-PrexxarCollapseFirstTunnel=true`, the party reached the original
Goblin rendezvous and acquired the Sappers. A Sapper destroyed the first
pair of support columns through an ordinary Kaboom order. The authored
collapse advanced the objective, and both heroes returned to the restored
Durotar archive.
With `-PrexxarCollectThunderHerbs=true`, Rexxar followed the original road
from Orgrimmar to Thunder Ridge. The original zone transition archived Durotar
and restored both heroes in Thunder Ridge. Shimmerweed is consumed on pickup
and has no active item ability, so the inventory now fires the pickup event
for that combination. The rendered replay collected six original plants and
verified completion of the native collection objective. With
`-PrexxarReturnThunderHerbs=true`, the desktop replay restored the Durotar
archive, preserved the objective, and completed Drek'Thar's original turn-in.
The native reward appeared at the authored location.
The original Goblin Merchant purchase fixture verifies the seller unit and
seller-player item sale events, including the buyer and purchased item handles.
A rejected out-of-stock purchase leaves the event count unchanged.
The first Harpy Threat replay reached and damaged the original Bloodfeather.
The party revived after falling in the encounter. The armory reward still
needs a replay through Bloodfeather's defeat and the return to Nazgrel.
The rendered round trip also verified that the original invisible sight marker
loads without a missing-model exception.
The separate native save and Quick Save audit verified each save event's slot
name and matched uninterrupted play in a fresh desktop process.

The Tomb of Sargeras item fixture verifies that the original lesser and greater
mana runes restore 125 and 300 mana in a 1,200-unit area on pickup, including
when Maiev's inventory is full. Nearby enemies and distant allies receive no
mana. [Blizzard's power-up item guide](https://classic.battle.net/war3/basics/heroitemspowerups.shtml)

The Orc05 Witch Doctor fixtures verify Sentry Ward detection, the Stasis
Trap's arming delay and shorter stun on heroes, and Healing Ward's Master
Training requirement. Healing affects nearby organic allies, including when
two wards overlap, at the authored rate of two percent per second. Ability
Builder templates now read classic unnumbered target fields when a numbered
field is empty. [Blizzard's Witch Doctor guide](https://classic.battle.net/war3/orc/units/witchdoctor.shtml)

The installed Undead03 fixture verifies Disease Cloud research, a single
damage pulse where clouds overlap, immunity for Undead, and a two-hit-point
floor. The lingering effect survives dispel. An Abomination's death and a
Meat Wagon impact each spawn the authored temporary ward.
[Blizzard's spell guide](https://classic.battle.net/war3/basics/spellbasics.shtml)
