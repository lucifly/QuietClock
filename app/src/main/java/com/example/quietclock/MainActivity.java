package com.example.quietclock;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private FlipClockView clock;
    private boolean animateNextTick;
    private SharedPreferences prefs;
    private long stopAt;
    private boolean resumed;
    private int foregroundColor;
    private boolean nightActive;
    private boolean timeReceiverRegistered;
    private int appliedInk = 0;
    private float appliedBrightness = -1f;
    private final List<Dialog> openDialogs = new ArrayList<>();
    private final Map<Dialog, TextView> timerLabels = new LinkedHashMap<>();
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refreshClock(); }
    };
    private final Runnable timerLabelTick = new Runnable() {
        @Override public void run() { updateTimerLabels(); }
    };
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!resumed) return;
            if (stopAt > 0 && SystemClock.elapsedRealtime() >= stopAt) { finish(); return; }
            updateClock();
            long delay = 60000 - System.currentTimeMillis() % 60000;
            if (stopAt > 0) delay = Math.min(delay, Math.max(1, stopAt - SystemClock.elapsedRealtime()));
            handler.postDelayed(this, delay);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("clock", MODE_PRIVATE);
        migrateNightBrightness();
        applyOrientation();
        stopAt = state == null ? 0 : state.getLong("stopAt", 0);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        clock = new FlipClockView(this);
        root.addView(clock, new FrameLayout.LayoutParams(-1, -1));
        root.setOnClickListener(v -> settings());
        root.setOnLongClickListener(v -> { settings(); return true; });
        setContentView(root);
        applyPreferences();
        if (!prefs.getBoolean("introduced", false)) {
            showClockDialog(new AlertDialog.Builder(this).setTitle(R.string.app_name)
                .setMessage(R.string.welcome_message)
                .setPositiveButton(R.string.start_clock, (d, w) -> prefs.edit().putBoolean("introduced", true).apply()).create());
        }
    }
    private TextView label(int size) {
        TextView view = new TextView(this);
        view.setTextSize(size); view.setGravity(Gravity.CENTER);
        view.setPadding(8, 6, 8, 6);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        return view;
    }
    private void applyOrientation() {
        int requested = prefs.getBoolean("rotate180", false)
            ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        if (getRequestedOrientation() != requested) setRequestedOrientation(requested);
    }
    private void applyPreferences() {
        Calendar now = Calendar.getInstance();
        applyDisplayPolicy(now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE));
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    private void applyDisplayPolicy(int minuteOfDay) {
        nightActive = NightSchedule.contains(prefs.getBoolean("nightEnabled", true),
            prefs.getInt("nightStart", 23 * 60), prefs.getInt("nightEnd", 7 * 60), minuteOfDay);
        int[] colors = { 0xff8a8a8a, 0xffb5a087, 0xffa8b7a6 };
        foregroundColor = colors[Math.max(0, Math.min(2, prefs.getInt("color", 0)))];
        int ink = nightActive && prefs.getInt("nightMode", 0) == 1 ? 0xff555555 : foregroundColor;
        if (ink != appliedInk) { clock.setInk(ink); appliedInk = ink; }
        // Night brightness defaults to 4%; the user can still choose the device minimum.
        float brightness = nightActive ? nightBrightnessPercent() / 100f : Math.max(.01f, Math.min(.3f, prefs.getInt("brightness", 5) / 100f));
        if (brightness != appliedBrightness) {
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.screenBrightness = brightness;
            getWindow().setAttributes(attrs);
            appliedBrightness = brightness;
            for (Dialog dialog : openDialogs) applyDialogBrightness(dialog, brightness);
        }
    }
    private void migrateNightBrightness() {
        if (prefs.getBoolean("nightBrightness4Applied", false)) return;
        SharedPreferences.Editor edit = prefs.edit().putBoolean("nightBrightness4Applied", true);
        if (prefs.contains("nightBrightness") && prefs.getInt("nightBrightness", 4) == 2)
            edit.putInt("nightBrightness", 4);
        edit.apply();
    }
    private int nightBrightnessPercent() {
        return Math.max(0, Math.min(10, prefs.getInt("nightBrightness", 4)));
    }
    private String nightBrightnessLabel() {
        int value = nightBrightnessPercent();
        return value == 0 ? getString(R.string.night_brightness_minimum)
            : getString(R.string.night_brightness_percent, value);
    }
    private void refreshClock() {
        animateNextTick = false;
        applyPreferences();
        handler.removeCallbacks(tick);
        if (resumed) handler.post(tick);
    }

    private void updateClock() {
        Date now = new Date();
        Calendar localTime = Calendar.getInstance();
        localTime.setTime(now);
        boolean wasNight = nightActive;
        applyDisplayPolicy(localTime.get(Calendar.HOUR_OF_DAY) * 60 + localTime.get(Calendar.MINUTE));
        boolean use24 = android.text.format.DateFormat.is24HourFormat(this);
        Locale locale = displayLocale();
        String hours = new SimpleDateFormat(use24 ? "HH" : "hh", locale).format(now);
        String minutes = new SimpleDateFormat("mm", locale).format(now);
        String date = new SimpleDateFormat(getString(R.string.date_pattern), locale).format(now);
        if (!use24) date += " · " + new SimpleDateFormat("a", locale).format(now);
        Intent status = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        String info = "";
        if (status != null) {
            int level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            boolean charging = status.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            if (level >= 0 && scale > 0) info = getString(charging ? R.string.charging_status : R.string.battery_status,
                level * 100 / scale);
        }
        clock.update(hours, minutes, date + (info.isEmpty() ? "" : "    ·    " + info),
            localTime.get(Calendar.HOUR_OF_DAY) * 60 + localTime.get(Calendar.MINUTE), animateNextTick && !nightActive && !wasNight);
        animateNextTick = true;
        updateTimerLabels();
    }

    private void settings() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(40, 16, 40, 16);
        TextView brightness = new TextView(this);
        brightness.setText(getString(R.string.regular_brightness_percent, prefs.getInt("brightness", 5)));
        panel.addView(brightness);
        SeekBar slider = new SeekBar(this); slider.setMax(29);
        slider.setProgress(Math.max(0, prefs.getInt("brightness", 5) - 1));
        slider.setContentDescription(getString(R.string.regular_brightness_accessibility));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (!user) return;
                prefs.edit().putInt("brightness", value + 1).apply();
                brightness.setText(getString(R.string.regular_brightness_percent, value + 1)); applyPreferences();
            }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {}
        });
        panel.addView(slider);
        TextView color = label(16); color.setText(R.string.color_switch); panel.addView(color);
        color.setOnClickListener(v -> {
            prefs.edit().putInt("color", (prefs.getInt("color", 0) + 1) % 3).apply(); applyPreferences();
            color.setTextColor(foregroundColor);
        });
        TextView timer = label(16);
        timer.setText(timerStatus()); panel.addView(timer);
        timer.setOnClickListener(v -> showClockDialog(new AlertDialog.Builder(this)
            .setTitle(R.string.timer_title)
            .setItems(new String[]{getString(stopAt > 0 ? R.string.timer_cancel : R.string.timer_none),
                getString(R.string.timer_30), getString(R.string.timer_60), getString(R.string.timer_120)}, (d, which) -> {
                int[] minutes = {0, 30, 60, 120};
                stopAt = which == 0 ? 0 : SystemClock.elapsedRealtime() + minutes[which] * 60000L;
                updateTimerLabels();
                handler.removeCallbacks(tick); if (resumed) handler.post(tick);
            }).create()));
        Switch rotate = new Switch(this);
        rotate.setText(R.string.rotate_180);
        rotate.setChecked(prefs.getBoolean("rotate180", false)); panel.addView(rotate);
        rotate.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("rotate180", checked).apply();
            applyOrientation();
        });
        Switch night = new Switch(this);
        night.setText(R.string.night_enabled);
        night.setChecked(prefs.getBoolean("nightEnabled", true)); panel.addView(night);
        night.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("nightEnabled", checked).apply(); refreshClock();
        });
        TextView nightBrightness = new TextView(this);
        nightBrightness.setText(nightBrightnessLabel()); panel.addView(nightBrightness);
        SeekBar nightSlider = new SeekBar(this); nightSlider.setMax(10);
        nightSlider.setProgress(nightBrightnessPercent());
        nightSlider.setContentDescription(getString(R.string.night_brightness_accessibility));
        nightSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (!user) return;
                prefs.edit().putInt("nightBrightness", value).apply();
                nightBrightness.setText(nightBrightnessLabel()); applyPreferences();
            }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {}
        });
        panel.addView(nightSlider);
        TextView start = label(16), end = label(16), mode = label(16);
        start.setText(getString(R.string.night_start, formatMinute(prefs.getInt("nightStart", 23 * 60))));
        end.setText(getString(R.string.night_end, formatMinute(prefs.getInt("nightEnd", 7 * 60))));
        start.setOnClickListener(v -> pickNightTime(true, start));
        end.setOnClickListener(v -> pickNightTime(false, end));
        panel.addView(start); panel.addView(end);
        String[] modes = {getString(R.string.night_mode_dim), getString(R.string.night_mode_save)};
        String[] explanations = {getString(R.string.night_dim_explanation), getString(R.string.night_save_explanation)};
        TextView modeHint = label(13);
        modeHint.setText(explanations[Math.max(0, Math.min(1, prefs.getInt("nightMode", 0)))]);
        mode.setText(modes[Math.max(0, Math.min(1, prefs.getInt("nightMode", 0)))]);
        mode.setOnClickListener(v -> showClockDialog(new AlertDialog.Builder(this).setTitle(R.string.night_mode_title)
            .setSingleChoiceItems(modes, prefs.getInt("nightMode", 0), (dialog, selected) -> {
                prefs.edit().putInt("nightMode", selected).apply(); mode.setText(modes[selected]); modeHint.setText(explanations[selected]);
                refreshClock(); dialog.dismiss();
            }).setNegativeButton(R.string.cancel, null).create()));
        panel.addView(mode); panel.addView(modeHint);
        TextView hint = label(13);
        hint.setText(R.string.night_hint);
        panel.addView(hint);
        ScrollView scroll = new ScrollView(this); scroll.addView(panel);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.settings_title).setView(scroll)
            .setPositiveButton(R.string.back_to_clock, (d, w) -> refreshClock())
            .setNegativeButton(R.string.exit_clock, (d, w) -> finish()).create();
        timerLabels.put(dialog, timer);
        showClockDialog(dialog);
        updateTimerLabels();
    }
    private String timerStatus() {
        if (stopAt <= 0) return getString(R.string.timer_off);
        long remaining = Math.max(0, stopAt - SystemClock.elapsedRealtime());
        String end = android.text.format.DateFormat.getTimeFormat(this).format(new Date(System.currentTimeMillis() + remaining));
        if (remaining < 60000) return getString(R.string.timer_under_minute, end);
        long minutes = (remaining + 59999) / 60000;
        return getString(R.string.timer_remaining, minutes, end);
    }
    private void updateTimerLabels() {
        handler.removeCallbacks(timerLabelTick);
        if (timerLabels.isEmpty()) return;
        String status = timerStatus();
        for (TextView label : timerLabels.values()) label.setText(status);
        long remaining = stopAt - SystemClock.elapsedRealtime();
        if (resumed && stopAt > 0 && remaining > 0) {
            long delay = remaining % 60000;
            if (delay == 0) delay = 60000;
            // Only a visible settings panel needs countdown-boundary refreshes.
            handler.postDelayed(timerLabelTick, Math.min(delay, remaining));
        }
    }
    private void applyDialogBrightness(Dialog dialog, float brightness) {
        if (dialog.getWindow() == null) return;
        WindowManager.LayoutParams attrs = dialog.getWindow().getAttributes();
        attrs.screenBrightness = brightness;
        attrs.buttonBrightness = brightness;
        dialog.getWindow().setAttributes(attrs);
    }
    private void showClockDialog(Dialog dialog) {
        // Set the brightness before the first frame; every nested dialog uses the same policy.
        applyPreferences();
        applyDialogBrightness(dialog, appliedBrightness);
        openDialogs.add(dialog);
        dialog.setOnDismissListener(d -> { openDialogs.remove(dialog); timerLabels.remove(dialog); updateTimerLabels(); });
        dialog.show();
    }

    private Locale displayLocale() {
        // Use the selected resource language so fallback locales do not mix languages in the clock.
        return Locale.forLanguageTag(getString(R.string.formatting_locale));
    }
    private String formatMinute(int minute) {
        return String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60);
    }
    private void pickNightTime(boolean isStart, TextView label) {
        String key = isStart ? "nightStart" : "nightEnd";
        int value = prefs.getInt(key, isStart ? 23 * 60 : 7 * 60);
        showClockDialog(new TimePickerDialog(this, (picker, hour, minute) -> {
            int chosen = hour * 60 + minute;
            int other = prefs.getInt(isStart ? "nightEnd" : "nightStart", isStart ? 7 * 60 : 23 * 60);
            if (chosen == other) {
                Toast.makeText(this, R.string.night_times_must_differ, Toast.LENGTH_SHORT).show(); return;
            }
            prefs.edit().putInt(key, chosen).apply();
            label.setText(getString(isStart ? R.string.night_start : R.string.night_end, formatMinute(chosen)));
            refreshClock();
        }, value / 60, value % 60, true));
    }

    @Override protected void onResume() {
        super.onResume(); resumed = true; animateNextTick = false;
        IntentFilter times = new IntentFilter();
        times.addAction(Intent.ACTION_TIME_CHANGED); times.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(timeReceiver, times, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(timeReceiver, times);
        timeReceiverRegistered = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        applyPreferences(); handler.removeCallbacks(tick); handler.post(tick);
    }
    @Override protected void onPause() {
        resumed = false; handler.removeCallbacks(tick); handler.removeCallbacks(timerLabelTick); clock.stopAnimation();
        if (timeReceiverRegistered) { unregisterReceiver(timeReceiver); timeReceiverRegistered = false; }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }
    @Override protected void onDestroy() {
        for (Dialog dialog : new ArrayList<>(openDialogs)) {
            dialog.setOnDismissListener(null);
            dialog.dismiss();
        }
        openDialogs.clear(); timerLabels.clear();
        super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putLong("stopAt", stopAt); super.onSaveInstanceState(out);
    }
}
