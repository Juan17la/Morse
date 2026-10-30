# Troubleshooting

## Error messages

| Message | Where | Cause | What to do |
|---------|-------|-------|------------|
| Wrong access code | Join | A host answered with a different code. Only the LAN mode can tell this directly. | Check the four digits on the host's code page and try again. |
| The group is full | Join | The group already has three watches. | Ask someone to leave, or start a second group. |
| No group found with that code | Join | Over Wi-Fi Direct, no nearby watch advertised that code within 20 seconds. In LAN mode, the host address could not be resolved. | Check the code. Make sure the host is still in the group and close by. In LAN mode, check the host address in Settings. |
| Can't reach the host | Join | The host was found but refused or ignored the connection within 5 seconds. | Make sure the host has created the group. In LAN mode, check the address and that port 8765 is forwarded or reachable. |
| Connection lost | Group | The link to the group dropped: the host left, went out of range, or the network changed. | Tap **Start over** (member) or **Retry** (host). |
| Not supported on this device | Create or join | The watch has no Wi-Fi Direct, which is always the case on emulators. | Switch to **Local network (emulator)** in Settings, or use the demo build. |
| Permission denied | Create or join | The Nearby devices (or location) permission was refused. | Tap the settings button in the alert and allow it, then try again. |
| "*word*" wasn't sent | Send | The word could not leave the watch, usually because you are no longer in a group. | Check the group page, rejoin if needed, then **Retry**. |
| Something went wrong | Anywhere | An error without a more specific reason. | Leave the group and create or join again. |

## Common problems

### Joining takes a long time

Over Wi-Fi Direct the watches first have to discover each other and form a
radio link before any message can flow. Around ten seconds is normal. When
several people join at once, the host accepts them one after another, so the
last one waits longest. For a smooth demo, join one watch at a time.

If the code is mistyped, the joining watch keeps looking for the full 20
seconds before it says **No group found with that code**, because it cannot
tell a wrong code from a host that has not been found yet.

### I don't feel anything

- Check that the watch is not in a do-not-disturb or theatre mode that blocks
  vibration.
- Open Settings and tap **Feel a sample (SOS)**. If that does not vibrate, the
  problem is the watch, not the group.
- Emulators have no vibration motor. Every pattern they would play is written
  to the log instead; see [Demo and emulators](../development/demo-and-emulators.md).

### Messages arrive on the screen edge instead of opening

That is on purpose. When you are busy in another Morse screen, an incoming
word lights the green ring around the edge in time with the vibration rather
than interrupting you. Open the group to see it, or go back to Home and the
next word opens in full.

### Everyone appears with the same name

Two watches of the same model, and two emulators, report the same device name.
Set a personal name in Settings, under **My name**.

### Smart replies always say Offline

The build has no AI key, or the watch has no internet connection. Offline
suggestions still work. See
[Turning on online AI](../development/building.md#turning-on-online-ai).

### The app is in the wrong language

Settings, **Language**. **System default** follows the watch, the other two
options force a language.

## Known limitations

These come from the transport stress tests in
[`docs_planning/PERFORMANCE_REPORT.md`](../../docs_planning/PERFORMANCE_REPORT.md)
and are worth knowing before a live demo.

- **A host that silently disappears is not noticed.** If the host's battery
  dies or it walks out of range without leaving, the other watches keep
  showing Connected and a send may still show the green check. Leave and
  rejoin if the group goes quiet for too long.
- **A watch that drops out can keep its seat** until the host notices the
  connection is closed, so a new watch may briefly be told the group is full.
- **Two groups in the same room** are told apart only by their access code.
  Pick distinct codes by creating again if two groups share one.
- **Wi-Fi Direct support varies.** Some Wear OS watches do not expose Wi-Fi
  Direct to apps. In that case Morse reports "Not supported on this device".
- **Battery.** While in a group the connection and the background service stay
  active until you leave. Leave the group when you are done.

## Getting logs

Connect the watch with ADB and filter the log by the parts of the app:

| Tag | What it logs |
|-----|--------------|
| `GroupSession` | Which connection mode was used, and every failed group operation |
| `GroupHostServer` | Members joining and dropping, failed relays (on the host) |
| `GroupClient` | When the link to the host ended (on members) |
| `WifiDirectPeerFinder` | Wi-Fi Direct actions that failed, with the system's reason code |
| `HapticPlayer` | Every Morse pattern played, in milliseconds |
| `MorseAssistant` | When the online AI failed and the offline rules were used |

The command is `adb logcat -s` followed by the tags you want, separated by
spaces.
