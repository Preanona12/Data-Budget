package com.databudget;

/** Pure budget maths (no Android classes) so it can be unit-tested on a JVM. */
public final class Budget {
    public final long balanceStartOfDay, baseDaily, allowanceToday, usedToday, leftToday, carryOver, remaining;
    public final int daysLeft, daysToExpiry;

    private Budget(long b, long base, long allow, long used, int dl, int de) {
        balanceStartOfDay = b; baseDaily = base; allowanceToday = allow; usedToday = used;
        leftToday = allow - used; carryOver = allow - base; remaining = b - used; daysLeft = dl; daysToExpiry = de;
    }

    /** Fastest believable mobile speed (40 MB/s = 320 Mbit/s). Anything above it between two samples is a counter artefact. */
    public static final long MAX_BYTES_PER_SEC = 40L * 1024 * 1024;

    /**
     * Last line of defence against the phone's mobile counter lying. It can jump by the whole since-boot total of a
     * connection the moment that connection appears (mobile data switched on). Returns the delta that is safe to count.
     * cellPrev/cellNow: 1 = a cellular internet network exists, 0 = none, -1 = unknown.
     */
    public static long guardDelta(long delta, long elapsedMs, int cellPrev, int cellNow) {
        if (delta <= 0) return delta;
        if (cellPrev == 0 && cellNow == 1) return 0;                       // cellular just came up: counter re-based itself
        long secs = Math.max(1, elapsedMs / 1000);
        if (delta > MAX_BYTES_PER_SEC * secs) return 0;                    // faster than any phone can download
        return delta;
    }

    /** Live-tracker refresh interval is kept between 1 and 30 seconds, whatever was saved. */
    public static int clampSeconds(int s) { return Math.max(1, Math.min(30, s)); }

    /** Bytes used since the last sample, from a since-boot counter. Handles first sample and reboots. */
    public static long counterDelta(long last, long cur, long elapsedLast, long elapsedNow) {
        if (last < 0) return 0;                                   // first sample = baseline
        if (elapsedNow < elapsedLast) return cur;                 // phone rebooted: counter restarted at 0
        if (cur < last) return 0;                                 // dropped with no reboot (mobile connection reset): re-baseline, never invent usage
        return cur - last;
    }

    /**
     * @param total        full data pack (bytes)
     * @param preUsed      already used before tracking began + manual adjustments (bytes)
     * @param usedBefore   tracked usage from start until today's midnight (bytes)
     * @param usedToday    tracked usage today (bytes)
     * @param validDays    days until the pack expires
     * @param budgetDays   days you want to spread the data over (<= validDays)
     * @param dayIndex     0 on the day the plan started
     */
    public static Budget compute(long total, long preUsed, long usedBefore, long usedToday,
                                 int validDays, int budgetDays, int dayIndex) {
        long balance = total - preUsed - usedBefore;               // what was left when today began
        int daysLeft = Math.max(budgetDays - dayIndex, 0);
        long allowance = balance / Math.max(daysLeft, 1);          // unused data rolls forward automatically
        long base = (total - preUsed) / Math.max(budgetDays, 1);
        return new Budget(balance, base, allowance, usedToday, daysLeft, Math.max(validDays - dayIndex, 0));
    }
}
