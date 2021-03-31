package org.gamedo.ecs.interfaces;

import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.function.Function;

@FunctionalInterface
public interface IGameLoopFunction<R> extends Function<IGameLoop, R>
{
    @Override
    R apply(IGameLoop gameLoop);
}
