# Data on the watch

Morse stores very little, and only on the watch. There is no account, no cloud
copy and no server. Messages are not saved to storage at all: they live in
memory for as long as the group lasts.

## What is stored

| Store | File | Keys | Holds |
|-------|------|------|-------|
| Identity | DataStore `morse_identity` | `member_id` | A random UUID created the first time it is needed. It identifies this watch inside groups and never changes. |
| | | `name_override` | The name typed in Settings or on the welcome, already cleaned. Absent means "use the device name". |
| Settings | DataStore `morse_settings` | `transport_mode` | `WIFI_DIRECT` (default) or `LAN`. |
| | | `lan_host` | The host address for LAN mode. Default `10.0.2.2`. |
| | | `welcome_seen` | Set once the welcome has been shown. |
| Words | DataStore `morse_words` | `custom_words` | Your own words, as a JSON list in the order you added them. |
| Language | SharedPreferences `morse_language` | `tag` | Empty for the system language, `en` or `es`. |

All files are private to the app. Android's automatic backup is allowed, so a
restored watch keeps its words and settings.

### Why the language uses SharedPreferences

The language has to be applied before the first screen is created, inside the
activity's and application's earliest setup step, which cannot wait for an
asynchronous read. DataStore is asynchronous, so this single value is kept in
SharedPreferences, which can be read synchronously. Every other value uses
DataStore, which is safe to read and write from coroutines.

## Your identity

A member is three things: an id, a name and whether it is the host.

- **The id** is generated once per install and sent when joining. The host
  uses it to recognise a watch that reconnects, and the session uses it to tell
  your own messages from everyone else's.
- **The name** is your override if set, otherwise the watch's device name
  from the system settings, otherwise the hardware model. It is always cleaned
  before use: spaces trimmed and collapsed, cut to 20 characters, and "?" if
  nothing is left.
- **Host** is set by the host when it builds the member list; a joining watch
  cannot claim it.

Names also arrive from other watches, so the host cleans every incoming name
too rather than trusting it. On screen, names are shortened to their first
word and to ten characters with an ellipsis, so "Juan Diego's Galaxy Watch"
reads "Juan".

## Your words

Adding a word normalises it (trimmed, inner spaces collapsed) and checks it:

| Result | When |
|--------|------|
| Added | The word is between 1 and 20 characters and not already in the list. |
| Duplicate | The same word is already there, ignoring capitalisation. |
| Invalid | Empty or longer than 20 characters. |

The check and the write happen in one DataStore transaction, so two quick adds
cannot create a duplicate. A corrupted list is read as empty rather than
crashing the app.

The five quick words are not stored. They are string resources, so they follow
the app's language.

## What lives only in memory

The [group session](../internals/group-session.md) keeps, for the current
group only:

- the connection state and the group's code and members,
- the last message, shown on the group and incoming screens,
- the last ten messages, used as context for smart replies.

Leaving the group, or the app process ending, clears all of it.

## What leaves the watch

| Data | Sent to | When |
|------|---------|------|
| Your member id and name | The group host | When you join |
| Your words as you send them, with your id, name and a timestamp | The other watches in the group | When you send |
| The text you dictate | Google's Gemini API | Only with an AI key built in and the watch online |
| The last ten group messages, with sender names | Google's Gemini API | When smart replies are prepared, same conditions |

Without an AI key nothing ever leaves the group. With a key, note that the
free Gemini tier may use prompts to improve Google's models. See
[AI assistant](../internals/ai-assistant.md#privacy-and-the-key).
