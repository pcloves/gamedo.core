package org.gamedo.util.function;

import org.gamedo.annotation.Subscribe;
import org.gamedo.gameloop.components.eventbus.interfaces.IEvent;
import org.gamedo.gameloop.components.eventbus.interfaces.IGameLoopEventBus;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.function.Supplier;

public interface IGameLoopEventBusFunction {

    /**
     * 定义一个行为：将object内的所有{@link Subscribe}方法注册到{@link IGameLoopEventBus}上
     *
     * @param object 要执行的object
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer的含义参考{@link IGameLoopEventBus#register(Object)} 的返回值
     */
    static GameLoopFunction<Integer> register(final Object object) {
        return gameLoop -> gameLoop.getComponent(IGameLoopEventBus.class)
                .map(eventBus -> eventBus.register(object))
                .orElse(0);
    }

    /**
     * 定义一个行为：将object内的所有{@link Subscribe}方法从{@link IGameLoopEventBus}取消注册
     *
     * @param object 要执行的object
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer的含义参考{@link IGameLoopEventBus#unregister(Object)} 的返回值
     */
    static GameLoopFunction<Integer> unregister(final Object object) {
        return gameLoop -> gameLoop.getComponent(IGameLoopEventBus.class)
                .map(eventBus -> eventBus.unregister(object))
                .orElse(0);
    }

    /**
     * 定义一个行为：发送一个事件到{@link IGameLoop}的{@link IGameLoopEventBus}组件上
     *
     * @param event 要发送的事件
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer的含义参考{@link IGameLoopEventBus#post(IEvent)}的返回值
     */
    @Deprecated
    static GameLoopFunction<Integer> post(final IEvent event) {
        return gameLoop -> gameLoop.getComponent(IGameLoopEventBus.class)
                .map(eventBus -> eventBus.post(event))
                .orElse(-1);
    }

    /**
     * 定义一个行为：发送一个事件到{@link IGameLoop}的{@link IGameLoopEventBus}组件上
     *
     * @param eventClazz 事件的Class
     * @param eventSupplier 事件提供者，要求提供的事件必须要和eventClazz一致
     * @param <T> 事件类型
     * @return 返回该行为的定义，其中GameLoopFunction中的Integer的含义参考{@link IGameLoopEventBus#post(Class, Supplier)}的返回值
     */
    static <T extends IEvent> GameLoopFunction<Integer> post(Class<T> eventClazz, final Supplier<T> eventSupplier) {
        return gameLoop -> gameLoop.getComponent(IGameLoopEventBus.class)
                .map(eventBus -> eventBus.post(eventClazz, eventSupplier))
                .orElse(-1);
    }
}
