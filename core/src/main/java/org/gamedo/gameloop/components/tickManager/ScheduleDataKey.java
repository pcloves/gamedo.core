package org.gamedo.gameloop.components.tickManager;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.concurrent.TimeUnit;

@Data
@EqualsAndHashCode
public class ScheduleDataKey {
    final long tick;
    final TimeUnit timeUnit;
    final boolean scheduleWithFixedDelay;

    String toTagString() {
        return tick + "-" + timeUnit + (scheduleWithFixedDelay ? "fixedDelay" : "fixedRate");
    }
}
