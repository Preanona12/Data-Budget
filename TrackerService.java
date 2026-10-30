package com.databudget;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;

/** Foreground service: samples the mobile counter at the interval chosen in Settings (default 15 s), refreshes the live notification and fires alerts. */
public class TrackerService extends Service {
    final Handler h = new Handler();
    boolean registered;
    final Runnable tick = new Runnable() { public void run() { update(); h.postDelayed(this, Tracker.intervalMs(TrackerService.this)); } };
    final BroadcastReceiver shutdown = new BroadcastReceiver() { public void onReceive(Context c, Intent i) { Tracker.sample(c); } };

    void update() {
        Tracker.Snap s = Tracker.snapshot(this, false);
        Notification n = Tracker.live(this, s);
        startForeground(2, n);
        Tracker.check(this, s);
    }
    @Override public int onStartCommand(Intent i, int flags, int id) {
        update();                                   // must call startForeground quickly
        if (!registered) { registerReceiver(shutdown, new IntentFilter(Intent.ACTION_SHUTDOWN)); registered = true; }
        h.removeCallbacks(tick); h.postDelayed(tick, Tracker.intervalMs(this));
        return START_STICKY;
    }
    @Override public void onDestroy() {
        h.removeCallbacks(tick);
        if (registered) { try { unregisterReceiver(shutdown); } catch (Exception ignored) { } }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i) { return null; }
}
