package org.gamedo.eventbus.interfaces;

import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;

public interface IEventBusFunction {

    /**
     * 定义一个行为：发送一个事件到{@link IGameLoop}的{@link IEventBus}组件上
     *
     * @param event 要发送的事件
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer的含义参考{@link IEventBus#post(IEvent)}的返回值
     */
    static GameLoopFunction<Integer> post(final IEvent event) {
        return gameLoop -> gameLoop.getComponent(IEventBus.class)
                .map(eventBus -> eventBus.post(event))
                .orElse(-1);
    }
}
