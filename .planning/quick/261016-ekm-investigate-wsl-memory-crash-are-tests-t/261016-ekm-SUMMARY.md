---
status: complete
---

# 261016-ekm: WSL memory investigation

## Findings

- No kernel OOM in the last four boots. WSL2 host-side kills leave no guest trace, so this does not rule out memory pressure.
- Idle baseline: 6.6 of 7.9 GB used, 1.45 GB available, 654 MB swap used, before any test.
- One `fastTest` run (mem-fastTest.csv): test workers 1,334 MB RSS, Gradle daemon peak 1,491 MB, swap to 1,860 of 2,048 MB, MemAvailable low of 887 MB.
- The tests are about half the pressure; the resident stack (ollama, headroom, JDTLS x2, freehire containers) is the rest.

## Change

- `gradle.properties`: `org.gradle.jvmargs=-Xmx768m`. Daemon peak 1,491 -> 971 MB (mem-fastTest-capped.csv).
- Tried and reverted: `maxHeapSize = '768m'` on `test` and `fastTest`. Worker RSS 1,334 -> 1,382 MB, no reduction.

## Not established

- That `fastTest` caused yesterday's crash. One run, no crash reproduced.
- Swap comparison between runs: the second run started with swap already at 1,848 MB.
- Wall clock 6m24s -> 4m44s is not attributable to the cap (different system state).
- The full `test` task (2 Redpanda containers) was not measured.
- `.wslconfig` (memory, swap) was not inspected: `/mnt/c/Users` is not mounted.
