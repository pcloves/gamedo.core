package org.gamedo.gameloop;

import org.gamedo.ecs.interfaces.IApplication;
import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopApplicationGroup;

import java.util.Optional;

public class GameLoopApplicationGroup extends GameLoopGroup implements IGameLoopApplicationGroup {
    private final IApplication application;

    public GameLoopApplicationGroup(IApplication application, String id, IGameLoop... gameLoops) {
        super(id, gameLoops);

        this.application = application;
    }

    public GameLoopApplicationGroup(IApplication application, String id, int gameLoopCount) {
        super(id, gameLoopCount);

        this.application = application;
    }

    private GameLoopApplicationGroup(IApplication application, String id) {
        super(id);

        this.application = application;
    }

    @Override
    public IEntity getOwner() {
        return application;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getInterface(Class<T> clazz) {
        return Optional.ofNullable(clazz.isInstance(this) ? (T) this : null);
    }

    @Override
    public Optional<IGameLoop> getBelongedGameLoop() {
        return Optional.empty();
    }

    @Override
    public IApplication application() {
        return application;
    }
}
