package org.gamedo.eventbus.interfaces;

import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;

public interface IGameLoopEventBusFunction {

    /**
     * 定义一个行为：发送一个事件到{@link IGameLoop}的{@link IGameLoopEventBus}组件上
     *
     * @param event 要发送的事件
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer的含义参考{@link IGameLoopEventBus#post(IEvent)}的返回值
     */
    static GameLoopFunction<Integer> post(final IEvent event) {
        return gameLoop -> gameLoop.getComponent(IGameLoopEventBus.class)
                .map(eventBus -> eventBus.post(event))
                .orElse(-1);
    }
}
