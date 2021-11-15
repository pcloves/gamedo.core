package org.gamedo.gameloop.components.scheduling;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.BaseUnits;
import lombok.extern.log4j.Log4j2;
import org.gamedo.annotation.Cron;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.logging.GamedoLogContext;
import org.gamedo.logging.Markers;
import org.gamedo.util.GamedoConfiguration;
import org.gamedo.util.Metric;
import org.gamedo.util.Pair;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class GameLoopScheduler extends GameLoopComponent implements IGameLoopScheduler {

    private final Map<String, Pair<AtomicLong, Gauge>> cron2GaugeMap = new HashMap<>(4);
    /**
     * cron表达式 --> 该表达式对应的所有运行时数据
     */
    private final Map<String, SchedulingRunnable> cron2schedulingRunnableMap = new HashMap<>(32);
    /**
     * 收到{@link TaskScheduler}
     */
    private final Function<String, Runnable> runnableFunction = cron -> {
        //注意：该Runnable最终就在owner线程中执行！！
        return () -> {
            final IGameLoop owner = ownerRef.get();
            if (owner == null) {
                log.error(Markers.GameLoopScheduler, "the {} hasn't a owner yet.", GameLoopScheduler.class.getSimpleName());
                return;
            }
            if (owner.isShutdown()) {
                log.warn(Markers.GameLoopScheduler, "the IGameLoop {} has shutdown, stop next schedule", owner.getId());
                return;
            }

            final int successCount = schedule(cron);
            log.debug(Markers.GameLoopScheduler, "schedule finish, cron:{}, totalCount:{}, successCount:{}",
                    () -> cron,
                    () -> cron2schedulingRunnableMap.get(cron).getScheduleInvokeDataSet().size(),
                    () -> successCount);
        };
    };

    public GameLoopScheduler(IGameLoop owner) {
        super(owner);
    }

    public boolean safeInvoke(SchedulingRunnable schedulingRunnable, ScheduleInvokeData scheduleInvokeData) {
        final Method method = scheduleInvokeData.getMethod();
        final Object object = scheduleInvokeData.getObject();
        final IGameLoop owner = ownerRef.get();
        if (owner == null) {
            log.error(Markers.GameLoopScheduler, "the {} hasn't a owner yet.", GameLoopScheduler.class.getSimpleName());
            return false;
        }
        final Timer timer = owner.getComponent(MeterRegistry.class)
                .map(meterRegistry -> GamedoConfiguration.isMetricCronEnable() ? meterRegistry : null)
                .map(meterRegistry -> {
                    final Tags tags = Metric.tags(owner);
                    return Timer.builder(Metric.MeterIdCronTimer)
                            .tags(tags)
                            .tag("class", object.getClass().getName())
                            .tag("method", method.getName())
                            .tag("cron", schedulingRunnable.getTrigger().getExpression())
                            .description("the @" + Cron.class.getSimpleName() + " method timing")
                            .register(meterRegistry);
                })
                .orElse(Metric.NOOP_TIMER);

        return Boolean.TRUE.equals(timer.record(() -> {
            try (final GamedoLogContext.CloseableEntityId ignored = GamedoLogContext.pushEntityIdAuto(object)) {
                final Long currentTimeMillis = System.currentTimeMillis();
                final SimpleTriggerContext triggerContext = schedulingRunnable.getTriggerContext();
                final Long lastExecutionTime = Optional.ofNullable(triggerContext.lastActualExecutionTime())
                        .map(Date::getTime)
                        .orElse((long) -1);
                method.invoke(object, currentTimeMillis, lastExecutionTime);
            } catch (Exception e) {
                final Class<?> clazz = object.getClass();
                log.error(Markers.GameLoopScheduler, "exception caught. class:" + clazz.getSimpleName() +
                        "method:" + method, e);
                return false;
            }

            return true;
        }));
    }

    @Override
    public int register(Object object) {

        final Class<?> clazz = object.getClass();
        @SuppressWarnings("DuplicatedCode") final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz))
                .filter(method -> method.isAnnotationPresent(Cron.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        if (annotatedMethodSet.isEmpty()) {
            log.info(Markers.GameLoopScheduler, "none annotation {} method found, clazz:{}",
                    Cron.class.getSimpleName(),
                    clazz.getName());
            return 0;
        }

        final int count = annotatedMethodSet.stream().mapToInt(method -> register(object, method) ? 1 : 0).sum();

        log.debug(Markers.GameLoopScheduler, "register schedule finish, clazz:{}, totalCount:{}, successCount:{}",
                clazz::getSimpleName,
                annotatedMethodSet::size,
                () -> count
        );

        return count;
    }

    @Override
    public boolean register(Object object, Method method) {

        if (!method.isAnnotationPresent(Cron.class)) {
            return false;
        }

        final Cron annotation = method.getAnnotation(Cron.class);
        final String cron = annotation.value();

        return register(object, method, cron);
    }

    @Override
    public boolean register(Object object, Method method, String cron) {

        final Class<?> clazz = object.getClass();
        final String clazzName = clazz.getName();
        final String methodName = method.getName();
        final IGameLoop owner = ownerRef.get();
        if (owner == null) {
            log.error(Markers.GameLoopScheduler, "the {} hasn't a owner yet.", GameLoopScheduler.class.getSimpleName());
            return false;
        }
        if (owner.isShutdown()) {
            log.warn(Markers.GameLoopScheduler, "the GameLoop has been shut down, register failed, clazz:{}, " +
                            "method:{}, cron:{}",
                    clazzName,
                    methodName,
                    cron);
            return false;
        }

        if (method.getParameterCount() != 2 ||
                method.getParameters()[0].getType() != Long.class ||
                method.getParameters()[1].getType() != Long.class) {
            log.error(Markers.GameLoopScheduler, "schedule method should has two parameter of " +
                            "(java.lang.Long, java.lang.Long)', clazz:{}, method:{}, cron:{}",
                    clazzName,
                    methodName,
                    cron);
            return false;
        }

        if (Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz)).noneMatch(method1 -> method1.equals(method))) {
            log.error(Markers.GameLoopScheduler, "the method:{} is not belong to clazz:{}, cron:{}",
                    methodName,
                    clazzName,
                    cron);
            return false;
        }

        SchedulingRunnable runnable = cron2schedulingRunnableMap.get(cron);
        boolean isNewRunnable = false;
        if (runnable == null) {
            try {
                runnable = new SchedulingRunnable(this, cron, runnableFunction.apply(cron));
                isNewRunnable = true;
            } catch (IllegalArgumentException e) {
                log.error(Markers.GameLoopScheduler, "invalid cron expression:" + cron +
                        ", clazz:" + clazzName +
                        ", method:" + methodName, e);
                return false;
            }
        }

        final Set<ScheduleInvokeData> scheduleInvokeDataSet = runnable.getScheduleInvokeDataSet();
        final ScheduleInvokeData scheduleInvokeData = new ScheduleInvokeData(object, method);

        if (scheduleInvokeDataSet.contains(scheduleInvokeData)) {
            log.warn(Markers.GameLoopScheduler, "duplicate methods registered, clazz:{}, method:{}",
                    clazzName,
                    method);
            return false;
        }

        ReflectionUtils.makeAccessible(method);
        scheduleInvokeDataSet.add(scheduleInvokeData);
        if (isNewRunnable) {
            if (runnable.schedule()) {
                cron2schedulingRunnableMap.put(cron, runnable);

                log.debug(Markers.GameLoopScheduler, "register success, clazz:{}, method:{}, cron:{}",
                        clazz::getSimpleName,
                        () -> methodName,
                        () -> cron);
            }
        }

        metricGauge(cron);

        return true;
    }

    @Override
    public int unregister(Class<?> clazz) {

        final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz))
                .filter(method -> method.isAnnotationPresent(Cron.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        if (annotatedMethodSet.isEmpty()) {
            log.info(Markers.GameLoopScheduler, "none annotation {} method found, clazz:{}",
                    Cron.class.getSimpleName(),
                    clazz.getName());
            return 0;
        }

        return annotatedMethodSet.stream().mapToInt(method -> unregister(clazz, method) ? 1 : 0).sum();
    }

    @Override
    public boolean unregister(Class<?> clazz, Method method) {

        final List<SchedulingRunnable> schedulingRunnableList = cron2schedulingRunnableMap.values()
                .stream()
                .filter(schedulingRunnable -> schedulingRunnable.containsMethod(method))
                .filter(schedulingRunnable -> schedulingRunnable.removeMethod(method))
                .collect(Collectors.toList());

        cron2schedulingRunnableMap.values().removeIf(runnable -> {
            final boolean empty = runnable.getScheduleInvokeDataSet().isEmpty();
            final CronTrigger trigger = runnable.getTrigger();
            if (empty) {
                //可能有调度正在等待中，直接取消掉吧
                final boolean cancel = runnable.getFuture().cancel(false);
                log.debug(Markers.GameLoopScheduler, "stop schedule {}, cancel:{}",
                        trigger::getExpression,
                        () -> cancel);
            }

            return empty;
        });

        schedulingRunnableList.forEach(schedulingRunnable -> metricGauge(schedulingRunnable.getTrigger().getExpression()));

        return !schedulingRunnableList.isEmpty();
    }

    @Override
    public int unregisterAll() {

        final Collection<SchedulingRunnable> collection = new HashSet<>(cron2schedulingRunnableMap.values());
        final int sum = collection.stream()
                .flatMap(schedulingRunnable -> schedulingRunnable.getScheduleInvokeDataSet()
                        .stream()
                        .map(scheduleInvokeData -> scheduleInvokeData.getObject().getClass()))
                .mapToInt(this::unregister)
                .sum();

        if (!cron2schedulingRunnableMap.isEmpty()) {
            log.error(Markers.GameLoopScheduler, "There are remaining {} in the map:{}",
                    SchedulingRunnable.class.getSimpleName(),
                    cron2schedulingRunnableMap.values());
            cron2schedulingRunnableMap.clear();
        }

        return sum;
    }

    private int schedule(String cron) {

        if (!cron2schedulingRunnableMap.containsKey(cron)) {
            return 0;
        }

        final SchedulingRunnable runnable = cron2schedulingRunnableMap.get(cron);
        return runnable.getScheduleInvokeDataSet()
                .stream()
                .mapToInt(scheduleInvokeData -> safeInvoke(runnable, scheduleInvokeData) ? 1 : 0)
                .sum();
    }

    private void metricGauge(String cron) {
        final IGameLoop owner = ownerRef.get();
        if (owner == null) {
            log.error(Markers.GameLoopScheduler, "the {} hasn't a owner yet.", GameLoopScheduler.class.getSimpleName());
            return;
        }
        owner.getComponent(MeterRegistry.class)
                .map(meterRegistry -> GamedoConfiguration.isMetricCronEnable() ? meterRegistry : null)
                .ifPresent(meterRegistry -> {
                    final long countNew = cron2schedulingRunnableMap.containsKey(cron) ?
                            cron2schedulingRunnableMap.get(cron).getScheduleInvokeDataSet().size() : 0;
                    cron2GaugeMap.computeIfAbsent(cron, key -> {

                        final Tags tags = Metric.tags(owner);
                        final Tag tag = Tag.of("cron", cron);
                        final AtomicLong count = new AtomicLong(countNew);
                        return Pair.of(count, Gauge.builder(Metric.MeterIdCronRegisterGauge, count, AtomicLong::longValue)
                                .tags(tags.and(tag))
                                .baseUnit(BaseUnits.OBJECTS)
                                .description("the instance count of a specific @" + Cron.class.getSimpleName())
                                .register(meterRegistry)
                        );
                    }).getK().set(countNew);
                });
    }
}
