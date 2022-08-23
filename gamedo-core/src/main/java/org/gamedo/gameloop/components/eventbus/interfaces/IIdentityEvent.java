package org.gamedo.gameloop.components.eventbus.interfaces;

import org.gamedo.ecs.interfaces.IIdentity;
import org.gamedo.gameloop.components.eventbus.EventData;

/**
 * 这是一个专门针对{@link IIdentity}类型订阅者进行过滤检测的{@link IFilterableEvent}事件，只有订阅者的类型是{@link IIdentity}及其
 * 子类时，才会过滤成功
 */
public interface IIdentityEvent extends IFilterableEvent<IIdentityEvent> {
    @Override
    default boolean filter(EventData subscriber, IIdentityEvent event) {
        return IIdentity.class.isAssignableFrom(subscriber.getObject().getClass());
    }
}
