package org.gamedo.ecs.interfaces;

import org.gamedo.gameloop.IGameLoop;

@FunctionalInterface
public interface IGameLoopAction<R>
{
    R apply(IGameLoop gameLoop);
}
