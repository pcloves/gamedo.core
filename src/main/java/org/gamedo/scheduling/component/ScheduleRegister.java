package org.gamedo.scheduling.component;

import lombok.extern.log4j.Log4j2;
import org.gamedo.ecs.Component;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.scheduling.CronScheduled;
import org.gamedo.scheduling.interfaces.IScheduleRegister;
import org.gamedo.scheduling.interfaces.IScheduleRegisterFunction;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class ScheduleRegister extends Component implements IScheduleRegister {

    private final IGameLoop iGameLoop;
    /**
     * cron表达式 --> 调度数据
     */
    private final Map<String, ScheduleData> cronToscheduleDataMap = new HashMap<>(32);
    /**
     * 收到{@link TaskScheduler}
     */
    private final Function<ScheduleFunctionData, Runnable> runnableFunction = data -> {
        //注意：该Runnable并非在本线程中！！！
        return () -> {

            if (data.getIGameLoop().isShutdown()) {
                data.getLog().debug("the IGameLoop {} has shutdown", () -> data.getIGameLoop().getId());
                return;
            }

            //投递到本线程中
            final GameLoopFunction<Integer> schedule = IScheduleRegisterFunction.schedule(data.getCron());
            final CompletableFuture<Integer> completableFuture = data.getIGameLoop().submit(schedule);
            completableFuture.whenCompleteAsync((c, t) -> {
                if (t != null) {
                    data.getLog().error("exception caught", t);
                }

                data.getLog().debug("shedule finish, cron:{}, trigger method:{}", () -> data.getCron(), () -> c);
            });
        };
    };

    public ScheduleRegister(IEntity owner, IGameLoop iGameLoop) {
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

        if (Arrays.stream(clazz.getDeclaredMethods()).noneMatch(method1 -> method1.equals(method))) {
            log.error("the object has none method of {}, clazz:{}, cron:{}",
                    method.getName(),
                    clazz.getName(),
                    cron);
            return false;
        }

        //先获取owner上的TaskScheduler组件
        final Optional<TaskScheduler> taskSchedulerOptional = owner.getComponent(TaskScheduler.class);
        if (taskSchedulerOptional.isEmpty()) {
            log.error("getComponent for {} failed.", TaskScheduler.class.getName());
            return false;
        }

        final TaskScheduler taskScheduler = taskSchedulerOptional.get();
        final ScheduleData scheduleData = cronToscheduleDataMap.computeIfAbsent(cron, cron1 -> {
            final ScheduleFunctionData functionData = new ScheduleFunctionData(iGameLoop, cron1, log);
            final Runnable apply = runnableFunction.apply(functionData);
            final CronTrigger trigger = new CronTrigger(cron1);
            final ScheduledFuture<?> future = taskScheduler.schedule(apply, trigger);
            return new ScheduleData(cron1, future);
        });

        final Set<ScheduleInvokeData> scheduleInvokeDataSet = scheduleData.getScheduleInvokeDataSet();
        final ScheduleInvokeData scheduleInvokeData = new ScheduleInvokeData(object, method);
        if (scheduleInvokeDataSet.contains(scheduleInvokeData)) {
            log.warn("the method {} has registered, clazz:{}", method.getName(), clazz.getName());
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

        final List<ScheduleData> scheduleDataList = cronToscheduleDataMap.values()
                .stream()
                .filter(scheduleData -> scheduleData.containsMethod(method))
                .filter(scheduleData -> scheduleData.removeMethod(method))
                .collect(Collectors.toList());

        scheduleDataList.removeIf(scheduleData -> {
            final boolean empty = scheduleData.getScheduleInvokeDataSet().isEmpty();
            if (empty) {
                scheduleData.getFuture().cancel(false);
                log.info("invokeData set is empty stop schedule, cron:{}", scheduleData.getCron());
            }
            return empty;
        });


        return !scheduleDataList.isEmpty();
    }

    @Override
    public int unregisterAll() {

        final int sum = cronToscheduleDataMap.values()
                .stream()
                .flatMap(scheduleData -> {
                    return scheduleData.getScheduleInvokeDataSet()
                            .stream()
                            .map(scheduleInvokeData -> scheduleInvokeData.getObject().getClass());
                })
                .mapToInt(this::unregister)
                .sum();

        cronToscheduleDataMap.clear();

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
