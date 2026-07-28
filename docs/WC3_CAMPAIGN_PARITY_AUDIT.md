# WC3 Campaign Full Parity Audit

**Goal:** Run the Warcraft III single-player campaigns (Reign of Chaos + The Frozen Throne) in Warsmash / OpenWCIII with full parity to retail WC3.

**Status (as of 2026-07):** Campaign menu + single-mission launch work. Full campaign progression (map chaining, movies, hero skill carry-over, post-mission UI, campaign AI) does **not**. Phase E in `ENGINE_MODERNIZATION_ANALYSIS.md` still lists campaign support as planned.

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
- Transmission text + portrait (no VO)
- Thematic music start/end
- `CustomVictory` / `CustomDefeat` fire events then exit to menu screen object
- Quest / multiboard **state** (no UI rendering)
- Script dialogs (visible; button handle broken — see P0)
- Timer dialogs
- Trigger exception isolation so one bad trigger does not kill the loop
- Core hero XP / level / `SelectHeroSkill` gameplay natives

---

## P0 — Progression spine (must ship first)

### 1. Chain map loading — `ChangeLevel`
- **Gap:** Native is **missing**. Retail campaigns flush gamecache then call `ChangeLevel` to load the next map.
- **Need:** Unload current map → keep gamecache → load next map path → restore script environment; honor `doScoreScreen` flag once score screen exists.
- **Where:** `Jass2.java` (new native); map-screen teardown/load path in `WarsmashGdxMapScreen` / `War3MapViewer`.

### 2. Restore campaign chrome after mission exit
- **Gap:** `MenuUI.onReturnFromGame()` restores ambient/background for `CAMPAIGN` / `MISSION_SELECT`, but the code that re-shows campaign frames is **commented out** (~2506–2547).
- **Need:** Uncomment/complete frame visibility restore; set `menuState` back to mission select; play campaign fade correctly.
- **Where:** `MenuUI.java`.

### 3. Victory / defeat → next step
- **Gap:** `CustomVictory` / `CustomDefeat` only run `exitGameRunnable` (back to menu). Score screen ignored despite `enableScoreScreen`. `EndGame` native missing.
- **Need:** Optional score screen; then either return to campaign menu with unlock updates **or** honor in-script `ChangeLevel`.
- **Where:** `MeleeUI.java`, `WarsmashUI.java`, `WarsmashGdxMapScreen.java`.

### 4. Fix `DialogAddButton` handle return
- **Gap:** Creates a button via `meleeUI.createScriptDialogButton(...)` but **returns `null`**, so scripts cannot register `TriggerRegisterDialogButtonEvent`.
- **Need:** Return the `CScriptDialogButton` handle.
- **Where:** `Jass2.java` ~2228–2234.

### 5. Selection natives for cinematic / scripted control
- **Gap:** `SelectUnit`, `ClearSelection`, `SelectGroup` are no-ops; `GroupEnumUnitsSelected` cannot see selection.
- **Need:** Wire to local `MeleeUI` selection state.
- **Where:** `Jass2.java` ~6150–6163; `MeleeUI` selection APIs.

### 6. Hero carry-over: learned abilities + proper name
- **Gap:** `StoredUnitData` has XP/stats/SP/items/`properName`, but **no ability id→level map**. `RestoreUnit` never re-learns skills and never applies `properName`.
- **Need:** Snapshot hero abilities on `StoreUnit`; on restore call skill-learn / `SetUnitAbilityLevel`; set proper name on `CAbilityHero`.
- **Where:** `StoredUnitData.java`, `CGameCache.java` serialization, `Jass2.snapshotUnit` / `RestoreUnit`.

### 7. Movie / intro cinematic playback
- **Gap:** `PlayCinematic` is an empty no-op (“movie playback is not yet implemented”). `PlayModelCinematic`, `SetIntroShotText`, `SetIntroShotModel` missing. No SMK/BIK (or Reforged video) decoder in-repo. CampaignInfo Intro/Open/End cinematic entries are parsed in `CampaignMenuData` but never played.
- **Need (minimum viable):** Blocking playback stub that advances campaign scripts (duration + skip), then real video decode for retail movie files.
- **Where:** `Jass2.java` ~6345–6348; new media handler; `MenuUI` / `CampaignMenuData` consumers.

### 8. Campaign AI bootstrap
- **Gap:** `StartCampaignAI`, `StartMeleeAI`, `CommandAI`, `PauseCompAI`, `GetAIDifficulty`, guard-position APIs missing. `JassAIEnvironment` is Sleep/StartThread-level only.
- **Need:** Load and run campaign `.ai` scripts with enough common.ai parity for stock missions.
- **Where:** AI environment + new natives in `Jass2.java`.

---

## P1 — Retail-critical mission features

### Campaign progress & menu state
- [ ] Persist mission / campaign / cinematic availability (`SetMissionAvailable` is no-op; `GetMissionAvailable` always `TRUE`)
- [ ] Implement missing availability natives: `SetCampaignAvailable`, `SetOpCinematicAvailable`, `SetEdCinematicAvailable`, `SetTutorialCleared`, `ForceCampaignSelectScreen`, `CustomCampaignButtonSetVisible`
- [ ] `SetCampaignMenuRace` / `Ex` + `GetCampaignMenuRace` (currently no-op / returns 0)
- [ ] Honor `CampaignMenuData.isDefaultOpen` for gating
- [ ] Enable or implement Custom Campaign / Load Saved / Options / Credits (`ENABLE_NOT_YET_IMPLEMENTED_BUTTONS=false` in `MenuUI`)

### Save / load UX & completeness
- [ ] Esc-menu Save/Load buttons (`MeleeUI` currently disabled)
- [ ] Main-menu Load Saved
- [ ] `ReloadGame` (currently logs “not implemented”)
- [ ] `SaveGameExists`, `CopySaveGame`, `RemoveSaveDirectory`, `RenameSaveDirectory`; `GetSaveBasicFilename` returns `""`
- [ ] `CachePlayerHeroData`
- [ ] Extend `SaveGame`/`LoadGame` beyond primitives (or document that campaign continuity is gamecache-only and match retail usage)

### In-mission UI parity
- [ ] Quest dialog UI (quest button disabled; `CQuest` is state-only)
- [ ] Multiboard rendering (`CMultiboard`: “Full rendering is not yet implemented”)
- [ ] Leaderboard API (`CreateLeaderboard` and family largely missing)
- [ ] Score screen after victory/defeat
- [ ] Dialog hotkey handling (`TODO use hotkey` in `MeleeUI`)

### Cinematic / presentation (in-map)
- [ ] Transmission voice / `soundLabel` playback (text-only today)
- [ ] Named transmission animations
- [ ] `ClearTransmissionQueue` / `EnableTransmission` (empty)
- [ ] `SetCinematicCamera` (no-op)
- [ ] Cine-filter API (`SetCineFilter*`, `DisplayCineFilter`, …)
- [ ] `PauseGame` (no-op; cinematics often pause)
- [ ] `CinematicSkipButton`, `SetCinematicAudio`, `SetSkyModel`

### Sound / music groups
- [ ] `VolumeGroupSetVolume` / `VolumeGroupReset` (no-ops)
- [ ] Stacked / ambient / remaining 3D sound setters
- [ ] Separate thematic music layer / fade parity
- [ ] `ClearMapMusic`

### Heroes / abilities / pathing used by scripts
- [ ] Fail soft or implement remaining ability rawcodes (`UnitAddAbility` “not been programmed yet”)
- [ ] `UnitModifySkillPoints`, `UnitStripHeroLevel`, `SetHeroProperName`, `DecUnitAbilityLevel`, `SetReservedLocalHeroButtons`
- [ ] Non-permanent `SetHeroStr` / `Agi` / `Int` (`Todo add else case`)
- [ ] `SetUnitPathing` (no-op; common in cinematics)

### Camera / images / blight (often used in campaign scripts)
- [ ] Missing camera noise / smooth / orient / rotate / stop variants
- [ ] Image / Ubersplat / blight natives largely missing

---

## P2 — Polish & verification

- [ ] `PolledWait` (BJ often wraps `TriggerSleepAction`)
- [ ] Trackables (NYI → null events)
- [ ] Chat event registration fires events
- [ ] `SyncStored*` (SP less critical)
- [ ] `HaveStoredMission`; make `ReloadGameCachesFromDisk` real
- [ ] Deduplicate early vs late `Store*` native registrations in `Jass2`
- [ ] Timer dialog color/speed
- [ ] `FlashQuestDialogButton` / `ForceQuestDialogUpdate`
- [ ] DefeatCondition as real type (dummy `Object` today)
- [ ] Campaign BLP alpha artifacts (README)
- [ ] Reforged FLAC quality (`docs/COMPATIBILITY.md`)
- [ ] `Cheat` native (cheat query natives exist)

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
