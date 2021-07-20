package org.gamedo.gameloop.components.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.gamedo.annotation.Cron;
import org.gamedo.ecs.GameLoopComponent;
import org.gamedo.gameloop.components.scheduling.interfaces.IGameLoopScheduler;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.logging.Markers;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class GameLoopScheduler extends GameLoopComponent implements IGameLoopScheduler {
    /**
     * cron表达式 --> 该表达式对应的所有运行时数据
     */
    private final Map<String, SchedulingRunnable> cronToscheduleDataMap = new HashMap<>(32);
    /**
     * 收到{@link TaskScheduler}
     */
    private final Function<String, Runnable> runnableFunction = cron -> {
        //注意：该Runnable最终就在owner线程中执行！！
        return () -> {
            if (owner.isShutdown()) {
                log.warn(Markers.GameLoopScheduler, "the IGameLoop {} has shutdown, stop next schedule", owner.getId());
                return;
            }

            final int successCount = schedule(cron);
            if (log.isDebugEnabled()) {
                log.debug(Markers.GameLoopScheduler, "schedule finish, cron:{}, totalCount:{}, successCount:{}",
                        cron,
                        cronToscheduleDataMap.get(cron).getScheduleInvokeDataSet().size(),
                        successCount);
            }
        };
    };

    public GameLoopScheduler(IGameLoop owner) {
        super(owner);
    }

    public static boolean safeInvoke(SchedulingRunnable schedulingRunnable, ScheduleInvokeData scheduleInvokeData) {
        final Method method = scheduleInvokeData.getMethod();
        final Object object = scheduleInvokeData.getObject();
        try {
            ReflectionUtils.makeAccessible(method);
            final Long currentTimeMillis = System.currentTimeMillis();
            final SimpleTriggerContext triggerContext = schedulingRunnable.getTriggerContext();
            final Long lastExecutionTime = Optional.ofNullable(triggerContext.lastActualExecutionTime())
                    .map(date -> date.getTime())
                    .orElse(Long.valueOf(-1));
            method.invoke(object, currentTimeMillis, lastExecutionTime);
        } catch (Throwable t) {
            final Class<?> clazz = object.getClass();
            log.error(Markers.GameLoopScheduler, "exception caught. class:" + clazz.getSimpleName() +
                    "method:" + method, t);
            return false;
        }

        return true;
    }

    @Override
    public int register(Object object) {

        final Class<?> clazz = object.getClass();
        final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz))
                .filter(method -> method.isAnnotationPresent(Cron.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        if (annotatedMethodSet.isEmpty()) {
            log.warn(Markers.GameLoopScheduler, "the Object has none annotated method, annotation:{}, clazz:{}",
                    Cron.class.getSimpleName(),
                    clazz.getName());
            return 0;
        }

        final int count = annotatedMethodSet.stream().mapToInt(method -> register(object, method) ? 1 : 0).sum();

        if (log.isDebugEnabled()) {
            log.debug(Markers.GameLoopScheduler, "register schedule finish, clazz:{}, totalCount:{}, successCount:{}",
                    clazz.getSimpleName(),
                    annotatedMethodSet.size(),
                    count
            );
        }

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

        SchedulingRunnable runnable = cronToscheduleDataMap.get(cron);
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

        scheduleInvokeDataSet.add(scheduleInvokeData);
        if (isNewRunnable) {
            if (runnable.schedule()) {
                cronToscheduleDataMap.put(cron, runnable);

                if (log.isDebugEnabled()) {
                    log.debug(Markers.GameLoopScheduler, "register success, clazz:{}, method:{}, cron:{}",
                            clazz.getSimpleName(),
                            methodName,
                            cron);
                }
            }
        }

        return true;
    }

    @Override
    public int unregister(Class<?> clazz) {

        final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz))
                .filter(method -> method.isAnnotationPresent(Cron.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        if (annotatedMethodSet.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(Markers.GameLoopScheduler, "the Object has none annotated method, annotation:{}, clazz:{}",
                        Cron.class.getSimpleName(),
                        clazz.getName());
            }
            return 0;
        }

        return annotatedMethodSet.stream().mapToInt(method -> unregister(clazz, method) ? 1 : 0).sum();
    }

    @Override
    public boolean unregister(Class<?> clazz, Method method) {

        final List<SchedulingRunnable> schedulingRunnableList = cronToscheduleDataMap.values()
                .stream()
                .filter(schedulingRunnable -> schedulingRunnable.containsMethod(method))
                .filter(schedulingRunnable -> schedulingRunnable.removeMethod(method))
                .collect(Collectors.toList());

        cronToscheduleDataMap.values().removeIf(runnable -> {
            final boolean empty = runnable.getScheduleInvokeDataSet().isEmpty();
            final CronTrigger trigger = runnable.getTrigger();
            if (empty) {
                //可能有调度正在等待中，直接取消掉吧
                final boolean cancel = runnable.getFuture().cancel(false);
                if (log.isDebugEnabled()) {
                    log.debug(Markers.GameLoopScheduler, "stop schedule {}, cancel:{}",
                            trigger.getExpression(),
                            cancel);
                }
            }

            return empty;
        });

        return !schedulingRunnableList.isEmpty();
    }

    @Override
    public int unregisterAll() {

        final Collection<SchedulingRunnable> collection = new HashSet<>(cronToscheduleDataMap.values());
        final int sum = collection.stream()
                .flatMap(schedulingRunnable -> {
                    return schedulingRunnable.getScheduleInvokeDataSet()
                            .stream()
                            .map(scheduleInvokeData -> scheduleInvokeData.getObject().getClass());
                })
                .mapToInt(this::unregister)
                .sum();

        if (!cronToscheduleDataMap.isEmpty()) {
            log.error(Markers.GameLoopScheduler, "There are remaining {} in the map:{}",
                    SchedulingRunnable.class.getSimpleName(),
                    cronToscheduleDataMap.values());
            cronToscheduleDataMap.clear();
        }

        return sum;
    }

    private int schedule(String cron) {

        if (!cronToscheduleDataMap.containsKey(cron)) {
            return 0;
        }

        final SchedulingRunnable runnable = cronToscheduleDataMap.get(cron);
        return runnable.getScheduleInvokeDataSet()
                .stream()
                .mapToInt(scheduleInvokeData -> safeInvoke(runnable, scheduleInvokeData) ? 1 : 0)
                .sum();
    }

}
