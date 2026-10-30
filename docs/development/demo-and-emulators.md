# Demo and emulators

Four ways to run a group without three real watches, from the simplest to the
most realistic. How each one works inside is explained in
[Demo build and PC tools](../internals/demo-and-tools.md) and
[Finding the host](../internals/finding-the-host.md).

| Setup | Watches or emulators | Build | Real network |
|-------|----------------------|-------|--------------|
| 1. Demo build | One | Demo | No |
| 2. PC simulator or panel | One or two | Full, LAN mode | Yes |
| 3. Two or three emulators | Two or three | Full, LAN mode | Yes |
| 4. Real watches | Two or three | Full, Wi-Fi Direct | Yes, over the air |

## 1. Demo build: one watch or emulator

1. Install with `./gradlew :app:installDemoDebug`.
2. Open **Morse Demo** and tap **Create group**.
3. Two fake members join over the next few seconds. Send any word; a fake
   member answers 2 to 4 seconds later, which opens the incoming screen and
   plays the vibration.

Nothing else to set up. Joining also works with any code.

Emulators have no vibration motor. To see the patterns, run
`adb logcat -s HapticPlayer`: every word is logged with its on and off
durations in milliseconds.

## 2. PC simulator: one emulator, real TCP

The simulator hosts a group of bots on your computer. The emulator joins it
over the network, so the full build's socket layer, handshake and code checks
are exercised for real.

1. Start the simulator with `python3 tools/morse_sim.py`. It prints the access
   code and which bots are in the group.
2. Install the full build: `./gradlew :app:installFullDebug`.
3. On the emulator: Settings, **Demo and testing**, choose **Local network
   (emulator)**. Leave **Host address** at `10.0.2.2`, which is the computer
   as seen from the emulator. No port forwarding is needed.
4. On Home, **Join group**, enter the printed code, **Join**.
5. Send a word: it appears in the terminal with its Morse, and a bot answers.

In the terminal, type a word to make the host bot send it, or "luis: on my
way" to make a specific bot send it. `/members` lists the group and `/quit`
ends it.

| Option | Effect |
|--------|--------|
| `--code 1234` | Use a fixed code instead of a random one |
| `--bots Ana` | Only one bot, leaving two seats for watches |
| `--selftest` | Check the encoder, reply rules and protocol, then exit |

### With the control panel

For a presentation, `python3 tools/morse_panel.py` runs the same simulator and
opens a web page at `http://localhost:8080`. Choose which bot speaks, type a
word (its Morse is previewed live) or tap a preset from the Normal, Silly or
Mine sets, and it lands on the watch. The page also shows the members, the
conversation and a switch for automatic replies. It accepts the same `--code`
and `--bots` options, plus `--port` for the page and `--no-browser`.

Stop the simulator before running the unit tests: both use port 8765.

## 3. Two or three emulators

Here one emulator really hosts the group and the others join it.

1. Start two Wear OS emulators. Their serials are listed by `adb devices`,
   usually `emulator-5554` and `emulator-5556`.
2. Install the full build on each, choosing the target by setting
   `ANDROID_SERIAL` to its serial before running `./gradlew :app:installFullDebug`.
3. On **every** emulator: Settings, **Demo and testing**, **Local network
   (emulator)**. Leave the host address at `10.0.2.2` on the joiners.
4. Forward the host emulator's port to the computer:
   `adb -s emulator-5554 forward tcp:8765 tcp:8765`.
5. On `emulator-5554`, **Create group** and note the code.
6. On `emulator-5556`, **Join group** with that code.
7. Send words both ways. A third emulator can join the same way.

The path is: joiner, to `10.0.2.2` (the computer), to the forwarded port, to
the host emulator.

Two emulators report the same device name, so set a different name on each in
Settings, **My name**, to tell the senders apart.

What you can try:

| Action | Expected result |
|--------|-----------------|
| Join with a wrong code | "Wrong access code" immediately |
| A fourth emulator joins | "The group is full" |
| Join before the host created the group, or without the forward | "Can't reach the host" |
| The host leaves | Members feel Connection lost and see "Connection lost" |

## 4. Real watches over Wi-Fi Direct

1. Check each watch supports Wi-Fi Direct: `adb shell pm list features`
   must list `android.hardware.wifi.direct`.
2. Install the full build on every watch (see
   [Building and running](building.md#installing-on-a-real-watch)).
3. Leave the connection mode on **Wi-Fi Direct** (the default).
4. Create the group on one watch and accept the Nearby devices permission.
5. Join from the others **one at a time**, accepting the permission on each.
   Expect about ten seconds per join.

If a watch does not support Wi-Fi Direct, put all watches on the same Wi-Fi
network, switch them to the local network mode, and type the host watch's IP
address as the host address on the members.

## Presentation checklist

- Charge the watches; the group keeps the radio and a background service
  active.
- Set a distinct name on every watch.
- Rehearse joining once; join one watch at a time.
- Have the demo build installed as a fallback: it needs no network at all.
- With the PC panel, switch auto-reply off if you want to control every
  message yourself.
- Add a Gemini key beforehand if you want the AI label on dictation and
  replies; without it the offline versions still work.
