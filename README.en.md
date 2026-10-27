<p align="center">
  <img src="docs/images/clock-icon.svg" width="88" height="88" alt="QuietClock app icon" />
</p>

<h1 align="center">QuietClock · 静夜时钟</h1>

<p align="center"><a href="README.md">简体中文</a> · <strong>English</strong></p>

<p align="center">A lightweight, native Android flip clock for landscape display</p>
<p align="center">Black background · Flip digits · Night schedule · Display shifting</p>

<p align="center">
  <a href="#download-and-install"><img src="docs/images/badge-android.svg" alt="Android 8.0 and above" /></a>
  <a href="https://github.com/lucifly/QuietClock/releases/tag/v0.1.4"><img src="docs/images/badge-version.svg" alt="0.1.4 test release" /></a>
  <a href="LICENSE"><img src="docs/images/badge-license.svg" alt="MIT License" /></a>
</p>

<p align="center">
  <a href="https://github.com/lucifly/QuietClock/releases/download/v0.1.4/QuietClock-0.1.4-debug.apk"><strong>Download APK</strong></a>
  · <a href="#preview">Preview</a>
  · <a href="#features">Features</a>
  · <a href="#development-and-build">Build from source</a>
  · <a href="https://github.com/lucifly/QuietClock/issues">Report an issue</a>
</p>

## Preview

<p align="center">
  <img src="docs/images/clock-landscape.png" alt="QuietClock in landscape: gray flip digits with a black fold line, with the date and battery level underneath" />
</p>

This preview was generated using the app's actual drawing code. The time, date, and battery level are examples. The entire clock shifts periodically; actual display brightness depends on the device and brightness settings.

## Download and install

**Current version: 0.1.4 test release · Android 8.0 or later**

1. [Download the APK](https://github.com/lucifly/QuietClock/releases/download/v0.1.4/QuietClock-0.1.4-debug.apk) on your phone.
2. Open the downloaded APK and follow Android's prompts to allow installation from that source.
3. Open QuietClock and place your phone horizontally. Tap the clock to open settings.

[View the release and checksum file](https://github.com/lucifly/QuietClock/releases/tag/v0.1.4). GitHub's automatically generated `Source code` archives contain the source, not the installer. Choose the `.apk` file to install the app. The current APK uses the same debug signing certificate as earlier test builds.

The app follows the system language automatically: Chinese and English are supported, with English as the fallback for unsupported languages.

## Features

| Feature | What it does |
| :--- | :--- |
| **Chinese and English** | Automatically localizes the interface, date, battery status, settings, and accessibility prompts to match the system language. Unsupported languages fall back to English. |
| **Flip clock** | Displays hours and minutes in separate groups with a black fold line through the digits. Brief flip animations show the card backgrounds, which return to black afterward. |
| **Date and battery** | Displays the date, battery level, and charging status. Follows the system's 12/24-hour time format. |
| **Display shifting** | Moves the time, date, and battery information together: small shifts every minute and larger position changes every 10 minutes, constrained to the visible area. |
| **Night schedule** | Repeats daily and supports schedules that cross midnight. Disables flip animations while continuing time updates and display shifting. |
| **Brightness and colors** | Separate regular and nighttime brightness settings, with gray, warm, and green text options. |
| **Orientation** | Supports 180° rotation and remembers your choice. Includes adaptive and monochrome app icons. |
| **Auto-stop timer** | Stops the clock after 30 minutes, 1 hour, or 2 hours. Settings show the remaining time and expected end time, with an option to cancel. |

## Usage and defaults

Tap the clock to open settings. Changes are saved locally on your device.

| Setting | Default | Options |
| :--- | :--- | :--- |
| Regular brightness | 5% | 1%–30% |
| Night schedule | Enabled, 23:00–07:00 the next day | Custom start and end times, including overnight schedules |
| Night brightness | 4% | Device minimum, or 1%–10% |
| Text color | Gray | Gray / warm / green |
| Auto-stop timer | Off | 30 minutes / 1 hour / 2 hours |
| 180° rotation | Off | Toggle to match your phone's placement |

**How it runs:** The app keeps the screen on while in the foreground. It does not replace or customize the system's Always-On Display (AOD). Pressing the power button or switching to another app allows the screen to turn off normally.

**OLED use:** Shifting spreads display time across different areas, but cannot guarantee prevention of burn-in. Actual brightness, battery use, and temperature still need testing on your device.

## Privacy

- **No network access:** The app does not request internet permission and includes no ads, analytics, or third-party tracking SDKs.
- **No account required:** It reads only the system time, battery level, and charging status locally on the device.
- **Local settings:** Brightness, rotation, night schedule, and other preferences are stored in local `SharedPreferences`.
- **No extra permissions:** It does not require location, camera, microphone, contacts, accessibility service, or overlay permissions.

## Development and build

Built with Java and Android's native drawing APIs, with no third-party runtime library dependencies.

<details>
<summary><strong>Requirements and build steps</strong></summary>

| Tool | Version |
| :--- | :--- |
| JDK | 17 |
| Android SDK Platform | 35 |
| Android SDK Build Tools | 35.0.0 |
| Android Gradle Plugin | 8.9.2 |
| Gradle Wrapper | 8.11.1, with distribution SHA-256 verification configured |

Open the project in Android Studio and configure the JDK and SDK. Alternatively, set `JAVA_HOME` and `ANDROID_HOME`, then run the following from the project root:

```sh
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

On Windows, use `gradlew.bat`. The first build and test run require dependency downloads.

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Configure `local.properties` on your own machine; do not commit it. Local SDK paths, build caches, logs, signing private keys, and generated installation packages are excluded from source commits.

Debug builds use the developer's local debug signing certificate. No private keys are included in the repository. For production releases, use a signing key that you manage securely.

</details>

<details>
<summary><strong>Test coverage and implementation notes</strong></summary>

- 20 Android behavior regression tests cover startup, touch interaction, pause/resume, dialog brightness, rotation, countdown timers, layout caching, app icons, and night brightness migration and persistence, language changes, localized dates, language priority, and settings preservation.
- Tests use Robolectric to simulate Android 15 (API 35) on the JVM and do not replace testing on a physical device.
- Layout, font metrics, and split digit strings are cached and updated when size, font configuration, content, or position changes.
- Night schedules use local time, including the start and excluding the end. The schedule is reevaluated after system time or time zone changes.
- Minimum brightness, device compatibility, battery use, temperature, and extended operation still need testing on physical devices.

</details>

## Changelog

**[0.1.4](https://github.com/lucifly/QuietClock/releases/tag/v0.1.4)** · Added English support with automatic system language selection, including localized dates, battery status, settings, and accessibility prompts.

<details>
<summary>Earlier versions</summary>

- **0.1.3:** Raised default night brightness from 2% to 4%, with migration for older settings while preserving subsequent manual adjustments.
- **0.1.2:** Added a night brightness slider.
- **0.1.1:** Improved auto-stop status, added app icons, and cached the drawing layout.
- **0.1:** Initial landscape flip clock prototype.

</details>

## Feedback

Found a bug or have a feature suggestion? [Open an issue](https://github.com/lucifly/QuietClock/issues). For bug reports, include your phone model, operating system version, and steps to reproduce the problem.

## License

QuietClock's original code and assets are licensed under the [MIT License](LICENSE), with the copyright notice `Copyright (c) 2026 lucifly`. This license also applies to the original portions of the APKs published in this repository.

Gradle Wrapper and other third-party components retain their own licenses. The Gradle Wrapper distributed with this repository is licensed under Apache-2.0; a copy is available at [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt).

## 🌟 Star History

<a href="https://www.star-history.com/#lucifly/QuietClock&amp;Date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=lucifly/QuietClock&amp;type=Date&amp;theme=dark" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=lucifly/QuietClock&amp;type=Date" />
    <img alt="QuietClock GitHub star history" src="https://api.star-history.com/svg?repos=lucifly/QuietClock&amp;type=Date" />
  </picture>
</a>
