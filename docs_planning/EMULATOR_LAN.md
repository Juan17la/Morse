# Demoing Morse on emulators

The app has three connection modes (Settings → Connection). All of them share the
same UI, protocol and group session; only the way watches find each other changes.

## 1. Simulated (default) — one emulator

No setup. Create a group and fake members ("Ana's Watch", "Luis' Watch") join after
a moment; every message you send gets a fake reply 2–4 s later, which opens the
Incoming screen and plays the Morse vibration. The emulator has no motor, so the
pattern is also logged:

```
adb logcat -s HapticPlayer
```

## 2. LAN / emulator — two or three emulators exchanging real TCP messages

After Wi-Fi Direct forms a group, the watches talk over plain TCP (port 8765). The LAN
mode uses that same socket layer, with the host address entered manually. On the
Android emulator `10.0.2.2` is the development machine, so with one `adb forward`
per host a second emulator can reach the first one.

1. Start two Wear OS emulators (e.g. `emulator-5554` and `emulator-5556`) and install
   the app on both.
2. On **both** emulators: Settings → Connection → **LAN / emulator**.
3. On the joiner(s) the *Host address* must be `10.0.2.2` (the default).
4. Forward the host port on the emulator that will create the group:

   ```
   adb -s emulator-5554 forward tcp:8765 tcp:8765
   ```

5. On `emulator-5554`: **Create group** — note the 4-digit code.
6. On `emulator-5556`: **Join group**, enter the code, **Join**.
   - Wrong code → "Wrong access code".
   - A 4th watch → "The group is full" (`GroupConfig.MAX_MEMBERS`).
7. Send a word from either side; the other side opens the Incoming screen with the
   sender's name, the word and its Morse.

Because two emulators report the same device name, set a different name on each one
(Settings → My name) so the sender is recognizable.

## 3. Wi-Fi Direct — real watches only

Uses `WifiP2pManager`: the creator becomes group owner and publishes a `_morse._tcp`
DNS-SD service carrying the access code; joiners discover the service whose code
matches, connect, and then use the same TCP layer against the group owner address.
Emulators do not support Wi-Fi P2P, so selecting this mode on an emulator shows
"Not supported on this device".
