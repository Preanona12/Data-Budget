# Changelog

Newest first. The same list is inside the app (menu → Changelog).

## v2.3

**New**

- Changelog screen (menu → Changelog) listing what changed in every version

## v2.2

**Fixes**

- Ignores impossible jumps in the mobile counter (anything faster than 320 Mbit/s), like the +906 MB in 13 seconds that was counted by mistake
- Ignores the counter jump that can happen the moment mobile data is switched on
- The diagnostics log now says when a jump was ignored, and shows whether the mobile network is up

## v2.1

**New**

- Tracker diagnostics in Settings: a log of reboots, counter drops and big jumps, with Copy and Clear buttons

**Fixes**

- A drop in the phone's counter with no reboot is no longer counted as usage (this caused the phantom 100+ MB while on Wi-Fi)
- If the phone hides the mobile counter while mobile data is off, the live count now stays as it is instead of switching to a less exact source

## v2.0

**New**

- Three-dot menu on the main screen: Settings, Appearance & themes, About
- Settings screen (alerts, background tracking and units moved here from the main screen)
- 12 themes: Light, Dark, Follow system; moods Calm, Energetic, Cozy, Focus; time of day Sunrise, Day, Sunset, Night, plus Auto by time
- Live tracker update speed slider, 1 to 30 seconds (default 15), applied right away

## v1.0 – 1.2 – First builds

**New**

- Daily budget from your pack size and expiry days, with unused data rolling over to the next day
- Start with data you have already used, and spread the budget over fewer days than the expiry
- Units: MB, GB, Mb and Gb
- Mobile data only: Wi-Fi is shown for reference but never counted
- Alerts at 80% and when today's budget is used up
- Keeps tracking in the background with a live notification, and restarts after a reboot
- Corrections: add or subtract usage in your chosen unit, with undo
- Live mobile-data counter for exact numbers, with Android usage stats as a fallback
