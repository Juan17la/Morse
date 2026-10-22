# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users
Small groups (max 3 watches) who exchange short words discreetly by wrist vibration, e.g. where phones or speech are not appropriate. The vibration is the message; the screen is secondary and must work glanced at or eyes-free.

## Product Purpose
A native Wear OS app (Kotlin/Compose) that sends words as Morse and plays incoming words as haptic patterns, so the wearer can understand the message by feel, reply, or pass it on. Built as a university interface-design project; success includes a live class presentation.

## Positioning
Silent group messaging whose primary channel is a Morse vibration on the wrist, with the dots/dashes shown in sync so users learn to read it by feel.

## Operating Context
Group created by a host with a 4-digit access code, over Wi-Fi Direct (full build) or LAN/emulator mode (TCP port 8765). A demo build simulates watches. `tools/morse_sim.py` (PC bot group host) and `tools/morse_panel.py` (web control panel) drive demos; the user wants a real watch to join the group via a PC script or the Python tool for the presentation.

## Capabilities and Constraints
- Max 3 members per group (`GroupConfig.MAX_MEMBERS`), access code check.
- Screens: Send, Incoming, Words, Group, Condense, Settings; AI dictation and smart replies (`MorseAssistant`).
- Spanish and English strings must stay in sync (values / values-es).
- Layouts adapt to small, medium and square watch screens.
- Flavors: full and demo.

## Evidence on Hand
Docs in `docs_planning/` (EMULATOR_LAN.md, flow and connection diagrams). No real user testing, testimonials or metrics exist; do not fabricate.

## Product Principles
- Haptics first: the feel of the message leads, visuals support it.
- Eyes-free and glanceable: large targets, minimal text.
- Understanding the Morse is part of the experience, not just delivery.
- Always demoable: a presentation path must work with emulator or real watch.

## Accessibility & Inclusion
Must remain usable without looking at the screen; ES and EN supported.
