package org.gamedo.gameloop;

import org.gamedo.gameloop.interfaces.GameLoopSelector;
import org.gamedo.gameloop.interfaces.IGameLoop;
import org.gamedo.gameloop.interfaces.IGameLoopGroup;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GameLoopGroup implements IGameLoopGroup
{
    private final AtomicInteger idx = new AtomicInteger(0);
    private final GameLoop[] gameLoops;

    public GameLoopGroup(GameLoop... gameLoops) {
        this.gameLoops = gameLoops.clone();
    }

    @Override
    public void shutdown() {
        Arrays.stream(gameLoops).forEach(gameLoop -> gameLoop.shutdown());
    }

    @Override
    public List<Runnable> shutdownNow() {

        return Arrays.stream(gameLoops)
                .map(gameLoop -> gameLoop.shutdownNow())
                .flatMap(runnables -> runnables.stream())
                .collect(Collectors.toList());
    }

    @Override
    public boolean isShutdown() {
        return Arrays.stream(gameLoops).allMatch(GameLoop::isShutdown);
    }

    @Override
    public boolean isTerminated() {
        return Arrays.stream(gameLoops).allMatch(GameLoop::isTerminated);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        //TODO:
        return true;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return null;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return null;
    }

    @Override
    public Future<?> submit(Runnable task) {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    @Override
    public void execute(Runnable command) {

    }

    @Override
    public GameLoop next() {
        return gameLoops[Math.abs(idx.getAndIncrement() % gameLoops.length)];
    }

    @Override
    public <T> List<IGameLoop> select(GameLoopSelector<T> selector) {

        return null;
    }
}
