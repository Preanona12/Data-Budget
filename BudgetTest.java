package com.databudget;
public class BudgetTest {
    static final long GB = 1024L * 1024 * 1024, MB = 1024L * 1024;
    static int fails = 0;
    static void eq(String n, long a, long b) { if (Math.abs(a - b) > 2) { fails++; System.out.println("FAIL " + n + ": " + a + " != " + b); } else System.out.println("ok   " + n); }
    static void str(String n, String a, String b) { if (!b.equals(a)) { fails++; System.out.println("FAIL " + n + ": " + a + " != " + b); } else System.out.println("ok   " + n); }
    static void ok(String n, boolean c) { if (!c) { fails++; System.out.println("FAIL " + n); } else System.out.println("ok   " + n); }
    static double lum(int c) {
        double[] v = {(c >> 16) & 255, (c >> 8) & 255, c & 255};
        for (int i = 0; i < 3; i++) { double x = v[i] / 255.0; v[i] = x <= 0.03928 ? x / 12.92 : Math.pow((x + 0.055) / 1.055, 2.4); }
        return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2];
    }
    static double contrast(int a, int b) { double x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); }
    public static void main(String[] x) {
        long t = 7 * GB;
        Budget a = Budget.compute(t, 0, 0, 0, 30, 30, 0);
        eq("7GB/30d daily", a.allowanceToday, t / 30);
        Budget b = Budget.compute(t, 0, 0, 100 * MB, 30, 30, 0);
        eq("left today after 100MB", b.leftToday, t / 30 - 100 * MB);
        // used nothing for 1 day -> surplus rolls to day 2
        Budget c = Budget.compute(t, 0, 0, 0, 30, 30, 1);
        eq("rollover day2", c.allowanceToday, t / 29);
        eq("carry over day2", c.carryOver, t / 29 - t / 30);
        // overspent day 1 -> day 2 shrinks
        Budget d = Budget.compute(t, 0, t / 30 + 200 * MB, 0, 30, 30, 1);
        eq("overspend shrinks day2", d.allowanceToday, (t - t / 30 - 200 * MB) / 29);
        // already used 3GB at install
        Budget e = Budget.compute(t, 3 * GB, 0, 0, 30, 30, 0);
        eq("pre-used 3GB", e.allowanceToday, 4 * GB / 30);
        // 30-day expiry but spread over 15 days
        Budget f = Budget.compute(t, 0, 0, 0, 30, 15, 0);
        eq("15-day budget", f.allowanceToday, t / 15);
        eq("expiry still 30", f.daysToExpiry, 30);
        // after budget window ends, everything left is available
        Budget g = Budget.compute(t, 0, 6 * GB, 0, 30, 15, 16);
        eq("window over", g.allowanceToday, 1 * GB);
        eq("window over days left", g.daysLeft, 0);
        eq("counter normal", Budget.counterDelta(1000, 1900, 50, 80), 900);
        eq("counter first sample", Budget.counterDelta(-1, 5000, 0, 10), 0);
        eq("counter reboot (reset)", Budget.counterDelta(9000, 400, 500, 5), 400);
        eq("counter reboot (grew past old)", Budget.counterDelta(100, 900, 500, 5), 900);
        // ---- v2.3: changelog data ----
        ok("changelog has entries", Changelog.ALL.length > 0);
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        for (Changelog.Entry en : Changelog.ALL) {
            ok("changelog " + en.version + ": version is unique", seen.add(en.version));
            ok("changelog " + en.version + ": has at least one line", en.added.length + en.fixed.length > 0);
            boolean blank = false;
            for (String ln : en.added) if (ln.trim().length() == 0) blank = true;
            for (String ln : en.fixed) if (ln.trim().length() == 0) blank = true;
            ok("changelog " + en.version + ": no blank lines", !blank);
        }
        str("changelog newest first = 2.3", Changelog.ALL[0].version, "2.3");
        // ---- v2.2: the counter lying when a mobile connection appears (your real log: +906.5 MB in 13 s) ----
        long MBY = 1024L * 1024;
        eq("your log: 906.5 MB in 13 s is ignored", Budget.guardDelta((long) (906.5 * MBY), 13000, 1, 1), 0);
        eq("mobile came up (off -> on): any jump ignored", Budget.guardDelta(120 * MBY, 15000, 0, 1), 0);
        eq("mobile came up, tiny delta also ignored", Budget.guardDelta(1 * MBY, 15000, 0, 1), 0);
        eq("normal use, mobile on all along", Budget.guardDelta(9 * MBY, 15000, 1, 1), 9 * MBY);
        eq("heavy but real: 30 MB in 15 s", Budget.guardDelta(30 * MBY, 15000, 1, 1), 30 * MBY);
        eq("real use then mobile went down", Budget.guardDelta(9 * MBY, 15000, 1, 0), 9 * MBY);
        eq("unknown cellular state, normal use", Budget.guardDelta(9 * MBY, 15000, -1, -1), 9 * MBY);
        eq("unknown cellular state, impossible speed", Budget.guardDelta(900 * MBY, 13000, -1, -1), 0);
        eq("long gap (job every 15 min) real use", Budget.guardDelta(300 * MBY, 900000, 1, 1), 300 * MBY);
        eq("sub-second sample, small delta ok", Budget.guardDelta(30 * MBY, 500, 1, 1), 30 * MBY);
        eq("zero delta stays zero", Budget.guardDelta(0, 15000, 0, 1), 0);
        // ---- v2.1: a drop without a reboot must never be counted as usage ----
        eq("counter dropped, no reboot", Budget.counterDelta(9000, 400, 500, 800), 0);
        eq("counter unchanged", Budget.counterDelta(500, 500, 10, 20), 0);
        eq("counter dropped to zero, no reboot", Budget.counterDelta(9000, 0, 500, 800), 0);
        eq("counter normal growth still counted", Budget.counterDelta(9000, 9150, 500, 800), 150);
        // ---- v2.0: interval slider ----
        eq("interval below 1 -> 1", Budget.clampSeconds(0), 1);
        eq("interval negative -> 1", Budget.clampSeconds(-5), 1);
        eq("interval 1 stays", Budget.clampSeconds(1), 1);
        eq("interval 15 stays", Budget.clampSeconds(15), 15);
        eq("interval 30 stays", Budget.clampSeconds(30), 30);
        eq("interval above 30 -> 30", Budget.clampSeconds(99), 30);
        // ---- v2.0: theme choice rules ----
        str("auto 04:59 night", Theme.resolve("auto", false, 4), "night");
        str("auto 05:00 sunrise", Theme.resolve("auto", false, 5), "sunrise");
        str("auto 09:59 sunrise", Theme.resolve("auto", false, 9), "sunrise");
        str("auto 10:00 day", Theme.resolve("auto", false, 10), "day");
        str("auto 16:59 day", Theme.resolve("auto", false, 16), "day");
        str("auto 17:00 sunset", Theme.resolve("auto", false, 17), "sunset");
        str("auto 19:59 sunset", Theme.resolve("auto", false, 19), "sunset");
        str("auto 20:00 night", Theme.resolve("auto", false, 20), "night");
        str("auto midnight night", Theme.resolve("auto", false, 0), "night");
        str("system dark", Theme.resolve("system", true, 12), "dark");
        str("system light", Theme.resolve("system", false, 12), "light");
        str("pinned mood", Theme.resolve("cozy", true, 3), "cozy");
        str("pinned time theme ignores clock", Theme.resolve("sunset", false, 3), "sunset");
        str("unknown -> light", Theme.resolve("banana", true, 3), "light");
        str("missing -> light", Theme.resolve(null, true, 3), "light");
        for (String[] ch : Theme.CHOICES) {
            String r = Theme.resolve(ch[0], false, 12);
            ok("choice '" + ch[0] + "' resolves to a real theme", Theme.byId(r).id.equals(r));
        }
        // ---- v2.0: every palette must be readable ----
        for (Theme th : Theme.ALL) {
            ok(th.id + ": text on background >= 4.5", contrast(th.ink, th.bg) >= 4.5);
            ok(th.id + ": text on cards >= 4.5", contrast(th.ink, th.card) >= 4.5);
            ok(th.id + ": grey text on cards >= 4.5", contrast(th.gray, th.card) >= 4.5);
            ok(th.id + ": grey text on background >= 4.0", contrast(th.gray, th.bg) >= 4.0);
            ok(th.id + ": white on buttons >= 3.5", contrast(0xFFFFFFFF, th.accent) >= 3.5);
            ok(th.id + ": secondary button text >= 4.5", contrast(th.onSoft, th.soft) >= 4.5);
            double heroMin = th.id.equals("light") ? 2.5 : 3.0;   // "light" keeps the exact v1.2 teal gradient
            ok(th.id + ": white on hero (both ends) >= " + heroMin, contrast(0xFFFFFFFF, th.heroA) >= heroMin && contrast(0xFFFFFFFF, th.heroB) >= heroMin);
            ok(th.id + ": white on over-budget (both ends) >= 3.0", contrast(0xFFFFFFFF, th.overA) >= 3.0 && contrast(0xFFFFFFFF, th.overB) >= 3.0);
            ok(th.id + ": selected label (accent) on tile >= 3.0", contrast(th.accent, th.bg) >= 3.0);
        }
        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED"); System.exit(fails);
    }
}
