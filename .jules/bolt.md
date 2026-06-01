## 2024-05-24 - Projectile Enum Allocations
**Learning:** In the `CSimulation` update loop, anonymous callback classes like `CUnitEnumFunction` and `CDestructableEnumFunction`, along with localized state tracking classes like `AtomicInteger` or `AbilityPointTarget`, cause significant per-frame allocations per active projectile when doing spatial queries (e.g. `enumUnitsInRect`).
**Action:** Extract these callbacks and wrapper classes to instance fields. Populate target state (like counts, `CSimulation` context, and coordinates) into instance primitive fields before passing the pre-allocated callback functions to the spatial query systems.
## 2024-05-18 - [Optimizing Aura spatial queries]
**Learning:** Frequent spatial queries in loops (e.g., `enumUnitsInRect` in `onTick` of Aura templates) create a substantial performance bottleneck by allocating new anonymous callback instances per tick, triggering frequent GC pauses.
**Action:** Always refactor these anonymous callback instances into cached private members. Pass the necessary local context (like `game`, `unit`) via member variables and reset them to `null` immediately afterward to avoid memory leaks.

## 2024-05-25 - [Object Pooling for Re-Entrant Spatial Queries]
**Learning:** Re-using a single callback instance for spatial queries (e.g. `CUnitEnumFunction` in `enumUnitsInRect`) across shared action instances (like `ABActionIterateUnitsInRangeOfUnit`) causes severe state-corruption and NPE bugs due to re-entrancy (nested casts of abilities).
**Action:** Do not use single instance variables for callback caching on shared ability definitions. Instead, use a LibGDX `Pool` (`Pool<IterateUnitsInRangeEnum>`). Use `pool.obtain()` before the query and `pool.free(enumFunction)` inside a `finally` block to guarantee safety and avoid allocations. Remember to `clear()` the references as well.
## 2024-06-25 - HashMap Lookup Optimization for Immutables
**Learning:** `StringKey.java` used as a hash key wrapper around `String` was calculating `toLowerCase().hashCode()` on every lookup, creating massive string allocations.
**Action:** When a wrapper object is primarily used as an immutable hash key, cache its case-insensitive hashCode during initialization without allocating intermediate strings (e.g. by hashing characters directly).
