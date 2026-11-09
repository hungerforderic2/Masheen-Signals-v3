# Masheen Signals V3 — Android Prototype

Masheen Signals is an HLP expressive communication prototype. V3 builds on the V2 Android/Compose foundation and coordinates tactile, audio and visual expression previews.

## V3 changes

- **4× timing:** Every nonzero haptic pulse and gap is four times its V2 duration (+300%). Audio tone durations are also four times their defined V2 baselines.
- **Expression-specific audio:** Each of the eight expressions has its own synthesized tone frequency, harmonic balance, duration and volume profile. Audio is generated locally as PCM; no audio assets or network access are required.
- **Haptic texture:** Soft expressions use a gentler, rounded amplitude profile. Sharper expressions retain stronger tactile accents.
- **Center-originating color bursts:** Each preview animates three expanding rings from the center of the display. Burst opacity responds to the selected intensity and uses the expression's color.
- **Intensity:** The 1–5 intensity setting scales haptic amplitude, audio volume and visual burst strength.
- **Independent controls:** Vibration and sound can each be disabled.

## Build on GitHub using a phone

1. Create a new repository or use a clean branch.
2. Extract this ZIP and upload the **contents** of `masheen_signals_android_v3` (the project root) to the repository root.
3. Keep `.github/workflows/android.yml`, the Gradle files and the `app` folder in their existing locations.
4. Open **Actions** and run **Build Masheen Signals**, or push to `main` to trigger the workflow.
5. Download the `Masheen-Signals-APK` artifact from the successful run, extract it and install `app-debug.apk`.

## Build configuration

- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- Gradle 8.9
- Java/Kotlin JVM target 17
- compileSdk 35; minSdk 26; targetSdk 35
- Version 0.3.0 (versionCode 3)

## Prototype limitations

This remains a local preview app: no device pairing, Bluetooth transport, messaging integration or background signaling. Android devices differ in their vibration actuators, audio output and system-level volume behavior. Haptic amplitude support varies by hardware. The visual burst is an on-screen effect and is visible only while the app is in the foreground.

Signals are authored communication patterns, not claims that a device literally experiences emotions.
