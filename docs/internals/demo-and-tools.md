# Demo build and PC tools

Morse was built to be presented live, often with only one watch or one
emulator at hand. Two things make a group possible without three watches:

- the **demo build**, where the other members are simulated inside the app;
- the **PC tools**, Python scripts that host a real group of bot watches on a
  computer, which the full build joins over the network.

| | Demo build | PC simulator and panel |
|---|---|---|
| Runs | Inside the app | On a computer, Python 3, standard library only |
| Network | None | Real TCP on port 8765, the app's own protocol |
| Exercises | The whole UI, haptics, session and AI | All of that plus the real socket layer, the handshake and access code checks |
| Control | Automatic | Type in a terminal or click in a web page |
| App build to use | `demo` | `full`, in LAN mode |

Step-by-step setups are in [Demo and emulators](../development/demo-and-emulators.md).

## The simulated transport (demo build)

The demo build replaces the socket transport with an in-memory one that
follows the same transport contract, so the session and screens cannot tell
the difference. It has three fake members, named in the app's language: "Ana's
watch", "Luis's watch" and "María's watch" ("Reloj de Ana" and so on in
Spanish).

| Action | What the simulation does |
|--------|--------------------------|
| Create group | Waits 0.8 s, generates a real random code, becomes Connected with you as host, then adds a fake member every 1.5 s until the group has three. |
| Join group | Accepts any code. Waits 1.2 s, becomes Connected with Ana as host and you as member, then adds Luis 2 s later. |
| Send | Echoes your message to your own stream like the real transport, then, 2 to 4 seconds later, a random fake member answers with a random quick word. The answer arrives as a normal incoming message: it vibrates, opens the incoming screen and gets smart replies. |
| Leave | Cancels every pending timer and returns to idle. |

Because every reply is a real message through the real session, the demo shows
the complete loop: sending, the Sent signal, receiving, the vibration, the
incoming screen, replies and passing on. The connection settings are hidden in
this build, since they would have no effect, and the Wi-Fi Direct permission is
never requested.

## The PC simulator

`tools/morse_sim.py` is a group host written in Python with only the standard
library. It re-implements the wire protocol, the Morse table and the offline
reply rules, so to the watch it is indistinguishable from a host watch.

**Bots.** By default the group has two bots, Ana (the host) and Luis, which
leaves one seat for a watch. The bot list can be changed at start; with a
single bot, two watches can join. The first bot is always the host.

**The host logic** mirrors the app's:

| Step | Behaviour |
|------|-----------|
| Accept | One thread per connection, port 8765 on every interface, address reuse enabled. |
| Hello | Anything other than `hello` closes the connection. |
| Code | A wrong code gets `rejected WRONG_CODE`, and the attempt is printed. |
| Name | Cleaned: spaces collapsed, cut to 20 characters. |
| Seat | A watch with an id already present replaces itself. Bots plus watches cannot exceed three; otherwise `rejected GROUP_FULL`. |
| Welcome | `welcome` to the newcomer, `members` to the other watches. |
| Messages | Printed with their Morse, kept in a log of the last 200, and relayed to every other watch. With auto-reply on, a random bot answers 2 to 4 s later using the same keyword rules as the app's offline replies. |
| Leaving | `bye` or a closed socket removes the watch and sends the new member list. Quitting the simulator sends `bye` from the host bot, so watches see the group end. |

**The console.** Typing a line makes the host bot send it. Prefixing it with a
bot name and a colon, for example "luis: on my way", makes that bot send it.
`/members` lists the group, the host marked with a star, and `/quit` ends it.
Words are cut to 20 characters.

**Self-test.** A built-in self-test checks the encoder against known codes
(SOS, accents, Ñ, digits), the reply rules, and that a message envelope
serialises to a single line and back.

## The control panel

`tools/morse_panel.py` runs the same simulator and adds a small web page on
`localhost:8080`, opened in the browser automatically. It is meant to sit on
the presenter's screen while the watch is shown to the audience.

| Area | What it does |
|------|--------------|
| Status | The access code, the members and how many seats are left. |
| Sender | Which bot speaks next. |
| Word | A text box with a live Morse preview as you type. |
| Presets | One-tap words in three sets: Normal, Silly and Mine. Mine holds words you save, kept in the browser's local storage. |
| Conversation | The last 60 messages from everyone, refreshed continuously. |
| Auto-reply | Turns the bots' automatic answers on or off. |

The page talks to the simulator through three small JSON endpoints: one to read
the state, one to make a bot send a word, and one to switch auto-reply. The
send endpoint collapses spaces, cuts the word to 20 characters, upper-cases it
and refuses words with nothing Morse can carry. The HTTP server listens on
localhost only, so nobody else on the network can drive the group.

The panel's port can be changed, the browser launch can be skipped, and it has
its own self-test of the endpoints.

## Things to know

- **One port, one host.** The simulator binds port 8765. The app's unit tests
  bind the same port on the computer, so stop the simulator before running
  them.
- **No Wi-Fi Direct.** The tools are reached over the network, so the watch or
  emulator must use the LAN mode with the computer's address (`10.0.2.2` from
  an emulator).
- **Bots answer everything.** Turn auto-reply off in the panel to control the
  conversation by hand during a presentation.
