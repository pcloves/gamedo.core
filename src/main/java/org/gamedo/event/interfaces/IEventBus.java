package org.gamedo.event.interfaces;

import org.gamedo.ecs.interfaces.IComponent;

/**
 * 事件总线接口
 */
public interface IEventBus extends IComponent {
    /**
     * 注册一个事件
     *
     * @param <E>          事件类型
     * @param eventType    事件类型
     * @param eventHandler 事件处理器，当事件处理器是一个lambada或者是函数引用时，由于java的底层机制，会导致重复注册，因此在实际开发时，最佳
     *                     实践是将lamda或函数引用声明为类的成员或者final的静态变量，错误的使用场景可以参考单元测试：EventBusTest#registerEventUsingLambda
     *                     和EventBusTest#registerEventUsingMethodReference，正确的使用场景参考单元测试：EventBusTest#registerEventUsingField和
     *                     EventBusTest#registerEventUsingStaticField
     * @param priority     事件处理器的优先级，当事件总线接受到一个事件时，处理该事件的规则为：1、EEventPriority高的优先分发；2、如果优先级
     *                     相同，先注册的优先分发
     * @return true表示注册成功，否则表示注册失败
     * @see <a href="ttps://stackoverflow.com/questions/24095875/is-there-a-way-to-compare-lambdas StackOverflow">Is there a way to compare lambdas?</a>
     */
    <E extends IEvent> boolean registerEvent(Class<E> eventType, IEventHandler<E> eventHandler, EventPriority priority);

    /**
     * 反注册某个事件处理器
     *
     * @param eventType    事件类型
     * @param eventHandler 事件处理器，必须被final修饰
     * @param <E>          事件类型
     */
    <E extends IEvent> void unRegisterEvent(Class<E> eventType, IEventHandler<E> eventHandler);

    /**
     * 为某个事件添加一个事件过滤器，{@link IEventBus}会在sendEvent时，首先调用所有的事件过滤器，任何一个过滤器过滤该事件，都会导致事件不会被
     * 消费
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
     * @return 正常消费该event的事件处理的数量（抛出异常的事件处理器不包含在内）
     */
    <E extends IEvent> int sendEvent(E event);

    /**
     * 投递一个事件，该事件不会被立即消费，而是被{@link IEventBus}缓存，直到下一个tick才会被触发
     *
     * @param event 要投递的事件
     * @param <E>   事件类型
     */
    <E extends IEvent> void postEvent(E event);
}
