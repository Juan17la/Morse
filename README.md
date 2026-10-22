<div align="center">

# Morse

### Silent messages you feel on your wrist.

A Wear OS app for small groups that talk without speaking. Pick a word, send it,
and it arrives on your friends' watches as a Morse vibration they can read by
feel, no need to look at the screen.

[![Platform](https://img.shields.io/badge/Wear%20OS-3%2B-4285F4?label=runs%20on)](#build-and-run)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF)](#how-it-works)
[![Languages](https://img.shields.io/badge/English%20%7C%20Espa%C3%B1ol-555?label=languages)](#good-to-know)

[Features](#what-you-can-do) · [How it works](#how-it-works) · [Build and run](#build-and-run) · [Demo guide](docs_planning/EMULATOR_LAN.md)

</div>

<br />

## What is Morse?

Morse is a native Wear OS app for groups of up to three watches. One person
creates a group and shares a 4-digit code, the others join, and from then on
every word anyone sends plays on everyone else's wrist as a haptic Morse
pattern.

The vibration is the message. The screen shows the dots and dashes in sync with
the buzz, so you learn to recognise words by feel and soon don't need to look at
all. It is made for places where phones or voices are out of place: a class, a
meeting, a theatre, a noisy room.

Morse started as a university interface-design project.

<br />

## What you can do

<table>
<tr>
<td width="50%" valign="top">

**Groups of three**

Create a group, share the access code, and up to three watches are connected
over Wi-Fi Direct. No phone, account or internet needed.

</td>
<td width="50%" valign="top">

**Feel every message**

Incoming words play as short and long vibrations, with the Morse lit up on
screen as it plays. Replay it, reply, or pass it on to the group.

</td>
</tr>
<tr>
<td valign="top">

**Your own words**

A ready-made list of short words plus your own. Sending one is a single tap.

</td>
<td valign="top">

**Dictate**

Say a sentence and AI condenses it to 20 characters or fewer, because each
letter costs about a second of vibration. You see how much time it saved.

</td>
</tr>
<tr>
<td valign="top">

**Smart replies**

Three one-tap replies based on the latest messages in the group.

</td>
<td valign="top">

**Stays out of your way**

A message that arrives while you are busy doesn't pull you off your screen: the
watch edge pulses with the Morse instead.

</td>
</tr>
<tr>
<td valign="top">

**English and Spanish**

Follows the watch language or picks one in Settings, without restarting.

</td>
<td valign="top">

**Fits every watch**

Layouts adapt to small, medium and square screens, in a dark watchOS-style look.

</td>
</tr>
</table>

<br />

## How it works

- **Connection.** Watches find each other with Wi-Fi Direct, then talk over plain
  TCP (port 8765). A LAN mode uses the same socket layer with a typed host
  address, so emulators can join too.
- **Background.** While you are in a group a foreground service keeps the
  connection alive with the screen off.
- **AI.** Dictation and smart replies use the Gemini API (free tier). Without a
  key or without internet they fall back to offline rules, so they always work.
- **Builds.** `full` is the real app. `demo` simulates the other watches, for
  showing the app on a single emulator.

The connection and screen flow diagrams live in
[`docs_planning/`](docs_planning/).

<br />

## Build and run

You need Android Studio (or the Android SDK) and a Wear OS emulator or watch
(API 30+).

```sh
./gradlew :app:installFullDebug   # the real app
./gradlew :app:installDemoDebug   # simulated group, one emulator is enough
./gradlew testFullDebugUnitTest   # unit tests
```

To turn on online AI, get a free key at
[aistudio.google.com/apikey](https://aistudio.google.com/apikey) and add it to
`local.properties` (it is gitignored):

```properties
GEMINI_API_KEY=your-key
```

### Demo without a second watch

`tools/` holds two Python 3 scripts (standard library only) that host a group of
bot watches on your PC:

```sh
python3 tools/morse_sim.py     # terminal simulator, prints the access code
python3 tools/morse_panel.py   # same, with a web control panel at localhost:8080
```

On the watch, set Settings → Connection → **LAN / emulator** and join with the
printed code. The [demo guide](docs_planning/EMULATOR_LAN.md) covers one, two or
three emulators step by step.

<br />

## Good to know

- **Groups are local.** Messages go watch to watch. Nothing is stored on a
  server; the only outside connection is to Gemini when you use the AI features
  with a key.
- **Up to three watches per group**, and the access code is checked on join.
- **Wi-Fi Direct needs the nearby devices permission**, which the app asks for
  before looking for other watches.

<div align="center">
<br />
<sub>Made by Juan Diego López Arias</sub>
</div>
