package org.gamedo.util;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.noop.*;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;

@SuppressWarnings("unused")
public final class Metric {

    public static final Meter.Id NOOP_ID = new Meter.Id("gamedo.noop",
            Tags.empty(),
            "",
            "noop id",
            Meter.Type.OTHER);
    public static final Timer NOOP_TIMER = new NoopTimer(NOOP_ID);
    public static final LongTaskTimer NOOP_LONG_TASK_TIMER = new NoopLongTaskTimer(NOOP_ID);
    public static final TimeGauge NOOP_TIMER_GAUGE = new NoopTimeGauge(NOOP_ID);
    public static final Gauge NOOP_GAUGE = new NoopGauge(NOOP_ID);
    public static final Counter NOOP_COUTER = new NoopCounter(NOOP_ID);
    public static final Meter NOOP_METER =  new NoopMeter(NOOP_ID);
    public static final DistributionSummary NOOP_DISTRIBUTION_SUMMARY = new NoopDistributionSummary(NOOP_ID);
    public static final FunctionCounter NOOP_FUNCTIONCOUNTER = new NoopFunctionCounter(NOOP_ID);
    public static final FunctionTimer NOOP_FUNCTION_TIMER = new NoopFunctionTimer(NOOP_ID);

    public static final String MetricNameEvent = "gamedo.gameloop.event";
    public static final String MetricNameCron = "gamedo.gameloop.cron";
    public static final String MetricNameTick = "gamedo.gameloop.tick";
    public static final String MetricNameEntity = "gamedo.gameloop.entity";

    private Metric() {
    }

    public static Tags tags(IGameLoop gameLoop) {
        final Tag tagName = Tag.of("name", gameLoop.getId());
        final Tag tagOwner = Tag.of("owner", gameLoop.owner().map(IGameLoopGroup::getId).orElse("null"));

        return Tags.of(tagName, tagOwner);
    }

}
