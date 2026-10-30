package com.databudget;

import android.Manifest;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends BaseActivity {
    LinearLayout hero, track; View barFill;
    TextView hLabel, hBig, hSub, vDaily, vCarry, vRemain, vExp, vDays, vWifi, note, fixInfo;
    Button grant;
    EditText fTotal, fValid, fBudget, fUsed, fFix;
    Spinner sTotal, sUsed, sFix;
    final Handler h = new Handler();
    // refreshes the screen at the same interval as the background tracker (Settings, 1-30 s)
    final Runnable tick = new Runnable() { public void run() {
        if (checkTheme()) return;                       // time-of-day theme rolled over
        refresh(); h.postDelayed(this, Tracker.intervalMs(MainActivity.this)); } };

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        buildUi(); schedule(); Tracker.startBg(this);
        ArrayList<String> need = new ArrayList<String>();
        if (Build.VERSION.SDK_INT < 29 && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) need.add("android.permission.POST_NOTIFICATIONS");
        if (!need.isEmpty()) requestPermissions(need.toArray(new String[0]), 1);
    }
    @Override protected void onResume() { super.onResume(); h.removeCallbacks(tick); h.post(tick); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacks(tick); }

    void schedule() {
        try {
            JobScheduler js = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            for (JobInfo j : js.getAllPendingJobs()) if (j.getId() == 7) return;
            js.schedule(new JobInfo.Builder(7, new ComponentName(this, CheckJob.class)).setPeriodic(15 * 60 * 1000L).setPersisted(true).build());
        } catch (Exception ignored) { }
    }

    TextView tile(LinearLayout row, String label) {
        LinearLayout t = new LinearLayout(this); t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(dp(14), dp(12), dp(14), dp(12)); t.setBackground(rounded(T.card, 16)); t.setElevation(dp(1));
        text(t, label, 12, false, T.gray);
        TextView v = text(t, "--", 17, true, T.ink);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f); p.setMargins(dp(4), dp(4), dp(4), dp(4));
        row.addView(t, p); return v;
    }

    void buildUi() {
        ScrollView sv = new ScrollView(this); sv.setId(9100); sv.setBackgroundColor(T.bg);
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(16), dp(20), dp(16), dp(28)); sv.addView(l);

        // header: title on the left, menu button on the right
        LinearLayout head = new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL);
        text(titles, "Data Budget", 26, true, T.ink);
        text(titles, "Mobile data only \u2022 Wi-Fi is never counted", 13, false, T.gray);
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        final DotsView dots = new DotsView(this);
        dots.setContentDescription("Menu");
        dots.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showMenu(dots); } });
        head.addView(dots, new LinearLayout.LayoutParams(dp(44), dp(44)));
        l.addView(head, lp(-1, -2, 0, 0, 0, 14));

        hero = new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setPadding(dp(22), dp(20), dp(22), dp(22)); hero.setElevation(dp(3));
        hLabel = text(hero, "LEFT TODAY", 12, true, 0xB3FFFFFF);
        hBig = text(hero, "--", 46, true, Color.WHITE);
        hSub = text(hero, "", 14, false, 0xE6FFFFFF);
        track = new LinearLayout(this); track.setWeightSum(1000); track.setBackground(rounded(0x40FFFFFF, 6));
        barFill = new View(this); barFill.setBackground(rounded(Color.WHITE, 6));
        track.addView(barFill, new LinearLayout.LayoutParams(0, -1, 0f));
        hero.addView(track, lp(-1, dp(10), 0, 14, 0, 0));
        l.addView(hero, lp(-1, -2, 0, 0, 0, 12));

        grant = btn(l, "Allow data-usage access to start tracking", true, new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); } });
        grant.setBackground(rounded(0xFFF59E0B, 14));

        LinearLayout r1 = row(l); vDaily = tile(r1, "Daily budget"); vCarry = tile(r1, "Carried over");
        LinearLayout r2 = row(l); vRemain = tile(r2, "Data remaining"); vExp = tile(r2, "Expires in");
        LinearLayout r3 = row(l); vDays = tile(r3, "Budget days left"); vWifi = tile(r3, "Wi-Fi today (not counted)");
        ((View) vWifi.getParent()).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); } });
        note = text(l, "", 13, false, T.gray); note.setPadding(dp(4), dp(8), dp(4), dp(8));

        LinearLayout plan = card(l);
        text(plan, "Your data plan", 18, true, T.ink);
        sTotal = spinner(Tracker.UNITS, sp.getInt("uTotal", 1)); sUsed = spinner(Tracker.UNITS, sp.getInt("uUsed", 0));
        double tot = sp.getLong("total", 7168L * Tracker.MB) / Tracker.UNIT[sp.getInt("uTotal", 1)];
        double pre = sp.getLong("pre", 0) / Tracker.UNIT[sp.getInt("uUsed", 0)];
        fTotal = input(plan, "Total data in your pack", num(tot), false, sTotal, 9001);
        fValid = input(plan, "Days until it expires", String.valueOf(sp.getInt("valid", 30)), false, null, 9002);
        fBudget = input(plan, "Days to spread the budget over (can be fewer than expiry days)", String.valueOf(sp.getInt("bud", 30)), false, null, 9003);
        fUsed = input(plan, "Already used when you start", num(pre), false, sUsed, 9004);
        btn(plan, "Save plan & start today", true, new View.OnClickListener() { public void onClick(View v) { save(); } });

        LinearLayout fix = card(l);
        text(fix, "Correct your usage", 18, true, T.ink);
        text(fix, "Counts toward today's usage. Use a minus sign to subtract.", 13, false, T.gray);
        sFix = spinner(Tracker.UNITS, 0);
        fFix = input(fix, "Amount to add or subtract", "", true, sFix, 9005); fFix.setHint("e.g. 50 or -50");
        fixInfo = text(fix, "", 13, false, T.gray); fixInfo.setPadding(0, dp(8), 0, 0);
        btn(fix, "Apply correction", true, new View.OnClickListener() { public void onClick(View v) { fix(false); } });
        btn(fix, "Undo all corrections", false, new View.OnClickListener() { public void onClick(View v) { fix(true); } });

        TextView f = text(l, "Data Budget v" + version(), 12, false, T.gray); f.setGravity(Gravity.CENTER); f.setPadding(0, dp(6), 0, 0);
        setContentView(sv);
    }
    String version() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) { return ""; }
    }
    void showMenu(View anchor) {
        new AlertDialog.Builder(this).setItems(new String[]{"Settings", "Appearance & themes", "Changelog", "About"}, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int which) {
                if (which == 0) startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                else if (which == 1) startActivity(new Intent(MainActivity.this, SettingsActivity.class).putExtra("section", "appearance"));
                else if (which == 2) startActivity(new Intent(MainActivity.this, ChangelogActivity.class));
                else new AlertDialog.Builder(MainActivity.this).setTitle("Data Budget v" + version())
                        .setMessage("Tracks mobile data only (Wi-Fi is never counted) and turns your pack into a daily budget that rolls over.")
                        .setPositiveButton("OK", null).show();
            } }).show();
    }
    static String num(double d) { return d == Math.floor(d) ? String.valueOf((long) d) : String.format(java.util.Locale.US, "%.2f", d); }
    double val(EditText e) { try { return Double.parseDouble(e.getText().toString().trim()); } catch (Exception x) { return 0; } }
    String f(long b) { return Tracker.fmt(this, b); }

    // ---------- actions ----------
    void save() {
        int ut = sTotal.getSelectedItemPosition(), uu = sUsed.getSelectedItemPosition();
        double tot = val(fTotal) * Tracker.UNIT[ut], used = val(fUsed) * Tracker.UNIT[uu];
        int valid = (int) val(fValid), bud = (int) val(fBudget);
        if (tot <= 0 || valid < 1 || bud < 1) { toast("Enter a data size and at least 1 day"); return; }
        if (bud > valid) { toast("Budget days can't be more than the expiry days"); return; }
        if (used >= tot) { toast("Already-used data must be less than the pack"); return; }
        sp.edit().putLong("total", (long) tot).putLong("pre", (long) used).putInt("uTotal", ut).putInt("uUsed", uu)
                .putInt("valid", valid).putInt("bud", bud).putLong("start", System.currentTimeMillis())
                .putLong("mBefore", 0).putLong("mToday", 0).putLong("mDay", Tracker.midnight(System.currentTimeMillis()))
                .putInt("nLvl", 0).putLong("nDay", 0).apply();
        Tracker.resetLedger(this); Tracker.sample(this); Tracker.startBg(this);
        toast("Plan saved"); refresh();
    }
    void fix(boolean undo) {
        if (sp.getLong("start", 0) == 0) { toast("Save a plan first"); return; }
        Tracker.snapshot(this); // makes sure earlier days' corrections are rolled over first
        if (undo) sp.edit().putLong("mBefore", 0).putLong("mToday", 0).apply();
        else {
            long bytes = (long) (val(fFix) * Tracker.UNIT[sFix.getSelectedItemPosition()]);
            if (bytes == 0) { toast("Enter an amount first"); return; }
            sp.edit().putLong("mToday", sp.getLong("mToday", 0) + bytes).apply();
        }
        fFix.setText(""); toast(undo ? "Corrections removed" : "Correction added to today's usage"); refresh();
    }

    GradientDrawable gradient(boolean over) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, over ? new int[]{T.overA, T.overB} : new int[]{T.heroA, T.heroB});
        g.setCornerRadius(dp(24)); return g;
    }
    void setBar(int w) { LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) barFill.getLayoutParams(); p.weight = w; barFill.setLayoutParams(p); }

    void refresh() {
        long fixTotal = sp.getLong("mBefore", 0) + sp.getLong("mToday", 0);
        fixInfo.setText("Corrections so far: " + (fixTotal >= 0 ? "+" : "") + f(fixTotal));
        Tracker.Snap s = Tracker.snapshot(this);
        grant.setVisibility(s != null && !s.live && !Tracker.hasAccess(this) ? View.VISIBLE : View.GONE);
        hero.setBackground(gradient(false));
        if (s == null) { hBig.setText("--"); hLabel.setText("LEFT TODAY"); hSub.setText("Set up your plan below to begin"); setBar(0); return; }
        Budget r = s.b; boolean over = r.leftToday < 0;
        hero.setBackground(gradient(over));
        hLabel.setText(over ? "OVER BUDGET TODAY" : "LEFT TODAY");
        hBig.setText(over ? f(-r.leftToday) : f(r.leftToday));
        hSub.setText("Used " + f(r.usedToday) + " of " + f(r.allowanceToday) + " today");
        setBar(r.allowanceToday > 0 ? (int) Math.min(1000, r.usedToday * 1000 / r.allowanceToday) : 1000);
        vDaily.setText(f(r.baseDaily));
        vCarry.setText((r.carryOver >= 0 ? "+" : "") + f(r.carryOver));
        vRemain.setText(f(r.remaining));
        vExp.setText(r.daysToExpiry + (r.daysToExpiry == 1 ? " day" : " days"));
        vDays.setText(r.daysLeft + " of " + s.bud);
        vWifi.setText(s.wifiToday < 0 ? "Tap to enable" : f(s.wifiToday));
        note.setText(r.daysLeft == 0 && r.daysToExpiry > 0 ? "Budget period over \u2013 the rest is free to use until it expires on " + DateFormat.getDateInstance().format(new Date(s.expiry)) + "."
                : "Expires " + DateFormat.getDateInstance().format(new Date(s.expiry)));
        note.setText(note.getText() + "\nSource: " + (s.live ? (s.paused ? "live mobile-data counter (waiting: mobile connection is off)" : "live mobile-data counter (exact, mobile only)") : "Android usage stats (can lag)"));
        Tracker.check(this, s);
    }
}
