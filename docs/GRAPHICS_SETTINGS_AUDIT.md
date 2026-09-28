# Graphics settings audit

Audited September 28, 2026. Scope: the main-menu Video panel, persisted options,
desktop launch flags and presets, and the renderer consumers of each setting.

## Control behavior

| Setting | Implemented behavior |
| --- | --- |
| Resolution | Seven window sizes plus desktop-resolution fullscreen. OK applies the choice; Cancel discards it. Mode and dimensions survive restart; explicit launch flags and profiles take precedence. |
| Gamma | The 0-100 slider maps to display gamma 0.5-2.0, with 50 neutral. OK applies through LWJGL and startup restores the saved value. Driver rejection is reported and any preceding display change is rolled back. |
| Model detail | Low/Medium/High select authored LOD 2/1/0 with safe fallback to an available level. Shared geosets remain visible. All levels stay loaded for live changes; source animation IDs are preserved. Classic single-level models retain their original geometry. |
| Animation quality | Low/Medium/High sample world skeletal tracks at 15 Hz / 30 Hz / every rendered frame. Animation clocks, events, particles, root movement, visibility, and attachment updates still advance every frame. Sequence changes and forced updates sample immediately. Menus and portraits remain full-rate. |
| Texture quality | Low/Medium/High sample quarter/half/full resolution per dimension for model textures, terrain, cliffs, and water. Tiny textures clamp to available levels. Original pixels/mips are retained for reversible switching, so this does not guarantee reduced VRAM use. UI atlases, fonts, fog, height, light, and bone data textures retain their resolution. |
| Particles | Low/Medium/High control MDL and MDX continuous/burst emission density at one third / two thirds / the existing full density. Existing particles expire normally; ribbons, events, and gameplay are unchanged. |
| Lights | On/off controls additional world model lights, retaining day/night illumination for terrain and units. Menu and portrait lights remain unchanged. |
| Shadows | On/off controls static map/building shadows and unit shadow splats. Map-boundary darkness, fog of war, selection circles, and other splats are preserved. The unrelated unfinished dynamic shadow-map pass is not exposed as a quality feature. |
| Occlusion | On/off smoothly fades live destructables that block the camera-to-visible-unit segment, matching retail trigger help. Uses transformed model bounds and visual occluder height. Hidden/dead units are excluded; dead trees restore opacity. Faded geometry moves to the translucent pass without depth writes. Authored colors and gameplay fog/pathing remain unchanged. |
| VSync | Launch flags and presets configure the backend. Both single- and double-dash flags work. The retail panel has no VSync control. |
| FPS cap | Launch flags and presets configure foreground/background limits. Zero remains uncapped. |
| MSAA | Launch flags and presets configure samples at context creation, subject to GPU support. No live menu control is exposed. |

## Other fixes

- Consistently parse normalized arguments, fixing `--option=value` and subsequent
  arguments being ignored or misread.
- Correct the safe preset's unsupported "reduced effects" claim and explicitly
  set fullscreen in the high preset.
- Include every graphics preference in defaults, validation, draft copying,
  persistence, and loading. Legacy resolution-only files remain windowed.
- Write options through a temporary file and atomic replacement when supported.
  Failed saves retain committed preferences, report an error, and attempt to
  restore the actual previous display mode and gamma. Failed rollback is visible
  in the error dialog. Display queries stay inside error handling and are skipped
  for changes that do not affect the display mode. Malformed properties escapes no longer crash
  startup.
- Implement `EnableOcclusion`, `GetDestructableOccluderHeight`, and
  `SetDestructableOccluderHeight`. Visual occluder height is separate from the
  existing gameplay vision-blocking rules.
- Respect layer/occlusion alpha in the HD fragment shader. Apply SD occlusion
  fading after authored alpha cutouts so tree foliage retains its silhouette.

## Verification

- `./gradlew :core:test`: 295 tests pass, including quality fallback, sampling
  cadence, tree intersections/fading, persistence, display-query failures, save rollback, and rollback error reporting.
- `./gradlew :desktop:graphicsSettingsRenderAudit`: passes in a real OpenGL 3.3
  context. Checks texture pixel samples through high/low/medium/high transitions,
  LOD buffer uploads, shared geosets, original animation references, and actual
  leaf-cutout/fade pixel output through opaque/faded/opaque transitions.
- `./gradlew :desktop:graphicsSettingsMapAudit`: passes on retail Human01 with
  isolated preferences. Runs low settings, then switches to high with shadows
  and local lights restored and occlusion disabled. Requires render-thread
  completion with no GL error and checks actual normalized launcher output.
  The backend returns -1 on normal shutdown, so the audit checks the explicit
  completion marker. Existing missing-asset warnings remain in its saved report.
- Retail data test verifies the video-control names and records the original
  occlusion help: trees blocking the view of units become transparent.

All retail Video-panel controls now have renderer consumers. Model detail still
depends on authored assets and gamma depends on driver support. Exact retail
visual parity on every map, custom asset, and GPU is not established by these
checks; manual review of the menu and visual transitions remains useful.
