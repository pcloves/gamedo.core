package org.gamedo.gameloop.interfaces;

import java.util.function.Function;

@FunctionalInterface
public interface GameLoopFunction<R> extends Function<IGameLoop, R> {
    @Override
    R apply(IGameLoop gameLoop);

    GameLoopFunction<Boolean> TRUE = gameLoop -> true;
}
