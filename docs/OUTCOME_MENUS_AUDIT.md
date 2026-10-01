# Victory and defeat menus

September 29, 2026. Windows, Java 17.0.20, Gradle 8.6, classic retail MPQs.
Archive fingerprints are recorded in [the campaign flow audit](CAMPAIGN_FLOW_AUDIT.md).

## Behavior

- `RemovePlayer`, `CustomVictory`, and `CustomDefeat` record the player result.
  `EndGame` uses the local result, so quitting a defeated campaign no longer
  displays Victory. A running mission starts with a neutral result.
- Result summaries show a prominent gold Victory or red Defeat heading, chapter
  title (with a map-name fallback), mission status, difficulty, and simulation
  elapsed time using the existing skin. They show resources remaining and living
  units, structures, and heroes owned by the local player when the panel opens.
  These are end-state snapshots, not lifetime gathered resources or score totals.
- Next-chapter results offer Continue and Quit Mission. Defeat results offer
  Restart Mission and Quit Mission when the current map is known. In-game
  restart confirmation offers Restart and Cancel; Escape cancels it and
  restores the previous pause/interface state. A defeated mission cannot be
  resumed through Cancel.
- Enter activates the first enabled choice; letter hotkeys and mouse clicks
  use the same button actions. Dialogs consume gameplay keys, scrolling, and
  clicks outside their bounds. Escape cannot dispatch a cinematic skip while
  a script dialog is open. Alternative result buttons share one guarded decision.
- Script dialogs resolve map string references, center their panels, wrap
  messages, and compute height from actual content instead of repeatedly
  multiplying panel height. Cleared dialogs remain tracked for keyboard input.
- Result screens hide the HUD, cinematic panel, portrait scene, and FPS overlay.
  Late script requests to restore the HUD cannot cover a visible result menu.
  Hidden scenes skip framebuffer setup/clearing so a hidden portrait cannot
  leave a black rectangle over the world. A GPU pixel regression covers this.
  Retail script-controlled choice dialogs retain their existing presentation.
- A dimmed backdrop separates results from the battlefield. Existing and late
  quest/error messages and cinematic filters stay hidden while the result is
  visible; cancelling an in-game restart restores them. Held arrow keys and
  edge scrolling cannot move the camera beneath any script dialog.
- The retail Load Game choice previously disappeared into a no-op. It now
  lists complete saves for the current profile and offers Back, which restores
  the originating paused defeat choices. An empty picker explains that no saves
  are available. The main-menu Load Saved flow uses the same v7 checkpoint
  service; see [MISSION_SAVE_RESUME_AUDIT.md](MISSION_SAVE_RESUME_AUDIT.md).

## Verification

```sh
./gradlew :core:test -PrequireRetailData=true
./gradlew :desktop:outcomeMenuRenderAudit
./gradlew :desktop:skyBackgroundRenderAudit
./gradlew :desktop:campaignFlowRenderAudit -PcampaignObjectiveEvents=true
```

The desktop menu audit uses fresh isolated preferences and Human01. It cancels
an in-game restart, invokes the retail defeat action, captures the four-choice
defeat menu at 960×540, 800×600, and 600×800, exercises Load/Back, and clicks
Quit through the real mouse input path. It checks the Defeat heading, uses
Enter to retry, verifies a fresh living hero and neutral result, invokes retail
victory actions, clicks Quit, verifies the Victory heading, and uses Enter to
return to the menu. Bounds assertions reject overlapping or out-of-panel buttons.
It also attempts to restore the gameplay HUD over a result and verifies that
the result remains in control of presentation. Both expanded result panels are
captured at all three sizes, with assertions for chapter naming, summary content,
held camera input, and suppressed late quest messages/cinematic filters.

The task writes `desktop/build/outcome-menu.log` and twelve PNGs under
`desktop/build/outcome-menu-captures`. Missing completion, JASS exceptions, or
GL errors fail the task; the process has a three-minute limit. Captures were
visually reviewed. Local command output is in `Logs/outcome-menu-verification.txt`
and `Logs/outcome-menu-followup.txt`; the final backdrop-only rerender is in
`Logs/outcome-menu-backdrop.txt`.
The companion progression audit verifies Continue into Human02 with hero
carryover and difficulty restoration. Core coverage includes mutually exclusive
retry/quit and stale result callbacks.

Final combined verification passed: 323 core tests, zero failures/errors/skips,
the GPU framebuffer audit, the menu render audit, and the campaign objective
event audit. The final victory capture no longer contains the portrait's black
rectangle; the live audit also rejects a portrait restored by a late script.
The expanded summaries passed the core suite and campaign objective-event
regression again. Twelve final captures were produced and result panels were
visually reviewed. The dimmed backdrop covers the full viewport in wide,
standard, and portrait layouts.

The September 30 pre-commit verification passed `assemble`, all **361** strict
retail-data core tests with no failures/errors/skips, and a fresh
`outcomeMenuRenderAudit`. Victory/defeat, retry, Quit, restart cancellation,
modal input and all three layouts passed again. The regenerated portrait victory
and wide defeat result captures were visually reviewed. The accompanying
`missionResumeFailureAudit` passed fresh-process continuation, defeat-to-load,
victory carryover and both engine/state rejection paths.

These are mission result menus, not a complete retail statistics screen.
Race-specific animated score backdrops, full score totals, multiplayer result
tables, broad save/resume coverage, and full campaign playthroughs remain
outside this verification. Existing missing-asset warnings remain in local logs.
