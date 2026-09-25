# Changelog

All notable changes to **OpenWCIII** (Warsmash-lineage engine) are documented
here. Product mission: faithful open-source Warcraft III, with QoL upgrades —
see [docs/MISSION.md](docs/MISSION.md).

Changes are grouped by category:

| Category | Meaning |
|---|---|
| `compat` | Hardware/driver/OS compatibility |
| `perf` | Performance or memory |
| `qol` | Quality of life (player or developer) |
| `render` | Rendering correctness or visuals |
| `fix` | General bug fix |
| `break` | Breaking change requiring user action |

---

## [Unreleased]

### fix
- Campaign hero carryover restores equipment before reconciling bonus stats, avoiding
  doubled item bonuses and duplicate techtree counts. Saved unit snapshots include
  this tick's creations and exclude pending removals.
- Human hero skill implementations are registered as fallbacks; script overrides
  invalidate cached ability types. Ability definitions load from launcher, test,
  and repository working directories, with deterministic ordering.
- Original campaign attack auras use legacy targeting defaults when expansion
  fields are absent; explicit map values still override those defaults.
- Interleaved movement searches keep separate scores and predecessor chains while
  sharing grid coordinates. Improved queue priorities are reordered correctly and
  resumed searches restore their collision size.
- JASS accepts unary-plus expressions used by the final Undead campaign AI.
  Script parsing failures now propagate instead of masquerading as loaded AI.
- Save format v5 preserves primitive script arrays and null strings while still
  reading v1–v4 saves. Restores reuse existing arrays and clear post-save entries.
  Saves write through a temporary file so serialization failures retain the last
  good save; malformed counts, value tags, array entries and trailing data are
  rejected. Main-menu load now waits for the queued map startup script to finish
  before applying saved state, preventing startup from overwriting it.
- Music honors fade-in duration, resumes the paused track, safely handles empty
  or missing playlists and invalid track indexes, and disposes replaced tracks.
- Campaign progress now persists per player profile without menu defaults erasing
  unlocks. Profile switching and deletion isolate progress.
- Closing a campaign map preserves shared game archives for the next chapter.
  Failed chapter transitions recover the menu, and in-place loads reject saves
  from other maps instead of applying unrelated globals/resources.
- Campaign audits propagate failures to Gradle, reject empty progression/soak
  runs, and report previously swallowed AI/object load errors. Corrected map
  override priority and unit-facing conversion in the soak runner. Removed the
  unsupported full-parity claim; complete mission resume and playthrough
  verification remain outstanding.
- **Gamecache in-session sharing and paired binary serialization**: `CommonEnvironment`
  now manages an in-memory gamecache registry `openGameCaches` (`InitGameCache`,
  `SaveGameCache`, `CachePlayerHeroData`, `ReloadGameCachesFromDisk`), guaranteeing
  that mutations in one trigger are immediately visible to subsequent triggers
  querying the cache within the mission session before writing to disk.
  `CGameCache.save()` now iterates directly over mission entries, eliminating
  binary key/value misalignment, and adds null safety for hero proper names and
  attributes. Covered by `CampaignHeroCarryoverTest`.
- **Default campaign difficulty parity**: `War3MapConfig.gameDifficulty` now defaults
  to `CMapDifficulty.NORMAL` (was previously `null`), and `GetGameDifficulty` in
  `Jass2.java` safely falls back to `NORMAL`. This guarantees correct script
  evaluations across 371 retail campaign script call sites checking
  `GetGameDifficulty() == MAP_DIFFICULTY_NORMAL`.
- **Campaign mission launch transition**: `MenuUI.launchCampaignMission` now
  sets `menuState = MenuState.GOING_TO_MAP` and triggers `campaignFade.setSequence("Birth")`
  so the fade-to-black animation runs cleanly prior to loading screen presentation.
  `startMap` and mission launches now provide a default fallback to slot 0 when no
  explicit W3I USER controller slot is flagged.
- **Versioned skin resolution precedence**: `GameUI.hasSkinField`, `getSkinField`,
  and `trySkinField` now check `file + "_V" + WarsmashConstants.GAME_VERSION`
  ahead of unversioned defaults. This resolves a long-standing defect where
  The Frozen Throne (`GAME_VERSION == 1`) always resolved Reign of Chaos
  defaults (`MainMenu3D.mdl`, `HumanCampaign3D.mdl`) because unversioned keys
  were evaluated first, preventing expansion backing screens from displaying.
  Covered by `GameUISkinResolutionTest`.
- **Menu backing screens camera FOV and animation polish**: accurate trigonometry
  calculates vertical FOV from horizontal MDX camera FOV based on actual viewport
  aspect ratio; non-birth backing screens start `Stand` with `MODEL_LOOP`
  without looping hitches; Battle.net door open/close transitions use
  `Stand Alternate` and `Morph Alternate` states; active campaign backdrops
  restore according to `CampaignProgressStore.campaignMenuRace`. Continuous
  verification provided by `BackingScreensRetailAuditTest` and
  `task campaignBackingAudit`.
- **Camera pan destination cleared on arrival**: when `panToTimed` (cinematics,
  scripted dialogues, or map events) moved the camera, `panDestination` was
  never cleared upon reaching the destination. As a result, mouse edge panning
  and middle-mouse dragging were overwritten every frame, locking the camera in
  place until keyboard arrow keys were pressed. The pan destination is now
  cleared once the camera reaches the destination, on minimap clicks, or on
  mouse dragging, covered by `GameCameraManagerTest`.
- **Ability Builder configs load again on Java 17**: Gson eagerly builds an
  adapter for every reachable field type, and the pooled enum functions added
  for spatial-query performance hold a `CSimulation` reference. The walk
  reached `CPlayerFogOfWar`'s `ByteBuffer`, which the module system refuses to
  open, so all 15 `abilityBehaviors` config files failed to parse and every
  Ability Builder ability was silently missing. Those pools are runtime state
  and are now `transient`, covered by `AbilityBuilderGsonBuilderTest`.
- **Scene lights no longer crash the render thread**: `LightInstance.bind` did
  a relative bulk put, so the light managers' buffer had nothing remaining when
  they set its limit and GL rejected the texture upload.
- **Older UI data boots**: the Battle.net account-email panels, password
  recovery button and change email button are treated as optional, since UI
  data from before those screens shipped has none of them.
- **Missing minimap icons are not fatal**: the entangled and haunted gold mine
  icons fall back to the gold mine icon on data that predates them.
- **Unparseable object data tables no longer abort map load**: a table whose
  layout does not match the parser is skipped with a warning, which is what
  already happened for tables that failed the end-marker check.
- **Cinematic presentation state tracked**: `SetSkyModel`,
  `SetCinematicCamera`, `PlayModelCinematic`, `SetIntroShotText` and
  `SetIntroShotModel` now persist normalized paths/values in the new
  `CinematicPresentationState` (exposed via `WarsmashUI`, stored by `MeleeUI`).
  `PlayModelCinematic` no longer routes through ffmpeg movie decoding and shows
  its own skippable letterbox overlay instead; sky-mesh swap and MDX
  camera-track playback remain pending. Covered by
  `CinematicPresentationStateTest`.
- **Thematic music on its own layer**: `PlayThematicMusic` / `Ex`,
  `PlayThematic`, `EndThematic(Music)` and `SetThematicMusicPlayPosition` now
  drive `MeleeUI` thematic methods backed by the new `ThematicMusicState`, so
  the map-music default survives thematic playback and end-thematic drops back
  to it. Fade intents are recorded; transitions still apply immediately.
  Covered by `ThematicMusicStateTest`.
- **SaveGame v2 records clock + camera**: saves now persist the time-of-day
  clock/scale and the local camera target alongside globals and resources, and
  `LoadGame`/`ReloadGame` restore them through a shared
  `restoreSaveGameState` helper. v1 files still load (new fields read as
  absent). Covered by new `CGameSaveTest` roundtrip and back-compat tests.
- **Real sky-mesh swap**: `SetSkyModel` now loads the sky model into the world
  scene (`War3MapViewer.setSkyModel`), loops its stand sequence, and re-centers
  it on the game camera every frame. Empty paths clear the sky; unresolvable
  paths keep the previous one so bad script calls cannot strand a mission
  skyless. All 13 retail sky paths validated present and parsing via the new
  `:desktop:campaignCinematicRefs` tool (0 missing across 85 maps).
- **MDX camera-track playback**: `SetCinematicCamera` models with a camera
  track now drive the world camera each frame (base + `KCTR`/`KTTR` offsets via
  the pure, unit-tested `CinematicCameraPlayer`; authored FOV/near/far; RTS
  motion suppressed while active; ESC ends the track and restores the game
  camera). Empty/missing/trackless models keep the previous stop-pans baseline.
  Retail validation found 0 campaign call sites, so this serves custom maps;
  `KCRL` roll stays ignored. Covered by `CinematicCameraPlayerTest`.
- **Main-menu Load Saved**: the retail `LoadSavedGameScreen` lists
  `~/.warsmash/saves/*.w3s`; selecting a save reloads its recorded map and
  `WarsmashGdxMapScreen` re-applies globals/resources/clock/camera after
  scripts boot. Saves record their map (`CGameSave` v3; pre-v3 saves are
  refused with a message rather than guessed), and QuickSave now records
  map/clock/camera like JASS saves. Corrupt saves and missing map assets reuse
  the existing error paths. Covered by new `CGameSaveTest` v3 roundtrip,
  back-compat, and save-listing tests.
- **Main-menu Credits**: launches the retail credits map
  (`WarCraftIIICredits.w3m`, falling back to `BonusCredits.w3m`) through the
  normal map path; the button stays disabled when neither map is in the data.
- **Main-menu Options, first pass**: the retail `OptionsMenu.fdf` opens with
  working Gameplay/Video/Sound tabs and OK/Cancel draft semantics; every
  slider and checkbox persists to `~/.warsmash/options.properties` via the new
  `OptionsSettingsStore` (covered by `OptionsSettingsStoreTest`). Music volume
  and toggle apply live to menu music and to missions at boot; the subtitles
  toggle applies to missions at boot through the new
  `setCinematicSubtitlesEnabled` (which also fixed the `|| true` that made
  subtitles unmutable). Sound/scroll/gamma/video-popup backends are still
  pending and documented in the parity audit.

### qol
- **Campaign progression spine audit**: Added `:desktop:campaignProgressionAudit`
  and CLI tool `CampaignProgressionAudit.java`, auditing all 85 retail campaign
  maps across Reign of Chaos and The Frozen Throne. 100% of all 70 distinct
  `ChangeLevel` / `SetNextLevelBJ` map progression links resolve to valid,
  readable map archives, backed by continuous verification in `CampaignLoadingTest`.
- **Campaign native coverage audit**: `./gradlew :desktop:campaignNativeAudit`
  extracts every retail campaign script from your own archives, walks the call
  graph through Blizzard.j, and reports which `common.j` natives the campaigns
  reach that the engine does not implement
  ([docs/CAMPAIGN_NATIVE_COVERAGE.md](docs/CAMPAIGN_NATIVE_COVERAGE.md)).
- Clarified product mission across README and docs: **faithful open-source
  Warcraft III** first, with **quality-of-life** upgrades on top
  ([docs/MISSION.md](docs/MISSION.md)).

### compat
- **`TriggerRegisterUnitInRange`** works: the simulation now watches a circle
  around the registered unit and fires the trigger as another unit crosses into
  it, once per entry, with the entering unit as the triggering unit. 24 campaign
  maps use it for the approach-and-talk triggers.
- **`CreateDeadDestructable`** and **`CreateDeadDestructableZ`** place a
  destroyed destructable, which is how maps lay out rubble at mission start.
- **`IsUnitPaused`** reports the unit's pause state.
- **Map config gets `CreateTimer` and `CreateGroup`**: Blizzard.j initializes
  timer and group globals where it declares them, so reading a map's config ran
  both before any config function did, and every map load logged seven
  missing-native errors.
- **Natives the campaigns call**: `SetRandomSeed`, `GetDefaultDifficulty`,
  `QuestCreateItem` (retail's name for the engine's `CreateQuestItem`),
  `UnitSuspendDecay`, `GetUnitDefaultMoveSpeed`, `IsUnitSelected`,
  `IsUnitVisible`, `IsUnitIllusion`, `IsLocationVisibleToPlayer`,
  `IsLocationFoggedToPlayer`, `IsLocationMaskedToPlayer`, `GetPlayerUnitCount`,
  `UnitHasItem`, `GetDestructableTypeId`, `SetDestructableMaxLife`,
  `SetTimeOfDayScale`, `GetTimeOfDayScale`, `DisplayTimedTextFromPlayer`,
  `SetCameraQuickPosition`, `PlayerSetLeaderboard`, `PlayerGetLeaderboard`,
  `LeaderboardHasPlayerItem` and `LeaderboardRemovePlayerItem` now work.
  Presentation switches with nothing to drive yet (`EnableOcclusion`,
  `EnableWorldFogBoundary`, `CameraSetSmoothingFactor`, the indicator and
  leaderboard styling calls) are accepted and ignored so scripts keep running.
- **Remaining audited campaign natives**: the 37 reachable-but-unimplemented
  natives from the campaign audit now resolve. `UnitRemoveBuffs` /
  `UnitRemoveBuffsEx` clear buff abilities, `UnitResetCooldown` clears ability
  cooldowns (`CUnit.clearAllAbilityCooldowns`), `UnitAddSleep` / `UnitWakeUp` /
  `UnitIsSleeping` drive the sleeping unit-type, `UnitApplyTimedLife` attaches
  a timed-life buff, `IsUnitIdType` checks hero / structure / targeting /
  classification flags, `SetBlightRect` paints blight over the rect,
  `UnitRemoveItemFromSlot` drops and returns the slotted item, and
  `SetSoundDistances` applies the max-distance cutoff. Doodad/destructable
  visuals (`SetDoodadAnimationRect`, `SetDestructableAnimationSpeed`,
  `QueueDestructableAnimation`, `ShowDestructable`, occluder heights),
  minimap icons, `RemoveWeatherEffect`, `TerrainDeformCrater`,
  `UnitUseItemPoint`, `IsUnitInTransport` / `IsUnitInvisible` / `IsUnitLoaded`,
  `SetItemTypeSlots`, `SetUnitTypeSlots`, `SetUnitUseFood`,
  `SetUnitCreepGuard`, `UnitIgnoreAlarm`, `DisplayLoadDialog` and
  `GetEventDamage` (zero until damage events carry data) are accepted so
  campaign scripts run without missing-native errors.
- **`TriggerRegisterPlayerStateEvent` fires**: registrations are watched every
  simulation tick (`CPlayerStateEvent`) and fire on the rising edge of the
  limit comparison, with `GetTriggerPlayer` and the new `GetEventPlayerState`
  populated in the event scope.
- **`RestartGame` restarts the mission**: reloads the running map through the
  `ChangeLevel` path (`War3MapViewer` now remembers its map file), honoring
  `doScoreScreen`.
- **Shop stock is tracked**: `AddItemToStock` extends the shop's sell-items
  catalog with current/max counts, purchases decrement the count and fail
  with out-of-stock at zero (`CAbilitySellItems`); `RemoveItemFromStock`
  pulls the entry. `AddUnitToStock` / `RemoveUnitFromStock` record
  mercenary counts on the shop unit (`CUnit` ledger). Timed replenish and
  unit-sale purchase gating are still TODO.
- **Campaign movies play real video**: `PlayCinematic` resolves DivX AVI
  movies through the data sources (`.mpq`, `.avi`, `.mp4` extensions and
  `Movies\` prefix) and decodes them with a user-provided `ffmpeg` (no codec
  bundled; `-Dwarsmash.ffmpeg=` / `WARSMASH_FFMPEG=` override, `ffmpeg` on
  `PATH` otherwise). Raw RGB frames stream with backpressure pacing into an
  aspect-ratio-preserved, centered fullscreen texture, game audio ducks
  during playback, movie audio streams to a PCM output device, the JASS
  thread sleeps for the true movie duration, and ESC / Space / Enter skips
  immediately while cleanly cancelling the sleep timer. Without `ffmpeg` or
  the movie file, the timed overlay fallback remains (see
  `docs/COMPATIBILITY.md`). In addition, `MenuUI` now instantiates campaign
  intro and outro cinematic buttons and refreshes their availability via
  `CampaignProgressStore`.
- **Campaign loading polished**: `War3MapViewer.beginLoadingMap` cross-resolves
  `.w3m` (Reign of Chaos) and `.w3x` (The Frozen Throne) extensions and
  normalizes path slashes with automatic `Maps\Campaign\` / `Maps\` fallback.
  Loading screen background visibility is restored for all subsequent mission
  loads; custom loading screen models (`LoadingScreenModel`) are respected, and
  `LoadingScreens` table lookups are safely guarded. Menu ambient loops and
  music are cleanly halted upon entering the loading screen.
  `ChangeLevel` preserves campaign state and binds the active profile name,
  `CampaignProgressStore.consumeForceCampaignSelectScreen()` routes campaign
  completions back to the campaign selection screen, and `OpenCinematic` is
  recognized alongside `IntroCinematic`. Covered by `CampaignLoadingTest`.

## Campaign Parity P0 Spine (2026-07-28)

### fix
- **`DialogAddButton`**: returns the created `button` handle so scripts can
  register `TriggerRegisterDialogButtonEvent`.
- **Selection natives**: `SelectUnit`, `ClearSelection`, `SelectGroup`, and
  `GroupEnumUnitsSelected` now drive local `MeleeUI` selection (including
  selection circles via `War3MapViewer.doSelectUnit`).
- **`ChangeLevel`**: implemented; unloads the current map and starts the next
  via `MenuUI` pending-change-level path; honors `doScoreScreen` Continue dialog.
- **Campaign chrome restore**: `MenuUI.onReturnFromGame()` re-shows mission
  select UI after leaving a mission (was commented out).
- **Hero carry-over**: `StoreUnit`/`RestoreUnit` now snapshot and restore
  learned hero abilities plus proper name; gamecache disk format bumped to v2
  (v1 files still load).
- **Campaign availability natives**: `SetMissionAvailable` /
  `GetMissionAvailable`, `SetCampaignAvailable`, cinematic availability,
  `SetTutorialCleared`, `ForceCampaignSelectScreen`,
  `CustomCampaignButtonSetVisible`, and campaign menu race natives backed by
  `CampaignProgressStore`.
- **`PlayCinematic` MVP**: fullscreen cinematic overlay + 5s JASS thread sleep
  (ESC skip via `CinematicSkipButton`); `PlayModelCinematic` / intro-shot
  natives stubbed so scripts bind.
- **`PauseGame`**: freezes unit/combat simulation while still advancing timers
  and JASS/AI threads (so cinematic sleeps keep working).
- **`EndGame`**: exits via the existing custom-victory menu path.
- **Campaign AI bootstrap**: `StartCampaignAI` / `StartMeleeAI` load
  `common.ai` + race script into `JassAIEnvironment` with working
  `StartThread`/`Sleep`/`GetAiPlayer`; AI scopes ticked from `CSimulation`.
- **Quest dialog**: quests button enabled; `CreateQuest` registers into a
  simple in-game quest log panel; flash/force-update natives wired.
- **Score screen MVP**: `CustomVictory`/`CustomDefeat`/`EndGame` with
  `enableScoreScreen=true` show a Continue dialog before exiting.
- **Multiboard overlay**: displayed boards render as a top-right text table;
  bulk `MultiboardSetItems*` and `MultiboardClear` implemented.
- **Leaderboard natives + overlay**: Create/Display/AddItem/etc. with a
  top-left text overlay.
- **Campaign menu gating**: mission/campaign buttons honor
  `CampaignProgressStore` and refresh on return from a mission.
- **`PolledWait`**, **`SaveGameExists`**, **`SetUnitPathing`**,
  **`CachePlayerHeroData`** implemented.
- **Transmission VO**: `TransmissionFrom*` plays `soundLabel` via UISounds;
  `ClearTransmissionQueue` / `EnableTransmission` wired.
- **Cine-filter MVP**: `SetCineFilter*` / `DisplayCineFilter` /
  `IsCineFilterDisplayed` drive a fullscreen tint overlay with color/UV lerp.
- **Volume groups**: `VolumeGroupSetVolume` / `VolumeGroupReset` (MUSIC applies
  to the music player).
- **Hero script natives**: `SetHeroProperName` / `BlzSetHeroProperName`,
  `UnitModifySkillPoints`, `UnitStripHeroLevel`, `DecUnitAbilityLevel`,
  `SetReservedLocalHeroButtons`; non-permanent `SetHeroStr`/`Agi`/`Int`.
- **AI unit counts + captain home**: `GetUnitCount` / `GetEnemyUnitCount` /
  `GetPlayerUnitTypeCount`; `SetCaptainHome` / `CaptainGoHome` /
  `CaptainAttack` / `GetCaptainX/Y` / `CaptainIsHome`.
- **Dialog hotkeys**: `DialogAddButton` hotkeys invoke the button from
  keyboard while the dialog is visible.
- **Save ops**: `CopySaveGame`, `RemoveSaveDirectory`, `RenameSaveDirectory`,
  `GetSaveBasicFilename`, `ReloadGame` (reloads last save globals).
- **`ClearMapMusic`**, camera **`StopCamera`** / noise natives, **`SetBlight`**,
  **`SetCinematicCamera`** baseline, **`CreateImage`/`ShowImage`/`SetImagePosition`**
  MVP, **`HaveStoredMission`**, basic **`Cheat`** strings.
- **Trackables**: `CreateTrackable` + hit/track events with mouse proximity.
- **Ubersplats**: `CreateUbersplat` / show / destroy MVP via terrain splat.
- **DefeatCondition** real type + quest-log listing.
- **`UnitAddAbility` soft-fail**: unknown rawcodes attach `CAbilityGenericDoNothing`
  instead of returning FALSE and aborting campaign scripts.
- **`ChangeLevel` score screen**: `doScoreScreen=true` shows Continue before
  chaining the next map.
- **`isDefaultOpen` gating**: campaign menu seeds availability from DefaultOpen
  (non-default campaigns stay locked until unlocked).
- **Named transmission anims**: `TransmissionFrom*WithNamedAnimation` selects
  portrait sequence by name.
- **`SetCinematicAudio`**: ducks MUSIC/AMBIENT while active.
- **`UnitShareVision`**: shares unit sight via fog modifiers.
- **AI assault MVP**: `AddAssault`/`AddDefenders` roster, `CaptainIsEmpty`/
  `CaptainGroupSize`, `CaptainAttack`/`CaptainGoHome` issue orders,
  `SuicidePlayer*` attack-moves combat units.
- **Chat events**: `TriggerRegisterPlayerChatEvent` registers+fires; Enter/chat
  button prompts; `GetEventPlayerChatString*` populated.
- **Terrain queries**: `IsTerrainPathable`, `GetTerrainType`,
  `GetTerrainVariance`; null-safe `GetTerrainCliffLevel`.
- **`PingMinimap` / `PingMinimapEx`**: timed colored minimap pings.
- **`EnableUserUI`**: maps to `enableUserControl` + `showInterface`.
- **Cleanup**: removed duplicate void `Store*` and early `SetSoundParamsFromLabel`
  registrations.
- **TimerDialog** title/time color + speed multiplier.
- **EnableSelect** / **EnableDragSelect** / **EnablePreSelect**, **ForceUIKey** /
  **ForceUICancel**.
- **Esc-menu Save/Load** QuickSave MVP; **SyncStored*** SP no-ops.

### test
- Extended `CGameCacheTest` for ability persistence.
- Added `CampaignProgressStoreTest`.
- Added `CPlayerEventChatMatchTest`.

---

## Campaign Reliability Pass (2026-03-07)

### fix
- **Trigger exception isolation**: `GlobalScope.runOneThreadLooop()` now drops
  faulting threads instead of propagating the exception when
  `CONTINUE_EXECUTING_ON_ERROR=true`.  `queueTrigger()` similarly wraps
  evaluation and execution in a try/catch so a bad trigger cannot crash the
  game loop.  `CSimulation` on-tick trigger loop guards each trigger
  individually.
- **SaveGame/LoadGame implemented**: `SaveGame` now serializes all JASS
  primitive globals (integer, real, boolean, string) and per-player resource
  totals to `~/.warsmash/saves/<name>.w3s`.  `LoadGame` reads the file and
  restores globals and player resources immediately.  Complex handle state
  (units, heroes) still requires full map-reload coordination but mission
  progress variables are now durable across crashes and intentional quits.
- **Item removal cleanup** (`CSimulation.removeItem`): the TODO on
  `setHidden(true)` is resolved — items are now properly deregistered from
  `worldCollision`, `handleIdToItem`, and the `items` list before being
  tombstoned.
- **`IsTimerDialogDisplayed`**: returns the real frame visibility state
  (previously hardcoded `false`).
- **`PlayThematic`/`EndThematic`**: now route through the existing music system
  (`playMusic` / `stopMusic + playMapMusic`).
- **`SetIntegerGameState`/`GetIntegerGameState`**: backed by an `EnumMap` in
  `CommonEnvironment`; values now persist for the session.
- **`CinematicMode`**: hides the HUD and disables user control on entry;
  restores both on exit.
- **`TriggerRegisterGameStateEvent` argument index bug**: `limitval` was
  incorrectly reading from arg index 2 (same as `opcode`) instead of arg 3.
- **`TriggerSleepAction`/`TriggerWaitForSound`**: no-thread edge case now logs
  and continues (with trigger handle context) instead of throwing when
  `CONTINUE_EXECUTING_ON_ERROR=true`.
- **`GetSoundDuration`**: null-guard added (was NPE-prone on a null sound handle).
- **`TriggerRegisterGameStateEvent` error spam**: `DIVINE_INTERVENTION` and
  `DISCONNECTED` states no longer print a stderr error; they silently return
  null handles (the correct behavior for not-yet-triggered events).

### test
- Added `CGameSaveTest` (11 tests): roundtrip for all primitive global types,
  player-resource roundtrip, map-path preservation, multi-global roundtrip,
  missing-file null return, corrupt-file null return, directory auto-creation.


| `qol` | Quality of life (player or developer) |
| `render` | Rendering correctness or visuals |
| `fix` | General bug fix |
| `break` | Breaking change requiring user action |

---

## Campaign Map Startup Reliability Pass (2026-03-06)

### fix
- **Campaign/skirmish startup failure handling hardened**: `MenuUI` now validates map preloading before hiding menu navigation state for campaign mission launches and direct `startMap(...)` calls. Failed preloads no longer proceed into a broken transition path; they now show `NETERROR_MAPFILEINCOMPLETE` and keep the menu usable.
- **Unified guarded map-config preload path**: a new `tryLoadAndCacheMapConfigs(...)` helper centralizes exception-safe handling for `loadAndCacheMapConfigs(...)`, covering both checked `IOException` and runtime parse/load failures encountered during map config/JASS loading.

## Phase D — Async Loading Pipeline Completion (2026-03-02)

### perf
- **Threaded map prefetch stage added**: `War3MapViewer` now exposes
  `createAsyncMapLoader(...)`, which performs tileset data-source setup and map
  parse/object-data preload (`w3e`, `wpm`, modifications) on a background
  worker thread before handing off to the existing render-thread map loader.
- **Blended loading progress**: loading completion ratio now reflects both the
  async prefetch stage and the render-thread loading-task stage, yielding more
  representative progress feedback on the loading screen.

### fix
- **Menu loading flow migrated to async loader**: `MenuUI` now uses
  `War3MapViewer.AsyncMapLoader` for map startup and closes loader resources on
  both success and failure paths to avoid lingering worker-thread resources.
- **Synchronous compatibility preserved**: existing `MapLoader` creation remains
  available (including the terrain editor flow), while optionally consuming
  preloaded async results when present.

### test
- Added `AsyncLoadCoordinatorTest` (5 tests) covering weighted progress,
  one-time prefetch→main-thread handoff, non-blocking behavior while prefetch
  is pending, failure propagation, and cancellation/close semantics.

### docs
- README and modernization analysis updated to mark Phase D as complete and to
  document the new async loading pipeline behavior.

## Performance/QoL/Docs Polish Pass (2026-03-02)

### perf
- **Lightning effect batch buffer accounting fixed**:
  `LightningEffectBatch` now correctly treats index-buffer capacity as a count
  of `short` elements (not bytes), uploads GL element buffers using byte sizes,
  and skips index-buffer rebuild/upload work when lightning instance count is
  unchanged across frames.
- **Hot-path reach checks avoid sqrt**: `CUnit.canReach(x, y, range)` now uses
  collision-aware squared-distance comparison for the non-pathing fast path.
  This removes repeated scalar sqrt work from high-frequency range queries.

### fix
- **Pathfinding edge bounds guard**: `CPathfindingProcessor` goal-neighborhood
  iteration now correctly uses `j < searchGraph.length` (not `<=`), preventing
  a potential out-of-range access near map boundaries.

### qol
- **Launcher override semantics made deterministic**:
  - profile presets are applied first;
  - explicit flags then override (`-window`, `-fps`, `-vsync/-novsync`, `-msaa`);
  - `-msaa 0` now explicitly disables MSAA.
- **`-help` is now display-independent**: launcher help exits before desktop
  display probing, allowing CLI help usage in headless/CI Linux environments.

### docs
- README reorganized with a clearer quick-start path, updated launcher flag
  behavior, and a tighter status/priority snapshot.
- `docs/ENGINE_MODERNIZATION_ANALYSIS.md` updated to include this polish pass in
  Phase D status tracking and to mark asset cache telemetry as complete.
- `docs/COMPATIBILITY.md` refreshed: removed stale fixed issues and added
  current guidance for 1.30/1.31 data-layout caveats and headless launcher use.

## Phase D — Finalization Pass (2026-03-02)

### fix
- **Canonical parser path for runtime mapped tables**: `MappedData.load(String)`
  no longer routes through legacy `SlkFile` / `IniFile`. It now parses with
  `DataTable.readSLK/readTXT` and ingests through `DataTableSource`, completing
  parser unification for terrain/splat/anim-sound table loads that flow through
  `MappedData`.
- **MappedData SLK type compatibility preserved**: when loading SLK buffers,
  `MappedData` now coerces canonical string cells back to legacy-compatible
  primitive types (`Float` and `Boolean`) so existing `MappedDataRow` numeric
  consumers continue to behave as before.
- **Event-object numeric parsing hardened**: `EventObjectEmitterObject` no longer
  blindly casts SLK fields to `Float`/`Number`; it now accepts both numeric and
  string-backed values, preventing `ClassCastException` risk if data source
  formats differ.
- **Destructable metadata SLK load fix**: `War3MapViewer.loadSLKs()` now applies
  `Units\\DestructableMetaData.slk` to `destructableMetaData` (previously it
  accidentally reloaded `DestructableData.slk` into that table).

### perf
- **ObjectPool wired into simulation allocations**:
  - `CWorldCollision.enumUnitsInRect`, `enumCorpsesInRect`, and
    `enumUnitsOrCorpsesInRect` now reuse pooled scratch `Set<CUnit>` instances
    instead of allocating a new `HashSet` on every call.
  - `CSimulation.update()` now uses a pooled scratch `Set<CTimer>` for duplicate
    timer validation instead of per-tick allocation.
  All pooled paths use `try/finally` acquire/release for exception-safe reuse.

### test
- `TableDataSourceTest` extended with
  `slkBooleansAndIntegersRemainTypedInMappedData`, asserting canonical-parser
  `MappedData` still exposes typed `Boolean` and numeric values for SLK cells.

### docs
- Updated README and modernization roadmap status for Phase D:
  parser remaining-caller migration and ObjectPool wiring now marked complete;
  async asset pipeline remains the primary pending Phase D item.

---

## Phase D — Implementation & Hardening (2026-03-02)

### perf
- **SimulationBudgetTracker wired**: `War3MapViewer.update()` now wraps each
  `CSimulation.update()` call with `SimulationBudgetTracker.beginTick()` /
  `endTick()`. Per-tick timing is reported every ~60 s with avg/max and overrun
  percentage.
- **Asset cache telemetry**: New `AssetCacheTelemetry` class instruments both
  cache paths in `ModelViewer` (`load()` and `loadGeneric()`). Hit/miss counts
  and hit-rate percentage are logged to stdout every 50 cache misses via
  `[AssetCache]` lines.

### compat
- **GL version guard**: `StartupDiagnostics.checkGLRequirements()` is now called
  from `WarsmashGdxMultiScreenGame.create()` immediately after the capability
  report. If the driver reports OpenGL < 3.3 the engine prints a user-readable
  error message (detected vs. required version, plus driver update suggestions)
  and exits with code 1. Requires no user action on supported hardware.
- **Named launch profiles**: `DesktopLauncher` accepts a new `-profile <name>`
  flag with three presets:
  - `safe` — windowed 1280×720, no MSAA, vsync on, 60 fps cap.
  - `balanced` — windowed 1280×720, 2× MSAA, vsync on, 60/30 fps cap.
  - `high` — fullscreen, 4× MSAA, vsync on, uncapped.
  Individual flags (`-window`, `-msaa`, `-fps`, etc.) still override the profile.
  Profile selection is announced in the startup log.

### fix
- **Server: O(n) disconnection lookup eliminated**: `GamingNetworkServerBusinessLogicImpl`
  previously iterated all active sessions to find the one matching a disconnected
  writer — an O(n) scan that became a DDoS amplification vector under load. A
  new `writerToSession` reverse-map is now kept in sync at `login()` /
  `killSession()`, reducing disconnection handling to O(1).
- **Server: login and account-creation rate limiting**: New `LoginRateLimiter`
  tracks failed auth attempts per remote address using a 60-second sliding window.
  After 5 failures the address is blocked for 5 minutes; subsequent requests are
  rejected before touching user storage. The block is announced in the server log
  with address, threshold, and cooldown duration.

### qol
- **Package ownership markers**: `package-info.java` files added for the four
  principal architecture layers — `render` (`viewer5`), `simulation`
  (`simulation`), `assets` (`datasources`), and `net` (`networking`) — documenting
  allowed and forbidden cross-layer dependencies.

### test
- `ObjectPoolTest` (8 tests): acquire/release round-trip, overflow behaviour,
  hit-rate calculation, `resetStats()` isolation.
- `SimulationBudgetTrackerTest` (5 tests): begin/end semantics, no-throw on
  zero-duration ticks, report-interval boundary at 3 600 ticks.
- `StartupDiagnosticsTest` (9 tests): `parseGLVersion()` against NVIDIA, Intel
  DCH, Mesa, ATI, and degenerate inputs.
- `AssetCacheTelemetryTest` (7 tests): hit/miss counting, hit-rate, reset, and
  periodic-report no-throw.

---

## Phase D — Parser Unification Kickoff (2026-03-02)

### fix
- Added a new read-only parser abstraction:
  `com.etheller.warsmash.util.table.TableDataSource`.
- Added adapter implementations for all current table stacks:
  - `SlkFileDataSource` (legacy `SlkFile`)
  - `IniFileDataSource` (legacy `IniFile`)
  - `DataTableSource` (canonical `DataTable`)
- Refactored `MappedData` to load through `TableDataSource` adapters instead of
  directly binding to parser implementations. This unifies the first caller onto
  the Phase D parser interface without changing existing runtime behavior.

### test
- Added `TableDataSourceTest` with:
  - SLK parity checks between `SlkFileDataSource` and `DataTableSource`
  - INI parity checks between `IniFileDataSource` and `DataTableSource`
  - A compatibility assertion that `MappedData` still exposes typed numeric
    SLK values needed by existing emitter-loading code paths

---

## Phase C — Render Hot-Path Performance (2026-03-02)

### perf
- **Light-data per-frame cache**: `LightInstance` now caches its packed 16-float
  GPU block in a `float[] cache` field guarded by a static generation counter.
  `LightInstance.advanceGeneration()` is called once per frame by
  `W3xSceneWorldLightManager.update()`. Subsequent `bind()` calls within the
  same frame bulk-copy from the cache via `FloatBuffer.put(float[], 0, 16)`
  instead of re-evaluating all keyframe tracks. Halves the number of keyframe
  sampler calls for every active point light regardless of how many GPU textures
  it is written into.
- **Separate unit/terrain light buffers**: `W3xSceneWorldLightManager` now owns
  a dedicated `unitLightBuffer` and `terrainLightBuffer`. Both are populated in
  a single loop over `this.lights`, eliminating the shared-buffer `clear()`/reuse
  pattern that forced sequential uploads and obscured buffer ownership.
- **Bone texture bulk copy**: `MdxComplexInstance.updateBoneTexture()` replaced
  16 absolute-indexed `FloatBuffer.put(int, float)` calls per bone with a single
  `FloatBuffer.put(float[], 0, 16)` bulk copy (JVM maps this to native `memcpy`).
  A trailing `flip()` correctly positions the buffer for `DataTexture.bindAndUpdate`.
  For a model with 80 bones this reduces per-frame JNI scalar writes from 1,280
  to 80 bulk copies.
- **Frame-pacing p95/p99**: `FramePacingTracker.report()` now sorts a copy of
  its ring buffer and includes the 95th- and 99th-percentile frame times in the
  60-second summary line. A spike-detection warning fires when p99 exceeds 3×
  the window average. Zero per-frame overhead — sorting happens only during the
  periodic report.
- **`ObjectPool<T>`**: New `com.etheller.warsmash.util.ObjectPool<T>` — a
  fixed-capacity stack-backed pool with `acquire()`/`release()` semantics and a
  `hitRate()` diagnostic. Provides the infrastructure for reducing GC pressure in
  particle-emission and simulation-allocation hot paths in Phase D.
- **`SimulationBudgetTracker`**: New `com.etheller.warsmash.util.SimulationBudgetTracker`
  measures wall time around any simulation block via `beginTick()`/`endTick()`.
  Reports avg/max per-tick cost, configured budget (default 8 ms), and overrun
  count every ~60 s. Ready to be wired around `CSimulation.step()` in Phase D.

### docs
- **OpenMW-equivalent vision**: README rewritten to open with Warsmash's long-term
  goal of being the OpenMW for Warcraft III. Roadmap table extended to include
  Phases C–F.
- **ENGINE_MODERNIZATION_ANALYSIS.md** updated: Phase C deliverables documented
  in detail; Phases D (parser unification, async pipeline, server hardening),
  E (scripting, map format), and F (modding layer) added with item lists.

---

## User-Testing Readiness (2026-03-02)

### fix
- **Particle emitters killed on view-cull**: `MdxComplexInstance.removeLights()`
  previously called `particleEmitter.onRemove()` for every emitter when the
  instance was pruned from the visible list during `Scene.update()`. This tore
  down particle effects any time a unit scrolled off screen. The fix separates
  concerns: `removeLights()` now only deregisters `LightInstance` objects;
  particle-emitter teardown is moved to a new `onInstanceRemoved()` hook that is
  called only from `Scene.removeInstance()` (permanent removal). View-culled
  instances continue to deregister their lights correctly.
- **Ghost batched instances after removal**: `Scene.removeInstance()` removed the
  instance from `this.instances` but not from `this.batchedInstances`. A removed
  batched unit could persist in the render list until the next full frame prune,
  producing a one-frame ghost. `batchedInstances.remove(instance)` is now called
  unconditionally.
- **vsHd GLSL divide-by-zero with empty light texture**: The first light in the
  `vsHd` vertex shader was read unconditionally via `0.5 / u_lightTextureHeight`,
  which evaluates to `+Infinity` in GLSL when no lights are active
  (`u_lightTextureHeight == 0`). The read is now guarded by
  `if (u_lightTextureHeight > 0.5)` and `v_lightDir` is pre-initialised to
  `vec4(0.0)` so the downstream fragment shader receives a safe value when the
  scene has no dynamic lights.
- **Uninitialised `mat4 bone` in vertex-group shader path**: The non-SKIN
  `getVertexGroupMatrix()` function in `Shaders.transforms` declared `mat4 bone;`
  without an initialiser. GLSL 3.30 core does not zero-initialise locals, so bone
  accumulation operated on undefined memory and could produce corrupted vertex
  positions for vertex-group-animated models. Changed to `mat4 bone = mat4(0.0);`.

### qol
- **Paired log file names**: `DesktopLauncher` now captures a single
  `System.currentTimeMillis()` value and uses it for both the `.out.log` and
  `.err.log` filenames, ensuring the two files from the same session share the
  same timestamp prefix.

### test
- Added 3 new unit tests to `MdxShadersTest` covering the shader fixes:
  `vsHd_firstLightIsGuardedByLightCount`, `vsHd_defaultsVLightDirToZero`, and
  `transforms_nonSkinPath_boneMatrixInitialised`. Total test count: 23.

---

## Phase B — Stability & Shader Normalization (2026-03-02)

### fix
- **Light-system memory leak**: `Scene.update()` now calls `removeLights(scene)`
  on every instance pruned from the active list before removing it, so orphaned
  `LightInstance` objects are properly unregistered from
  `W3xSceneWorldLightManager`. Previously these remained in the light manager
  indefinitely, causing unbounded memory growth and frame-time drift on long
  sessions.

### perf
- `W3xSceneWorldLightManager.remove()` is now idempotent: `ArrayList.remove()`
  silently ignores missing elements, preventing spurious state corruption on
  double-removal.
- `W3xSceneWorldLightManager` logs active dynamic light count to stdout every
  ~60 seconds (`[LightManager] active dynamic lights=N`) so the leak fix can be
  verified without a heap profiler.

### render
- **GLSL version normalization**: MDX HD shaders (`vsHd` / `fsHd`) upgraded
  from `#version 120` to `#version 330 core`:
  - `attribute` → `in` (vertex inputs).
  - `varying` → `out` (vertex) / `in` (fragment) for all interpolated
    variables.
  - `texture2D()` → `texture()` for all active sampler calls.
  - `gl_FragColor` replaced by an explicit `out vec4 fragColor` declaration.
  - `Shaders.boneTexture` embedded in `vsHd` updated via a `#version 330
    core`-specific copy (`BONE_TEXTURE_330`) that replaces `texture2D` with
    `texture`.
  - `Shaders.transforms` (used exclusively by `vsHd`) updated: `attribute`
    → `in` for all vertex-input declarations.
- Test shaders in `WarsmashTestGame2` and `WarsmashTestGame3` lowered from
  `#version 450 core` to `#version 330 core` — no 450-specific features were
  used.

### docs
- Added `docs/PARSER_CONSOLIDATION_DESIGN.md`: design document for unifying
  the duplicate SLK/INI parser stacks behind a single `TableDataSource`
  interface (implementation deferred to Phase C).

---

## Phase A — Compatibility, Diagnostics & Documentation (2026-03-02)

### qol
- Added startup capability report: GL vendor/renderer/version, GLSL version,
  display resolution, Java version, and OS info are printed to stdout at launch.
- Added frame-pacing diagnostics: min/max/average frame time and effective FPS
  are logged to stdout every 60 seconds.
- Added `-validate` / `--validate` launcher flag: checks that every data source
  path declared in `warsmash.ini` exists on disk and exits with a pass/fail
  summary without starting the game window.
- Added `-help` launcher flag (and `--help`, `-h` aliases) to print all
  available command-line options and exit.
- Added `-window` / `-windowed [width height]` launcher flag to start in
  windowed mode instead of fullscreen (defaults to 1280×720).
- Added `-vsync` / `-novsync` launcher flags to force VSync on or off.
- Added `-fps <value>` launcher flag to cap foreground and background frame rate
  (`0` = uncapped).
- Added `-msaa <samples>` launcher flag to control MSAA sample count.
- Added `-ini <path>` launcher flag to specify a custom INI file.
- Added `-loadfile <path>` launcher flag to auto-load a map or TOC file.
- Added `-nolog` launcher flag to keep stdout/stderr on the console instead of
  writing to `Logs/` files.
- Added `CONTRIBUTING.md` with coding conventions, architecture overview, and
  profiling workflow.
- Added `docs/COMPATIBILITY.md` with tested GPU/OS/driver configurations,
  known issues, and troubleshooting steps.
- Added GitHub Actions CI workflow: compiles all subprojects on Ubuntu and
  Windows against Java 17 and 21.

### compat
- Added `docs/COMPATIBILITY.md` documenting supported Warcraft III patch asset
  layouts, known caveats, and minimum hardware/software requirements.
- Upgraded Gradle wrapper from 7.3.3 to 8.6 for Java 21 compatibility.
- Upgraded `org.beryx.runtime` Gradle plugin from 1.12.5 to 1.13.1.

---

## Pre-changelog history

Earlier development history is tracked via git log. See `git log --oneline`
for a summary of changes before this changelog was introduced.
