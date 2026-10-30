# Morse documentation

Morse is a standalone Wear OS app for groups of up to three watches that send
each other short words as Morse code vibrations. It is written in Kotlin with
Jetpack Compose for Wear OS; watches connect over Wi-Fi Direct and talk over
plain TCP, with no phone, server or account.

These pages are grouped by who they are for. People using the app only need
the user guide. Everything else explains how the app works inside.

## User guide

For people wearing the watch. No technical knowledge needed.

| Page | What it covers |
|------|----------------|
| [Getting started](user-guide/getting-started.md) | What you need, installing, full or demo, the first start, Home, your first message |
| [Groups](user-guide/groups.md) | Creating, joining with the code, the three group pages, join and leave signals, leaving |
| [Messages](user-guide/messages.md) | Sending, the confirmation, dictating, receiving, the incoming screen, replies, your own words |
| [Reading Morse by feel](user-guide/reading-morse.md) | Beats and pauses, how long words take, Learn Morse, the system signals |
| [Settings](user-guide/settings.md) | Your name, language, signals, AI status, the connection mode |
| [Troubleshooting](user-guide/troubleshooting.md) | Every error message, common problems, known limitations, logs |

## Architecture

How the application is put together.

| Page | What it covers |
|------|----------------|
| [Overview](architecture/overview.md) | The layers, every package, the full and demo flavours, how objects are wired, the path of one message |
| [Screens and navigation](architecture/screens-and-navigation.md) | Every route, when an incoming message takes over, each screen, fitting round, square and small watches |
| [Design system](architecture/design-system.md) | Colour roles, typography, building blocks, motion, send feedback, accessibility |
| [Data on the watch](architecture/data-on-the-watch.md) | Every stored value, identity and names, your words, what stays in memory, what leaves the watch |

## How it works

One page per subsystem, for contributors and the curious.

| Page | What it covers |
|------|----------------|
| [Connection](internals/connection.md) | The star topology, the wire protocol, the join handshake, relaying, leaving, states and errors, known weaknesses |
| [Finding the host](internals/finding-the-host.md) | Wi-Fi Direct service discovery step by step, permissions, timing, LAN mode and its setups |
| [Group session](internals/group-session.md) | The app-wide owner of the group, sending and receiving, event signals, the foreground service |
| [Morse and haptics](internals/morse-and-haptics.md) | Encoding text, unit timing, the vibration waveform, system signals, keeping the screen in step |
| [AI assistant](internals/ai-assistant.md) | Gemini condense and smart replies, sanitising, the offline rules, privacy and the key |
| [Demo build and PC tools](internals/demo-and-tools.md) | The simulated group, the Python simulator and its web control panel |

## Development

| Page | What it covers |
|------|----------------|
| [Building and running](development/building.md) | Requirements, project layout, Gradle tasks, installing on a watch, emulators, the AI key, translations |
| [Testing](development/testing.md) | Unit tests, real-socket transport tests, the Wi-Fi Direct simulation, the on-watch smoke test, a manual checklist |
| [Demo and emulators](development/demo-and-emulators.md) | Four ways to run a group without three watches, step by step |
| [Git workflow](development/git-workflow.md) | Branches, one commit per change, merging, checks before merging |

## Design notes

Planning material from the interface-design course lives in
[`docs_planning/`](../docs_planning/):

| File | What it is |
|------|-----------|
| [`flow_morse.png`](../docs_planning/flow_morse.png) | The screen flow diagram |
| [`connections_morse.png`](../docs_planning/connections_morse.png) | The connection diagram |
| [`PERFORMANCE_REPORT.md`](../docs_planning/PERFORMANCE_REPORT.md) | Transport stress tests, the Wi-Fi Direct simulation results and the ranked findings |
| [`EMULATOR_LAN.md`](../docs_planning/EMULATOR_LAN.md) | The original emulator demo notes, now covered by [Demo and emulators](development/demo-and-emulators.md) |

## Glossary in one minute

| Term | Meaning |
|------|---------|
| Group | Up to three watches that feel each other's messages |
| Host | The watch that created the group; it relays every message and ends the group when it leaves |
| Member | A watch that joined the group with its access code |
| Access code | Four random digits that identify a group and are checked on join |
| Envelope | One line of JSON on the wire: hello, welcome, rejected, members, message or bye |
| Peer finder | The part that turns an access code into the host's address: Wi-Fi Direct or LAN |
| Unit, beat | The base Morse duration, 120 ms: a dot is 1, a dash 3 |
| Signal | A vibration for an event (connected, sent, not sent, someone left, connection lost), never confused with Morse |
| Full, demo | The two builds: real connections, or simulated members |
