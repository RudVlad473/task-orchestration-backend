# 261016-ekm: cap test and Gradle daemon memory

Evidence: mem-fastTest.csv. One `fastTest` run added about 2 GB (test JVMs 1,334 MB, daemon +536 MB, containers +160 MB) and pushed swap to 1,860 of 2,048 MB on a VM with 1.4 GB headroom.

## Tasks

1. `build.gradle`: `maxHeapSize = '768m'` on `test` and `fastTest`, with a comment citing the measurement.
2. New `gradle.properties`: `org.gradle.jvmargs=-Xmx768m`.
3. Re-run `fastTest` under `measure-memory.fish` and compare against the baseline run. Accept only if it passes and wall clock stays within the documented ~18 s run-to-run variance of the 6m24s baseline; otherwise state the loss.

## Alternatives considered

| Approach | Pros / Cons | Why picked |
|---|---|---|
| A. Fixed `maxHeapSize` per fork + daemon `-Xmx` | Simple, deterministic ceiling, matches how `maxParallelForks` was already tuned in build.gradle. Risk: OOM in heavy contexts, GC slowdown | Picked |
| B. `-XX:MaxRAMPercentage=15` | Scales with machine, no magic number. Hard to reason about on CI vs WSL, and the same percentage means different MB per host | Rejected: the cap should be a measured number |
| C. `maxParallelForks = 1` | Halves JVMs and containers. Measured earlier at +30% wall clock (370 s vs 276 s) | Rejected: trades time for memory more steeply than a heap cap |

## Trade-offs

- Time: a smaller heap raises GC frequency; verified by re-measurement, not assumed.
- Failure mode: too small a cap shows as `OutOfMemoryError`, which is loud, not a silent wrong result.
- Gradle daemon: `-Xmx768m` also applies to CI and to the pre-commit hook, which is intended.
