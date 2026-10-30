# Design system

Morse looks like a watch app first: a true black face, softly lit grey
platters, one warm accent and vivid tints that tell things apart. The visual
layer is deliberately quiet, because the main channel is the wrist; what the
screen must do well is show the rhythm in step with the vibration.

## Principles

1. **Haptics first.** Visuals mirror the vibration; they never replace it.
2. **Glanceable.** One main action per screen, big targets, little text.
3. **Learn by seeing the rhythm.** Morse is always drawn as dots and dashes,
   never as a string of characters.
4. **Calm by default.** Motion is short and purposeful, and disappears when
   the system asks for less motion.

## Colour

Colours are defined once in the theme and used by role. The base is the
watchOS-style dark palette, chosen so the black background disappears into
the bezel on OLED screens and saves battery.

| Token | Value | Role |
|-------|-------|------|
| Background | Black `#000000` | Every screen |
| Platter | `#1C1C1E` to `#2C2C2E`, as a vertical gradient | Rows, tiles and buttons |
| Pressed platter | `#3A3A3C` | Reserved for pressed surfaces |
| Accent | Orange `#FF9F0A`, deep `#FF6A00` | The main action on each screen, the letter and symbol playing now |
| Secondary text | Grey `#8E8E93` | Captions, subtitles, inactive Morse |
| Muted | Grey `#7C7C80` | Unplayed letters, idle dots |
| Green | `#30D158` | Connected, the edge ring, Pass on, Create group |
| Blue | `#0A84FF` | Join group, sending, the default incoming tint |
| Red | `#FF453A` | Errors, Leave, Delete |
| Purple | `#BF5AF2` | AI, My words, dictation |
| Teal, Indigo, Pink | `#64D2FF`, `#5E5CE6`, `#FF375F` | Learn, selected options, avatars |

**The sunrise gradient** (yellow to orange to deep orange) paints the single
most important action on a screen: Send message, Dictate, Add word, Start,
Replay, and the suggested short text.

**Avatar colours.** Each member gets one of seven tints, picked from a hash of
their name. The same name always gets the same colour, on every screen and on
every watch, without sending any colour over the network.

**Top glow.** A soft radial light falls from the top of each screen in the
screen's tint (blue for Send, purple for My words and Shortened, teal for
Learn, the sender's colour on Incoming, green, orange or red on Group for the
connection state). When the tint changes it cross-fades over 600 ms.

## Typography

The Wear Material type scale, tuned toward an SF-like rhythm: bold titles with
slightly tight tracking and semibold labels. All sizes are in scalable units,
so the watch's font size setting is respected. Monospace is used only for the
access code digits, where equal widths keep the four tiles aligned.

## Building blocks

| Component | What it is | Used for |
|-----------|------------|----------|
| Scaffold | The time at the top, a vignette, a position indicator, the top glow and a scaling list with responsive margins and an optional tinted title with a small "-.-" rule under it | Every list screen |
| Platter | A rounded row, at least 56 dp tall, with a glossy round icon or an avatar, a title and either a subtitle or drawn Morse. It can be the accent (sunrise) row, disabled (38% opacity) or read-only (no press role) | Almost every row in the app |
| Morse glyphs | Dots drawn as circles and dashes as capsules three times as long, with wider gaps between letters, cut off at the edge for long words | Under every word, the title rule, the Learn legend |
| Morse visualiser | Larger glyphs that wrap letter by letter and light up with the playback | Incoming, Welcome, Learn |
| Beacon | One light that is on exactly while the motor is | Incoming, Welcome, Learn |
| Avatar | A circle with the name's initial in the name's colour | People, senders, the name row |
| Pill button | A rounded platter with icon and label, 48 dp tall | Group page actions |
| Code tile | A glass tile with one digit; new digits roll up into place | The access code |
| Status dot | Green, red or breathing orange | Group code page |
| Page dots | Three dots on the right edge with a spoken position | Group pager |
| Edge ring | A green glow around the face, round or rounded-square to match the screen | Messages that arrive while you are busy |

Icons come from the Phosphor set, imported as vector drawables (`ph_*`), so
they share one stroke weight.

## Motion

| Moment | Motion | With reduced motion |
|--------|--------|---------------------|
| Pressing a platter | Shrinks to 96% on a soft spring instead of a ripple | No shrink |
| A new incoming word | Pops in from 60% scale on a spring | Appears at full size |
| Active Morse symbol | Swells 35% and glows | Kept: it carries information, not decoration |
| Home hub changing | Cross-fade with a slight scale | System animator scale applies |
| Access code digits | Roll up into place | System animator scale applies |
| Status dot while connecting | Breathes | Steady |
| Buzz-time bars | Grow over 700 ms | System animator scale applies |

One spring is shared by all presses, entrances and value changes so the app
feels consistent. Reduced motion is detected from the system "Remove
animations" setting (animator duration scale of zero).

## Send feedback

Every send, reply and pass on goes through one shared helper so the answer is
always the same:

- **Success:** a Wear confirmation with a green check, "Sent *word*" and the
  word's Morse in the accent colour, for 1.6 seconds. Then the screen goes
  back.
- **Failure:** a Wear alert, "*word* wasn't sent", with Cancel and Retry.
  Retry sends the same word again.

A second tap while a send is in progress is ignored, so a word is never sent
twice by accident.

## Accessibility

| Area | What Morse does |
|------|-----------------|
| Eyes-free use | Every message and event has its own vibration; the screen is optional. |
| Touch targets | Rows at least 56 dp, buttons at least 48 dp, orbs 56 to 68 dp. |
| TalkBack | Icon-only buttons have spoken labels (Replay, Send message, Cancel, Retry, Delete). Each code digit picker is announced as "Digit 1" to "Digit 4". The group pager announces "Page 1 of 3, swipe up or down". Decorative icons are silent. |
| Font size | All text is in scalable units; layouts were checked at 1.3 times. |
| Contrast | White or light-grey text on black, black text on the orange accent. |
| Motion | Decorative motion follows the system Remove animations setting. |
| Language | Every string exists in English and Spanish. |
