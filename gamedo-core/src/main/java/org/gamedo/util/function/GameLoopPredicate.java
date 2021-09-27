package org.gamedo.util.function;

import org.gamedo.gameloop.interfaces.IGameLoop;

@SuppressWarnings("unused")
@FunctionalInterface
public interface GameLoopPredicate extends EntityPredicate<IGameLoop> {
    @Override
    Boolean apply(IGameLoop gameLoop);
}
