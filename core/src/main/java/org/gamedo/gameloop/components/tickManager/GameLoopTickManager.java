package org.gamedo.gameloop.components.tickManager;

import lombok.extern.slf4j.Slf4j;
import org.gamedo.annotation.Tick;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.gameloop.components.tickManager.interfaces.IGameLoopTickManager;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.logging.Markers;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class GameLoopTickManager extends GameLoopComponent implements IGameLoopTickManager {

    private final Map<TickData, ScheduledFuture<?>> tickDataFutureMap = new HashMap<>(32);

    public GameLoopTickManager(IGameLoop owner) {
        super(owner);
    }

    @Override
    public int register(Object object) {

        final Class<?> clazz = object.getClass();
        final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz))
                .filter(method -> method.isAnnotationPresent(Tick.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        if (annotatedMethodSet.isEmpty()) {
            log.warn(Markers.GameLoopTickManager, "the Object has none annotated method, annotation:{}, clazz:{}",
                    Tick.class.getSimpleName(),
                    clazz.getName());
            return 0;
        }

        final int count = annotatedMethodSet.stream().mapToInt(method -> register(object, method) ? 1 : 0).sum();
        if (log.isDebugEnabled()) {
            log.debug(Markers.GameLoopTickManager, "register tick finish, clazz:{}, totalCount:{}, successCount:{}",
                    clazz.getSimpleName(),
                    annotatedMethodSet.size(),
                    count
            );
        }

        return count;
    }

    @Override
    public boolean register(Object object, Method method) {

        if (!method.isAnnotationPresent(Tick.class)) {
            log.error(Markers.GameLoopTickManager, "the method:{} of clazz:{} is not annotated by:{}",
                    method.getName(),
                    object.getClass().getName(),
                    Tick.class.getName());
            return false;
        }

        final Tick annotation = method.getAnnotation(Tick.class);
        return register(object,
                method,
                annotation.delay(),
                annotation.tick(),
                annotation.timeUnit(),
                annotation.scheduleWithFixedDelay());
    }

    @Override
    public boolean register(Object object,
                            Method method,
                            long delay,
                            long tick,
                            TimeUnit timeUnit,
                            boolean scheduleWithFixedDelay) {

        final Class<?> clazz = object.getClass();
        if (owner.isShutdown()) {
            log.warn(Markers.GameLoopTickManager, "the GameLoop has been shut down, register failed, " +
                            "clazz:{}, method:{}, delay:{}, tick:{}, timeUnit:{}, scheduleWithFixedDelay:{}",
                    clazz.getName(),
                    method.getName(),
                    delay,
                    tick,
                    timeUnit,
                    scheduleWithFixedDelay);
            return false;
        }

        if (delay < 0) {
            log.error(Markers.GameLoopTickManager, "invalid param:delay, register failed, clazz:{}, " +
                            "method:{}, delay:{}, tick:{}, timeUnit:{}, scheduleWithFixedDelay:{}",
                    clazz.getName(),
                    method.getName(),
                    delay,
                    tick,
                    timeUnit,
                    scheduleWithFixedDelay);
            return false;
        }

        if (tick < 0) {
            log.error(Markers.GameLoopTickManager, "invalid param:tick, register failed, clazz:{}, " +
                            "method:{}, delay:{}, tick:{}, timeUnit:{}, scheduleWithFixedDelay:{}",
                    clazz.getName(),
                    method.getName(),
                    delay,
                    tick,
                    timeUnit,
                    scheduleWithFixedDelay);
            return false;
        }

        if (method.getParameterCount() != 2 ||
                method.getParameters()[0].getType() != Long.class ||
                method.getParameters()[1].getType() != Long.class) {
            log.error(Markers.GameLoopTickManager, "tick method should has two parameter of " +
                            "(java.lang.Long, java.lang.Long)', clazz:{}, method:{}, delay:{}, tick:{}, " +
                            "timeUnit:{}, scheduleWithFixedDelay:{}",
                    clazz.getName(),
                    method.getName(),
                    delay,
                    tick,
                    timeUnit,
                    scheduleWithFixedDelay);
            return false;
        }

        if (Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz)).noneMatch(method1 -> method1.equals(method))) {
            log.error(Markers.GameLoopTickManager, "the method {} is not belong to clazz:{}, delay:{}, " +
                            "tick:{}, timeUnit:{}, scheduleWithFixedDelay:{}",
                    method.getName(),
                    clazz.getName(),
                    delay,
                    tick,
                    timeUnit,
                    scheduleWithFixedDelay);
            return false;
        }

        final TickData tickData = new TickData(object, method);
        if (tickDataFutureMap.containsKey(tickData)) {
            log.error(Markers.GameLoopTickManager, "the method:{} has registered, clazz:{}, delay:{}, " +
                            "tick:{}, timeUnit:{}, scheduleWithFixedDelay:{}",
                    method.getName(),
                    clazz.getName(),
                    delay,
                    tick,
                    timeUnit,
                    scheduleWithFixedDelay);
            return false;
        }

        ReflectionUtils.makeAccessible(method);
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final long currentTimeMillis = System.currentTimeMillis();
                final long lastTickMilliSecond = tickData.getLastTickMilliSecond();
                try {
                    tickData.getMethod().invoke(tickData.getObject(), currentTimeMillis, lastTickMilliSecond);
                }
                catch (Throwable throwable) {
                    log.error(Markers.GameLoopTickManager, "exception caught, clazz:" + clazz.getName() +
                            ", method:" + method.getName() +
                            ", delay:" + delay +
                            ", tick:" + tick +
                            ", timeUnit:" + timeUnit +
                            ", scheduleWithFixedDelay:" + scheduleWithFixedDelay, throwable);
                } finally {
                    tickData.setLastTickMilliSecond(currentTimeMillis);
                }
            }
        };

        final ScheduledFuture<?> scheduledFuture;
        if (scheduleWithFixedDelay) {
            scheduledFuture = owner.scheduleWithFixedDelay(runnable, delay, tick, timeUnit);
        } else {
            scheduledFuture = owner.scheduleAtFixedRate(runnable, delay, tick, timeUnit);
        }

        tickDataFutureMap.put(tickData, scheduledFuture);

        if (log.isDebugEnabled()) {
            log.debug(Markers.GameLoopTickManager, "register tick success, clazz:{}, method:{}, delay:{}, " +
                            "tick:{}, timeUnit:{}, scheduleWithFixdDelay:{}",
                    clazz.getName(),
                    method.getName(),
                    delay,
                    tick,
                    timeUnit,
                    scheduleWithFixedDelay);
        }
        return true;
    }

    @Override
    public int unregister(Object object) {

        final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(object.getClass()))
                .filter(method -> method.isAnnotationPresent(Tick.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        if (annotatedMethodSet.isEmpty()) {
            log.warn(Markers.GameLoopTickManager, "the Object has none annotated method, annotation:{}, " +
                            "clazz:{}",
                    Tick.class.getName(),
                    object.getClass().getName());
            return 0;
        }

        return annotatedMethodSet.stream().mapToInt(method -> unregister(object, method) ? 1 : 0).sum();
    }

    @Override
    public boolean unregister(Object object, Method method) {

        final TickData tickData = new TickData(object, method);
        if (!tickDataFutureMap.containsKey(tickData)) {
            return false;
        }

        final ScheduledFuture<?> scheduledFuture = tickDataFutureMap.remove(tickData);
        scheduledFuture.cancel(false);

        if (log.isDebugEnabled()) {
            log.debug("unregister tick, clazz:{}, method:{}", object.getClass().getName(), method.getName());
        }
        return true;
    }

    @Override
    public int unregisterAll() {
        return new HashMap<>(tickDataFutureMap).keySet()
                .stream()
                .mapToInt(tickData -> unregister(tickData.getObject(), tickData.getMethod()) ? 1 : 0).sum();
    }
}
