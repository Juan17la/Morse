# Performance critique and Wi-Fi Direct simulation

Branch `impeccable/critique-fixes`. 49 unit tests pass (0 failures).
Re-run: `./gradlew :app:testFullDebugUnitTest --offline`. Raw numbers land in `app/build/sim-results/*.txt`.

## What this is, and what it is not

No real watches or emulators were available, so nothing here was measured on a watch.

| Layer | How it was tested | Trust |
|---|---|---|
| TCP host/client, handshake, relay, ordering, failure handling | The app's **real** `GroupHostServer`, `GroupClient`, `SocketGroupTransport` over loopback | High for logic and app-side cost; loopback has no RF loss |
| Wi-Fi Direct discovery, group-owner negotiation | `SimulatedAir`, a model that mirrors `WifiDirectPeerFinder` (publish, one-shot discovery, 20 s timeout, serialized invitations) | **Assumed** timings (`RadioProfile`), not measured. Radio delays run 50x faster than real and are scaled back up in the figures below |
| Compose UI cost | Static code review only | Hypotheses; confirm with Layout Inspector / Perfetto on a watch |

The assumed radio figures are: group create 0.6-1.5 s, service discovery 3-9 s, connect 2-6 s. Replace them with times logged on your watches before quoting any join time as fact.

## Results

### Two watches forming a group (15 runs, assumed radio)

| Metric | Result |
|---|---|
| Join time p50 / p95 / max | 10.0 s / 12.7 s / 12.8 s |
| App-side handshake p50 / p95 | 2 ms / 3 ms |

Almost all of the wait is the radio. The app's own part is negligible, so tuning the socket code will not make joining faster.

### Three watches joining at once

First watch connected at 8.7 s, last at 14.2 s. The group owner handles one invitation at a time, so the second joiner always waits behind the first. For a class demo, have people join one after another, not together.

### Failure timing

| Case | Result |
|---|---|
| Mistyped code | `HOST_NOT_FOUND` only after **20.1 s** of "Connecting…". LAN mode reports `WRONG_CODE` instantly. |
| Fourth watch | `GROUP_FULL` arrives only after discovery and connect (~8 s). The group does not advertise that it is full. |

### Messages (3 watches, 300 messages, real socket code)

| Path | App-side p50 / p95 | With assumed 15 ms RF per hop |
|---|---|---|
| host to member (1 hop) | 5.9 / 11.2 ms | ~26 ms |
| member to host (1 hop) | 0.35 / 0.52 ms | ~16 ms |
| member to member (2 hops via host) | 6.1 / 11.2 ms | ~41 ms |

A burst of 3 senders x 200 messages lost nothing and kept per-sender order on every watch.

**Perspective:** the transport adds tens of milliseconds, but the vibration itself takes seconds. At the default 120 ms unit: "no" 2.3 s, "yes" 3.0 s, "wait" 3.5 s, "I'm here" 5.4 s, "maybe" 5.6 s, a 20-character word **33 s** (159 steps). Delivery speed is not the problem; the time to feel a word is.

### Codec cost (JVM, a watch CPU will be several times slower)

Envelope encode 0.7 µs, decode 1.7 µs (241 bytes for a 20-char word). Morse encode 1.6 µs, timing 0.9 µs. Not a bottleneck.

## Findings, ranked

Each finding below is reproduced by a test in `TransportResilienceTest` (they pass today; each fails once the weakness is fixed).

**P1. A silent host is never noticed.** The client has no heartbeat, no read timeout and no TCP keepalive. After 3 s of silence it still reports `Connected`. Sending to a dead host writes into a socket buffer and shows the green "Sent" confirmation although nobody will feel it. This breaks the product promise that you can trust the wrist signal.

**P1. A rejoining watch is evicted by its own stale socket.** On a Wi-Fi blip the watch reconnects with the same id. The host replaces the list entry but never closes the old socket. When the old socket finally dies, its cleanup removes every client with that id, including the live one. The test shows the host's member list dropping to `[host]` while the new socket is open.

**P2. A dead member keeps its seat.** A silent member stays listed after 3 s and holds one of the three seats, so a fourth watch may be told `GROUP_FULL`.

**P2. Wrong code feedback takes 20 s.** See failure timing above. There is no progress text during that wait and the worst-case join budget is 50 s (20 s discovery + 30 s connect).

**P2. Writes have no timeout.** A peer that stops reading blocked the host's `send` after 18,700 messages (~3.4 MiB) and stalled relay to every other watch. Unlikely in a short demo (small messages), but the same stall happens with a half-dead peer.

**P3. Blocking handshake threads.** 100 silent connections to port 8765 exhausted the IO threads and a real join got no answer in 3 s. Wi-Fi Direct groups are closed, so this matters little here; it does matter in LAN mode on a shared network.

## Wi-Fi Direct risks from reading the code (not testable here)

1. **Hardware support.** Many Wear OS watches do not expose `FEATURE_WIFI_DIRECT`. The app then fails with `NOT_SUPPORTED`. Check first: `adb shell pm list features | grep wifi.direct`.
2. Discovery is a single `discoverServices` call with no retry. Some Android stacks need `discoverPeers()` first. One missed advertisement costs the full 20 s.
3. The connection-changed receiver completes the join on any `groupFormed` info. `removeGroup` runs first, but it is asynchronous, so a stale group could in theory be picked up.
4. Port 8765 is fixed. Two groups in the same room are only told apart by the access code in the service record.
5. Battery: the foreground service and the P2P group owner stay active until Leave, with no idle timeout.

## UI performance (static review, unmeasured)

- **No R8 and no baseline profile** (`isMinifyEnabled = false`). Cold start and first-scroll jank on a watch will be worse than necessary. Highest-value, lowest-risk fix.
- `MorseGradients.of(tint)` builds a new `Brush` on every recomposition (Platter icon, Avatar, Home choices). Wrap in `remember(tint)`.
- `TopGlow` reads `animateColorAsState` in composition, so its box recomposes on every frame of the 600 ms cross-fade. Read the color inside `drawBehind`.
- `MorseNavHost` reads `buzzing` in its body; each toggle (up to 159 per long word) re-enters that scope. Check in Layout Inspector that the nav host itself is skipped.
- `Playback` is not annotated `@Stable`, so `MorseVisualizer` recomposes its whole pill list every step. Fine for short words; mark it `@Immutable` or pass primitives.

## Recommended fixes, in order

1. Heartbeat envelope (e.g. every 5 s, dead after 15 s) plus `SO_TIMEOUT` on both sides; show "Connection lost" and a failed send when it trips.
2. On same-id rejoin close the old socket, and in cleanup remove only the client instance that ended.
3. Handshake read timeout (5 s) and a bounded accept backlog.
4. Per-client writer queue with a write timeout so one slow peer cannot stall the relay.
5. Progress text during discovery, retry once, and advertise "full" in the service record.
6. Enable R8 and add a baseline profile; `remember` the brushes.

## Test plan for two real watches (what would make this report factual)

1. Run the pm-features check on both watches.
2. Log timestamps at `publish` start/end, `discoverServices` start, service found, `connect` start, `connectionInfo`, and `Connected`. Repeat 10 times and replace `RadioProfile`.
3. Walk one watch out of range for 30 s and back; record whether the UI ever leaves `Connected` and whether the rejoin evicts it.
4. Send "maybe" from each watch and compare the sender's "Sent" time to the first buzz on the other (screen-record both).
5. Leave both in a group with the screen off for 10 minutes and note battery drop.
