# Main menu graphics and initial audio audit

Audited September 28, 2026.

## Single-player mission controls

The rendered Human01 audit verifies that the Idle Workers button selects a
worker and centers the camera. F8 and tilde cycle the selection. F10 opens
the mission menu and pauses local simulation ticks. Escape closes it without
sending a cinematic skip. Manual Pause Game stays active after the menu
closes, and Resume Game lets the mission advance again. The same audit exercises the original
restart, defeat, and victory dialogs after those checks.

Backspace cycles the camera through completed local Town Halls without
changing selection. The first hero-key press selects that hero, and a
second press centers the camera on the selection. F9 toggles the Quest Log.
Ctrl plus a number assigns the selection to a control group. The number
recalls it, and a second quick press centers the camera on the group.
Hold Position reaches the selected group. Tab and Shift-Tab cycle the
active subgroup. With Subgroup Order Modifier enabled in Gameplay Options,
Ctrl limits an order to the active subgroup. The desktop audit verifies that
Ctrl-S stops the highlighted hero while the other hero holds position, and
that applying the option persists it. The original numeric keypad inventory mapping uses the
same item-order path as the inventory buttons. The desktop audit verifies
Numpad 7 with a first-slot healing potion.
The rendered Help and Tips panels use the installed game's text. The audit
opens both, pages through Tips, and returns to the mission menu.

The in-mission Gameplay, Video and Sound panels share the front-end settings
store. The Human01 audit verifies that Escape discards a draft and that OK
applies and persists a subtitles change. The saved scroll speeds and tooltip
setting drive the live mission controls. The rendered audit drags a slider
and checks that a click outside Options is consumed by the menu. Space cycles
recent map alerts, including the two original camera-notification paths.

The installed mission frame exposes three Game Speed positions. The Single
Player slider uses those positions and applies the profile preference to the
local simulation and world animation clocks. The Human01 audit verifies
that Escape leaves speed unchanged, OK persists Fast, and the mission
advances at the selected rate. The slow and fast rates are engine values
whose exact correspondence to the installed client has not been measured.

F6 writes the active profile's Quick Save. The mission Save Game and Load
Game menus expose the profile's named slots. The rendered Human01 audit
types a slot name, writes the checkpoint, cancels an overwrite, confirms a
deletion, and resumes from a named checkpoint through Load Game. The separate resume audit
loads a checkpoint in a fresh process and compares its continuation with an
uninterrupted run.

Portable FFmpeg decoded moving frames and MP3 audio in all
eleven installed campaign movies. The rendered Human opening audit checks
six seconds of playback, centered letterboxing with no mission view in
the bars, and Escape returning to the mission. The decoder is configured
through `WARSMASH_FFMPEG` for that audit.

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
- Loops started directly from a unit or menu sound label now use the same
  live gain and positional updates. Stopping a loop by ID releases its own
  playback handle.
- Positional Audio now controls spatial playback for shared sound sources and
  updates active script sounds. Live script position and pitch updates reach the
  desktop backend.

Scripted sampled sounds use their authored fade-in and fade-out times.
The recording-backend test measures gain during both fades, verifies that
the mixer still controls the same voice, and confirms that the voice stops
when its fade ends. Movement and world ambience categories, EAX, and a
listening comparison on physical devices still need further auditing.

Script sounds attached to a unit now follow its position and height as it
moves. An explicit sound position releases the attachment. Playback tests
check the initial location, movement, and that closing the map stops the
attached sound without affecting an independent instance of the same asset.

## Classic MIDI ambience

The desktop client now resolves scripted MIDI labels through `MIDISounds.slk`
and plays the corresponding sequence with its supplied DLS instruments. The
soundbank loader retries classic banks with malformed top-level INFO metadata
while preserving every instrument and wave chunk. MIDI ambience follows the
sound and ambience settings, delays output during checkpoint reconstruction,
and releases its playback devices when the mission closes.

On November 23, 2026, all 34 MIDI labels in the local classic archives decoded
successfully. A Frozen Throne opening run with isolated preferences opened the desktop
synthesizer and produced active voices from the six-instrument Dalaran bank.
The night sequence also produced active voices in a desktop device check.
Master mute and the ambience toggle applied zero gain and restored playback
gain without restarting the sequence. Explicit stop closed both MIDI devices.
The mission's ambient volume group now scales MIDI gain as well. Cinematics
can duck an already-playing sequence and restore its volume while the
user's ambience and master-volume settings still apply.
MIDI fades, sound environments, and a listening comparison with the
original client remain open.

Campaign `Preload` now stages audio bytes through the active map data source.
The cache holds up to 8 MiB per map, serves independent streams to playback,
and clears when the map data source changes or the chapter closes. The Frozen
Throne opening render audit completed with zero audio handler errors. The
earlier opening recorded 29 audio handler errors.

The same cache stages JASS, AI, and text resources before they are read.
The Frozen Throne opening previously sent five files of those types to the
renderer. The opening now completes with zero `Preload` errors.

Hearthglen stages `Splats\SpawnData.slk` and `Splats\SplatData.slk` through
the same map cache. Model event loading can then read the staged bytes.
The desktop opening reached Defend Hearthglen without the two table handler
errors or the missing villager death effect warning. The model loader resolves
the effect to its subdirectory in the installed archive.
The same opening verified that the animation sound loader skips the empty
Creep Sleep entry. Animation events choose from successfully decoded sounds.

Campaign sound handles retain their authored duration in milliseconds for
JASS timing. The rendered Landfall startup read back the original
12,356 ms duration from a Cairne line. Sound label settings use the
installed tables for gain, pitch, reference distance, attenuation
distance, and distance cutoff. The playback tests check these values
and preserve the existing settings when a field is blank. Setting a
playback offset seeks the active voice and uses its current pitch to
calculate the remaining time.

Explicit classic script channels with corresponding volume groups follow
the chapter's group gain. A muted group keeps an active clip at zero gain
and restores its authored volume without restarting the clip.
When a 3D sound is authored to stop out of range, the client checks its
cutoff during playback as the camera or source moves. The regression
checks that an unrestricted voice continues independently.
The campaign checkpoint audit also executes the original no-argument
music resume command after pausing the playlist. A fresh process restores
that checkpoint and matches uninterrupted continuation.

Four installed campaign maps specify nonzero camera roll. Camera setups
and the direct JASS camera command now tilt the view and interpolate the
roll over the supplied transition. The camera test verifies that roll
changes the view orientation while preserving the eye position. A rendered
checkpoint run restored an active roll transition in a separate process.

## Verification

466 core tests pass with retail assets required and no failures or skips.
Desktop compilation and the real menu render audit pass. The retail asset
audit passes 47/47 checks. A fresh desktop process also restored a named
mission save and matched uninterrupted continuation.
The GPU graphics audit also passes the menu border and tile sampling checks.
The desktop input audit verified that holding either bracket key shows the
corresponding side's world health bars. Alt shows both sides, and the bars
clear when the key is released or the mission menu opens.
[Blizzard's special commands guide](https://classic.battle.net/war3/basics/specialcommands.shtml)

The NightElf02 opening now resolves the numbered barricade portrait, the
classic Moon Priestess effect model, and the unpadded footprint rows in the
installed archives. The render audit completed without errors for those
assets. Three animation sound IDs still lack matching table entries in the
tested archives.

## Reproduction

- `./gradlew :desktop:campaignBackingAudit`
- `./gradlew :desktop:menuGraphicsAudit`
- `./gradlew :core:test`

The menu audit opens a short-lived window and captures rendered frames. It checks
an explicit completion marker because the bundled backend uses -1 for normal
shutdown. Personal preferences are not modified.
