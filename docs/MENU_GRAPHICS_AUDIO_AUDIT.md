# Main menu graphics and initial audio audit

Audited September 28, 2026.

## Menu graphics

- Retail backdrop/skin/camera/sequence audit: all 47 checks pass.
- Fixed portrait/tall-window backdrop centering: top and bottom margins are now
  equal, matching the fitted UI viewport. Widescreen and 4:3 behavior is retained.
- Ignore zero-size resize events and refresh the UI camera after positioning it.
- Disable scissoring and enable depth writes before clearing depth for the UI
  overlay, so prior translucent model state cannot prevent a complete clear.
- Disable depth testing for the 2D menu overlay so the interface is independent
  of depth state left by the preceding 3D materials.
- Correct partial backdrop borders: vertical edge UVs are now normalized and
  fractional edge lengths are retained on all four sides. Background insets no
  longer shrink the authored tile size. A GPU gradient-atlas check covers all four
  partial edges and inset background tiling.
- Hide the menu FPS overlay by default; opt in with `-Dwarsmash.showMenuFps=true`.
  Debug text also restores the shared font color after drawing.
- Compared button textures and fog values directly with the installed retail
  archives. The blue button artwork and pale TFT fog are authored asset settings.
- Smooth magnified low-resolution, unshaded additive textures in the menu backdrop.
  The TFT ice halo overlaps several planes using the 32x32 `Blue_Glow2.blp` texture.
  Positive cubic B-spline filtering softens the bilinear texture-grid transitions
  without introducing ringing at dark edges. It is enabled only for backdrop
  textures up to 64x64 and only during magnification; ordinary surfaces, UI and
  gameplay scenes retain their existing sampling. This is a visual polish change,
  not a claim of reproducing the retail renderer exactly.
  GPU checks cover a magnified impulse, neutral black, constant white/alpha and
  restoring ordinary sampling. Menu captures include `-bilinear.png` baselines
  rendered at the same animation frame for direct visual comparison.
- Added an opt-in render/capture audit at 800x600, 1280x720, and 600x800, using
  isolated preferences. Captures are under desktop/build/menu-audit-captures.
- Fixed the exposed lower geometry edge in the TFT main-menu backdrop. The menu
  treated the authored FOV as a conventional horizontal angle, producing a vertical
  view of about 0.817 radians at 4:3. Using Warcraft's diagonal-scaled projection,
  `fovY = authoredFov / sqrt(aspect * aspect + 1)`, gives about 0.628 radians and
  keeps the finite scene geometry covering the viewport. Authored camera position,
  target and clip planes are preserved.
  The projection convention is based on [Wareditor's first-hand investigation of
  the Warcraft III 1.26 projection matrix](https://www.hiveworkshop.com/threads/making-an-accurate-get-mouse-screen-position.350018/).
  Applying that convention to glue cameras is supported by the local render result;
  this is not a claim of pixel-perfect retail parity.
- Visually inspected corrected captures at all three sizes: the gap is gone,
  controls remain readable and aligned, and portrait framing is centered.
- The render audit now checks a water strip near the bottom of the fitted backdrop
  for exposed clear-black pixels, excluding window margins and UI controls. A
  negative-control run with the old projection correctly failed (10077 of 10080
  pixels clear black in the first capture). This catches the original visual defect
  even when asset loading and OpenGL checks succeed.

## Initial audio work

- Shared sound-source playback now applies the saved sound-effects volume/mute.
  Invalid or muted gain does not start a source.
- Unit acknowledgements respect the Unit Sounds toggle.
- Menu ambience respects Ambient Sounds and reapplies committed sound settings
  when Options is saved.
- Script-created looping sounds stop their own previous instance before restart,
  report playback only after a successful start, and never stop unrelated sound
  instances when no valid playback handle exists.
- Script volume updates now affect the active instance with master gain applied.
- Invalid unit-sound variant indices fail silently instead of throwing.

- Label-created script sounds now own independent playback handles, retain their
  volume/pitch/position settings across restarts, honor the requested loop and 2D
  flags, and expose their selected sound duration before first playback.
- Active filename/label script sounds refresh master volume and positional mode
  on the render thread. Muting/unmuting an already-playing loop changes its gain
  without restarting it. Weak references avoid retaining discarded handles.
- Positional Audio now controls spatial playback for shared sound sources and
  updates active script sounds. Live script position and pitch updates reach the
  desktop backend.

Audio remains an initial pass: direct UnitSound world loops do not yet all refresh
when master settings change. Movement/world-ambient categories, EAX, fade behavior,
and listening on physical devices still need further auditing.
Tests use a recording audio backend; no claim of audible quality is made.

## Verification

320 tests pass with no failures or skips. Desktop compilation and the real menu
render audit pass; the retail asset audit passes 47/47 checks.
The GPU graphics audit also passes the menu border and tile sampling checks.

## Reproduction

- `./gradlew :desktop:campaignBackingAudit`
- `./gradlew :desktop:menuGraphicsAudit`
- `./gradlew :core:test`

The menu audit opens a short-lived window and captures rendered frames. It checks
an explicit completion marker because the bundled backend uses -1 for normal
shutdown. Personal preferences are not modified.
