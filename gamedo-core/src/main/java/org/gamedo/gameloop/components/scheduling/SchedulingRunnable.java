package org.gamedo.gameloop.components.scheduling;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.logging.Markers;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Getter
@ToString(onlyExplicitlyIncluded = true)
class SchedulingRunnable implements Runnable {
    private final GameLoopScheduler gameLoopScheduler;
    private final Runnable runnable;
    private final IGameLoop gameLoop;
    @ToString.Include
    private final CronTrigger trigger;
    private final SimpleTriggerContext triggerContext;
    private final Set<ScheduleInvokeData> scheduleInvokeDataSet = new HashSet<>(128);
    private Date scheduledExecutionTime;
    private CompletableFuture<Void> future;

    SchedulingRunnable(GameLoopScheduler gameLoopScheduler, String cron, Runnable runnable) {
        this.gameLoopScheduler = gameLoopScheduler;
        this.runnable = runnable;
        gameLoop = gameLoopScheduler.getOwner();
        trigger = new CronTrigger(cron);
        triggerContext = new SimpleTriggerContext();
    }

    boolean containsMethod(Method method) {
        return scheduleInvokeDataSet.stream()
                .anyMatch(scheduleInvokeData -> scheduleInvokeData.getMethod().equals(method));
    }

    boolean removeMethod(Method method) {
        final Set<ScheduleInvokeData> removedSet = scheduleInvokeDataSet.stream()
                .filter(scheduleInvokeData -> scheduleInvokeData.getMethod().equals(method))
                .collect(Collectors.toSet());

        return scheduleInvokeDataSet.removeAll(removedSet);
    }

    boolean schedule() {

        scheduledExecutionTime = trigger.nextExecutionTime(triggerContext);
        if (scheduledExecutionTime != null) {
            final long delay = scheduledExecutionTime.getTime() - triggerContext.getClock().millis();
            final Executor executor = CompletableFuture.delayedExecutor(delay,
                    TimeUnit.MILLISECONDS,
                    gameLoopScheduler.getOwner());

            future = CompletableFuture.runAsync(this, executor);

            log.debug(Markers.GameLoopScheduler, "schedule next delay:{}, cron:{}",
                    () -> delay,
                    trigger::getExpression);

            return true;
        } else {
            //???????????????????????????CronTrigger.nextExecutionTime?????????null???????????????spring???????????????????????????????????????????????????????????????????????????
            //???????????????spring???cron?????????????????????year????????????????????????cron?????????????????????????????????nextExecutionTime????????????????????????????????????
            //??????????????????error??????
            log.error(Markers.GameLoopScheduler,
                    "next trigger time is null, stop schedule, cron:{}",
                    trigger.getExpression());
            return false;
        }
    }

    @Override
    public void run() {
        final Date actualExecutionTime = new Date(triggerContext.getClock().millis());
        try {
            runnable.run();
        } catch (Throwable e) {
            log.error(Markers.GameLoopScheduler, "exception caught when run, cron:" + trigger.getExpression(), e);
        } finally {
            Date completionTime = new Date(triggerContext.getClock().millis());
            triggerContext.update(scheduledExecutionTime, actualExecutionTime, completionTime);
            if (!gameLoop.isShutdown()) {
                if (!schedule()) {
                    final HashSet<ScheduleInvokeData> set = new HashSet<>(scheduleInvokeDataSet);
                    set.forEach(data -> gameLoopScheduler.unregister(data.getClass(), data.getMethod()));
                }
            }
        }
    }
}
