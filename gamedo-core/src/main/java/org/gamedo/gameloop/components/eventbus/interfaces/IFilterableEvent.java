package org.gamedo.gameloop.components.eventbus.interfaces;

/**
 * 这是一个自带过滤特性的事件，只有当订阅者通过事件检测（{@link IFilterableEvent#filter(Object)}）时才往该订阅者派发事件，当前仅当某订阅者
 * 同时满足如下2个条件时，{@link IGameLoopEventBus}事件总线才会为其派发事件：
 * <ul>
 * <li> 订阅者的类型为：{@link IFilterableEvent#getType()}
 * <li> 订阅者通过{@link IFilterableEvent#filter(Object)}检测
 * </ul>
 * 当该类型的事件投递到{@link IGameLoopEventBus}事件总线上后，事件总线会对所有该事件类型的订阅者依次进行这2条检测，两者同时满足时才会派发事件
 */
public interface IFilterableEvent<T> extends IEvent {

    /**
     * 订阅者的类型，只有订阅者是该类型或其子类时，才满足条件
     * @return 订阅者类型
     */
    Class<T> getType();

    /**
     * 对订阅者进行过滤
     * @param subscriber 要被检测的订阅者
     * @return true表示该订阅者关心该事件，false表示事件订阅者不关心该事件
     */
    boolean filter(T subscriber);
}
