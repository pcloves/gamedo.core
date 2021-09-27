package org.gamedo.gameloop.components.tickManager;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.Method;

@Data
@EqualsAndHashCode(of = {"object", "method"})
public class TickData {
    /**
     * 要执行心跳的实例
     */
    private final Object object;
    /**
     * 要执行心跳的函数
     */
    private final Method method;
    /**
     * 首次运行时间
     */
    private final long firstTickMilliSecond;
    /**
     * 最近一次运行时间
     */
    private long lastTickMilliSecond;

    public TickData(Object object, Method method) {
        this.object = object;
        this.method = method;
        firstTickMilliSecond = -1;
        lastTickMilliSecond = -1;
    }

    public TickData(Object object, Method method, long firstTickMilliSecond) {
        this.object = object;
        this.method = method;
        this.firstTickMilliSecond = firstTickMilliSecond;
        lastTickMilliSecond = -1;
    }

    @Override
    public String toString() {
        return "TickData{" +
                "object=" + object.getClass().getSimpleName() +
                ", method=" + method.getName() +
                ", firstTickMilliSecond=" + firstTickMilliSecond +
                ", lastTickMilliSecond=" + lastTickMilliSecond +
                '}';
    }
}
