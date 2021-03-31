package org.gamedo.gameloop;

import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IGameLoopFunction;
import org.gamedo.event.interfaces.EventPriority;
import org.gamedo.event.interfaces.IEvent;
import org.gamedo.event.interfaces.IEventBus;
import org.gamedo.event.interfaces.IEventHandler;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Objects;
import java.util.Optional;

public final class GameLoops {

    public static final ThreadLocal<Optional<IGameLoop>> GAME_LOOP_THREAD_LOCAL = ThreadLocal.withInitial(Optional::empty);

    private GameLoops() {

    }

    public static Optional<IGameLoop> currentGameLoop() {
        return GAME_LOOP_THREAD_LOCAL.get();
    }

    /**
     * 定义一个行为：注册一个{@link IEventHandler 事件处理器}到{@link IGameLoop}的{@link IEventBus}组件中
     *
     * @param eventClass    要注册的事件类型
     * @param iEventHandler 事件处理器
     * @param eventPriority 该事件处理器的优先级
     * @param <E>           事件类型
     * @param <R>           事件处理器的返回值类型
     * @return 返回该行为的定义
     */
    public static <E extends IEvent, R> IGameLoopFunction<Boolean> registerEvent(final Class<E> eventClass,
                                                                                 final IEventHandler<E> iEventHandler,
                                                                                 final EventPriority eventPriority) {
        return gameLoop -> gameLoop.getComponent(IEventBus.class)
                .filter(Objects::nonNull)
                .map(iEventBus -> iEventBus.registerEvent(eventClass, iEventHandler, eventPriority))
                .orElse(false);
    }

    /**
     * 定义一个行为：发送一个事件到{@link IGameLoop}的{@link IEventBus}
     *
     * @param event 要发送的事件
     * @param <T>   事件被事件处理器处理后的返回值类型
     * @return 返回该行为的定义
     */
    public static <T> IGameLoopFunction<Integer> sendEvent(final IEvent event) {
        return gameLoop -> gameLoop.getComponent(IEventBus.class)
                .filter(Objects::nonNull)
                .map(eventBus -> eventBus.sendEvent(event))
                .orElse(0);
    }

    public static IGameLoopFunction<String> getEventBusOwnerId() {
        return gameLoop -> gameLoop.getComponent(IEventBus.class)
                .filter(Objects::nonNull)
                .map(iEventBus -> iEventBus.getOwnerId())
                .orElse("");
    }

    public static IGameLoopFunction<Boolean> registerEntity(final IEntity entity) {
        return gameLoop -> gameLoop.registerEntity(entity);
    }

}
