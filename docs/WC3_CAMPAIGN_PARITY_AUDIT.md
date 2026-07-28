# WC3 Campaign Full Parity Audit

**Goal:** Run the Warcraft III single-player campaigns (Reign of Chaos + The Frozen Throne) in Warsmash / OpenWCIII with full parity to retail WC3.

**Status (as of 2026-07-28):** Campaign menu + single-mission launch work.
**P0 progression spine is largely landed** (`ChangeLevel` + score Continue,
dialog buttons, selection, hero carry-over, menu restore, availability store,
score/boards MVP, transmission VO + named anims, cine filters, volume groups,
hero natives, AI assault MVP). Still missing for full parity: real movie
decode, competitive build AI, sky mesh, RoC/TFT soak.

**Severity legend**

| Tag | Meaning |
|-----|---------|
| **P0** | Blocks finishing a campaign or advancing between missions |
| **P1** | Mission playable but missing retail-critical behavior |
| **P2** | Polish / parity nicety / needs soak testing |

---

## Already working

- Campaign menu loads `CampaignFile` / `UI\CampaignInfoClassic.txt`; race + mission lists (`CampaignMenuData`, `CampaignMenuUI`, `MenuUI`)
- Mission launch with guarded map preload (failed preload keeps menu usable)
- Campaign backgrounds, fog, ambient loop, cursor, door fade
- `InitGameCache` / store-get-flush / `SaveGameCache` with disk persistence (`CGameCache`)
- Partial `StoreUnit` / `RestoreUnit` (XP, stats, skill points, inventory)
- Partial `SaveGame` / `LoadGame` (JASS primitive globals + gold/lumber)
- In-map cinematic HUD mode (`CinematicMode`, `ShowInterface`, `SetCinematicScene`)
- Transmission text + portrait + VO label playback (`soundLabel`)
- Thematic music start/end
- `CustomVictory` / `CustomDefeat` fire events then exit to menu screen object
- Quest / multiboard / leaderboard state + text overlays
- Script dialogs (visible; button handle returns)
- Timer dialogs
- Trigger exception isolation so one bad trigger does not kill the loop
- Core hero XP / level / `SelectHeroSkill` gameplay natives
- Cine-filter MVP overlay; volume-group MUSIC scaling
- Hero script natives (`SetHeroProperName`, `UnitModifySkillPoints`, …)

---

## P0 — Progression spine (must ship first)

### 1. Chain map loading — `ChangeLevel`
- **Status:** **DONE (MVP)** — native calls `WarsmashUI.requestChangeLevel`;
  `MenuUI` loads the next map via pending-change-level (score screen skipped).
- **Remaining:** honor `doScoreScreen` once score screen exists; verify retail
  map-path resolution quirks across RoC/TFT.

### 2. Restore campaign chrome after mission exit
- **Status:** **DONE** — `MenuUI.onReturnFromGame()` restores mission-select
  chrome and ambient for campaign returns.

### 3. Victory / defeat → next step
- **Status:** **DONE (MVP)** — exit-to-menu works; score Continue dialog;
  `EndGame` and `ChangeLevel(..., true)` honor score screen before proceed.
  In-script `ChangeLevel` chains maps.

### 4. Fix `DialogAddButton` handle return
- **Status:** **DONE** — returns `button` handle.

### 5. Selection natives for cinematic / scripted control
- **Status:** **DONE** — wired to `MeleeUI` + selection circles.

### 6. Hero carry-over: learned abilities + proper name
- **Status:** **DONE** — `StoredUnitData` + gamecache v2 + restore path.

### 7. Movie / intro cinematic playback
- **Status:** **MVP DONE** — `PlayCinematic` shows overlay and blocks the
  calling JASS thread (~5s / ESC skip). Real SMK/BIK/video decode still TODO.
  `PlayModelCinematic` / `SetIntroShot*` registered as stubs.

### 8. Campaign AI bootstrap
- **Status:** **MVP DONE** — `StartCampaignAI`/`StartMeleeAI` load scripts into
  `JassAIEnvironment` with `StartThread`/`Sleep`. Unit-count + captain-home
  natives work; **assault roster** (`AddAssault`/`CaptainIsEmpty`/`CaptainAttack`
  orders + `SuicidePlayer` attack-move) MVP landed. Build-queue natives remain
  stubs — AI will not yet produce competitive economies.

### Also landed this pass
- **`PauseGame`** freezes sim (timers/threads still run).
- **`EndGame`** exits to menu via custom-victory path.
- **Quest log panel** (toolbar button + ScriptDialog list).
- **Score screen MVP** (Victory/Defeat Continue dialog).
- **Multiboard + leaderboard** text overlays and Create* natives.
- **Campaign menu gating** from `CampaignProgressStore`.
- **`PolledWait`**, **`SaveGameExists`**, **`SetUnitPathing`**,
  **`CachePlayerHeroData`**.


---

## P1 — Retail-critical mission features

### Campaign progress & menu state
- [x] Persist mission / campaign / cinematic availability (`CampaignProgressStore`)
- [x] Availability natives: `SetCampaignAvailable`, `SetOpCinematicAvailable`, `SetEdCinematicAvailable`, `SetTutorialCleared`, `ForceCampaignSelectScreen`, `CustomCampaignButtonSetVisible`
- [x] `SetCampaignMenuRace` / `Ex` + `GetCampaignMenuRace`
- [x] Honor `CampaignMenuData.isDefaultOpen` for gating (seed all campaigns)
- [ ] Enable or implement Custom Campaign / Options / Credits (`ENABLE_NOT_YET_IMPLEMENTED_BUTTONS=false` in `MenuUI`); Load Saved still menu-gated

### Save / load UX & completeness
- [x] Esc-menu Save/Load buttons (QuickSave MVP)
- [ ] Main-menu Load Saved
- [x] `ReloadGame` (restores last-save globals/resources MVP)
- [x] `SaveGameExists`, `CopySaveGame`, `RemoveSaveDirectory`, `RenameSaveDirectory`; `GetSaveBasicFilename`
- [x] `CachePlayerHeroData`
- [ ] Extend `SaveGame`/`LoadGame` beyond primitives (or document that campaign continuity is gamecache-only and match retail usage)

### In-mission UI parity
- [x] Quest dialog UI (quest button + ScriptDialog list)
- [x] Multiboard rendering (text overlay MVP)
- [x] Leaderboard API + text overlay MVP
- [x] Score screen after victory/defeat (Continue dialog MVP)
- [x] Dialog hotkey handling

### Cinematic / presentation (in-map)
- [x] Transmission voice / `soundLabel` playback (UISounds MVP)
- [x] Named transmission animations (`setSequence` by name; fallback PORTRAIT/TALK)
- [x] `ClearTransmissionQueue` / `EnableTransmission`
- [x] `SetCinematicCamera` (stops pans/noise; MDX track playback still TODO)
- [x] Cine-filter API MVP (`SetCineFilter*`, `DisplayCineFilter`, …)
- [x] `PauseGame` (sim freeze; timers/threads still run)
- [x] `CinematicSkipButton` (ESC skip for `PlayCinematic`); `SetSkyModel` accepted (mesh swap pending); `SetCinematicAudio` ducks MUSIC/AMBIENT MVP

### Sound / music groups
- [x] `VolumeGroupSetVolume` / `VolumeGroupReset` (MUSIC → music player; others stored)
- [ ] Stacked / ambient / remaining 3D sound setters
- [ ] Separate thematic music layer / fade parity
- [x] `ClearMapMusic`

### Heroes / abilities / pathing used by scripts
- [x] Soft-fail `UnitAddAbility` for unprogrammed rawcodes (`CAbilityGenericDoNothing`)
- [x] `UnitModifySkillPoints`, `UnitStripHeroLevel` (MVP; no ability unlearn), `SetHeroProperName`, `DecUnitAbilityLevel`, `SetReservedLocalHeroButtons` (stored)
- [x] Non-permanent `SetHeroStr` / `Agi` / `Int`
- [x] `SetUnitPathing`
- [x] `UnitShareVision` (per-unit fog modifier)

### Camera / images / blight (often used in campaign scripts)
- [x] Camera noise / stop variants (`CameraSet*Noise`, `StopCamera`)
- [x] Image MVP (`CreateImage`/`ShowImage`/`SetImagePosition` via ground splat); Ubersplat MVP (`CreateUbersplat`/show/destroy)
- [x] `SetBlight` / `SetBlightLoc`
- [x] Terrain queries: `IsTerrainPathable`, `GetTerrainType`, `GetTerrainVariance`, null-safe `GetTerrainCliffLevel`
- [x] `PingMinimap` / `PingMinimapEx`
- [x] `EnableUserUI` → control + interface visibility

---

## P2 — Polish & verification

- [x] `PolledWait` (BJ often wraps `TriggerSleepAction`)
- [x] Trackables (`CreateTrackable` + hit/track events MVP)
- [x] Chat event registration fires events (Enter/chat button prompt MVP)
- [x] `SyncStored*` (SP no-op)
- [x] `HaveStoredMission`; `ReloadGameCachesFromDisk` still returns TRUE (InitGameCache already loads disk)
- [x] Deduplicate early vs late `Store*` native registrations in `Jass2`
- [x] Timer dialog color/speed
- [x] `FlashQuestDialogButton` / `ForceQuestDialogUpdate`
- [x] DefeatCondition as real type
- [ ] Campaign BLP alpha artifacts (README)
- [ ] Reforged FLAC quality (`docs/COMPATIBILITY.md`)
- [x] `Cheat` native (basic: whosyourdaddy / greedisgood / pointbreak / thereisnospoon)

### Mission soak matrix (required for “full parity” sign-off)

Play and log missing natives / abilities / crashes for each:

1. RoC Prologue → Human → Undead → Orc → Night Elf (all missions + interlude movies)
2. TFT Blood Elf → Undead → Orc → Night Elf / Illidan finale
3. Optional: custom campaign button path once enabled

For each mission record: start OK, mid-mission script errors, victory path, hero carry-over correctness, next-map transition.

---

## Suggested implementation order

| Step | Work | Unlocks |
|------|------|---------|
| 1 | `DialogAddButton` return + selection natives | Scripted dialogs / cine control |
| 2 | Hero ability + properName in `StoredUnitData` | Correct carry-over |
| 3 | `ChangeLevel` + gamecache continuity | Multi-map campaigns |
| 4 | `onReturnFromGame` UI restore + mission availability state | Menu progression between manual launches |
| 5 | Victory/defeat → score screen → menu or `ChangeLevel` | Retail end-of-mission flow |
| 6 | `PlayCinematic` MVP (skippable stub → real video) | Interludes / intro shots |
| 7 | `StartCampaignAI` + common.ai coverage | Enemy AI on campaign maps |
| 8 | Quests / multiboard / leaderboard UI | Scripted objectives UI |
| 9 | Transmission VO, cine filters, pause, volume groups | Presentation parity |
| 10 | RoC + TFT soak matrix; fix remaining natives/abilities | Full parity sign-off |

---

## Code anchors

| Area | Primary files |
|------|----------------|
| Campaign menu | `core/.../ui/menu/CampaignMenu*.java`, `MenuUI.java` |
| Mission UI / victory | `MeleeUI.java`, `WarsmashUI.java` |
| JASS natives | `core/.../parsers/jass/Jass2.java` |
| Gamecache / stores | `CGameCache.java`, `StoredUnitData.java`, `CGameSave.java` |
| Roadmap phase | `docs/ENGINE_MODERNIZATION_ANALYSIS.md` Phase E |
| Recent campaign fixes | `CHANGELOG.md` — Campaign Reliability Pass (2026-03-07), Startup Reliability (2026-03-06) |

---

## Out of scope clarification

This audit targets **Warcraft III** campaign parity inside Warsmash (OpenWCIII). It does **not** cover loading native **Warcraft II** (Tides of Darkness / Beyond the Dark Portal) assets or `.pud` maps; that would be a separate engine/format project.
