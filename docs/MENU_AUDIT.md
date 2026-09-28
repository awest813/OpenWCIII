# Menu audit and polish

## September 27 live follow-up

- Confirmed a duplicate menu ambient-loop start during initialization and show.
  Removed the initialization start; screen-owned playback now stops earlier
  copies before restarting. Menu teardown stops ambient audio and disposes music.
  The user confirmed that the overlap was gone in the relaunched build.
- Observed the Options panels drawing on top of each other. The scoped frame
  search used a reverse-positioned iterator with forward traversal and found no
  descendants. Corrected traversal, restored main-menu buttons on OK/Cancel,
  and hid the underlying main-menu controls while Options is open. Unimplemented
  dropdowns now display a disabled "Unavailable" label instead of template text.
- Populated campaign difficulty with Easy/Normal/Hard and applied the selection
  to campaign map configuration, including subsequent campaign map loads. This
  selection currently lasts for the menu session; profile persistence is pending.
- Routed movie cinematics to a dedicated playback screen with audio, aspect-ratio
  preservation, Escape skip, cleanup, and return to the campaign menu. Scripted
  cinematic maps still use map loading. Missing movie/decoder errors are explicit.
  Follow-up: visually identified the Illidan opening sequence in `tftmovie136.avi`
  and copied it to `F:/WC3Data/Movies/IntroX.avi`, preserving the original. SHA-256:
  `FC1689EEFF44B657BB0E93A950BF00D7E712A5C869A81F7ABA958FB50B61ACEF`.
  Full video/audio decoding completed with no errors. Identification is visual;
  the installer archive has no listfile and probing the canonical name failed.
  Added optional Emulator `FfmpegPath` configuration and configured this machine's
  existing decoder. No movies or decoder binaries are checked in.
  Live startup exposed missing TEXTBUTTON label inflation, now repaired;
  the updated game reached the main menu. In-game movie playback remains unverified.

The latest Options, difficulty, and movie changes still need live verification.
Desktop control repeatedly rejected input after detecting local user interaction.
No mission has been played through, and no end-to-end campaign pass is claimed.
Regression evidence: `Logs/menu-campaign-blockers-full.txt`; tests cover nested
frame lookup, exclusive audio playback, and map-versus-movie classification.

Reviewed September 25, 2026. This pass covers menu input, focus, buttons, and
message/confirmation dialog lifecycle. It is a code and headless-state audit,
not rendered visual approval.

| Area | Fix | Verification |
|---|---|---|
| Dialog sequences | Close the old dialog before invoking callbacks, so callbacks can safely open a follow-up | State tests verify dismissal ordering, follow-up survival, cancellation, and stale/repeated response rejection |
| Keyboard focus | Tab wraps forward; Shift+Tab wraps backward; hidden and non-focusable fields are skipped; unavailable focus is cleared before input | Navigation tests cover first focus, both wrap directions, hidden/disabled fields, one field, and no eligible fields |
| Modal input | Dialogs consume keyboard input; Escape cancels questions or dismisses messages; Enter acknowledges messages. Question confirmation still uses its explicit button | Routing reviewed in MenuUI/DialogWar3; held-key repeats, typed input, and release of a consumed key are blocked from the underlying menu |
| Back/cancel | Escape activates the visible enabled Back/Cancel button on single-player, campaign/chapter, skirmish, load-save, and options screens | Existing button callbacks are reused; transitions and other states are excluded |
| Buttons | Disabled buttons and buttons under hidden ancestors reject direct activation; disabling clears hover highlight | Tests instantiate real GlueButtonFrame and parent panels |

Validation: **248 core tests passed; zero failures/errors/skips**, with retail
fixtures required. Six tests were added. Evidence: `Logs/menu-audit.txt`.

```powershell
.\gradlew.bat :core:test -PretailAssets=F:/WC3Data -PrequireRetailData=true
```

Remaining review: rendered alignment, clipping and text wrapping at supported
resolutions; full mouse/keyboard walkthroughs of profile and online menus;
localization; controller support; tab focus for controls that do not implement
FocusableFrame. Escape routing is intentionally limited to the listed screens.
The keyboard wiring and modal visuals have not been exercised in a live window.
