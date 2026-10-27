package com.example.quietclock;

/** Local wall-clock minutes. Start is inclusive, end is exclusive. */
public final class NightSchedule {
    private NightSchedule() {}

    public static boolean contains(boolean enabled, int start, int end, int now) {
        return enabled && start != end && (start < end
            ? now >= start && now < end
            : now >= start || now < end);
    }
}
