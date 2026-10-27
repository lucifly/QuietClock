package com.example.quietclock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

/** Draws only when time/data change, or during the brief minute transition. */
public final class FlipClockView extends View {
    private static final class Digits {
        final String text, first, second;
        Digits(String value) {
            text = value;
            first = value.length() == 2 ? value.substring(0, 1) : value;
            second = value.length() == 2 ? value.substring(1, 2) : "";
        }
    }
    private final Paint shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint digitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics digitMetrics = new Paint.FontMetrics();
    private final Paint.FontMetrics detailMetrics = new Paint.FontMetrics();
    private Digits hours = new Digits(""), minutes = new Digits("");
    private Digits oldHours = hours, oldMinutes = minutes;
    private String details = "";
    private float progress = 1f;
    private long layoutMinute;
    private ClockLayout cachedLayout;
    private float detailBaseline, digitBaselineOffset, backgroundInset, radius, foldHeight;
    private ValueAnimator animation;

    public FlipClockView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        digitPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        detailPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        digitPaint.setTextAlign(Paint.Align.CENTER);
        detailPaint.setTextAlign(Paint.Align.CENTER);
        setInk(0xff8a8a8a);
    }
    public void setInk(int color) {
        digitPaint.setColor(color); detailPaint.setColor(color); invalidate();
    }
    public void update(String h, String m, String info, long minute, boolean animate) {
        boolean changed = !hours.text.equals(h) || !minutes.text.equals(m);
        boolean layoutChanged = cachedLayout == null || layoutMinute != minute || !details.equals(info);
        stopAnimation();
        oldHours = hours; oldMinutes = minutes;
        if (!hours.text.equals(h)) hours = new Digits(h);
        if (!minutes.text.equals(m)) minutes = new Digits(m);
        details = info; layoutMinute = minute;
        if (layoutChanged) rebuildLayout();
        setContentDescription(getResources().getString(R.string.clock_accessibility, h, m, info));
        if (animate && changed && !oldHours.text.isEmpty() && ValueAnimator.areAnimatorsEnabled()) {
            animation = ValueAnimator.ofFloat(0, 1);
            animation.setDuration(480);
            animation.addUpdateListener(a -> { progress = (float) a.getAnimatedValue(); invalidate(); });
            animation.start();
        }
        invalidate();
    }
    public void stopAnimation() {
        if (animation != null) { animation.cancel(); animation = null; }
        progress = 1f; invalidate();
    }
    @Override protected void onDetachedFromWindow() { stopAnimation(); super.onDetachedFromWindow(); }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh); rebuildLayout();
    }
    @Override protected void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config); rebuildLayout(); invalidate();
    }
    private void rebuildLayout() {
        if (getWidth() <= 0 || getHeight() <= 0) { cachedLayout = null; return; }
        float basis = ClockLayout.basis(getWidth(), getHeight());
        detailPaint.setTextSize(Math.max(11 * getResources().getDisplayMetrics().scaledDensity,
            basis * ClockLayout.DETAIL_SIZE));
        float textWidth = detailPaint.measureText(details);
        if (textWidth > getWidth() * .9f) {
            detailPaint.setTextSize(detailPaint.getTextSize() * getWidth() * .9f / textWidth);
            textWidth = detailPaint.measureText(details);
        }
        float detailHeight = detailPaint.getTextSize() * ClockLayout.DETAIL_LINE_HEIGHT;
        detailPaint.getFontMetrics(detailMetrics);
        cachedLayout = new ClockLayout(getWidth(), getHeight(), textWidth, detailHeight, layoutMinute);
        detailBaseline = cachedLayout.detailTop + detailHeight / 2 - (detailMetrics.ascent + detailMetrics.descent) / 2;
        digitPaint.setTextSize(basis * ClockLayout.DIGIT_SIZE);
        digitPaint.getFontMetrics(digitMetrics);
        digitBaselineOffset = cachedLayout.tileHeight / 2 - (digitMetrics.ascent + digitMetrics.descent) / 2;
        backgroundInset = basis * ClockLayout.BACKGROUND_INSET;
        radius = basis * ClockLayout.CORNER_RADIUS;
        foldHeight = Math.max(1f, basis * ClockLayout.FOLD_HEIGHT);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ClockLayout layout = cachedLayout;
        if (layout == null) return;
        canvas.save();
        canvas.translate(layout.shiftX, layout.shiftY);
        tile(canvas, layout.x, layout.y, layout.tileWidth, layout.tileHeight, oldHours, hours);
        tile(canvas, layout.x + layout.tileWidth, layout.y, layout.tileWidth, layout.tileHeight, oldMinutes, minutes);
        canvas.drawText(details, getWidth() / 2f, detailBaseline, detailPaint);
        canvas.restore();
    }
    private void tile(Canvas c, float x, float y, float w, float h, Digits old, Digits next) {
        if (old.text.equals(next.text) || progress >= 1 || old.text.isEmpty()) {
            page(c, x, y, w, h, next);
        } else {
            float mid = y + h / 2;
            half(c, x, y, w, h, next, true);
            half(c, x, y, w, h, old, false);
            c.save();
            float scale = (float) Math.cos(progress * Math.PI);
            c.scale(1, Math.abs(scale), x + w / 2, mid);
            half(c, x, y, w, h, progress < .5f ? old : next, progress < .5f);
            c.restore();
        }
        shapePaint.setColor(Color.BLACK);
        c.drawRect(x, y + h / 2 - foldHeight / 2, x + w, y + h / 2 + foldHeight / 2, shapePaint);
    }
    private void half(Canvas c, float x, float y, float w, float h, Digits digits, boolean top) {
        c.save();
        c.clipRect(x, top ? y : y + h / 2, x + w, top ? y + h / 2 : y + h);
        page(c, x, y, w, h, digits); c.restore();
    }
    private void page(Canvas c, float x, float y, float w, float h, Digits digits) {
        shapePaint.setColor(progress < 1f ? 0xff111111 : Color.BLACK);
        c.drawRoundRect(x + backgroundInset, y, x + w - backgroundInset, y + h, radius, radius, shapePaint);
        if (!digits.second.isEmpty()) {
            c.drawText(digits.first, x + w * .27f, y + digitBaselineOffset, digitPaint);
            c.drawText(digits.second, x + w * .73f, y + digitBaselineOffset, digitPaint);
        } else {
            c.drawText(digits.text, x + w / 2, y + digitBaselineOffset, digitPaint);
        }
    }
}
