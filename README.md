# ChunkyFolia

Multithreaded world chunk pre-generation engine built specifically for Paper's **Folia** regional multi-threading platform.

## Architecture
Standard chunk generators rely on single-threaded tick loops or blocking `world.getChunkAt(x, z)` calls. Under Folia, world state is partitioned into independent thread regions. ChunkyFolia solves this by:
* Queueing coordinates off-thread via `AsyncScheduler`.
* Dispatching asynchronous generation batches to target chunk coordinates using `Bukkit.getRegionScheduler().execute(...)`.
* Using non-blocking `World#getChunkAtAsync(x, z, true)` futures so region threads never stall on neighboring terrain boundaries.

## Build
```bash
mvn clean package
```
