package com.example.quietclock;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.LocaleList;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.time.Duration;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, qualifiers="en-rUS-land")
@LooperMode(LooperMode.Mode.PAUSED)
public class LocalizationBehaviorTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private SharedPreferences prefs;

    @Before public void start() {
        prefs = RuntimeEnvironment.getApplication().getSharedPreferences("clock", Context.MODE_PRIVATE);
        prefs.edit().clear().putBoolean("introduced", true).putBoolean("nightEnabled", false).apply();
        Settings.System.putString(RuntimeEnvironment.getApplication().getContentResolver(), Settings.System.TIME_12_24, "24");
        battery(false);
        controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        activity = controller.get();
        shadowOf(Looper.getMainLooper()).idle();
    }
    @After public void close() {
        if (controller != null) controller.pause().stop().destroy();
    }
    private void battery(boolean charging) {
        RuntimeEnvironment.getApplication().sendStickyBroadcast(new Intent(Intent.ACTION_BATTERY_CHANGED)
            .putExtra(BatteryManager.EXTRA_LEVEL, 78).putExtra(BatteryManager.EXTRA_SCALE, 100)
            .putExtra(BatteryManager.EXTRA_PLUGGED, charging ? BatteryManager.BATTERY_PLUGGED_USB : 0));
    }
    private View root() { return ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0); }
    private FlipClockView clock() { return (FlipClockView) ((ViewGroup) root()).getChildAt(0); }
    private AlertDialog settings() { root().performClick(); return (AlertDialog) ShadowDialog.getLatestDialog(); }
    private TextView find(View view, String prefix) {
        if (view instanceof TextView && ((TextView) view).getText().toString().startsWith(prefix)) return (TextView) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            TextView found = find(((ViewGroup) view).getChildAt(i), prefix);
            if (found != null) return found;
        }
        return null;
    }
    private String texts(View view) {
        StringBuilder text = new StringBuilder();
        if (view instanceof TextView) text.append(((TextView) view).getText()).append('\n');
        if (view.getContentDescription() != null) text.append(view.getContentDescription()).append('\n');
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
            text.append(texts(((ViewGroup) view).getChildAt(i)));
        return text.toString();
    }
    private void changeLocales(Locale... locales) {
        Configuration config = new Configuration(activity.getResources().getConfiguration());
        config.setLocales(new LocaleList(locales));
        controller.configurationChange(config);
        activity = controller.get();
        shadowOf(Looper.getMainLooper()).idle();
    }
    private long deadline() throws Exception {
        Field field = MainActivity.class.getDeclaredField("stopAt");
        field.setAccessible(true); return field.getLong(activity);
    }
    private String englishDate() { return new SimpleDateFormat("EEE, MMM d", Locale.ENGLISH).format(new Date()); }

    @Test public void englishClockUsesEnglishDateBatteryAndAccessibility() {
        assertEquals("QuietClock", activity.getString(activity.getApplicationInfo().labelRes));
        String content = clock().getContentDescription().toString();
        assertTrue(content, content.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9],.*"));
        assertTrue(content, content.contains(englishDate()));
        assertTrue(content, content.contains("Battery · 78%"));
        assertTrue(content, content.endsWith("Tap to open settings"));
        assertFalse(content, content.matches("(?s).*[\\p{IsHan}].*"));
        battery(true);
        activity.sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED));
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(clock().getContentDescription().toString().contains("Charging · 78%"));
        Settings.System.putString(activity.getContentResolver(), Settings.System.TIME_12_24, "12");
        activity.sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED));
        shadowOf(Looper.getMainLooper()).idle();
        content = clock().getContentDescription().toString();
        assertTrue(content, content.matches("(?:0[1-9]|1[0-2]):[0-5][0-9],.*"));
        assertTrue(content, content.contains("AM") || content.contains("PM"));
    }

    @Test public void englishSettingsAndNestedDialogsAreTranslated() {
        AlertDialog parent = settings();
        String text = texts(parent.getWindow().getDecorView());
        assertTrue(text, text.contains("Clock settings"));
        assertTrue(text, text.contains("Regular brightness (outside night hours): 5%"));
        assertTrue(text, text.contains("Night brightness: 4%"));
        assertTrue(text, text.contains("Rotate 180°"));
        assertTrue(text, text.contains("Back to clock"));
        assertFalse(text, text.matches("(?s).*[\\p{IsHan}].*"));
        find(parent.getWindow().getDecorView(), "Start time:").performClick();
        TimePickerDialog picker = (TimePickerDialog) ShadowDialog.getLatestDialog();
        picker.updateTime(22, 30);
        picker.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        assertEquals(22 * 60 + 30, prefs.getInt("nightStart", -1));
        assertNotNull(find(parent.getWindow().getDecorView(), "Start time: 22:30"));
        find(parent.getWindow().getDecorView(), "Start time:").performClick();
        picker = (TimePickerDialog) ShadowDialog.getLatestDialog();
        picker.updateTime(7, 0);
        picker.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        assertEquals("Start and end times must be different", ShadowToast.getTextOfLatestToast());
        assertEquals(22 * 60 + 30, prefs.getInt("nightStart", -1));
        find(parent.getWindow().getDecorView(), "Dim display").performClick();
        AlertDialog mode = (AlertDialog) ShadowDialog.getLatestDialog();
        assertTrue(texts(mode.getWindow().getDecorView()).contains("Night display mode"));
        assertEquals("Darker static display", mode.getListView().getAdapter().getItem(1));
        mode.getListView().performItemClick(null, 1, 1);
        assertEquals(1, prefs.getInt("nightMode", -1));
    }

    @Test public void englishIntroAndCountdownAreTranslated() {
        controller.pause().stop().destroy();
        prefs.edit().putBoolean("introduced", false).apply();
        controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        activity = controller.get();
        AlertDialog intro = (AlertDialog) ShadowDialog.getLatestDialog();
        assertTrue(texts(intro.getWindow().getDecorView()).contains("Tap the screen to adjust brightness"));
        assertEquals("Start", intro.getButton(DialogInterface.BUTTON_POSITIVE).getText().toString());
        intro.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        AlertDialog parent = settings();
        find(parent.getWindow().getDecorView(), "Auto-stop:").performClick();
        AlertDialog timer = (AlertDialog) ShadowDialog.getLatestDialog();
        assertEquals("No timer", timer.getListView().getAdapter().getItem(0));
        assertEquals("In 30 minutes", timer.getListView().getAdapter().getItem(1));
        timer.getListView().performItemClick(null, 1, 1);
        assertNotNull(find(parent.getWindow().getDecorView(), "Auto-stop: 30 min remaining"));
        parent.dismiss();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(29).plusSeconds(30));
        parent = settings();
        assertNotNull(find(parent.getWindow().getDecorView(), "Auto-stop: less than 1 min remaining"));
        find(parent.getWindow().getDecorView(), "Auto-stop:").performClick();
        timer = (AlertDialog) ShadowDialog.getLatestDialog();
        assertEquals("Cancel auto-stop", timer.getListView().getAdapter().getItem(0));
        timer.getListView().performItemClick(null, 0, 0);
        assertNotNull(find(parent.getWindow().getDecorView(), "Auto-stop: off"));
    }

    @Test public void languageChangesPreservePreferencesAndActiveTimer() throws Exception {
        prefs.edit().putInt("nightBrightness", 6).putBoolean("rotate180", true).apply();
        AlertDialog parent = settings();
        find(parent.getWindow().getDecorView(), "Auto-stop:").performClick();
        ((AlertDialog) ShadowDialog.getLatestDialog()).getListView().performItemClick(null, 2, 2);
        long expected = deadline();
        changeLocales(Locale.SIMPLIFIED_CHINESE);
        assertFalse(parent.isShowing());
        assertEquals(expected, deadline());
        assertEquals("静夜时钟", activity.getString(R.string.app_name));
        assertEquals(6, prefs.getInt("nightBrightness", -1));
        assertTrue(prefs.getBoolean("rotate180", false));
        assertTrue(clock().getContentDescription().toString().contains("星期"));
        parent = settings();
        assertNotNull(find(parent.getWindow().getDecorView(), "自动结束：剩余 60 分钟"));
        changeLocales(Locale.US);
        assertFalse(parent.isShowing());
        assertEquals(expected, deadline());
        assertEquals("QuietClock", activity.getString(R.string.app_name));
        assertTrue(clock().getContentDescription().toString().contains(englishDate()));
        assertNotNull(find(settings().getWindow().getDecorView(), "Auto-stop: 60 min remaining"));
    }

    @Test @Config(qualifiers="fr-rFR-land") public void unsupportedLanguageFallsBackToEnglish() {
        assertEquals("QuietClock", activity.getString(R.string.app_name));
        assertTrue(clock().getContentDescription().toString().contains(englishDate()));
        assertNotNull(find(settings().getWindow().getDecorView(), "Clock settings"));
    }

    @Test public void languageListUsesMatchingChineseTranslation() {
        changeLocales(Locale.FRANCE, Locale.SIMPLIFIED_CHINESE);
        assertEquals("静夜时钟", activity.getString(R.string.app_name));
        assertTrue(clock().getContentDescription().toString().contains(
            new SimpleDateFormat("M月d日", Locale.SIMPLIFIED_CHINESE).format(new Date())));
        assertTrue(clock().getContentDescription().toString().contains("电量 · 78%"));
    }

    @Test public void englishTakesPriorityWhenChineseIsSecond() {
        changeLocales(Locale.US, Locale.SIMPLIFIED_CHINESE);
        assertEquals("QuietClock", activity.getString(R.string.app_name));
        assertTrue(clock().getContentDescription().toString().contains("Battery · 78%"));
    }
}
