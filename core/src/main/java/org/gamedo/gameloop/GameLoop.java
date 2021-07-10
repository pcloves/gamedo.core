package org.gamedo.gameloop;

import lombok.experimental.Delegate;
import lombok.extern.log4j.Log4j2;
import org.gamedo.concurrent.NamedThreadFactory;
import org.gamedo.ecs.Entity;
import org.gamedo.gameloop.interfaces.GameLoopFunction;
import org.gamedo.gameloop.interfaces.IGameLoop;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Log4j2
public class GameLoop extends Entity implements IGameLoop {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<IGameLoop> gameLoopOptional = Optional.of(this);
    @Delegate(types = ScheduledExecutorService.class)
    private final ScheduledExecutorService scheduledExecutorService;

    private volatile Thread thread;

    public GameLoop(final String id) {

        super(id);

        scheduledExecutorService = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(id)) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);

                thread = Thread.currentThread();
                IGameLoop.GAME_LOOP_THREAD_LOCAL.set(gameLoopOptional);
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);

                IGameLoop.GAME_LOOP_THREAD_LOCAL.set(Optional.empty());
                thread = null;
            }
        };
    }

    @Override
    public boolean inGameLoop() {
        return thread == Thread.currentThread();
    }

    @Override
    public <R> CompletableFuture<R> submit(GameLoopFunction<R> function) {

        if (inGameLoop()) {
            return CompletableFuture.completedFuture(function.apply(this));
        } else {
            return CompletableFuture.supplyAsync(() -> function.apply(this), this);
        }
    }
}
