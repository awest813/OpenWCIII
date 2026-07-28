# OpenWCIII Mission

## One-liner

**OpenWCIII is a faithful open-source Warcraft III, with quality-of-life upgrades.**

It is an engine reimplementation (forked from Warsmash). You bring legally owned
Warcraft III assets; OpenWCIII brings an open, modern client that aims to play
classic WC3 — campaigns, melee, and custom maps — the way players remember,
then improves ergonomics around that core.

---

## What “faithful” means

- Prefer **retail Warcraft III behavior** for supported patches, maps, and
  scripts (JASS natives, UI flows, campaign progression).
- Treat classic **Reign of Chaos** and **The Frozen Throne** single-player as
  first-class targets for parity work.
- When behavior must diverge (missing native, modern GL, asset layout),
  **document it** instead of silently inventing a new game.

Faithful does **not** mean cloning every Reforged regression or supporting
every post-1.32 format before classic/campaign fidelity is solid.

---

## What “QoL upgrades” means

Quality-of-life changes are welcome when they:

- make launch, config, and debugging easier (`-profile`, `-validate`, logs),
- improve stability/performance on modern OS/GPUs,
- clarify docs and contribution paths,
- add opt-in conveniences that do not change competitive or campaign identity
  without a clear, documented switch.

QoL must not become an excuse to break map/script compatibility or “rebalance”
Warcraft III by default.

---

## Non-goals (for now)

- Shipping Blizzard game assets or keys
- Native **Warcraft II** (`.pud` / ToD / BtDP) as a supported product line —
  that would be a separate format/engine effort
- Guaranteed Reforged 1.33+ model-format support before classic campaign/map
  fidelity is in good shape

---

## How work is prioritized

1. **Correctness vs retail WC3** for claimed features (campaign spine, JASS,
   selection, victory/defeat, gamecache, etc.)
2. **Soak** on RoC/TFT missions and popular custom maps
3. **Engine modernization** that preserves behavior (Phases A–D complete;
   Phase E fidelity; Phase F modding layer)
4. **Player/dev QoL** that stays compatible with (1)–(3)

See also:

- [ENGINE_MODERNIZATION_ANALYSIS.md](ENGINE_MODERNIZATION_ANALYSIS.md)
- [COMPATIBILITY.md](COMPATIBILITY.md)
- [../CHANGELOG.md](../CHANGELOG.md)
- Campaign parity PRs / audits on this fork when present

---

## Naming

| Name | Role |
|------|------|
| **OpenWCIII** | This fork’s product name and mission frame |
| **Warsmash** | Upstream engine project and historical codebase name (packages, binaries, many class names still say Warsmash) |

In docs, prefer “OpenWCIII” for product/mission language and “Warsmash” when
referring to upstream history or Java package/engine identity.
