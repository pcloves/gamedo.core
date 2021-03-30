package org.gamedo.gameloop;

import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IGameLoopAction;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public interface IGameLoop extends Executor, IEntity
{
    boolean inGameLoop();

    boolean run(long initialDelay, long period, TimeUnit periodTimeUnit);

    boolean shutdown(long timeout, TimeUnit timeUnit) throws InterruptedException;

    boolean registerEntity(IEntity entity);

    <T> CompletableFuture<T> executeAsync(IGameLoopAction<T> action);
}
