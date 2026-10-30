package com.databudget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Calendar;

/** Menu > Settings: look (themes), live-tracker speed, alerts, background, units. */
public class SettingsActivity extends BaseActivity {
    ScrollView sv; LinearLayout appearance;
    Button battery; TextView secLabel, secHint;
    SeekBar slider; TextView diag;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        sv = new ScrollView(this); sv.setId(9100); sv.setBackgroundColor(T.bg);
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(16), dp(20), dp(16), dp(28)); sv.addView(l);

        LinearLayout head = new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = new TextView(this); back.setText("\u2039"); back.setTextSize(34); back.setTextColor(T.ink); back.setGravity(Gravity.CENTER);
        back.setBackground(rounded(T.card, 22)); back.setElevation(dp(1)); back.setContentDescription("Back");
        back.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { finish(); } });
        head.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView title = text(head, "Settings", 26, true, T.ink); title.setPadding(dp(14), 0, 0, 0);
        l.addView(head, lp(-1, -2, 0, 0, 0, 14));

        buildAppearance(l); buildTracker(l); buildGeneral(l); buildDiag(l);
        setContentView(sv);
        if (s == null && "appearance".equals(getIntent().getStringExtra("section")))
            sv.post(new Runnable() { public void run() { sv.smoothScrollTo(0, appearance.getTop()); } });
    }
    @Override protected void onResume() {
        super.onResume();
        showDiag();
        battery.setVisibility(((android.os.PowerManager) getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName()) ? View.GONE : View.VISIBLE);
    }

    // ---------- Appearance ----------
    void buildAppearance(LinearLayout l) {
        appearance = card(l);
        text(appearance, "Appearance", 18, true, T.ink);
        String cur = sp.getString("theme", "light");
        group("Basics", "", 0, 3, cur);
        group("Moods", "Pick a feeling", 3, 7, cur);
        int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        group("Time of day", "Auto switches by itself: Sunrise 5\u201310, Day 10\u201317, Sunset 17\u201320, Night 20\u20135."
                + ("auto".equals(cur) ? " Right now: " + Theme.byId(Theme.forHour(hr)).name + "." : ""), 7, 12, cur);
    }
    void group(String name, String caption, int from, int to, String cur) {
        TextView g = text(appearance, name, 13, true, T.gray); g.setPadding(0, dp(14), 0, dp(2));
        if (caption.length() > 0) { TextView c = text(appearance, caption, 12, false, T.gray); c.setPadding(0, 0, 0, dp(4)); }
        LinearLayout r = null;
        for (int i = from; i < to; i++) {
            if ((i - from) % 3 == 0) { r = new LinearLayout(this); appearance.addView(r, lp(-1, -2, -4, 0, -4, 0)); }
            r.addView(swatchTile(Theme.CHOICES[i][0], Theme.CHOICES[i][1], cur));
        }
        // keep the last row's tiles the same width as the others
        int extra = (3 - (to - from) % 3) % 3;
        for (int k = 0; k < extra; k++) { View pad = new View(this); r.addView(pad, new LinearLayout.LayoutParams(0, 1, 1f)); }
    }
    View swatchTile(final String id, String label, String cur) {
        boolean sel = id.equals(cur);
        LinearLayout t = new LinearLayout(this); t.setOrientation(LinearLayout.VERTICAL); t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable bg = rounded(T.bg, 14); if (sel) bg.setStroke(dp(2), T.accent); t.setBackground(bg);
        int[] c = Theme.swatch(id);
        GradientDrawable sw = new GradientDrawable(GradientDrawable.Orientation.TL_BR, c); sw.setCornerRadius(dp(10));
        if (T.dark) sw.setStroke(dp(1), 0x44FFFFFF); else sw.setStroke(dp(1), 0x22000000);
        View v = new View(this); v.setBackground(sw);
        t.addView(v, new LinearLayout.LayoutParams(-1, dp(40)));
        TextView n = text(t, (sel ? "\u2713 " : "") + label, 12, sel, sel ? T.accent : T.ink); n.setGravity(Gravity.CENTER); n.setPadding(0, dp(6), 0, 0); n.setSingleLine(true);
        t.setOnClickListener(new View.OnClickListener() { public void onClick(View x) { sp.edit().putString("theme", id).apply(); recreate(); } });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f); p.setMargins(dp(4), dp(4), dp(4), dp(4)); t.setLayoutParams(p);
        return t;
    }

    // ---------- Live tracker speed ----------
    void buildTracker(LinearLayout l) {
        LinearLayout c = card(l);
        text(c, "Live tracker", 18, true, T.ink);
        int sec = Tracker.intervalSec(this);
        secLabel = text(c, "", 15, true, T.ink); secLabel.setPadding(0, dp(12), 0, 0);
        slider = new SeekBar(this); slider.setMax(29); slider.setProgress(sec - 1);   // 0..29 shows as 1..30 seconds
        slider.setProgressTintList(ColorStateList.valueOf(T.accent)); slider.setThumbTintList(ColorStateList.valueOf(T.accent));
        slider.setProgressBackgroundTintList(ColorStateList.valueOf(T.gray));
        c.addView(slider, lp(-1, -2, 0, 4, 0, 0));
        LinearLayout ends = new LinearLayout(this);
        TextView a = text(ends, "1 s", 12, false, T.gray); a.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView b = text(ends, "30 s", 12, false, T.gray); b.setGravity(Gravity.END); b.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        c.addView(ends);
        secHint = text(c, "", 12, false, T.gray); secHint.setPadding(0, dp(8), 0, 0);
        showSeconds(sec);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) { int v = p + 1; sp.edit().putInt("interval", v).apply(); showSeconds(v); }
            public void onStartTrackingTouch(SeekBar s) { }
            public void onStopTrackingTouch(SeekBar s) { Tracker.startBg(SettingsActivity.this); } // restarts the background timer at the new speed
        });
        btn(c, "Reset to 15 s", false, new View.OnClickListener() { public void onClick(View v) { slider.setProgress(14); Tracker.startBg(SettingsActivity.this); } });
    }
    void showSeconds(int s) {
        secLabel.setText("Update every " + s + (s == 1 ? " second" : " seconds"));
        secHint.setText((s <= 5 ? "Very fast: freshest numbers, but noticeably more battery. " : s >= 20 ? "Gentle on battery. " : "Balanced. ")
                + "Applies to the live notification and this app's screen, right away.");
    }

    // ---------- Alerts, background, units ----------
    void buildGeneral(LinearLayout l) {
        LinearLayout set = card(l);
        text(set, "Alerts & background", 18, true, T.ink);
        toggle(set, "Alert me at 80% and when today's budget is used up", sp.getBoolean("notif", true), 12, new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) {
                sp.edit().putBoolean("notif", on).apply();
                if (on && android.os.Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 2); } });
        toggle(set, "Keep tracking in the background (small live notification)", sp.getBoolean("bg", true), 8, new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) {
                sp.edit().putBoolean("bg", on).apply(); if (on) Tracker.startBg(SettingsActivity.this); else Tracker.stopBg(SettingsActivity.this); } });
        battery = btn(set, "Stop Android from pausing the app (recommended)", false, new View.OnClickListener() { public void onClick(View v) {
            try { startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:" + getPackageName()))); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); } } });
        TextView dl = text(set, "Show amounts in", 13, false, T.gray); dl.setPadding(0, dp(12), 0, 0);
        Spinner sDisp = spinner(new String[]{"Auto (MB / GB)", "MB", "GB", "Mb", "Gb"}, sp.getInt("disp", 0));
        sDisp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> a, View v, int pos, long id) { sp.edit().putInt("disp", pos).apply(); }
            public void onNothingSelected(AdapterView<?> a) { } });
        set.addView(sDisp);
        text(set, "MB/GB = bytes, Mb/Gb = bits (8 Mb = 1 MB). 1 GB = 1024 MB.", 12, false, T.gray).setPadding(0, dp(8), 0, 0);
    }

    // ---------- Diagnostics: what the tracker saw, for when a number drifts ----------
    void buildDiag(LinearLayout l) {
        LinearLayout c = card(l);
        text(c, "Tracker diagnostics", 18, true, T.ink);
        text(c, "If your numbers ever drift from your phone's own data usage, this shows what the tracker saw. Copy it and send it over.", 12, false, T.gray).setPadding(0, dp(6), 0, dp(8));
        diag = text(c, "", 12, false, T.ink); diag.setTypeface(Typeface.MONOSPACE);
        diag.setBackground(rounded(T.bg, 12)); diag.setPadding(dp(12), dp(10), dp(12), dp(10));
        btn(c, "Copy log", false, new View.OnClickListener() { public void onClick(View v) {
            ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Data Budget log", diag.getText()));
            toast("Log copied"); } });
        btn(c, "Clear log", false, new View.OnClickListener() { public void onClick(View v) { sp.edit().putString("log", "").apply(); showDiag(); } });
    }
    void showDiag() {
        String log = sp.getString("log", "");
        String now = "Now: mobile counter " + (sp.getBoolean("unsup", false) ? "unavailable" : "OK") + " \u2022 active network " + Tracker.activeNet(this) + " \u2022 mobile network " + (Tracker.cellularUp(this) == 1 ? "up" : Tracker.cellularUp(this) == 0 ? "down" : "unknown")
                + "\nCounted today: " + Tracker.mb(sp.getLong("lToday", 0)) + "\n\n";
        diag.setText(now + (log.length() == 0 ? "No events yet \u2013 nothing unusual seen." : log));
    }
}
