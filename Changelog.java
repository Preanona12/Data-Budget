package com.databudget;

/** What changed in each version, newest first. Plain data, so it is unit-tested on a JVM. */
public final class Changelog {
    public static final class Entry {
        public final String version, title; public final String[] added, fixed;
        Entry(String version, String title, String[] added, String[] fixed) { this.version = version; this.title = title; this.added = added; this.fixed = fixed; }
    }
    static final String[] NONE = {};

    public static final Entry[] ALL = {
        new Entry("2.3", "", new String[]{
            "Changelog screen (menu \u2192 Changelog) listing what changed in every version",
        }, NONE),
        new Entry("2.2", "", NONE, new String[]{
            "Ignores impossible jumps in the mobile counter (anything faster than 320 Mbit/s), like the +906 MB in 13 seconds that was counted by mistake",
            "Ignores the counter jump that can happen the moment mobile data is switched on",
            "The diagnostics log now says when a jump was ignored, and shows whether the mobile network is up",
        }),
        new Entry("2.1", "", new String[]{
            "Tracker diagnostics in Settings: a log of reboots, counter drops and big jumps, with Copy and Clear buttons",
        }, new String[]{
            "A drop in the phone's counter with no reboot is no longer counted as usage (this caused the phantom 100+ MB while on Wi-Fi)",
            "If the phone hides the mobile counter while mobile data is off, the live count now stays as it is instead of switching to a less exact source",
        }),
        new Entry("2.0", "", new String[]{
            "Three-dot menu on the main screen: Settings, Appearance & themes, About",
            "Settings screen (alerts, background tracking and units moved here from the main screen)",
            "12 themes: Light, Dark, Follow system; moods Calm, Energetic, Cozy, Focus; time of day Sunrise, Day, Sunset, Night, plus Auto by time",
            "Live tracker update speed slider, 1 to 30 seconds (default 15), applied right away",
        }, NONE),
        new Entry("1.0 \u2013 1.2", "First builds", new String[]{
            "Daily budget from your pack size and expiry days, with unused data rolling over to the next day",
            "Start with data you have already used, and spread the budget over fewer days than the expiry",
            "Units: MB, GB, Mb and Gb",
            "Mobile data only: Wi-Fi is shown for reference but never counted",
            "Alerts at 80% and when today's budget is used up",
            "Keeps tracking in the background with a live notification, and restarts after a reboot",
            "Corrections: add or subtract usage in your chosen unit, with undo",
            "Live mobile-data counter for exact numbers, with Android usage stats as a fallback",
        }, NONE),
    };
}
