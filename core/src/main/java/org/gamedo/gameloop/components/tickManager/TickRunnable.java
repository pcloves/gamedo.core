package org.gamedo.gameloop.components.tickManager;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.logging.GamedoLogContext;
import org.gamedo.logging.Markers;

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
    final ScheduleDataKey scheduleDataKey;
    final ScheduledFuture<?> future;
    private final List<TickData> tickDataList = new ArrayList<>(2);
    private final Map<TickData, TickData> tickDataMap = new HashMap<>(2);

    public TickRunnable(IGameLoop gameLoop, ScheduleDataKey scheduleDataKey) {
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

        final Method method = tickData.getMethod();
        final Object object = tickData.getObject();
        final Class<?> clazz = object.getClass();
        final long lastTickMilliSecond = tickData.getLastTickMilliSecond();

        try(final GamedoLogContext.CloseableEntityId ignored = GamedoLogContext.pushEntityIdAuto(object)) {
            method.invoke(object, currentTimeMillis, lastTickMilliSecond);
        }
        catch (Throwable throwable) {
            log.error(Markers.GameLoopTickManager, "exception caught, clazz:" + clazz.getName() +
                    ", method:" + method.getName() +
                    ", tick:" + scheduleDataKey.getTick() +
                    ", timeUnit:" + scheduleDataKey.getTimeUnit() +
                    ", scheduleWithFixedDelay:" + scheduleDataKey.isScheduleWithFixedDelay(), throwable);

        } finally {
            tickData.setLastTickMilliSecond(currentTimeMillis);
        }
    }

    @Override
    public String toString() {
        return "TickRunnable{" +
                "scheduleDataKey=" + scheduleDataKey +
                ", tickDataSize=" + tickDataList.size() + ',' + tickDataMap.size() +
                '}';
    }
}
