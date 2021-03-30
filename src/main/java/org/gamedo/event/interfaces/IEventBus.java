package org.gamedo.event.interfaces;

import org.gamedo.ecs.interfaces.IComponent;

import java.util.Optional;

/**
 * 事件总线接口
 */
public interface IEventBus extends IComponent {
    /**
     * 注册一个事件
     *
     * @param <E>          事件类型
     * @param eventType    事件类型
     * @param eventHandler 事件处理器，必须被final修饰
     * @param priority     事件处理器的优先级，事件被分发时，优先级遵循两个原则：1、EEventPriority高的优先 分发；2、先注册的优先分发
     * @return 注册结果
     */
    <E extends IEvent, R> boolean registerEvent(Class<E> eventType, IEventHandler<E, R> eventHandler, EventPriority priority);

    /**
     * 反注册某个事件处理器
     *
     * @param eventType    事件类型
     * @param eventHandler 事件处理器，必须被final修饰
     * @param <E>          事件类型
     */
    <E extends IEvent, R> void unRegisterEvent(Class<E> eventType, IEventHandler<E, R> eventHandler);

    /**
     * 为某个事件添加一个事件过滤器，{@link IEventBus}会在sendEvent时，首先调用所有的事件过滤器，任何一个过滤器截 获事件，都会导致事件不会被分发到{@link IEventHandler}上去
     *
     * @param eventType   事件类型
     * @param eventFilter 事件过滤器
     * @param <E>         事件类型
     */
    <E extends IEvent> void addEventFilter(Class<E> eventType, IEventFilter<IEvent> eventFilter);

    /**
     * 删除一个事件过滤器
     *
     * @param eventType   事件类型
     * @param eventFilter 事件处理器
     * @param <E>         事件类型
     */
    <E extends IEvent> void removeEventFilter(Class<E> eventType, IEventFilter<E> eventFilter);

    /**
     * 发送一个事件，该事件会立刻分发到所有已经注册的{@link IEventHandler}
     *
     * @param <E>   事件类型
     * @param event 要发送的事件
     * @return {@link IEventHandler#apply(Object)} 返回值
     */
    <E extends IEvent, T> Optional<T> sendEvent(E event);

    /**
     * 投递一个事件，该事件不会被立即消费，而是被{@link IEventBus}缓存，直到被调用
     * {@link #dispatchCachedEvent(int)}才会触发
     *
     * @param event 要投递的事件
     * @param <E>   事件类型
     */
    <E extends IEvent> void postEvent(E event);

    /**
     * 分发事件，对所有缓存的时间进行分发
     *
     * @param maxDispatchCount 表示最多从缓存中分发多少事件
     * @return 分发事件的数量
     */
    int dispatchCachedEvent(int maxDispatchCount);
}
