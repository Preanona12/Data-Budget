package com.databudget;

import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import java.util.Calendar;
import java.util.Locale;

/** Shared logic used by the screen and by the background alert job. */
public final class Tracker {
    static final long MB = 1024L * 1024L, DAY = 86400000L;
    static final String[] UNITS = {"MB", "GB", "Mb", "Gb"};
    static final double[] UNIT = {MB, 1024.0 * MB, MB / 8.0, 1024.0 * MB / 8.0}; // bytes per unit (b = bit)

    static final java.util.HashSet<String> CHANNELS = new java.util.HashSet<String>();
    static final class Snap { Budget b; long wifiToday; boolean live, paused; int valid, bud; long expiry; }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("plan", 0); }
    /** How often the live tracker refreshes, from Settings (1-30 s, default 15). */
    static int intervalSec(Context c) { return Budget.clampSeconds(prefs(c).getInt("interval", 15)); }
    static long intervalMs(Context c) { return intervalSec(c) * 1000L; }

    static boolean hasAccess(Context c) {
        AppOpsManager a = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
        return a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }
    static String subscriber(Context c) {
        if (Build.VERSION.SDK_INT >= 29) return null; // null = all mobile networks
        try { return ((TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE)).getSubscriberId(); } catch (Exception e) { return null; }
    }
    /** type = TYPE_MOBILE counts only cellular bytes; TYPE_WIFI is queried separately and never added. */
    static long usage(Context c, int type, long from, long to) {
        if (to <= from) return 0;
        try {
            NetworkStatsManager m = (NetworkStatsManager) c.getSystemService(Context.NETWORK_STATS_SERVICE);
            NetworkStats.Bucket b = m.querySummaryForDevice(type, type == ConnectivityManager.TYPE_MOBILE ? subscriber(c) : null, from, to);
            return b.getRxBytes() + b.getTxBytes();
        } catch (Exception e) { return 0; }
    }
    static long midnight(long t) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(t);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Null if no plan yet. Also rolls yesterday's manual corrections into "earlier days". */
    static Snap snapshot(Context c) { return snapshot(c, true); }
    static Snap snapshot(Context c, boolean wifi) {
        SharedPreferences p = prefs(c);
        long start = p.getLong("start", 0); if (start == 0) return null;
        long now = System.currentTimeMillis(), mid = midnight(now), sMid = midnight(start);
        if (p.getLong("mDay", 0) != mid)
            p.edit().putLong("mBefore", p.getLong("mBefore", 0) + p.getLong("mToday", 0)).putLong("mToday", 0).putLong("mDay", mid).apply();
        int idx = (int) ((mid - sMid + DAY / 2) / DAY);
        boolean live = sample(c) || p.getBoolean("liveOk", false);   // counter hidden while mobile data is off: keep the ledger, do not switch sources
        long before, today;
        if (live) { before = p.getLong("lBefore", 0); today = p.getLong("lToday", 0); }
        else { // phones without a mobile counter: fall back to Android's usage stats
            before = idx == 0 ? 0 : usage(c, ConnectivityManager.TYPE_MOBILE, start, mid);
            today = usage(c, ConnectivityManager.TYPE_MOBILE, Math.max(start, mid), now);
        }
        before = Math.max(0, before + p.getLong("mBefore", 0));
        today = Math.max(0, today + p.getLong("mToday", 0));   // corrections count as used TODAY
        Snap s = new Snap(); s.live = live; s.paused = live && p.getBoolean("unsup", false); s.valid = p.getInt("valid", 30); s.bud = p.getInt("bud", 30);
        s.b = Budget.compute(p.getLong("total", 0), p.getLong("pre", 0), before, today, s.valid, s.bud, idx);
        s.wifiToday = wifi && hasAccess(c) ? usage(c, ConnectivityManager.TYPE_WIFI, Math.max(start, mid), now) : -1;
        Calendar e = Calendar.getInstance(); e.setTimeInMillis(sMid); e.add(Calendar.DAY_OF_YEAR, s.valid); s.expiry = e.getTimeInMillis();
        return s;
    }

    // ---------- live counter: mobile interfaces only (Wi-Fi is never included) ----------
    static synchronized boolean sample(Context c) {
        SharedPreferences p = prefs(c);
        long rx = TrafficStats.getMobileRxBytes(), tx = TrafficStats.getMobileTxBytes();
        long mid = midnight(System.currentTimeMillis()), before = p.getLong("lBefore", 0), today = p.getLong("lToday", 0);
        if (p.getLong("lDay", 0) != mid) { before += today; today = 0; }          // new day (also while the counter is unavailable)
        if (rx == TrafficStats.UNSUPPORTED || tx == TrafficStats.UNSUPPORTED) {
            // Some phones hide the mobile counter while mobile data is off. Keep the live ledger as it is (nothing was used).
            if (p.getBoolean("liveOk", false) && !p.getBoolean("unsup", false)) log(c, "mobile counter unavailable (mobile data off?) - ledger kept as is");
            p.edit().putLong("lBefore", before).putLong("lToday", today).putLong("lDay", mid).putBoolean("unsup", true).putInt("cellUp", cellularUp(c)).apply();
            return false;
        }
        long last = p.getLong("cLast", -1), eLast = p.getLong("eLast", 0);
        long cur = rx + tx, el = SystemClock.elapsedRealtime();
        long delta = Budget.counterDelta(last, cur, eLast, el);
        int cellPrev = p.getInt("cellUp", -1), cell = cellularUp(c);
        if (p.getBoolean("unsup", false)) log(c, "mobile counter is back");
        if (last >= 0) {
            if (el < eLast) log(c, "reboot detected: counter restarted, counted " + mb(delta));
            else if (cur < last) log(c, "counter dropped by " + mb(last - cur) + " with no reboot - re-baselined, nothing added");
            else {
                long safe = Budget.guardDelta(delta, el - eLast, cellPrev, cell);
                if (safe != delta) {
                    log(c, (cellPrev == 0 && cell == 1 ? "mobile network came up: counter jumped +" : "impossible speed: counter jumped +") + mb(delta)
                            + " in " + (el - eLast) / 1000 + " s - ignored (not real usage)");
                    delta = safe;
                } else if (delta >= 50 * MB) log(c, "big jump counted: +" + mb(delta) + " in " + (el - eLast) / 1000 + " s, active network: " + activeNet(c));
                else if (delta >= 5 * MB && !"MOBILE".equalsIgnoreCase(activeNet(c))) log(c, "mobile counter grew +" + mb(delta) + " while active network was " + activeNet(c));
            }
        }
        if (cell != cellPrev && cellPrev != -1 && cell != -1) log(c, "mobile network " + (cell == 1 ? "up" : "down"));
        p.edit().putInt("cellUp", cell).putLong("cLast", cur).putLong("eLast", el).putLong("lBefore", before).putLong("lToday", today + delta).putLong("lDay", mid)
                .putBoolean("liveOk", true).putBoolean("unsup", false).apply();
        return true;
    }
    /** 1 = a cellular network with internet exists right now (mobile data is on), 0 = none, -1 = can't tell. */
    static int cellularUp(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            for (android.net.Network n : cm.getAllNetworks()) {
                android.net.NetworkCapabilities nc = cm.getNetworkCapabilities(n);
                if (nc != null && nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                        && nc.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)) return 1;
            }
            return 0;
        } catch (Exception e) { return -1; }
    }
    static String mb(long b) { return String.format(Locale.US, "%.1f MB", b / (double) MB); }
    static String activeNet(Context c) {
        try {
            android.net.NetworkInfo n = ((ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            return n == null ? "none" : n.getTypeName();
        } catch (Exception e) { return "?"; }
    }
    /** Keeps the last 40 tracker events so a drifting number can be explained. */
    static synchronized void log(Context c, String msg) {
        SharedPreferences p = prefs(c);
        String line = new java.text.SimpleDateFormat("MMM d HH:mm:ss", Locale.US).format(new java.util.Date()) + "  " + msg;
        java.util.ArrayList<String> all = new java.util.ArrayList<String>();
        for (String x : p.getString("log", "").split("\n")) if (x.length() > 0) all.add(x);
        all.add(line);
        while (all.size() > 40) all.remove(0);
        StringBuilder sb = new StringBuilder();
        for (String x : all) { if (sb.length() > 0) sb.append('\n'); sb.append(x); }
        p.edit().putString("log", sb.toString()).apply();
    }
    static void resetLedger(Context c) {
        prefs(c).edit().putLong("lBefore", 0).putLong("lToday", 0).putLong("lDay", midnight(System.currentTimeMillis())).putLong("cLast", -1).putBoolean("liveOk", false).apply();
    }
    static void startBg(Context c) {
        try {
            SharedPreferences p = prefs(c);
            if (p.getLong("start", 0) == 0 || !p.getBoolean("bg", true)) return;
            Intent i = new Intent(c, TrackerService.class);
            if (Build.VERSION.SDK_INT >= 26) Context.class.getMethod("startForegroundService", Intent.class).invoke(c, i); else c.startService(i);
        } catch (Exception ignored) { }
    }
    static void stopBg(Context c) { try { c.stopService(new Intent(c, TrackerService.class)); } catch (Exception ignored) { } }

    static String fmt(Context c, long bytes) {
        double a = Math.abs((double) bytes); String sg = bytes < 0 ? "-" : "";
        int d = prefs(c).getInt("disp", 0);
        if (d == 0) d = a >= 1024.0 * MB ? 2 : 1;
        switch (d) {
            case 1: return sg + String.format(Locale.US, "%.0f MB", a / MB);
            case 2: return sg + String.format(Locale.US, "%.2f GB", a / (1024.0 * MB));
            case 3: return sg + String.format(Locale.US, "%.1f Mb", a * 8 / MB);
            default: return sg + String.format(Locale.US, "%.2f Gb", a * 8 / (1024.0 * MB));
        }
    }

    // ---------- alerts: 80% heads-up and 100% "used up", once per day each ----------
    static void check(Context c, Snap s) {
        SharedPreferences p = prefs(c);
        if (s == null || !p.getBoolean("notif", true)) return;
        long mid = midnight(System.currentTimeMillis());
        if (p.getLong("nDay", 0) != mid) p.edit().putLong("nDay", mid).putInt("nLvl", 0).apply();
        Budget b = s.b; int want = 0;
        if (b.allowanceToday <= 0 || b.usedToday >= b.allowanceToday) want = 2;
        else if (b.usedToday * 10 >= b.allowanceToday * 8) want = 1;
        if (want <= p.getInt("nLvl", 0)) return;
        p.edit().putInt("nLvl", want).apply();
        if (want == 2) send(c, "Daily data budget used up", "You've used " + fmt(c, b.usedToday) + " of today's " + fmt(c, b.allowanceToday) + ". Tomorrow's budget will adjust.");
        else send(c, "80% of today's data used", fmt(c, b.leftToday) + " left for today.");
    }
    static Notification.Builder builder(Context c, String id, String name, int importance) throws Exception {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) { // channel APIs are newer than the compile jar, so use reflection
            if (!CHANNELS.contains(id)) {   // create each channel once per process (the live tracker can tick every second)
                Class<?> cc = Class.forName("android.app.NotificationChannel");
                Object ch = cc.getConstructor(String.class, CharSequence.class, int.class).newInstance(id, name, importance);
                nm.getClass().getMethod("createNotificationChannel", cc).invoke(nm, ch);
                CHANNELS.add(id);
            }
            return Notification.Builder.class.getConstructor(Context.class, String.class).newInstance(c, id);
        }
        return new Notification.Builder(c).setPriority(importance >= 4 ? Notification.PRIORITY_HIGH : Notification.PRIORITY_LOW);
    }
    static android.app.PendingIntent open(Context c) {
        return android.app.PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), android.app.PendingIntent.FLAG_IMMUTABLE);
    }
    static void send(Context c, String title, String text) {
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(1, builder(c, "budget", "Budget alerts", 4).setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text).setContentIntent(open(c)).setAutoCancel(true).build());
        } catch (Exception ignored) { }
    }
    /** The small ongoing notification that keeps the tracker alive in the background. */
    static Notification live(Context c, Snap s) {
        String title = "Data Budget", text = "Set up your plan to start tracking";
        if (s != null) {
            title = s.b.leftToday >= 0 ? "Left today: " + fmt(c, s.b.leftToday) : "Over budget by " + fmt(c, -s.b.leftToday);
            text = "Used " + fmt(c, s.b.usedToday) + " of " + fmt(c, s.b.allowanceToday) + " \u2022 " + s.b.daysToExpiry + " days to expiry";
        }
        try {
            return builder(c, "tracker", "Live tracker", 2).setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text)
                    .setContentIntent(open(c)).setOngoing(true).build();
        } catch (Exception e) { return new Notification.Builder(c).setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text).build(); }
    }
}
