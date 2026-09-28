# Unit AI audit

Audited September 28, 2026. Scope: automatic attack/cast acquisition, Hold
Position, follow/combat handoffs, patrol legs, route exhaustion, attack-move
reuse, and automatic versus player orders.

## Corrections

- Hold Position and immobile units now skip enemies beyond weapon range before
  choosing an attack. Previously an unreachable enemy could win enumeration,
  report acquisition success, and prevent attacking another enemy within reach.
- Autocast checks its enabled state and activation requirements before scanning
  targets. Unavailable spells no longer perform unnecessary target searches.
- Autocast candidates obey circular acquisition range and, when movement is
  prohibited, cast range. Hold Position no longer starts a cast that requires
  walking to its target.
- Rejected automatic orders return failure so normal attack acquisition can run.
- Automatic casts preserve the default Hold Position/patrol behavior and queued
  player orders. They do not interrupt uninterruptible behavior or units that
  are not accepting orders. Explicit player orders retain replacement behavior.
- Enabling the same autocast repeatedly no longer switches it off internally.
- Patrol advances after arrival or route exhaustion, preserving its first leg
  and unfinished leg after combat. Exhausted attack-move routes release queued
  orders instead of repeatedly restarting the same failed route.
- Reused patrol and attack-move behaviors clear pending acquisition state when
  given a new destination. Removed unused patrol imports and movement state.
- Automatic attack selection handles a missing attack ability without a null
  dereference.

- Follow no longer acquires enemies inside order initialization or overwrites a
  newly acquired attack. Distant follow orders resume the follow behavior after
  combat, and restore the followed target when reusing the movement behavior.
- Cancelling queued Stop orders or orders whose abilities were removed no longer
  dereferences a missing ability. Both immediate replacement and deferred orders
  use the guarded cancellation path.

## Verification

`./gradlew :core:test :desktop:compileJava` passes: 311 tests, zero failures or
skips, including 16 focused unit AI scenarios. `git diff --check` also passes.

The focused retail-data simulation scenarios cover distant/near Hold Position
attack targets, patrol arrival/resumption, failed and unavailable autocasts,
cast-range restrictions, repeat toggles, real peasant repair preserving queued
orders and Hold Position, reissued patrol/attack-move orders, Follow transitions,
queued Stop cancellation, and route-exhaustion callbacks. The initial six
regressions and four subsequent Follow/queue regressions failed before their
fixes and passed after correction. Route-exhaustion tests exercise the behavior
callbacks; they do not simulate a full obstacle-heavy pathfinding workload.

These are headless, flat-map simulations using retail Human01 object data, not
manual playthroughs. Campaign strategy scripts, large-army performance, and
obstacle-heavy pathfinding are outside this pass. Target ranking among otherwise
eligible enemies remains unchanged.
