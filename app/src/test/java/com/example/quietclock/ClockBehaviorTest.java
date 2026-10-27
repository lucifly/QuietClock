package com.example.quietclock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Switch;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Calendar;
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
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, qualifiers="zh-rCN-land")
@LooperMode(LooperMode.Mode.PAUSED)
public class ClockBehaviorTest {
    ActivityController<MainActivity> controller;
    MainActivity activity;
    SharedPreferences prefs;
    @Before public void start() {
        prefs=RuntimeEnvironment.getApplication().getSharedPreferences("clock",Context.MODE_PRIVATE);
        prefs.edit().clear().putBoolean("introduced",true).putBoolean("nightEnabled",false).apply();
        controller=Robolectric.buildActivity(MainActivity.class).setup().visible();
        activity=controller.get();
        shadowOf(Looper.getMainLooper()).idle();
    }
    @After public void close() {
        if(controller!=null) controller.pause().stop().destroy();
    }
    View root() {return ((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);}
    AlertDialog settings() {root().performClick();return (AlertDialog)ShadowDialog.getLatestDialog();}
    View find(View view,String prefix) {
        if(view instanceof TextView && ((TextView)view).getText().toString().startsWith(prefix))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            View match=find(((ViewGroup)view).getChildAt(i),prefix);if(match!=null)return match;
        }
        return null;
    }
    long deadline() throws Exception {
        Field field=MainActivity.class.getDeclaredField("stopAt");field.setAccessible(true);return field.getLong(activity);
    }
    void recreate() {
        Bundle state=new Bundle();
        controller.pause().saveInstanceState(state).stop().destroy();
        controller=Robolectric.buildActivity(MainActivity.class).create(state).start().restoreInstanceState(state).resume().visible();
        activity=controller.get();shadowOf(Looper.getMainLooper()).idle();
    }
    @Test public void startupAndPauseResume() {
        assertTrue((activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)!=0);
        FlipClockView clock=(FlipClockView)((ViewGroup)root()).getChildAt(0);
        assertTrue(clock.getContentDescription().toString().contains(":"));
        assertNotNull(settings());
        controller.pause();
        assertEquals(0,activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        controller.resume();shadowOf(Looper.getMainLooper()).idle();
        assertTrue((activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)!=0);
    }
    @Test public void allDialogsFollowNightBrightness() {
        Calendar now=Calendar.getInstance();int minute=now.get(Calendar.HOUR_OF_DAY)*60+now.get(Calendar.MINUTE);
        prefs.edit().putBoolean("nightEnabled",true).putInt("nightStart",minute).putInt("nightEnd",(minute+600)%1440).apply();
        AlertDialog parent=settings();
        assertEquals(.04f,activity.getWindow().getAttributes().screenBrightness,.0001f);
        assertEquals(.04f,parent.getWindow().getAttributes().screenBrightness,.0001f);
        View start=find(parent.getWindow().getDecorView(),"开始时间：");assertNotNull(start);start.performClick();
        Dialog child=ShadowDialog.getLatestDialog();assertNotSame(parent,child);
        assertEquals(.04f,child.getWindow().getAttributes().screenBrightness,.0001f);
        prefs.edit().putBoolean("nightEnabled",false).apply();
        activity.sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED));shadowOf(Looper.getMainLooper()).idle();
        assertEquals(.05f,activity.getWindow().getAttributes().screenBrightness,.0001f);
        assertEquals(.05f,parent.getWindow().getAttributes().screenBrightness,.0001f);
        assertEquals(.05f,child.getWindow().getAttributes().screenBrightness,.0001f);
    }
    private View findDescription(View view,String text) {
        if(text.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            View match=findDescription(((ViewGroup)view).getChildAt(i),text);if(match!=null)return match;
        }
        return null;
    }
    @Test public void nightBrightnessIsAdjustableAndRemembered() {
        Calendar now=Calendar.getInstance();int minute=now.get(Calendar.HOUR_OF_DAY)*60+now.get(Calendar.MINUTE);
        prefs.edit().putBoolean("nightEnabled",true).putInt("nightStart",minute).putInt("nightEnd",(minute+600)%1440).apply();
        AlertDialog parent=settings();
        android.widget.SeekBar slider=(android.widget.SeekBar)findDescription(parent.getWindow().getDecorView(),
            activity.getString(R.string.night_brightness_accessibility));
        assertNotNull(slider);assertEquals(4,slider.getProgress());
        Bundle value=new Bundle();value.putFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,6f);
        assertTrue(slider.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId(),value));
        assertEquals(6,prefs.getInt("nightBrightness",-1));
        assertEquals(.06f,activity.getWindow().getAttributes().screenBrightness,.0001f);
        assertEquals(.06f,parent.getWindow().getAttributes().screenBrightness,.0001f);
        value.putFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,0f);
        assertTrue(slider.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId(),value));
        assertEquals(0f,activity.getWindow().getAttributes().screenBrightness,0f);
        recreate();assertEquals(0,prefs.getInt("nightBrightness",-1));
        assertEquals(0f,activity.getWindow().getAttributes().screenBrightness,0f);
    }
    @Test public void oldNightBrightnessIsRaisedOnlyOnce() {
        prefs.edit().putInt("nightBrightness",2).remove("nightBrightness4Applied").apply();
        recreate();assertEquals(4,prefs.getInt("nightBrightness",-1));
        prefs.edit().putInt("nightBrightness",2).apply();
        recreate();assertEquals(2,prefs.getInt("nightBrightness",-1));
        prefs.edit().putInt("nightBrightness",0).remove("nightBrightness4Applied").apply();
        recreate();assertEquals(0,prefs.getInt("nightBrightness",-1));
    }
    @Test public void timerSurvivesActivityRecreationAndFinishes() throws Exception {
        AlertDialog parent=settings();find(parent.getWindow().getDecorView(),"自动结束：").performClick();
        AlertDialog timer=(AlertDialog)ShadowDialog.getLatestDialog();
        timer.getListView().performItemClick(null,1,1);
        long expected=deadline();assertTrue(expected>0);
        recreate();assertEquals(expected,deadline());
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(30));
        assertTrue(activity.isFinishing());
    }
    @Test public void rotationIsRemembered() {
        AlertDialog parent=settings();
        Switch rotate=(Switch)find(parent.getWindow().getDecorView(),"旋转180");assertNotNull(rotate);
        rotate.setChecked(true);
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,activity.getRequestedOrientation());
        recreate();
        assertTrue(prefs.getBoolean("rotate180",false));
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,activity.getRequestedOrientation());
    }
    @Test public void timerStatusPersistsAcrossReopenAndUpdates() {
        AlertDialog parent=settings();
        find(parent.getWindow().getDecorView(),"自动结束：").performClick();
        ((AlertDialog)ShadowDialog.getLatestDialog()).getListView().performItemClick(null,1,1);
        assertTrue(((TextView)find(parent.getWindow().getDecorView(),"自动结束：")).getText().toString().contains("30"));
        parent.dismiss();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(7));
        parent=settings();
        TextView status=(TextView)find(parent.getWindow().getDecorView(),"自动结束：");
        assertTrue(status.getText().toString(),status.getText().toString().contains("23"));
        assertTrue(status.getText().toString().contains("预计"));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(1));
        assertTrue(status.getText().toString(),status.getText().toString().contains("22"));
        status.performClick();
        AlertDialog timer=(AlertDialog)ShadowDialog.getLatestDialog();
        assertEquals("取消自动结束",timer.getListView().getAdapter().getItem(0));
        timer.getListView().performItemClick(null,0,0);
        assertEquals("自动结束：未开启",status.getText().toString());
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(31));
        assertFalse(activity.isFinishing());
    }
    @Test public void timerStatusSurvivesRotation() throws Exception {
        AlertDialog parent=settings();
        find(parent.getWindow().getDecorView(),"自动结束：").performClick();
        ((AlertDialog)ShadowDialog.getLatestDialog()).getListView().performItemClick(null,2,2);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(10));
        long expected=deadline();recreate();assertEquals(expected,deadline());
        TextView label=(TextView)find(settings().getWindow().getDecorView(),"自动结束：");
        assertTrue(label.getText().toString(),label.getText().toString().contains("50"));
    }
    @Test public void timerShowsFinalPartialMinute() {
        AlertDialog parent=settings();
        find(parent.getWindow().getDecorView(),"自动结束：").performClick();
        ((AlertDialog)ShadowDialog.getLatestDialog()).getListView().performItemClick(null,1,1);
        parent.dismiss();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(29).plusSeconds(30));
        TextView label=(TextView)find(settings().getWindow().getDecorView(),"自动结束：");
        assertTrue(label.getText().toString(),label.getText().toString().contains("不足1分钟"));
    }
    @Test public void layoutIsReusedAcrossFramesAndStyleChanges() throws Exception {
        FlipClockView clock=(FlipClockView)((ViewGroup)root()).getChildAt(0);
        clock.layout(0,0,2412,1080);
        clock.update("12","34","10月27日 星期二 · 电量78%",754,false);
        Field cache=FlipClockView.class.getDeclaredField("cachedLayout");cache.setAccessible(true);
        Object layout=cache.get(clock);assertNotNull(layout);
        android.graphics.Canvas canvas=new android.graphics.Canvas();
        for(int i=0;i<40;i++)clock.draw(canvas);
        assertSame(layout,cache.get(clock));
        clock.setInk(0xff555555);clock.draw(canvas);
        assertSame(layout,cache.get(clock));
        clock.update("12","35","10月27日 星期二 · 电量78%",755,false);
        assertNotSame(layout,cache.get(clock));layout=cache.get(clock);
        clock.layout(0,0,1080,480);assertNotSame(layout,cache.get(clock));layout=cache.get(clock);
        clock.update("12","35","10月27日 星期二 · 充电中 79%",755,false);
        assertNotSame(layout,cache.get(clock));
    }
    @Test public void applicationHasAdaptiveIcon() throws Exception {
        assertEquals(R.mipmap.ic_launcher,activity.getApplicationInfo().icon);
        android.graphics.drawable.Drawable icon=activity.getDrawable(activity.getApplicationInfo().icon);
        assertTrue(icon.getClass().getName(),icon instanceof android.graphics.drawable.AdaptiveIconDrawable);
        assertNotNull(((android.graphics.drawable.AdaptiveIconDrawable)icon).getMonochrome());
    }
    @Test public void firstRunIntro() {
        controller.pause().stop().destroy();
        prefs.edit().clear().commit();
        controller=Robolectric.buildActivity(MainActivity.class).setup().visible();
        activity=controller.get();shadowOf(Looper.getMainLooper()).idle();
        AlertDialog intro=(AlertDialog)ShadowDialog.getLatestDialog();
        assertTrue(intro.isShowing());
        assertEquals(activity.getWindow().getAttributes().screenBrightness,intro.getWindow().getAttributes().screenBrightness,0f);
        intro.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(prefs.getBoolean("introduced",false));
        assertFalse(intro.isShowing());
    }
    @Test public void mainClockTouchOpensSettings() {
        root().dispatchTouchEvent(android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,100,100,0));
        root().dispatchTouchEvent(android.view.MotionEvent.obtain(0,20,android.view.MotionEvent.ACTION_UP,100,100,0));
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(ShadowDialog.getLatestDialog().isShowing());
    }
}
