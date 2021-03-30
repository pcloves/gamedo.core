package org.gamedo.gameloop;

import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IGameLoopAction;
import org.gamedo.event.interfaces.IEvent;
import org.gamedo.event.interfaces.IEventBus;

import java.util.Objects;
import java.util.Optional;

public final class GameLoops {

    public static final ThreadLocal<Optional<IGameLoop>> GAME_LOOP_THREAD_LOCAL = ThreadLocal.withInitial(Optional::empty);

    private GameLoops() {

    }

    public static Optional<IGameLoop> currentGameLoop() {
        return GAME_LOOP_THREAD_LOCAL.get();
    }

    public static <T> IGameLoopAction<Optional<T>> sendEvent(final IEvent event) {
        return gameLoop -> gameLoop.getComponent(IEventBus.class)
                                   .filter(Objects::nonNull)
                                   .flatMap(eventBus -> eventBus.sendEvent(event));
    }

    public static IGameLoopAction<Boolean> registerEntity(final IEntity entity) {
        return gameLoop -> gameLoop.registerEntity(entity);
    }

}
