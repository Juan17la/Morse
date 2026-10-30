# Testing

Morse has three layers of tests: fast JVM unit tests for the logic, transport
simulations that run the real socket code, and a smoke test that runs on a
watch.

| Layer | Where | Runs on | Command |
|-------|-------|---------|---------|
| Unit tests and simulations | `app/src/test` | The computer's JVM | `./gradlew testFullDebugUnitTest` (or `testDemoDebugUnitTest`) |
| On-watch smoke test | `app/src/androidTest` | A connected watch or Wear emulator | `./gradlew connectedDemoDebugAndroidTest connectedFullDebugAndroidTest` |
| PC tool self-tests | `tools/` | Python 3 | `python3 tools/morse_sim.py --selftest`, `python3 tools/morse_panel.py --selftest` |

The 49 unit tests run in a few seconds. They run once per flavour, so a run of
both flavours reports 98.

## Unit tests

| Suite | Tests | What it proves |
|-------|-------|----------------|
| Morse codec | 8 | Single words, case insensitivity, word separators, whitespace, dropping unsupported characters, digits, Spanish accents, empty input. |
| Morse timing | 8 | Dot 1 unit, dash 3, symbol gap 1, letter gap 3, word gap 7, the waveform starts with 0 and alternates, steps point back to their symbol, empty input. |
| Short names | 1 | Names keep their first word and long ones are cut with an ellipsis. |
| Protocol | 4 | Hello, message and rejection envelopes survive a round trip; an encoded envelope never contains a newline, so it is always one line on the wire. |
| AI assistant | 8 | Sanitising (upper case, dropping what Morse cannot carry, cutting at word boundaries, rejecting empty output), offline condensing in English and Spanish, offline replies that fit the message, a build without a key never going online, parsing a real Gemini response. |

## Transport tests with real sockets

These suites start the app's real host server and clients on the computer's
loopback interface, port 8765, so they test the actual code that runs on the
watch, minus the radio.

| Suite | Tests | What it proves |
|-------|-------|----------------|
| Socket group transport | 6 | Joining with the right code shares the member list; the host cleans names; a wrong code is rejected; the three-member limit holds; messages reach every member; leaving updates the others. |
| Resilience | 5 | Pins five known weaknesses: a silent host is never noticed, a silent member keeps its seat, a rejoin can be dropped by its own stale socket, idle connections can starve new joins, and a peer that stops reading blocks the host. These pass today **because** the weakness exists; each is meant to start failing when it is fixed, as a reminder to update it. |
| Codec performance | 3 | Measures envelope and Morse encoding cost and the vibration time of typical words. |

Because these tests bind port 8765, stop the PC simulator before running them.

## Wi-Fi Direct simulation

Wi-Fi Direct cannot run on a computer, so its behaviour is modelled. The
`SimulatedAir` helper mirrors what the Wi-Fi Direct peer finder does
(publishing, a single discovery with its 20 second window, serialised
invitations at the group owner) with assumed radio timings, and connects real
host and client sockets once a "link" forms. Radio delays run 50 times faster
than real and are scaled back up in the results.

| Suite | Tests | What it measures |
|-------|-------|------------------|
| Join simulation | 4 | Two watches forming a group, a mistyped code failing only after the whole discovery window, three watches queueing on the group owner, a fourth watch learning the group is full only after paying the radio cost. |
| Messaging simulation | 2 | Latency per hop (host to member, member to host, member to member), and three simultaneous senders losing nothing and keeping per-sender order. |

The raw numbers are written to `app/build/sim-results/`. The findings, and how
much to trust each number, are in the
[performance report](../../docs_planning/PERFORMANCE_REPORT.md).

## The on-watch smoke test

The smoke test is a Compose UI test that drives the real app on a watch or
emulator, to check it installs, starts and works on that screen.

| Test | Flavours | Steps |
|------|----------|-------|
| Launches to Home | Both | Start the app. On a first launch, scroll the welcome to **Start** and tap it. Home must appear with Create group or Send message. |
| Create a group and send a word | Demo only | Reach Home, create a group (or reuse the one already open), open Send, tap **Yes**, and wait for the **Sent "Yes"** confirmation. |

In the full build the second test is skipped, because creating a group there
needs a real Wi-Fi Direct peer or a LAN host. Both tests handle a fresh
install and a returning user, so they can be run repeatedly on the same
watch. Gradle installs the app, runs the tests and uninstalls it afterwards.

The test uses Espresso 3.7, because older versions cannot inject touches on
Android 14 and newer. It last passed on the Wear OS large round emulator,
API 37, on a fresh install and on a second run.

## Checking a release by hand

The automated tests cannot feel a vibration or measure a radio. Before a
presentation, on real watches:

1. Check Wi-Fi Direct is exposed: `adb shell pm list features` must list
   `android.hardware.wifi.direct` on every watch.
2. Create a group on one watch and join from the others, one at a time.
3. Send a quick word from each watch and confirm the others feel it and see it
   light up in step.
4. Turn the screens off, send again, and confirm the words still arrive.
5. Leave from a member, then from the host, and feel the matching signals.
6. Repeat the screens at a large font size and in Spanish.

The performance report lists further measurements (join timing, range,
battery) that would turn the modelled numbers into real ones.
