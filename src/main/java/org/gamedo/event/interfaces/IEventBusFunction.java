package org.gamedo.event.interfaces;

import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;

public interface IEventBusFunction {
    /**
     * 定义一个行为：注册一个{@link IEventHandler 事件处理器}到{@link IGameLoop}的{@link IEventBus}组件中
     *
     * @param eventClass    要注册的事件类型
     * @param iEventHandler 事件处理器
     * @param eventPriority 该事件处理器的优先级
     * @param <E>           事件类型
     * @return 返回该行为的定义
     */
    static <E extends IEvent> GameLoopFunction<Boolean> registerEvent(final Class<E> eventClass,
                                                                      final IEventHandler<E> iEventHandler,
                                                                      final EventPriority eventPriority) {
        return gameLoop -> gameLoop.getComponent(IEventBus.class)
                .map(iEventBus -> iEventBus.registerEvent(eventClass, iEventHandler, eventPriority))
                .orElse(false);
    }

    /**
     * 定义一个行为：发送一个事件到{@link IGameLoop}的{@link IEventBus}组件上
     *
     * @param event 要发送的事件
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer的含义参考{@link IEventBus#sendEvent(IEvent)}的返回值
     */
    static GameLoopFunction<Integer> sendEvent(final IEvent event) {
        return gameLoop -> gameLoop.getComponent(IEventBus.class)
                .map(eventBus -> eventBus.sendEvent(event))
                .orElse(-1);
    }
}
