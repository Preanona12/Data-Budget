package com.databudget;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** Menu > Changelog: what changed in every version, newest first. */
public class ChangelogActivity extends BaseActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        ScrollView sv = new ScrollView(this); sv.setId(9100); sv.setBackgroundColor(T.bg);
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(16), dp(20), dp(16), dp(28)); sv.addView(l);

        LinearLayout head = new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = new TextView(this); back.setText("\u2039"); back.setTextSize(34); back.setTextColor(T.ink); back.setGravity(Gravity.CENTER);
        back.setBackground(rounded(T.card, 22)); back.setElevation(dp(1)); back.setContentDescription("Back");
        back.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { finish(); } });
        head.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView title = text(head, "Changelog", 26, true, T.ink); title.setPadding(dp(14), 0, 0, 0);
        l.addView(head, lp(-1, -2, 0, 0, 0, 14));

        String installed = installedVersion();
        for (Changelog.Entry e : Changelog.ALL) {
            LinearLayout c = card(l);
            LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
            text(top, "v" + e.version, 20, true, T.ink);
            if (e.version.equals(installed)) {
                TextView tag = text(top, "You're on this", 11, true, T.onSoft); tag.setBackground(rounded(T.soft, 10));
                tag.setPadding(dp(8), dp(3), dp(8), dp(3)); tag.setLayoutParams(lp(-2, -2, 10, 0, 0, 0));
            }
            c.addView(top);
            if (e.title.length() > 0) { TextView t = text(c, e.title, 13, false, T.gray); t.setPadding(0, dp(2), 0, 0); }
            section(c, "New", e.added);
            section(c, "Fixes", e.fixed);
        }
        setContentView(sv);
    }
    String installedVersion() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception x) { return ""; }
    }
    void section(LinearLayout c, String label, String[] items) {
        if (items.length == 0) return;
        TextView h = text(c, label.toUpperCase(), 11, true, T.accent); h.setPadding(0, dp(12), 0, dp(2));
        for (String it : items) {
            LinearLayout r = new LinearLayout(this);
            TextView dot = text(r, "\u2022", 14, false, T.gray); dot.setLayoutParams(new LinearLayout.LayoutParams(dp(16), -2));
            TextView t = text(r, it, 14, false, T.ink); t.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            c.addView(r, lp(-1, -2, 0, 4, 0, 0));
        }
    }
}
