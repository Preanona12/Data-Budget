# Data Budget

An Android app that turns your mobile data pack into a **daily budget**. Tell it how much data you have and when it expires, and it shows how much you can use today. Whatever you don't use rolls over to tomorrow.

Made for people who ration mobile data. Phones already show how much you've used, but they don't tell you how much you *can* use today.

## Features

- **Daily budget with rollover.** Unused data carries over to the next day.
- **Flexible plan.** Start with data you have already used, and spread the budget over fewer days than the pack's expiry (for example, a 30-day pack budgeted over 15 days).
- **Mobile data only.** Wi-Fi is shown for reference but never counted.
- **Live tracking.** Update speed is adjustable from 1 to 30 seconds (default 15), with a small live notification.
- **Alerts** at 80% and when today's budget is used up.
- **Units:** MB, GB, Mb and Gb.
- **Corrections.** Add or subtract usage in your chosen unit, with undo, to match your phone's own counter.
- **Themes.** Light, Dark and Follow system; moods (Calm, Energetic, Cozy, Focus); time of day (Sunrise, Day, Sunset, Night, or Auto by time).
- **Tracker diagnostics** (Settings): a log of counter resets and ignored jumps, with a Copy button for bug reports.
- Keeps working in the background and restarts after a reboot.

## Privacy

The app has **no internet permission**, so it cannot send your data anywhere. Everything stays on your phone. Permissions it asks for, and why:

| Permission | Why |
|---|---|
| Usage access (`PACKAGE_USAGE_STATS`) | Fallback source for mobile usage, and the Wi-Fi figure shown for reference |
| `READ_PHONE_STATE` | Android 9 and older only, needed to read mobile usage stats |
| `ACCESS_NETWORK_STATE` | Detects when the mobile network goes up or down (used to ignore counter glitches) |
| `POST_NOTIFICATIONS` | Alerts and the live notification (Android 13+) |
| `FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED` | Background tracking, restarted after reboot |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Optional button so Android doesn't pause tracking |

## Install

1. Download the latest `DataBudget-vX.Y.apk` from the **Releases** page.
2. Open it on your phone and allow installing from this source if asked.
3. Open the app, allow data-usage access when prompted, then enter your plan.

Requires Android 6.0 (API 23) or newer.

## How the numbers work

The app reads Android's live mobile-data counter and adds up the difference each time it refreshes. If a phone doesn't offer that counter, it falls back to Android's usage statistics, which can lag.

Phones differ here. Some reset or shift this counter when the mobile connection goes off and on. The app ignores resets, impossible speed jumps and jumps that happen when the mobile network comes up, and records them in **Settings > Tracker diagnostics**. Your phone's built-in data usage screen is the best reference: if the two disagree, use **Correct your usage** to match it.

**Tested on one phone so far (Tecno KJ6).** Other brands may behave differently. If your numbers drift, please open an issue and paste the diagnostics log (Settings > Tracker diagnostics > Copy log).

## Build from source

No Gradle or Android Studio needed. Tested on Ubuntu 24.04:

```bash
sudo apt install openjdk-21-jdk-headless aapt apksigner android-sdk-platform-23 dalvik-exchange zipalign
bash build.sh
```

This runs the unit tests (budget maths, theme rules, counter-jump guards), then builds and signs `DataBudget-vX.Y.apk`.

**About the signing key.** On first build, `build.sh` creates `debug.keystore` next to the script. Keep it private (it is in `.gitignore`) and back it up. Android only installs an update over an existing install if it is signed with the same key, so if you lose it, people have to uninstall first and lose their saved plan.

## Project layout

```
AndroidManifest.xml
build.sh                 build, test and sign
res/                     icon and strings (the UI itself is built in code)
src/com/databudget/
  Budget.java            budget maths and counter-jump guards (pure Java, unit-tested)
  Theme.java             palettes and theme rules (pure Java, unit-tested)
  Changelog.java         the in-app changelog data
  Tracker.java           reads the counters, keeps the ledger and diagnostics log
  TrackerService.java    live background tracking
  MainActivity.java      dashboard
  SettingsActivity.java  themes, update speed, alerts, units, diagnostics
  ChangelogActivity.java changelog screen
  BudgetTest.java        unit tests, run by build.sh
```

See [CHANGELOG.md](CHANGELOG.md) for version history.

## Credits

Built with [Claude](https://claude.ai), an AI assistant made by Anthropic. Claude made this app possible: it wrote the code, tests and build script.

The idea, feature requests, direction and all real-phone testing came from the repository owner.

## License

MIT. See [LICENSE](LICENSE). Replace `<YOUR NAME>` in that file with your name before publishing.
