# Building and running

## Requirements

| Tool | Version |
|------|---------|
| JDK | 17 or newer (the project has been built with 21) |
| Android SDK | Platform 35, build tools and platform tools |
| Gradle | 8.7, downloaded by the wrapper in the repository |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 1.9.24, with the Compose compiler 1.5.14 |
| A Wear OS target | A Wear OS emulator or watch, API 30 or newer |
| Python 3 | Only for the PC tools in `tools/` |

Android Studio provides all of the Android pieces. Open the project folder and
let it sync. Without Android Studio, install the SDK command-line tools and
point `sdk.dir` in `local.properties` to the SDK.

## Project layout

| Path | What it holds |
|------|---------------|
| `app/src/main` | The app: Kotlin sources, the manifest, resources in English (`values`) and Spanish (`values-es`) |
| `app/src/full` | The full flavour's flag |
| `app/src/demo` | The demo flavour's simulated transport and fake member names |
| `app/src/test` | JVM unit tests and transport simulations |
| `app/src/androidTest` | The on-watch smoke test |
| `tools/` | The PC group simulator and its web control panel |
| `docs/` | This documentation |
| `docs_planning/` | Planning material: flow and connection diagrams, the emulator guide, the performance report |

## Builds

There are two flavours, `full` and `demo`, and the usual `debug` and `release`
build types. All Gradle tasks are run from the project root with the wrapper,
`./gradlew` (or `gradlew.bat` on Windows).

| Task | Result |
|------|--------|
| `:app:installFullDebug` | Builds and installs Morse on the connected watch or emulator |
| `:app:installDemoDebug` | Builds and installs Morse Demo |
| `:app:assembleFullDebug`, `:app:assembleDemoDebug` | Builds the APK without installing it, into `app/build/outputs/apk/` |
| `:app:assembleFullRelease` | A release APK. It needs a signing configuration of your own. |
| `:app:lintFullDebug` | Android lint, report in `app/build/reports/` |

The two flavours have different application ids, so both can be installed on
the same watch.

When several devices are connected, choose one by setting the
`ANDROID_SERIAL` environment variable to its serial (as listed by
`adb devices`) before running an install task.

## Installing on a real watch

1. On the watch, open Settings, System, About and tap the build number seven
   times to unlock Developer options.
2. In Developer options, turn on **ADB debugging** and **Debug over Wi-Fi**.
   The watch shows an address and port.
3. Put the computer on the same Wi-Fi and connect with `adb connect` followed
   by that address. Accept the prompt on the watch.
4. Run an install task. The app appears in the watch's app list.

Some watches show a separate pairing code under **Wireless debugging**; in that
case pair first with `adb pair` and the code shown.

Before relying on Wi-Fi Direct, check that the watch exposes it to apps:
`adb shell pm list features` should list `android.hardware.wifi.direct`. If it
does not, use the LAN mode on a shared Wi-Fi network instead.

## Running on an emulator

Create a **Wear OS** virtual device in Android Studio's Device Manager (any
size; the large round one is used for the documentation screenshots), start
it, and run an install task.

Emulators have no Wi-Fi Direct and no vibration motor:

- Use the demo build, or the full build in **Local network (emulator)** mode
  together with a second emulator or the PC tools. See
  [Demo and emulators](demo-and-emulators.md).
- Every vibration pattern is logged under the `HapticPlayer` tag instead.

## Turning on online AI

Dictation and smart replies use Gemini when a key is built in.

1. Get a free key at [aistudio.google.com/apikey](https://aistudio.google.com/apikey).
2. Add a line `GEMINI_API_KEY=` followed by the key to `local.properties` in
   the project root. This file is ignored by git, so the key is never
   committed.
3. Rebuild and reinstall. Settings now says **Smart replies: AI on**.

The key is read when Gradle configures the build and compiled into the app as
a build constant. An empty or missing key gives a fully working build with the
offline rules. Restrict the key to the Generative Language API in the Google
Cloud console, because a key inside an APK can be extracted. More in
[AI assistant](../internals/ai-assistant.md#privacy-and-the-key).

## Versions

The version is set in `app/build.gradle.kts`: a version code that increases
with every release and a version name (currently 1.1). The demo flavour adds
`-demo` to the name.

## Languages

Every string lives in `app/src/main/res/values/strings.xml` (English) and
`values-es/strings.xml` (Spanish). Add every new string to both. Brand names
and language names are marked as not translatable.

App bundles keep all languages in one download on purpose, because the in-app
language switch needs both on the watch even when the system language is
different.
