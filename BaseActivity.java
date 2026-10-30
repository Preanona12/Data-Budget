package com.databudget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.Calendar;

/** Shared look: picks the theme, styles the system bars, and builds themed widgets for both screens. */
public abstract class BaseActivity extends Activity {
    SharedPreferences sp;
    Theme T;
    String applied;

    static String resolvedId(Context c) {
        boolean night = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return Theme.resolve(Tracker.prefs(c).getString("theme", "light"), night, Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
    }

    @Override protected void onCreate(Bundle s) {
        sp = Tracker.prefs(this);
        applied = resolvedId(this); T = Theme.byId(applied);
        setTheme(T.dark ? android.R.style.Theme_Material_NoActionBar : android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(s);
        getWindow().setBackgroundDrawable(new ColorDrawable(T.bg));
        getWindow().setStatusBarColor(T.bg);
        getWindow().setNavigationBarColor(T.bg);
        int flags = 0;
        if (!T.dark) { flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; if (Build.VERSION.SDK_INT >= 26) flags |= 0x10; /* LIGHT_NAVIGATION_BAR */ }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }
    /** If the theme changed (picked in Settings, system dark mode flipped, or the time-of-day theme rolled over) restyle the screen. */
    @Override protected void onResume() { super.onResume(); checkTheme(); }
    boolean checkTheme() {
        if (applied.equals(resolvedId(this))) return false;
        recreate(); return true;
    }

    // ---------- UI helpers ----------
    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
    GradientDrawable rounded(int c, int r) { GradientDrawable g = new GradientDrawable(); g.setColor(c); g.setCornerRadius(dp(r)); return g; }
    LinearLayout.LayoutParams lp(int w, int hh, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, hh); p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p;
    }
    TextView text(LinearLayout p, String t, int size, boolean bold, int color) {
        TextView v = new TextView(this); v.setText(t); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        p.addView(v); return v;
    }
    LinearLayout card(LinearLayout p) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(18)); c.setBackground(rounded(T.card, 20)); c.setElevation(dp(2));
        p.addView(c, lp(-1, -2, 0, 0, 0, 12)); return c;
    }
    LinearLayout row(LinearLayout p) { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); p.addView(r, lp(-1, -2, -4, 0, -4, 0)); return r; }
    Button btn(LinearLayout p, String t, boolean primary, View.OnClickListener c) {
        Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setTextSize(15); b.setStateListAnimator(null);
        b.setTextColor(primary ? Color.WHITE : T.onSoft); b.setBackground(rounded(primary ? T.accent : T.soft, 14)); b.setOnClickListener(c);
        p.addView(b, lp(-1, dp(48), 0, 10, 0, 0)); return b;
    }
    Spinner spinner(String[] items, int sel) {
        ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items) {
            @Override public View getView(int pos, View v, ViewGroup g) { TextView t = (TextView) super.getView(pos, v, g); t.setTextColor(T.ink); return t; }
            @Override public View getDropDownView(int pos, View v, ViewGroup g) { TextView t = (TextView) super.getDropDownView(pos, v, g); t.setTextColor(T.ink); t.setBackgroundColor(T.card); return t; }
        };
        Spinner s = new Spinner(this); s.setAdapter(a); s.setSelection(sel);
        s.setPopupBackgroundDrawable(new ColorDrawable(T.card));
        if (s.getBackground() != null) s.getBackground().setColorFilter(T.gray, PorterDuff.Mode.SRC_ATOP);
        return s;
    }
    EditText input(LinearLayout p, String label, String def, boolean signed, Spinner unit, int id) {
        TextView l = text(p, label, 13, false, T.gray); l.setPadding(0, dp(12), 0, 0);
        LinearLayout r = new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL);
        EditText e = new EditText(this); e.setId(id); e.setText(def);   // fixed id so typed text survives a theme change
        e.setTextColor(T.ink); e.setHintTextColor(T.gray); e.setBackgroundTintList(ColorStateList.valueOf(T.accent));
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | (signed ? InputType.TYPE_NUMBER_FLAG_SIGNED : 0));
        r.addView(e, new LinearLayout.LayoutParams(0, -2, 1f)); if (unit != null) r.addView(unit);
        p.addView(r); return e;
    }
    Switch toggle(LinearLayout p, String label, boolean on, int top, CompoundButton.OnCheckedChangeListener l) {
        Switch sw = new Switch(this); sw.setText(label); sw.setChecked(on); sw.setPadding(0, dp(top), 0, dp(4)); sw.setTextColor(T.ink);
        int[][] st = {{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(st, new int[]{T.accent, T.gray}));
        sw.setTrackTintList(new ColorStateList(st, new int[]{(T.accent & 0xFFFFFF) | 0x66000000, (T.gray & 0xFFFFFF) | 0x55000000}));
        sw.setOnCheckedChangeListener(l); p.addView(sw); return sw;
    }
    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }

    /** Round button with three dots (drawn, so no font or icon file is needed). */
    final class DotsView extends View {
        final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        DotsView(Context c) { super(c); pt.setColor(T.ink); setBackground(rounded(T.card, 22)); setElevation(dp(1)); }
        @Override protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, r = dp(2), gap = dp(7);
            c.drawCircle(cx, cy - gap, r, pt); c.drawCircle(cx, cy, r, pt); c.drawCircle(cx, cy + gap, r, pt);
        }
    }
}
