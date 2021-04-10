package org.gamedo.scheduling;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Logger;
import org.gamedo.ecs.Component;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.scheduling.interfaces.IScheduler;
import org.gamedo.scheduling.interfaces.ISchedulerFunction;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class Scheduler extends Component implements IScheduler {

    /**
     * 所归属的{@link IGameLoop}
     */
    final IGameLoop iGameLoop;
    /**
     * cron表达式 --> 调度数据
     */
    private final Map<String, SchedulingRunnable> cronToscheduleDataMap = new HashMap<>(32);
    /**
     * 收到{@link TaskScheduler}
     */
    private final Function<ScheduleFunctionData, Runnable> runnableFunction = data -> {
        //注意：该Runnable可能在在本线程中，也可能不在本线程中，取决于怎么实现！！！
        return () -> {

            final String cron = data.getCron();
            final Scheduler scheduleRegister = data.getScheduleRegister();
            final IGameLoop gameLoop = scheduleRegister.iGameLoop;
            final Logger log1 = data.getLog();
            if (gameLoop.isShutdown()) {
                log1.debug("the IGameLoop {} has shutdown", () -> gameLoop.getId());
                return;
            }

            //投递到本线程中
            final GameLoopFunction<Integer> schedule = ISchedulerFunction.schedule(cron);
            final CompletableFuture<Integer> completableFuture = gameLoop.submit(schedule);
            completableFuture.whenCompleteAsync((c, t) -> {
                if (t != null) {
                    log1.error("exception caught, cron:" + cron, t);
                }
            }, gameLoop);
        };
    };

    public Scheduler(IEntity owner, IGameLoop iGameLoop) {
        super(owner);
        this.iGameLoop = iGameLoop;
    }

    public static boolean safeInvoke(ScheduleInvokeData scheduleInvokeData) {
        ReflectionUtils.makeAccessible(scheduleInvokeData.getMethod());
        try {
            scheduleInvokeData.getMethod().invoke(scheduleInvokeData.getObject());
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("exception caught. method:" + scheduleInvokeData.getMethod() + ", data:" + scheduleInvokeData.getObject(), e);
            return false;
        }

        return true;
    }

    @Override
    public int register(Object object) {

        final Class<?> clazz = object.getClass();
        final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz))
                .filter(method -> method.isAnnotationPresent(CronScheduled.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        if (annotatedMethodSet.isEmpty()) {
            log.debug("the Object has none annotated method, annotation:{}, clazz:{}",
                    () -> CronScheduled.class.getSimpleName(),
                    () -> clazz.getName());
            return 0;
        }

        return annotatedMethodSet.stream().mapToInt(method -> register(object, method) ? 1 : 0).sum();
    }

    @Override
    public boolean register(Object object, Method method) {

        if (!method.isAnnotationPresent(CronScheduled.class)) {
            return false;
        }

        final CronScheduled annotation = method.getAnnotation(CronScheduled.class);
        final String cron = annotation.value();

        return register(object, method, cron);
    }

    @Override
    public boolean register(Object object, Method method, String cron) {

        final Class<?> clazz = object.getClass();
        if (method.getParameterCount() != 0) {
            log.error("schedule method need has zero parameter, clazz:{}, method:{}, cron:{}",
                    clazz.getName(),
                    method.getName(),
                    cron);
            return false;
        }

        if (Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz)).noneMatch(method1 -> method1.equals(method))) {
            log.error("the object has none method of {}, clazz:{}, cron:{}",
                    method.getName(),
                    clazz.getName(),
                    cron);
            return false;
        }

        final SchedulingRunnable schedulingRunnable = cronToscheduleDataMap.computeIfAbsent(cron, cron1 -> {
            final CronTrigger trigger = new CronTrigger(cron1);
            final SimpleTriggerContext context = new SimpleTriggerContext();
            final ScheduleFunctionData functionData = new ScheduleFunctionData(this, cron1, log);
            final Runnable runnable = runnableFunction.apply(functionData);

            return new SchedulingRunnable(this, trigger, context, runnable).schedule();
        });

        final Set<ScheduleInvokeData> scheduleInvokeDataSet = schedulingRunnable.getScheduleInvokeDataSet();
        final ScheduleInvokeData scheduleInvokeData = new ScheduleInvokeData(object, method);
        if (scheduleInvokeDataSet.contains(scheduleInvokeData)) {
            log.warn("duplicate methods registered, clazz:{}, method:{}", clazz.getName(), method);
            return false;
        }

        scheduleInvokeDataSet.add(scheduleInvokeData);

        return true;
    }

    @Override
    public int unregister(Class<?> clazz) {

        final Set<Method> annotatedMethodSet = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz))
                .filter(method -> method.isAnnotationPresent(CronScheduled.class) && !method.isSynthetic())
                .collect(Collectors.toSet());

        if (annotatedMethodSet.isEmpty()) {
            log.debug("the Object has none annotated method, annotation:{}, clazz:{}",
                    () -> CronScheduled.class.getSimpleName(),
                    () -> clazz.getName());
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
            if (empty) {
                log.debug("stop schedule {}", () -> runnable.getTrigger().getExpression());
                //可能有调度正在等待中，直接取消掉吧
                runnable.getFuture().cancel(false);
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
            log.error("There are remaining {} in the map:{}",
                    SchedulingRunnable.class.getSimpleName(),
                    cronToscheduleDataMap.values());
        }

        return sum;
    }

    @Override
    public int schedule(String cron) {
        if (!cronToscheduleDataMap.containsKey(cron)) {
            return 0;
        }

        return cronToscheduleDataMap.get(cron).getScheduleInvokeDataSet()
                .stream()
                .mapToInt(scheduleInvokeData -> safeInvoke(scheduleInvokeData) ? 1 : 0)
                .sum();
    }

}
