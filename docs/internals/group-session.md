# Group session

The **group session** is the single owner of "the group I am in" for the whole
app. It lives as long as the app process, so the group does not depend on which
screen is open. Every screen reads the group from it, and every send goes
through it.

## What it holds

| Value | Meaning |
|-------|---------|
| State | Idle, Creating, Joining, Connected or Error with a reason. |
| Group | The access code and the current member list, or nothing. |
| Last message | The most recent message in the group, from anyone. |
| History | The last ten messages, oldest first, yours included. |
| Received | A stream of messages from **other** members only. |
| Self id | This watch's member id for the current group. |
| Is host | Whether this watch created the group. |

Screens observe these values through their ViewModels and redraw when they
change. The navigation host listens to the received stream to decide whether
to open the incoming screen.

## Creating and joining

Both start the same way: leave whatever group there was, read the connection
mode from Settings, and build a **new transport** for that mode. A fresh
transport per group means a mode change in Settings simply applies to the next
group, with no half-reconfigured objects.

Right after building the transport, the session starts three collectors that
copy the transport's state, members and incoming messages into its own values.
Then:

- **Create.** The host flag is set before anything else, so the group screen,
  opened in the same tap, already knows to start on the code page. When the
  transport returns the code, the group is published with the host as its
  only member.
- **Join.** The host flag is cleared, the group is set with the typed code and
  no members yet, and the transport joins. The member list arrives with the
  welcome.

Any failure is turned into an Error state with the transport's reason, or
`UNKNOWN` if the failure had none, and logged.

## Sending

Sending a word:

1. Fails immediately, returning "not sent", if there is no transport.
2. Builds a message: a new random id, this watch's id and current name, the
   word, its Morse encoding, and the current time.
3. Hands it to the transport.
4. On success, plays the **Sent** signal and returns "sent". On failure, plays
   **Not sent**, moves to the Error state and returns "not sent".

The return value is what the send feedback on screen shows: a check or a retry
alert.

## Receiving

Every message the transport delivers, including the echo of your own sends,
goes through one function:

1. Append it to the history and keep only the last ten.
2. Make it the last message. The history is updated first because smart
   replies, triggered by the last message changing, read the history.
3. If it came from someone else: **play it on the vibration motor** and emit it
   on the received stream.

Vibration therefore happens in the session, not in a screen. A message is felt
whatever is on the screen, with the screen off, and with another app on top,
as long as the process is alive.

## Haptic signals for events

The session watches the transport's state and member list and plays a signal
when something worth feeling happens:

| Change | Signal |
|--------|--------|
| Any state to Connected | Connected |
| Connected to Error | Connection lost |
| The member list gets shorter | Someone left |
| A send succeeds | Sent |
| A send fails | Not sent |

The signals are described in
[Morse and haptics](morse-and-haptics.md#system-signals).

## Leaving

Leaving cancels the three collectors, asks the transport to leave (which says
goodbye to the other side and closes every socket and radio resource), and
clears the group, the last message, the history and the state. It is also the
first step of every create and join, so the app can never be in two groups.

## Staying alive in the background

Android restricts the network access of apps that are not visible. With the
screen off, or with the keyboard or another app on top, a plain background app
would lose its group socket, and the watch could never receive a message while
idle.

To prevent this, the application watches the session's state and, whenever it
becomes Connected, starts a **foreground service**; whenever it stops being
Connected, it stops the service.

| Property | Value |
|----------|-------|
| Service type | Connected device |
| Notification channel | "Group connection", low importance, so it never makes a sound |
| Notification | "Morse · Connected to your group", ongoing, with the vibration icon |
| Tapping it | Opens Morse |
| Restart policy | Not restarted by the system if the process dies; the group would be gone anyway |

The service holds no logic of its own: its only job is to mark the process as
in use. The group itself stays in the session.

Being Connected only ever starts from a tap in the app, so the app is always
in the foreground when the service starts, which is what Android requires for
starting a foreground service.

## Message history and smart replies

The ten-message history exists for the AI. When a message from someone else
becomes the last message, the incoming screen asks the assistant for three
replies, passing that message and the history. Your own messages are part of
the history, so the suggestions follow the whole conversation, not only the
last word. See [AI assistant](ai-assistant.md#smart-replies).
