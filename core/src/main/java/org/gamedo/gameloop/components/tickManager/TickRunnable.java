package org.gamedo.gameloop.components.tickManager;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.gamedo.annotation.Tick;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.logging.GamedoLogContext;
import org.gamedo.logging.Markers;
import org.gamedo.util.GamedoConfiguration;
import org.gamedo.util.Metric;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Data
@Log4j2
public class TickRunnable implements Runnable {
    final IGameLoop gameLoop;
    final ScheduleDataKey scheduleDataKey;
    final ScheduledFuture<?> future;
    private final List<TickData> tickDataList = new ArrayList<>(2);
    private final Map<TickData, TickData> tickDataMap = new HashMap<>(2);

    public TickRunnable(IGameLoop gameLoop, ScheduleDataKey scheduleDataKey) {
        this.gameLoop = gameLoop;
        this.scheduleDataKey = scheduleDataKey;
        final long delay = scheduleDataKey.getTick();
        final TimeUnit timeUnit = scheduleDataKey.getTimeUnit();
        future = scheduleDataKey.isScheduleWithFixedDelay() ?
                gameLoop.scheduleWithFixedDelay(this, 0, delay, timeUnit) :
                gameLoop.scheduleAtFixedRate(this, 0, delay, timeUnit) ;
    }

    @Override
    public void run() {
        new ArrayList<>(tickDataList).forEach(this::safeInvoke);
    }

    void addTickData(TickData tickData) {
        tickDataMap.put(tickData, tickData);
        tickDataList.add(tickData);
    }

    TickData removeTickData(TickData tickData) {
        tickDataList.remove(tickData);
        return tickDataMap.remove(tickData);
    }

    private void safeInvoke(TickData tickData) {
        final long firstTickMilliSecond = tickData.getFirstTickMilliSecond();
        final long currentTimeMillis = System.currentTimeMillis();

        if (firstTickMilliSecond > currentTimeMillis) {
            return;
        }

        final Object object = tickData.getObject();
        final Class<?> clazz = object.getClass();
        final Method method = tickData.getMethod();
        final Timer timer = gameLoop.getComponent(MeterRegistry.class)
                .map(meterRegistry -> GamedoConfiguration.isMetricTickEnable() ? meterRegistry : null)
                .map(meterRegistry -> {

                    final Tags tags = Metric.tags(gameLoop);
                    return Timer.builder(Metric.MeterIdTickTimer)
                            .tags(tags)
                            .tag("class", clazz.getName())
                            .tag("method", method.getName())
                            .tag("tick", scheduleDataKey.toTagString())
                            .description("the @" + Tick.class.getSimpleName() + " method timing")
                            .register(meterRegistry);
                })
                .orElse(Metric.NOOP_TIMER);

        timer.record(() -> {
            final long lastTickMilliSecond = tickData.getLastTickMilliSecond();
            try(final GamedoLogContext.CloseableEntityId ignored = GamedoLogContext.pushEntityIdAuto(object)) {
                method.invoke(object, currentTimeMillis, lastTickMilliSecond);
            }
            catch (Exception e) {
                log.error(Markers.GameLoopTickManager, "exception caught, clazz:" + clazz.getName() +
                        ", method:" + method.getName() +
                        ", tick:" + scheduleDataKey.getTick() +
                        ", timeUnit:" + scheduleDataKey.getTimeUnit() +
                        ", scheduleWithFixedDelay:" + scheduleDataKey.isScheduleWithFixedDelay(), e);

            } finally {
                tickData.setLastTickMilliSecond(currentTimeMillis);
            }
        });
    }

    @Override
    public String toString() {
        return "TickRunnable{" +
                "scheduleDataKey=" + scheduleDataKey +
                ", tickDataSize=" + tickDataList.size() + ',' + tickDataMap.size() +
                '}';
    }
}
