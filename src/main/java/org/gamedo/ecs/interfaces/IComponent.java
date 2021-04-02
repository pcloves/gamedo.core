package org.gamedo.ecs.interfaces;

import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Optional;

public interface IComponent extends ITickable {
    /**
     * 该组件所归属的实体Id
     */
    String getOwnerId();

    /**
     * 该组件所归属的{@link IGameLoop}
     */
    default Optional<IGameLoop> gameLoop() {
        return IGameLoop.currentGameLoop();
    }
}
