package org.gamedo.scheduling.component;

import lombok.Value;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Value
class ScheduleData {
    String cron;
    Set<ScheduleInvokeData> scheduleInvokeDataSet = new HashSet<>(128);
    ScheduledFuture<?> future;

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
}
