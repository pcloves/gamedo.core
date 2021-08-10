package org.gamedo.util.function;

import org.gamedo.gameloop.interfaces.IGameLoop;

@FunctionalInterface
public interface GameLoopFunction<R> extends EntityFunction<IGameLoop, R> {
    @Override
    R apply(IGameLoop gameLoop);

}
