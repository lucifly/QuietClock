package com.example.quietclock;

/** Proportions shared with the approved landscape preview. All distances use display width. */
public final class ClockLayout {
    public static final float TILE_WIDTH = .24f;
    public static final float TILE_HEIGHT = .21f;
    public static final float DIGIT_SIZE = .19f;
    public static final float DETAIL_SIZE = .021f;
    public static final float DETAIL_LINE_HEIGHT = 1.4f;
    public static final float DETAIL_GAP = .03f;
    public static final float BACKGROUND_INSET = .008f;
    public static final float CORNER_RADIUS = .014f;
    public static final float FOLD_HEIGHT = .0055f;
    public static final float SAFE_X = .02f;
    public static final float SAFE_Y = .015f;
    public static final float MICRO_STEP = .001f;
    public static final float LARGE_X = .16f;
    public static final float LARGE_Y = .08f;
    private static final int[][] POSITIONS = {{0, 0}, {-1, -1}, {1, 1}, {-1, 1}, {1, -1}};

    public final float basis, tileWidth, tileHeight, x, y, shiftX, shiftY, detailTop;

    public static float basis(float width, float height) {
        return Math.min(width, height / (TILE_HEIGHT + DETAIL_GAP + DETAIL_SIZE * DETAIL_LINE_HEIGHT + 2 * SAFE_Y));
    }
    public ClockLayout(float width, float height, float detailWidth, float detailHeight, long minute) {
        basis = basis(width, height);
        tileWidth = basis * TILE_WIDTH;
        tileHeight = basis * TILE_HEIGHT;
        float contentWidth = Math.max(2 * tileWidth, detailWidth);
        float contentHeight = tileHeight + basis * DETAIL_GAP + detailHeight;
        x = (width - 2 * tileWidth) / 2;
        y = (height - contentHeight) / 2;
        detailTop = y + tileHeight + basis * DETAIL_GAP;
        int slot = (int) Math.floorMod(minute / 10, 5L);
        float desiredX = basis * (POSITIONS[slot][0] * LARGE_X + (minute % 7 - 3) * MICRO_STEP);
        float desiredY = basis * (POSITIONS[slot][1] * LARGE_Y + ((minute / 7) % 7 - 3) * MICRO_STEP);
        float maxX = Math.max(0, (width - contentWidth) / 2 - basis * SAFE_X);
        float maxY = Math.max(0, (height - contentHeight) / 2 - basis * SAFE_Y);
        shiftX = Math.max(-maxX, Math.min(maxX, desiredX));
        shiftY = Math.max(-maxY, Math.min(maxY, desiredY));
    }
}
