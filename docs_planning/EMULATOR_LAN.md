# Demoing Morse on emulators

There are two builds. **full** is the real app, with the LAN and Wi-Fi Direct connection
modes (Settings → Connection). **demo** is the same UI with simulated watches, installed
next to it as *Morse Demo*. Everything shares the same UI, protocol and group session;
only the way watches find each other changes.

## 1. Demo build — one emulator

```sh
./gradlew :app:installDemoDebug
```

No setup. Create a group and fake members ("Ana's watch", "Luis's watch") join after
a moment; every message you send gets a fake reply 2–4 s later, which opens the
Incoming screen and plays the Morse vibration. The emulator has no motor, so the
pattern is also logged:

```sh
adb logcat -s HapticPlayer
```

## 2. LAN / emulator — two or three emulators exchanging real TCP messages

After Wi-Fi Direct forms a group, the watches talk over plain TCP (port 8765). The LAN
mode uses that same socket layer, with the host address entered manually. On the
Android emulator `10.0.2.2` is the development machine, so with one `adb forward`
per host a second emulator can reach the first one.

1. Start two Wear OS emulators (e.g. `emulator-5554` and `emulator-5556`) and install
   the full build on both:

   ```sh
   ANDROID_SERIAL=emulator-5554 ./gradlew :app:installFullDebug
   ANDROID_SERIAL=emulator-5556 ./gradlew :app:installFullDebug
   ```

2. On **both** emulators: Settings → Connection → **LAN / emulator**.
3. On the joiner(s) the *Host address* must be `10.0.2.2` (the default).
4. Forward the host port on the emulator that will create the group:

   ```sh
   adb -s emulator-5554 forward tcp:8765 tcp:8765
   ```

5. On `emulator-5554`: **Create group** — note the 4-digit code.
6. On `emulator-5556`: **Join group**, enter the code, **Join**.
   - Wrong code → "Wrong access code".
   - A 4th watch → "The group is full" (`GroupConfig.MAX_MEMBERS`).
   - Host not running or port not forwarded → "Can't reach the host".
7. Send a word from either side; the other side opens the Incoming screen with the
   sender's name, the word and its Morse.

Because two emulators report the same device name, set a different name on each one
(Settings → My name, up to 20 characters) so the sender is recognizable.

## 3. PC simulator — one emulator, real TCP

`tools/morse_sim.py` (Python 3, stdlib only) is a group host on your PC with bot members
("Ana" hosts, "Luis" joins). It speaks the real protocol, so the full build's socket layer,
access-code checks and smart replies are exercised for real.

```sh
python3 tools/morse_sim.py            # prints the access code
python3 tools/morse_sim.py --code 1234 --bots Ana   # 2 free slots for emulators
python3 tools/morse_sim.py --selftest
```

On the emulator: Settings → Connection → **LAN / emulator**, host `10.0.2.2` (default), then
**Join group** with the printed code. No `adb forward` is needed: `10.0.2.2` is the PC itself.
Every message you send gets a bot reply 2–4 s later. Type `luis: on my way` in the terminal to
make a bot speak, `/members` to list the group, `/quit` to end it. Stop it before running the
unit tests: `SocketGroupTransportTest` binds the same port 8765.

### Control panel

`tools/morse_panel.py` runs the same simulator with a small web UI (opens `http://localhost:8080`):
pick which bot talks, type a word (live Morse preview) or tap a preset — *Normal*, *Silly*, or
*Mine* (words you save, kept in the browser) — and it lands on the emulator's Incoming screen.
It also shows the members, the conversation, and an auto-reply switch.

```sh
python3 tools/morse_panel.py                       # random code, bots Ana + Luis
python3 tools/morse_panel.py --code 1234 --port 8090 --no-browser
python3 tools/morse_panel.py --selftest
```

## 4. AI features (Gemini free tier)

**Dictate** (Send screen) condenses a spoken sentence to 20 characters or fewer, because each
haptic letter costs about a second. **Reply** (Incoming screen) suggests three one-tap answers
based on the last 10 group messages.

1. Get a free key at <https://aistudio.google.com/apikey> (no card needed).
2. Add `GEMINI_API_KEY=your_key` to `local.properties` (gitignored) and rebuild.
3. Settings shows "AI: Gemini…" when a key is built in.

With no key or no internet, both features use an offline fallback (stopword removal, keyword
replies) and are labelled *Offline*, so the demo never dead-ends. The model is set in
`MorseAssistant.MODEL`. Notes: free-tier prompts may be used by Google to improve its models,
and a key inside an APK can be extracted, so restrict it to the Generative Language API in
Google Cloud console.

## 5. Wi-Fi Direct — real watches only

Uses `WifiP2pManager`: the creator becomes group owner and publishes a `_morse._tcp`
DNS-SD service carrying the access code; joiners discover the service whose code
matches, connect, and then use the same TCP layer against the group owner address.
Emulators do not support Wi-Fi P2P, so selecting this mode on an emulator shows
"Not supported on this device".
