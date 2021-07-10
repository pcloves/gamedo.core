package org.gamedo.gameloop.components.tickManager;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.Method;

@Data
@EqualsAndHashCode(exclude = "lastTickMilliSecond")
public class TickData {
    private final Object object;
    private final Method method;
    private long lastTickMilliSecond;

    public TickData(Object object, Method method) {
        this.object = object;
        this.method = method;
        lastTickMilliSecond = -1;
    }
}
