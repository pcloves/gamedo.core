package org.gamedo.ecs.interfaces;

import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Optional;

public interface IComponent<T extends IEntity> extends ITickable, IInterfaceQueryable {

    T getOwner();

    /**
     * 返回锁归属的{@link IGameLoop}
     *
     * @return 所归属的{@link IGameLoop}
     */
    default Optional<IGameLoop> getOwnerBelongedGameLoop() {
        return getOwner().getBelongedGameLoop();
    }
}
