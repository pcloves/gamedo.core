package org.gamedo.scheduling;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
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
    private final Scheduler scheduleRegister;
    @ToString.Include
    private final CronTrigger trigger;
    private final SimpleTriggerContext triggerContext;
    private final Set<ScheduleInvokeData> scheduleInvokeDataSet = new HashSet<>(128);
    private final Runnable runnable;
    private Date scheduledExecutionTime;
    private CompletableFuture<Void> future;

    SchedulingRunnable(Scheduler scheduleRegister, CronTrigger trigger, SimpleTriggerContext triggerContext, Runnable runnable) {
        this.scheduleRegister = scheduleRegister;
        this.trigger = trigger;
        this.triggerContext = triggerContext;
        this.runnable = runnable;
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

    SchedulingRunnable schedule() {

        scheduledExecutionTime = trigger.nextExecutionTime(triggerContext);
        final long delay = scheduledExecutionTime.getTime() - triggerContext.getClock().millis();
        final Executor executor = CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS, scheduleRegister.iGameLoop);

        future = CompletableFuture.runAsync(this, executor);

        return this;
    }

    @Override
    public void run() {
        final Date actualExecutionTime = new Date(triggerContext.getClock().millis());
        final String threadName = Thread.currentThread().getName();
        try {
            runnable.run();
            log.debug("schedule complete, cron:{}, thread:{}",
                    () -> trigger.getExpression(),
                    () -> threadName);
        } catch (Throwable e) {
            Date completionTime = new Date(triggerContext.getClock().millis());
            triggerContext.update(scheduledExecutionTime, actualExecutionTime, completionTime);
            log.error("exception caught when run, cron:" + trigger.getExpression() + ", thread:" + threadName, e);
        }
        finally {
            if (!scheduleRegister.iGameLoop.isShutdown()) {
                schedule();
            }
        }
    }
}
