package org.gamedo.scheduling;

import lombok.ToString;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Value
@Log4j2
@ToString(onlyExplicitlyIncluded = true)
class SchedulingRunnable implements Runnable {
    Scheduler scheduleRegister;
    @ToString.Include
    CronTrigger trigger;
    SimpleTriggerContext triggerContext;
    Set<ScheduleInvokeData> scheduleInvokeDataSet = new HashSet<>(128);
    Runnable runnable;

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

        final long nextExecutionTime = trigger.nextExecutionTime(triggerContext).getTime();
        final long delay = nextExecutionTime - triggerContext.getClock().millis();
        final Executor executor = CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS, scheduleRegister.iGameLoop);

        CompletableFuture.runAsync(this, executor);

        return this;
    }

    @Override
    public void run() {
        final String threadName = Thread.currentThread().getName();
        try {
            runnable.run();
            log.debug("schedule complete, cron:{}, thread:{}",
                    () -> trigger.getExpression(),
                    () -> threadName);
        } catch (Throwable e) {
            log.error("exception caught when run, cron:" + trigger.getExpression() + ", thread:" + threadName, e);
        }
        finally {
            if (!scheduleRegister.iGameLoop.isShutdown()) {
                schedule();
            }
        }
    }
}
