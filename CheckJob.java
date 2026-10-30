package com.databudget;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** Runs about every 15 minutes in the background so alerts work even when the app is closed. */
public class CheckJob extends JobService {
    @Override public boolean onStartJob(final JobParameters p) {
        new Thread(new Runnable() { public void run() {
            try { Tracker.check(CheckJob.this, Tracker.snapshot(CheckJob.this)); } catch (Exception ignored) { }
            jobFinished(p, false);
        } }).start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters p) { return true; }
}
