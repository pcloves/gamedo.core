package org.gamedo.gameloop.interfaces;

import org.gamedo.ecs.interfaces.IEntity;
import org.gamedo.ecs.interfaces.IGameLoopFunction;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * gamedo.core的线程模型借鉴了netty 4的线程模型设计，每一个IGameLoop代表一个线程，对应于Netty 4的EventLoop，
 */
public interface IGameLoop extends ExecutorService, IEntity
{
    /**
     * @return true 表示当前处于本GameLoop线程中
     */
    boolean inGameLoop();

    boolean run(long initialDelay, long period, TimeUnit periodTimeUnit);

    boolean registerEntity(IEntity entity);

    <T> CompletableFuture<T> submit(IGameLoopFunction<T> function);
}
