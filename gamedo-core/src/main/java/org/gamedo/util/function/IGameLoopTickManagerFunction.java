package org.gamedo.util.function;

import org.gamedo.annotation.Tick;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.components.tickManager.interfaces.IGameLoopTickManager;
import org.gamedo.gameloop.interfaces.IGameLoop;

public interface IGameLoopTickManagerFunction {

    /**
     * 定义一个行为：向{@link IGameLoop}的{@link IGameLoopTickManager}组件注册心跳逻辑，object
     * 的所有包含{@link Tick}注解的无参函数都会被注册
     *
     * @param object 要注册调度的实例
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer代表被成功注册的方法的数量
     */
    static GameLoopFunction<Integer> register(Object object) {
        return gameLoop -> gameLoop.getComponent(IGameLoopTickManager.class)
                .map(iGameLoopTickManager -> iGameLoopTickManager.register(object))
                .orElse(0);
    }

    /**
     * 定义一个行为：将object内的所有{@link Tick}方法从{@link IGameLoopEventBus}取消注册
     * @param object 要执行的object
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer的含义参考{@link IGameLoopTickManager#unregister(Object)} 的返回值
     */
    static GameLoopFunction<Integer> unregister(Object object) {
        return gameLoop -> gameLoop.getComponent(IGameLoopTickManager.class)
                .map(iGameLoopTickManager -> iGameLoopTickManager.unregister(object))
                .orElse(0);
    }
}
