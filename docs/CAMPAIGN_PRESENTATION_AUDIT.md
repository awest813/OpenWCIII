# Campaign screens, messages, and events audit

## September 27: opening-mission black screen

The NightElfX01 opening was reproduced with the real mission scripts. Its
`Preloader("Scripts\\NightElfX01.pld")` loaded a functions-only script into the
existing interpreter. `JassProgram.initialize()` then called the stale global
initializer again, resetting mission handles to null. `IsPlayerInForce` failed
inside `CinematicModeExBJ` after the black fade had been displayed. The mission
thread stopped before applying its camera, revealing the scene, or restoring UI.

After initialization, the interpreter now removes the completed global-initializer
entry point. Later script loads initialize only newly declared globals.
Two regressions failed before this fix and pass after it: functions-only/empty
loads preserve mission values and handles, and later globals initialize once.
The full core suite passed 257 tests with zero failures/errors/skips and retail
fixtures required (`Logs/black-screen-regression.txt`).

The rendered startup probe now retains Maiev (`Ewrd`), turns off cinematic fog,
advances camera shots, and clears the fade. This is targeted startup evidence,
not a mission completion or visual/audio approval. Existing missing-asset and
sound-preload warnings remain separate issues.

For a bounded reproduction that logs camera, player units, fog, and filter
state, then closes the game automatically (seconds begin at map rendering):

```powershell
.\gradlew.bat :desktop:runGame '-Pargs=-window 800 600 -nolog -loadfile Maps\FrozenThrone\Campaign\NightElfX01.w3x' -PstartupProbeSeconds=180
```

Normal launches do not enable this diagnostic or auto-exit behavior.

### Post-intro fog blackout follow-up

The user's screenshot and the follow-up probe exposed a second failure after
cinematic cleanup: `ResetTerrainFog` restored LINEAR black fog with start/end
both zero, despite the camera target being VISIBLE and lighting remaining valid.
Retail TFT `DefaultZFog` has one record (start 20000, end 50000), while `MenuZFog`
has separate RoC/TFT records. Selecting index 1 unconditionally read missing
numeric entries as zero. Fog parsing now falls back to the first available
record per field and treats colors as complete four-component records. Explicit
expansion values, including zero, are preserved.

The fog-of-war texture now uploads tightly packed rows with unpack alignment 1
and restores the previous OpenGL alignment afterward. A 129-column row regression
models GPU row stride to catch visibility drifting between rows. Three fog-setting
tests cover retail defaults, expansion menu overrides, and whole-color fallback.
All 261 core tests passed with retail fixtures required and zero failures/errors/
skips (`Logs/fog-black-screen-regression.txt`). Broader particle, missing-asset,
and cinematic visual parity is still unverified.

The corrected 75-second rendered probe (`Logs/fog-black-screen-fixed.txt`)
reached gameplay with fog/mask enabled, the cinematic filter cleared, and the
camera target VISIBLE. From tick 800 through tick 1500, restored terrain fog
retained start 20000 and end 50000, including after the camera moved. No new
JASS error log was produced. This verifies the faulty reset no longer occurs;
the corrected image and broader effects still require visual confirmation.

### Intro sky occluding terrain

The user confirmed gameplay now works, but the intro still shows unusual
cloud/fog patches. The retail LordaeronSummerSky mesh spans approximately
-1957 to +1967 units around its origin (`Logs/cinematic-sky-bounds.txt`). The
engine centered this mesh on the camera and rendered it with ordinary world
geometry. Its surface could therefore obscure terrain beyond that radius,
leaving a small patch of terrain inside the dome visible.

The sky now renders before terrain in a separate background pass. Its color
is retained while its depth is cleared, and both later world passes exclude
the sky instance. The mission's authored blue distance fog remains in place.
The desktop `skyBackgroundRenderAudit` task reads actual OpenGL pixels to
check that distant terrain covers a nearer sky surface, sky color remains
visible elsewhere, and the sky is not drawn again over the world. This task
requires an available desktop graphics context; it is separate from the core
test suite. It passed alongside all 261 core tests (zero failures/errors/skips,
retail data required); evidence: `Logs/intro-sky-regression.txt`. The running
game was visually checked and left in progress with the prior build. Full
intro visual confirmation on the sky fix remains outstanding.

Reviewed September 25, 2026. Scope: campaign menus and chapter presentation;
no gameplay balance or skirmish changes were sought in this pass. Some fixes
live in shared UI/native code used by campaign scripts.

## Findings and changes

| Area | Finding | Change and evidence |
|---|---|---|
| Campaign/chapter selection | Locked labels retained their normal colors; a stale click could activate the wrapper after locking | Dim locked labels, preserve dimming on hover, restore skin colors when enabled, reject hidden/disabled activation. Tests instantiate the real button and label classes |
| Victory/defeat presentation | `CustomVictory` and `CustomDefeat` showed a local outcome for any player | Native handlers retain their simulation events but route presentation only to the named local player. Routing tests cover local, other, and null-player cases |
| Outcome/next-chapter dialog | Repeated Continue callbacks could repeat exits/loads; replaced callbacks retained their actions | Replaceable, single-use continuations reject stale and repeated clicks. Tests cover replacement, cancellation, and reentrant replacement |
| Cinematic transmissions | Explicit scene end left the letterbox portrait and subtitles active; lifetime shared animation counters | End clears portrait panel, speaker, and dialogue. Separate lifetime handles expiration, replacement, explicit end, and invalid durations; tests cover these state transitions |
| Movie fallback | Unavailable playback still said “Playing” | Loading text is replaced with a plain unavailable message and a Continue hint only when skipping is enabled. The movie overlay clears the previous portrait and makes its message panel visible |
| UI initialization | Text-frame loading allocated the debug graphics renderer even with debugging off | Allocate it only when rendering debug outlines; real label-state tests now run without a graphics context |

## Validation boundaries

Run the suite with owned retail fixtures required:

```powershell
.\gradlew.bat :core:test -PretailAssets=F:/WC3Data -PrequireRetailData=true
```

Result: **242 tests passed, zero failures/errors/skips**, with retail fixtures
required. Nine tests were added for this pass.

Evidence log: `Logs/campaign-presentation.txt`. Retail asset parsing remains part
of the full suite. The new tests exercise event-routing policy, continuation
state, cinematic lifetimes, and real button/label state. They do not render the
screens or execute the full JASS native-to-screen path. Native event firing was
reviewed in code; its existing calls remain in place.

## Remaining campaign presentation work

- Victory/defeat use a simple outcome dialog, not the retail statistics screen.
- Several outcome labels and fallback messages are English literals rather than
  verified localized retail strings.
- `DisplayTimedTextToPlayer` still ignores requested x/y placement in the UI.
- Rendered chapter selection, letterbox layout, portrait animation, subtitle
  wrapping, hotkeys/focus, movie audio/video synchronization, and transitions
  need in-game inspection at supported resolutions.
- A complete mission playthrough must verify objective messages, cinematic-skip
  triggers, victory/defeat events, and next-chapter loading together. Passing
  headless tests or idle campaign smoke checks does not establish that result.
