package org.gamedo.ecs.interfaces;

import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Optional;

public interface IComponent extends ITickable, IInterfaceQueryable {

    IEntity getOwner();

    /**
     * 返回锁归属的{@link IGameLoop}
     * @return 所归属的{@link IGameLoop}
     */
    default Optional<IGameLoop> getBelongedGameLoop() {
        return getOwner().getBelongedGameLoop();
    }
}
