package org.gamedo.gameloop.components.tickManager;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import lombok.extern.log4j.Log4j2;
import org.gamedo.annotation.GamedoComponent;
import org.gamedo.annotation.Tick;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.gameloop.components.tickManager.interfaces.IGameLoopTickManager;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.logging.Markers;
import org.gamedo.util.GamedoConfiguration;
import org.gamedo.util.Metric;
import org.gamedo.util.Pair;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Log4j2
@GamedoComponent
public class GameLoopTickManager extends GameLoopComponent implements IGameLoopTickManager {

    private final Map<ScheduleDataKey, TickRunnable> scheduleDataMap = new HashMap<>(32);
    private final Map<TickData, TickRunnable> tickDataScheduleDataMap = new HashMap<>(128);
    private final Map<ScheduleDataKey, Pair<AtomicLong, Gauge>> scheduleDataKey2GaugeMap = new HashMap<>(128);

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
            log.info(Markers.GameLoopTickManager, "none annotation {} method found, clazz:{}",
                    Tick.class.getSimpleName(),
                    clazz.getName());
            return 0;
        }

        final int count = annotatedMethodSet.stream().mapToInt(method -> register(object, method) ? 1 : 0).sum();
        log.debug(Markers.GameLoopTickManager, "register tick finish, clazz:{}, totalCount:{}, successCount:{}",
                () -> clazz.getSimpleName(),
                () -> annotatedMethodSet.size(),
                () -> count
        );

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
                method.getParameters()[0].getType() != Long.class &&
                method.getParameters()[0].getType() != long.class ||
                method.getParameters()[1].getType() != Long.class &&
                method.getParameters()[1].getType() != long.class) {
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

        final long currentTimeMillis = System.currentTimeMillis();
        final ScheduleDataKey scheduleDataKey = new ScheduleDataKey(tick, timeUnit, scheduleWithFixedDelay);
        final TickData tickData = new TickData(object, method, currentTimeMillis + timeUnit.toMillis(delay));
        if (tickDataScheduleDataMap.containsKey(tickData)) {
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

        final TickRunnable tickRunnable = scheduleDataMap.computeIfAbsent(scheduleDataKey, key -> new TickRunnable(owner, scheduleDataKey));
        tickRunnable.addTickData(tickData);
        tickDataScheduleDataMap.put(tickData, tickRunnable);

        log.debug(Markers.GameLoopTickManager, "register tick success, clazz:{}, method:{}, delay:{}, " +
                        "tick:{}, timeUnit:{}, scheduleWithFixdDelay:{}",
                () -> clazz.getName(),
                () -> method.getName(),
                () -> delay,
                () -> tick,
                () -> timeUnit,
                () -> scheduleWithFixedDelay);

        metricGauge(tickRunnable);

        return true;
    }


    @Override
    public int unregister(Object object) {

        final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(object.getClass()))
                .filter(method -> method.isAnnotationPresent(Tick.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        if (annotatedMethodSet.isEmpty()) {
            log.info(Markers.GameLoopTickManager, "none annotation {} method found, clazz:{}",
                    Tick.class.getName(),
                    object.getClass().getName());
            return 0;
        }

        return annotatedMethodSet.stream().mapToInt(method -> unregister(object, method) ? 1 : 0).sum();
    }

    @Override
    public boolean unregister(Object object, Method method) {

        final TickData tickData = new TickData(object, method);
        final TickRunnable tickRunnable = tickDataScheduleDataMap.remove(tickData);
        if (tickRunnable == null) {
            return false;
        }

        tickRunnable.removeTickData(tickData);
        if (tickRunnable.getTickDataMap().isEmpty()) {
            scheduleDataMap.remove(tickRunnable.scheduleDataKey);
            tickRunnable.future.cancel(false);
        }

        log.debug(Markers.GameLoopTickManager, "unregister tick, clazz:{}, method:{}",
                () -> object.getClass().getName(),
                () -> method.getName());

        metricGauge(tickRunnable);

        return true;
    }

    @Override
    public int unregisterAll() {

        return new HashSet<>(scheduleDataMap.values())
                .stream()
                .flatMap(tickRunnable -> new ArrayList<>(tickRunnable.getTickDataList()).stream())
                .mapToInt(tickData -> unregister(tickData.getObject(), tickData.getMethod()) ? 1 : 0).sum();
    }

    private void metricGauge(TickRunnable tickRunnable) {
        owner.getComponent(MeterRegistry.class)
                .map(meterRegistry -> GamedoConfiguration.isMetricTickEnable() ? meterRegistry : null)
                .ifPresent(meterRegistry -> {
                    final long countNew = tickRunnable.getTickDataList().size();
                    scheduleDataKey2GaugeMap.computeIfAbsent(tickRunnable.getScheduleDataKey(), key -> {
                        final AtomicLong count = new AtomicLong(countNew);
                        final Tag tag = Tag.of("tick", tickRunnable.scheduleDataKey.toTagString());
                        final Tags tags = Metric.tags(owner).and(tag);
                        return Pair.of(count, Gauge.builder(Metric.MeterIdTickRegisterGauge, count, AtomicLong::longValue)
                                        .tags(tags)
                                        .description("the instance count of a specific @" + Tick.class.getSimpleName())
                                        .baseUnit(BaseUnits.OBJECTS)
                                        .register(meterRegistry));
                            }
                    ).getK().set(countNew);
                });
    }
}
