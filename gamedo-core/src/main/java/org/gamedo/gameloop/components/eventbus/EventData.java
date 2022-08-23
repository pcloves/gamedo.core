package org.gamedo.gameloop.components.eventbus;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;

@Value
@EqualsAndHashCode
public class EventData {
    /**
     * 事件的订阅者
     */
    Object object;
    /**
     * 事件回调方法
     */
    Method method;
    /**
     * 泛型事件的类型（如果非泛型事件，则恒为null）
     */
    Class<?> genericClazz;
    /**
     * 排序值
     */
    @EqualsAndHashCode.Exclude
    long compareValue;

    public EventData(Object object, Method method) {
        this.object = object;
        this.method = method;
        this.genericClazz = ResolvableType.forMethodParameter(method, 0).resolveGeneric(0);
        this.compareValue = 0L;
    }

    public EventData(Object object, Method method, long compareValue) {
        this.object = object;
        this.method = method;
        this.genericClazz = ResolvableType.forMethodParameter(method, 0).resolveGeneric(0);
        this.compareValue = compareValue;
    }
}
