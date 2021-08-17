package org.gamedo.util;

public final class GamedoConfiguration {
    public static final int MAX_EVENT_POST_DEPTH_DEFAULT = 20;
    public static final String MAX_EVENT_POST_DEPTH_KEY = "gamedo.gameloop.max-event-post-depth";

    public static final boolean METRIC_ENTITY_ENABLE_DEFAULT = true;
    public static final boolean METRIC_CRON_ENABLE_DEFAULT = true;
    public static final boolean METRIC_EVENT_ENABLE_DEFAULT = true;
    public static final boolean METRIC_TICK_ENABLE_DEFAULT = true;

    public static final String GAMEDO_METRIC_ENTITY_ENABLE_KEY = "gamedo.metric.entity.enable";
    public static final String GAMEDO_METRIC_CRON_ENABLE_KEY = "gamedo.metric.cron.enable";
    public static final String GAMEDO_METRIC_EVENT_ENABLE_KEY = "gamedo.metric.event.enable";
    public static final String GAMEDO_METRIC_TICK_ENABLE_KEY = "gamedo.metric.tick.enable";

    private GamedoConfiguration() {
    }

    public static int getMaxEventPostDepth() {
        return Integer.getInteger(MAX_EVENT_POST_DEPTH_KEY, MAX_EVENT_POST_DEPTH_DEFAULT);
    }

    public static boolean isMetricEntityEnable() {
        return Boolean.getBoolean(GAMEDO_METRIC_ENTITY_ENABLE_KEY);
    }

    public static boolean isMetricCronEnable() {
        return Boolean.getBoolean(GAMEDO_METRIC_CRON_ENABLE_KEY);
    }

    public static boolean isMetricEventEnable() {
        return Boolean.getBoolean(GAMEDO_METRIC_EVENT_ENABLE_KEY);
    }

    public static boolean isMetricTickEnable() {
        return Boolean.getBoolean(GAMEDO_METRIC_TICK_ENABLE_KEY);
    }
}
