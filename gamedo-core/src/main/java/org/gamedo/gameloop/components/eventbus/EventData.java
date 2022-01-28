package org.gamedo.gameloop.components.eventbus;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.lang.reflect.Method;

@Value
@EqualsAndHashCode
public class EventData {
    Object object;
    Method method;
    @EqualsAndHashCode.Exclude
    long compareValue;

    public EventData(Object object, Method method) {
        this.object = object;
        this.method = method;
        this.compareValue = 0L;
    }

    public EventData(Object object, Method method, long compareValue) {
        this.object = object;
        this.method = method;
        this.compareValue = compareValue;
    }
}
