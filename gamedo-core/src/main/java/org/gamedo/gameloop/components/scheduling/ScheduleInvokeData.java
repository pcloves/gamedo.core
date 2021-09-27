package org.gamedo.gameloop.components.scheduling;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.lang.reflect.Method;

@Value
@EqualsAndHashCode
class ScheduleInvokeData {
    Object object;
    Method method;
}
